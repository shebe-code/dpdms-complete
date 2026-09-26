# API test plan

| Test | Expected result |
|---|---|
| Recorder creates incident in own hazard | 201 Created, status PENDING |
| Recorder targets another hazard service | 403 Forbidden |
| Recorder changes ward header | Backend ignores spoofed ward and uses JWT ward |
| Recorder updates approved incident | 400 Bad request / workflow rejection |
| Supervisor approves own hazard | status APPROVED + audit entry |
| Supervisor calls another hazard | 403 Forbidden |
| Reject without reason | 400 Bad request |
| National reads approved incidents | 200 OK |
| National writes | 403 Forbidden |
| Pending incident in approved endpoint | never returned |
| Dashboard user | only approved data returned from accessible services |
| Report with PENDING status request | 403 Forbidden |
| Alert delivery without credentials | asynchronous SIMULATED log |
