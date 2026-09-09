"""Run explicit ReNova Azure deployment stages with locally generated secrets.

Invoke using .env.azure-tools/Scripts/python.exe; requires Azure CLI login.
Credentials remain in a gitignored local environment file and Azure secrets.
"""

import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess
import sys
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
GROUP = "rg-renova-student"
REGION = "newzealandnorth"
MYSQL = "renova-mysql-yh6060909"
REGISTRY = "renovayh6060909"
STORAGE = "renovafilesyh6060909"
ENVIRONMENT = "renova-environment"
APP = "renova"
SECRET_FILE = ROOT / ".env.azure-secrets"


def load_secrets():
    if not SECRET_FILE.exists():
        values = {key: secrets.token_urlsafe(48) for key in (
            "RENOVA_DB_ADMIN_PASSWORD", "RENOVA_DB_PASSWORD", "JWT_SECRET")}
        SECRET_FILE.write_text("".join(f"{k}={v}\n" for k, v in values.items()))
    for line in SECRET_FILE.read_text().splitlines():
        key, value = line.split("=", 1)
        os.environ[key] = value


def az(*args):
    print("Azure: " + " ".join(args[:3]), flush=True)
    result = subprocess.run(
        [sys.executable, "-m", "azure.cli", *args, "--only-show-errors", "-o", "json"],
        capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=ROOT)
    if result.returncode:
        message = result.stderr
        for key in ("RENOVA_DB_ADMIN_PASSWORD", "RENOVA_DB_PASSWORD", "JWT_SECRET"):
            message = message.replace(os.environ.get(key, "<unset>"), "[REDACTED]")
        raise RuntimeError(message)
    return json.loads(result.stdout) if result.stdout.strip() else None


def provision():
    az("mysql", "flexible-server", "create", "-g", GROUP, "-n", MYSQL,
       "-l", REGION, "--admin-user", "renovaadmin",
       "--admin-password", os.environ["RENOVA_DB_ADMIN_PASSWORD"],
       "--sku-name", "Standard_B1ms", "--tier", "Burstable", "--version", "8.4",
       "--storage-size", "32", "--storage-auto-grow", "Disabled",
       "--backup-retention", "7", "--geo-redundant-backup", "Disabled",
       "--high-availability", "Disabled", "--public-access", "None", "--yes")
    az("acr", "create", "-g", GROUP, "-n", REGISTRY, "-l", REGION, "--sku", "Standard")
    az("storage", "account", "create", "-g", GROUP, "-n", STORAGE, "-l", REGION,
       "--sku", "Standard_LRS", "--kind", "StorageV2", "--min-tls-version", "TLS1_2",
       "--allow-blob-public-access", "false", "--https-only", "true")
    az("containerapp", "env", "create", "-g", GROUP, "-n", ENVIRONMENT,
       "-l", REGION, "--enable-workload-profiles", "false",
       "--logs-destination", "none", "--no-wait")
    print("Provisioning submitted. Verify resource readiness before deploying.", flush=True)


def configure_database():
    import certifi
    import pymysql

    with urlopen("https://api.ipify.org", timeout=20) as response:
        import ipaddress
        local_ip = str(ipaddress.IPv4Address(response.read().decode().strip()))
    az("mysql", "flexible-server", "firewall-rule", "create", "-g", GROUP,
       "-n", MYSQL, "--rule-name", "deployment-workstation",
       "--start-ip-address", local_ip, "--end-ip-address", local_ip)
    az("mysql", "flexible-server", "db", "create", "-g", GROUP,
       "--server-name", MYSQL, "--database-name", "renova")
    host = az("mysql", "flexible-server", "show", "-g", GROUP, "-n", MYSQL)["fullyQualifiedDomainName"]
    connection = pymysql.connect(host=host, user="renovaadmin",
        password=os.environ["RENOVA_DB_ADMIN_PASSWORD"], database="renova",
        ssl=ssl.create_default_context(cafile=certifi.where()), connect_timeout=30)
    try:
        with connection.cursor() as cursor:
            cursor.execute("CREATE USER IF NOT EXISTS 'renova_app'@'%%' IDENTIFIED BY %s",
                           (os.environ["RENOVA_DB_PASSWORD"],))
            cursor.execute("GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON renova.* TO 'renova_app'@'%'")
        connection.commit()
    finally:
        connection.close()
    print("Application database and restricted account ready over verified TLS.", flush=True)


def publish_image():
    credentials = az("acr", "login", "-n", REGISTRY, "--expose-token")
    subprocess.run(["docker", "login", credentials["loginServer"], "-u",
        "00000000-0000-0000-0000-000000000000", "--password-stdin"],
        input=credentials["accessToken"], text=True, check=True)
    for name in ("renova-backend", "renova-frontend"):
        tag = "azure-20260909-login" if name == "renova-frontend" else "azure-20260909"
        image = credentials["loginServer"] + f"/{name}:{tag}"
        subprocess.run(["docker", "tag", f"{name}:{tag}", image], check=True)
        subprocess.run(["docker", "push", image], check=True)


def configure_storage():
    az("storage", "share-rm", "create", "-g", GROUP, "--storage-account", STORAGE,
       "--name", "uploads", "--quota", "5", "--access-tier", "TransactionOptimized")
    key = az("storage", "account", "keys", "list", "-g", GROUP, "-n", STORAGE)[0]["value"]
    az("containerapp", "env", "storage", "set", "-g", GROUP, "-n", ENVIRONMENT,
       "--storage-name", "uploads", "--azure-file-account-name", STORAGE,
       "--azure-file-account-key", key, "--azure-file-share-name", "uploads",
       "--access-mode", "ReadWrite")


def application_config(environment, identity, registry, website, database):
    values = {
        "SPRING_PROFILES_ACTIVE": "prod", "DB_USERNAME": "renova_app",
        "SPRING_DATASOURCE_URL": f"jdbc:mysql://{database}/renova?sslMode=VERIFY_IDENTITY&serverTimezone=UTC",
        "CORS_ALLOWED_ORIGINS": f"https://{website}",
        "SERVER_FORWARD_HEADERS_STRATEGY": "framework",
        "RENOVA_UPLOAD_DIR": "/var/lib/renova/uploads",
        "JAVA_TOOL_OPTIONS": "-XX:MaxRAMPercentage=70.0 -XX:ActiveProcessorCount=2",
        "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE": "3",
        "SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE": "0",
    }
    env = [{"name": key, "value": value} for key, value in values.items()]
    env += [{"name": "DB_PASSWORD", "secretRef": "db-password"}, {"name": "JWT_SECRET", "secretRef": "jwt-secret"}]
    container = {"name": "api", "image": registry + "/renova-backend:azure-20260909",
        "resources": {"cpu": 0.5, "memory": "1Gi"}, "env": env,
        "volumeMounts": [{"volumeName": "uploads", "mountPath": "/var/lib/renova/uploads"}],
        "probes": [{"type": kind, "httpGet": {"path": "/actuator/health", "port": 8080},
                    "initialDelaySeconds": 10, "periodSeconds": 10, "timeoutSeconds": 5,
                    "failureThreshold": 30 if kind == "Startup" else 3} for kind in ("Startup", "Readiness")]}
    return {"location": REGION, "identity": {"type": "UserAssigned", "userAssignedIdentities": {identity: {}}},
        "properties": {"managedEnvironmentId": environment,
            "configuration": {"activeRevisionsMode": "Single",
                "ingress": {"external": True, "targetPort": 80, "transport": "auto", "allowInsecure": False},
                "registries": [{"server": registry, "identity": identity}],
                "secrets": [{"name": "db-password", "value": os.environ["RENOVA_DB_PASSWORD"]},
                            {"name": "jwt-secret", "value": os.environ["JWT_SECRET"]}]},
            "template": {"containers": [container, {
                "name": "web", "image": registry + "/renova-frontend:azure-20260909-login",
                "resources": {"cpu": 0.25, "memory": "0.5Gi"},
                "probes": [{"type": "Readiness", "httpGet": {"path": "/health", "port": 80},
                            "periodSeconds": 10}]}],
                "volumes": [{"name": "uploads", "storageType": "AzureFile", "storageName": "uploads"}],
                "scale": {"minReplicas": 0, "maxReplicas": 1, "cooldownPeriod": 60,
                    "rules": [{"name": "http", "http": {"metadata": {"concurrentRequests": "10"}}}]}}}}


def deploy_api():
    identity = az("identity", "create", "-g", GROUP, "-n", "renova-image-pull", "-l", REGION)
    registry = az("acr", "show", "-g", GROUP, "-n", REGISTRY)
    az("role", "assignment", "create", "--assignee-object-id", identity["principalId"],
       "--assignee-principal-type", "ServicePrincipal", "--role", "AcrPull", "--scope", registry["id"])
    environment = az("containerapp", "env", "show", "-g", GROUP, "-n", ENVIRONMENT)
    if environment["properties"]["provisioningState"] != "Succeeded":
        raise RuntimeError("Container environment is not ready yet")
    website = APP + "." + environment["properties"]["defaultDomain"]
    database = az("mysql", "flexible-server", "show", "-g", GROUP, "-n", MYSQL)["fullyQualifiedDomainName"]
    config = application_config(environment["id"], identity["id"], registry["loginServer"], website, database)
    config_file = ROOT / ".env.azure-containerapp.json"
    config_file.write_text(json.dumps(config), encoding="utf-8")
    az("containerapp", "create", "-g", GROUP, "-n", APP, "--yaml", str(config_file), "--no-wait")


def allow_api_database_access():
    app = az("containerapp", "show", "-g", GROUP, "-n", APP)
    addresses = app["properties"].get("outboundIpAddresses", [])
    if not addresses:
        raise RuntimeError("Container app has not published its outbound IP addresses yet")
    server_id = az("mysql", "flexible-server", "show", "-g", GROUP, "-n", MYSQL)["id"]
    for index, address in enumerate(addresses):
        body = {"properties": {"startIpAddress": address, "endIpAddress": address}}
        az("rest", "--method", "put", "--url",
           f"https://management.azure.com{server_id}/firewallRules/renova-api-{index}?api-version=2023-12-30",
           "--body", json.dumps(body))
    print("Database allows the application's listed outbound IPs only.", flush=True)


def budget():
    subscription = az("account", "show")["id"]
    email = os.environ.get("RENOVA_BUDGET_EMAIL")
    if not email:
        raise RuntimeError("Set RENOVA_BUDGET_EMAIL to the budget notification recipient")
    notification = {"enabled": True, "operator": "GreaterThanOrEqualTo",
        "threshold": 80, "contactEmails": [email], "thresholdType": "Actual"}
    body = {"properties": {"category": "Cost", "amount": 5, "timeGrain": "Monthly",
        "timePeriod": {"startDate": "2026-09-01T00:00:00Z", "endDate": "2027-09-09T00:00:00Z"},
        "filter": {"dimensions": {"name": "ResourceGroupName", "operator": "In", "values": [GROUP]}},
        "notifications": {"Actual80": notification, "Actual100": {**notification, "threshold": 100}}}}
    body_file = ROOT / ".env.azure-budget.json"
    body_file.write_text(json.dumps(body), encoding="utf-8")
    resource = f"https://management.azure.com/subscriptions/{subscription}/providers/Microsoft.Consumption/budgets/renova-monthly?api-version=2024-08-01"
    az("rest", "--method", "put", "--url", resource, "--body", "@" + str(body_file))
    print("Monthly budget alert configured; alerts are not a spending cap.", flush=True)


if __name__ == "__main__":
    os.environ["AZURE_CONFIG_DIR"] = str(ROOT / ".env.azure-config")
    stages = {"provision": provision, "database": configure_database,
              "image": publish_image, "storage": configure_storage,
              "api": deploy_api, "firewall": allow_api_database_access,
              "budget": budget}
    if len(sys.argv) != 2 or sys.argv[1] not in stages:
        raise SystemExit("Usage: azure-deploy.py " + "|".join(stages))
    load_secrets()
    stages[sys.argv[1]]()
