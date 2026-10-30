# Job Service

Job Service is a Spring Boot 3.5.6 application targeting Java 21. It listens on
port `8083`. For a local process, its base URL is `http://localhost:8083`; a
different service or container must use the reachable host and port configured
for that environment. This repository does not currently configure Docker
Compose service networking or service discovery.

## Database design

The initial schema consists of `jobs` and `job_skills`. `jobs.id` is an
auto-incrementing `BIGINT`; `job_skills.job_id` references it and
`(job_id, skill)` is the skills table primary key. Job columns are title,
description, requirements, location, employment type, experience years, status,
and creation/update timestamps.

The status values are `OPEN` and `CLOSED`. New postings default to `OPEN`;
closing a posting sets it to `CLOSED`. Search returns only `OPEN` postings, and
`acceptingApplications` is true exactly when status is `OPEN`. These are the
initial Job Service decisions because no shared schema or status contract was
provided. The SQL initializer only creates missing tables; Hibernate validates
the mappings and does not update or drop tables.

Set `JOB_DB_URL`, `JOB_DB_USERNAME`, and `JOB_DB_PASSWORD` in the runtime
environment. For example, a local development URL may be
`jdbc:mysql://localhost:3306/revhire_job`; database name, host, user, and
password must match the environment's provisioned MySQL instance. Credentials
are not stored in this repository.

## HTTP API

All paths start with `/api/jobs`.

| Method and path | Behavior |
|---|---|
| `POST /api/jobs` | Create an `OPEN` job; responds `201 Created` and a `Location` header. |
| `GET /api/jobs/{id}` | Return job details, status, `exists`, and `acceptingApplications`; missing IDs return `404`. |
| `GET /api/jobs` | Search and paginate `OPEN` jobs. |
| `PATCH /api/jobs/{id}` | Partially update supplied fields; omitted fields remain unchanged. |
| `PATCH /api/jobs/{id}/close` | Change status to `CLOSED`; repeated requests are safe. |

Search query parameters can be combined: `keywords` (title, description, or
requirements), `title`, `location`, `skill`, `employmentType`,
`minExperienceYears`, and `maxExperienceYears`. Pagination uses `page` (zero
based, default `0`) and `size` (default `20`, maximum `100`). Sorting is
restricted to `CREATED_AT`, `TITLE`, or `LOCATION`; `direction` is `ASC` or
`DESC`.

Create example:

```json
{
  "title": "Java Engineer",
  "description": "Build backend services",
  "requirements": "Java and Spring experience",
  "location": "Remote",
  "employmentType": "FULL_TIME",
  "experienceYears": 2,
  "skills": ["Java", "Spring Boot"]
}
```

Job detail example (open):

```json
{
  "jobId": 123,
  "exists": true,
  "status": "OPEN",
  "acceptingApplications": true,
  "title": "Java Engineer",
  "description": "Build backend services",
  "requirements": "Java and Spring experience",
  "location": "Remote",
  "employmentType": "FULL_TIME",
  "experienceYears": 2,
  "skills": ["Java", "Spring Boot"],
  "createdAt": "2026-10-28T10:00:00",
  "updatedAt": "2026-10-28T10:00:00"
}
```

Job detail responses also include the configured details for closed jobs, with
`status: "CLOSED"` and `acceptingApplications: false`. Error responses use
`timestamp`, `status`, `error`, `message`, `path`, and optional
`fieldErrors`; internal exception details are not returned.

## Security and integration boundary

Spring Security remains enabled. No shared identity provider, role mapping, or
service-to-service credential contract is configured in this repository, so
the Spring Boot default authentication currently protects all endpoints.
The default CSRF protection also applies to state-changing requests; clients
must provide an authenticated session and CSRF token for those requests.
Recruiter-specific authorization is not implemented. Before deployment,
Application Service must use a reachable Job Service base URL (port `8083`)
and the authentication credentials/mechanism agreed for the target environment.
The detail contract it needs is `GET /api/jobs/{id}`: `200` with `jobId`,
`exists`, `status`, and `acceptingApplications`, or `404` with the standard
error body.

No delete endpoint is provided because no deletion policy was established.
