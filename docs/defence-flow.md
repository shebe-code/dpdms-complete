# Suggested defence flow

1. Start Eureka and show service registration.
2. Start gateway and show the single client entry point.
3. Log in as a flood recorder; capture a flood and show PENDING status.
4. Log in as a flood supervisor; approve it and show the audit trail.
5. Log in as the national user and show approved flood/drought/fire/zoonotic/mining records while write buttons are absent/read-only.
6. Try using a flood supervisor token against `/api/v1/droughts` and show 403.
7. Open the dashboard and demonstrate counts, recent records, trend and map markers.
8. Generate PDF, DOCX, XLSX and CSV reports.
9. Explain that alert channels are asynchronous and can run in simulated mode without secrets.
10. Explain the OOP mapping in `docs/oop-mapping.md`.
