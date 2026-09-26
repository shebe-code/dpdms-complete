# Architecture and security notes

## Request path

1. React sends HTTPS/HTTP requests to the gateway.
2. Gateway validates the JWT signature and expiry.
3. Gateway removes client-provided identity headers.
4. Gateway injects verified role/hazard/ward/district/province headers.
5. Gateway adds an internal service secret.
6. The destination hazard service validates the internal secret and independently enforces scope.
7. Approved records are aggregated by dashboard/report services.

## Why the security rule is duplicated

The lecturer requires hazard scoping to be enforced by the backend service itself. The gateway is therefore not the only security boundary. A flood supervisor calling the drought service is rejected by the drought service even if the request bypasses the frontend menu.

## Database separation

Each stateful application has its own MySQL database. This avoids a shared persistence layer between bounded contexts and keeps the five hazards separately deployable.
