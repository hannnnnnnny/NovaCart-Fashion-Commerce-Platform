# ReNova on Azure

Live site: https://renova.ambitiousground-6d474fa7.newzealandnorth.azurecontainerapps.io/

Deployed on 9 September 2026 to the Azure for Students subscription. The frontend is Vue served by Nginx, proxying `/api` and `/uploads` to the Java container in the same Container App. Payments and shipping remain simulated; authentication and stored data are real. Production does not seed demo accounts. Users should register their own accounts.

## Resources

All resources are in `rg-renova-student`, New Zealand North.

| Resource | Name | Configuration |
|---|---|---|
| Container App | `renova` | Consumption, 0–1 replicas; API 0.5 vCPU / 1 GiB, frontend 0.25 vCPU / 0.5 GiB |
| Container environment | `renova-environment` | Consumption-only; no billable Log Analytics workspace |
| MySQL Flexible Server | `renova-mysql-yh6060909` | MySQL 8.4, B1ms, 32 GB, 7-day local backups, storage auto-grow disabled |
| Private container registry | `renovayh6060909` | Standard, student free allowance verified in portal |
| Storage account | `renovafilesyh6060909` | Standard LRS, HTTPS only, TLS 1.2 minimum |
| Azure Files share | `uploads` | Transaction Optimized, 5 GB quota, mounted at `/var/lib/renova/uploads` |
| Managed identity | `renova-image-pull` | AcrPull scoped to the registry |
| Budget | `renova-monthly` | NZD 5/month for this resource group; 80% and 100% email alerts |

The budget uses the subscription billing currency, NZD. It is an alert, not a hard cap. Student MySQL allowances are 750 B1ms hours/month plus 32 GB data and 32 GB backup; Standard registry allowance is 31 days/month. These benefits expire on 9 September 2027. Container Apps has a monthly free compute grant. Usage above allowances and storage transactions can consume the USD 100 student credit. The low-traffic budget target is not a price guarantee. The frontend now sleeps with the API, so first access after inactivity can take longer.

## Security and persistence

- HTTPS-only ingress, with Nginx forwarding to the API on localhost inside the app.
- JDBC uses `sslMode=VERIFY_IDENTITY`. MySQL's `renova_app` user is limited to the application schema; the server administrator is not used by the application.
- Database firewall rules allow the Container App's published outbound IPs. Re-run the firewall stage if those addresses change. Deletion of the temporary `deployment-workstation` rule has been submitted; Azure's asynchronous deletion was still in progress at handoff. Confirm its absence before treating cleanup as complete.
- The production login page hides demo account shortcuts. They are available only when `VITE_RENOVA_DEMO=true`; Azure users must register their own accounts.
- JWT and database passwords are generated locally and injected as Azure Container App secrets. `.env.azure-secrets`, `.env.azure-config`, and `.env.azure-containerapp.json` are gitignored and must never be committed or shared.
- Images are mounted from Azure Files; database records and images survived an actual Container App revision restart.
- The application runs with the `prod` profile. Browser demo data is disabled during Docker builds.

## Deploy again

The local Azure CLI is installed in `.env.azure-tools`. Its login state uses `.env.azure-config`.

```powershell
$env:AZURE_CONFIG_DIR = Join-Path $PWD '.env.azure-config'
& ./.env.azure-tools/Scripts/az.bat account show

docker build -t renova-backend:azure-20260909 ./backend
docker build -t renova-frontend:azure-20260909-login --build-arg VITE_API_BASE_URL=/api --build-arg NGINX_CONFIG=nginx.azure.conf ./frontend
& ./.env.azure-tools/Scripts/python.exe scripts/azure-deploy.py image
& ./.env.azure-tools/Scripts/python.exe scripts/azure-deploy.py api
```

For subsequent code releases, choose fresh image tags and update the script's image references before deploying so revisions are reproducible. Deployment stages are explicit: `provision`, `database`, `image`, `storage`, `api`, `firewall`, and `budget`. Do not rerun initial provisioning blindly against an existing environment. The database stage temporarily adds the workstation IP; remove that rule when initialization is complete.

Azure Static Web Apps returned repeated service-side HTTP 500 errors in the only permitted supported region, East Asia; no static site was created. ACR Tasks is disabled for this student subscription, so images were built locally and pushed to ACR.

## Validation

- Backend: 47 tests passed.
- Frontend: 36 tests passed; production and Docker builds passed.
- Live HTTPS, SPA fallback, anonymous access rejection, signup, login, profile, upload/download, invalid-image rejection, and listing creation passed.
- Login, listing and uploaded image survived a revision restart. The temporary public listing was removed afterwards.
- Browser inspection at 375px found no horizontal overflow; browser error log was empty.

Docker Desktop's startup failure was repaired by renaming stale socket-only runtime directories under Local AppData. Backups have `.stale-renova-20260909` suffixes; Docker images, containers and volumes were not reset.
