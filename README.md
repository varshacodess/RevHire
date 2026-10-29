# RevHire

Hiring & Recruitment Platform.

Microservices:
- authentication-service
- profile-service
- job-service
- application-service
- discovery-server
- api-gateway

Frontend:
- angular-frontend

## Continuous integration

GitHub Actions runs the Maven `verify` lifecycle independently for all six
microservices on every push and pull request. Each job uses Java 21 and caches
Maven dependencies. You can also start the workflow manually from the Actions
tab using **CI → Run workflow**.
