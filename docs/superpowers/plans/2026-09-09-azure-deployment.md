# Azure deployment plan

> For agentic workers: execute this operational plan in the current session, preserving existing workspace changes. Use executing-plans for the execution workflow.

**Goal:** Publish ReNova with real authentication, persistent MySQL data and persistent uploaded images within the student's low-traffic budget.

**Architecture:** Vue/Nginx and Java share an Azure Container App with zero minimum replicas; MySQL Flexible Server uses the student's free B1ms allowance; Azure Files persists uploads. Static Web Apps repeatedly returned service-side HTTP 500, so the final deployment uses same-origin Nginx instead. Credentials are environment variables backed by Azure secrets.

**Tech Stack:** Vue 3, Java 21, Spring Boot 4, MySQL, Azure CLI.

## Constraints

- Target USD 5/month, contingent on the subscription's free MySQL allowance and low traffic. Budget alerts do not enforce a spending cap.
- Preserve user edits in BrowsePage.vue, OffersPage.vue, OrdersPage.vue and DataState.vue.
- Never commit credentials or local deployment tools. Keep production demo modes disabled.
- Do not upgrade the subscription or substitute a more expensive database without agreement.

## Tasks

- [x] Inspect current Docker, API, database, upload and authentication configuration.
- [x] Run frontend unit tests and production build; repair the mismatched RouterLink closing tag and missing DataState closing tag in OrdersPage.vue.
- [x] Run `backend/mvnw.cmd test`: 47 tests passed. Frontend unit tests: 33 passed; build passed after the template repair.
- [x] Verify an active Azure for Students subscription and expiry of free benefits in the portal.
- [x] Authenticate isolated Azure CLI and check allowed locations, quotas, and MySQL free benefit.
- [x] Configure verified database TLS, schema-scoped credentials, JWT secret, CORS and persistent image mount.
- [x] Configure same-origin Nginx SPA fallback and API/upload proxy; retain tested support for separate API origins.
- [x] Build and publish versioned images, deploy resources, and disable browser demo data.
- [x] Set a NZD 5 monthly budget alert in the subscription's billing currency, with 80% and 100% notifications; verify 0–1 replicas.
- [x] Verify HTTPS, category retrieval, signup/login, authenticated upload, invalid-image rejection, listing creation and persistence after restart; inspect at 375px.
- [x] Record resource names, URL, configuration and commands in `docs/azure-deployment.md`.

## Current status

Live: https://renova.ambitiousground-6d474fa7.newzealandnorth.azurecontainerapps.io/

Final revision `renova--0000001` is successfully provisioned. Backend: 47 passing tests. Frontend: 36 passing tests. Production Docker builds and live persistence checks passed. Temporary test listing was removed. No static website resource was created. The Docker startup issue was resolved by preserving stale socket runtime directories under renamed paths.
