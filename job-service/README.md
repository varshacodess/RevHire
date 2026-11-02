# Job Service

Job Service is a Spring Boot 3.5.6 application targeting Java 21. It listens on
port `8083`. For a local process, its base URL is `http://localhost:8083`; a
different service or container must use the reachable host and port configured
for that environment. This repository does not currently configure Docker
Compose service networking or service discovery.

## Database design

The schema consists of `jobs` and `job_skills`. `jobs.id` is an
auto-incrementing `BIGINT`; `job_skills.job_id` references it and
`(job_id, skill)` is the skills table primary key. Job columns are title,
description, requirements, location, employment type, experience years, status,
creation/update timestamps, and nullable `recruiter_id`. New jobs store the
authenticated recruiter's ID there. Existing jobs are left unassigned; they
are not attributed to an arbitrary recruiter and do not appear in a recruiter's
own-job results.

The status values are `OPEN` and `CLOSED`. New postings default to `OPEN`;
closing a posting sets it to `CLOSED`. Search returns only `OPEN` postings, and
`acceptingApplications` is true exactly when status is `OPEN`. These are the
initial Job Service decisions because no shared schema or status contract was
provided. The SQL initializer only creates missing tables; Hibernate validates
the mappings and does not update or drop tables.

For an existing database, apply
`src/main/resources/db/manual-migrations/add_job_recruiter_id.sql` once before
starting the updated service. It adds a nullable ownership column and index
without changing or deleting existing jobs. The project does not currently
include an automatic migration runner.

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
| `GET /api/jobs/recruiter/me` | Return the authenticated recruiter's jobs as an array of `JobResponse` objects. |
| `PATCH /api/jobs/{id}` | Partially update supplied fields; omitted fields remain unchanged. |
| `PATCH /api/jobs/{id}/close` | Change status to `CLOSED`; repeated requests are safe. |

Recruiter ownership is derived from the validated JWT `sub` claim (the Auth
user ID), never from request data. Only the owner can update or close a job.
Requests for another recruiter's job return `404` to avoid disclosing whether
that job exists. Legacy jobs without an owner are not included in the recruiter
endpoint and cannot be changed through recruiter-only operations.

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

## Security and Application Service integration

Job Service validates the Authentication Service's signed Bearer JWT. Set
`JWT_SECRET` in the Job Service runtime environment to the same Base64-encoded
secret used by Authentication Service; the secret is decoded before building
the HMAC signing key. Do not commit the secret. Tokens must have a valid
signature and expiration and include the Auth user ID in the `sub` claim and a
`role` claim (`CANDIDATE` or `RECRUITER`).

All endpoints require authentication. Candidates and recruiters can search;
only candidates can read individual job details, and only recruiters can
create, list their own jobs, update, or close postings. Recruiter job listing,
creation, updates, and closing use the numeric Auth user ID from the signed
JWT subject.
Missing, invalid, or expired tokens return `401 Unauthorized`; an authenticated
role without permission returns `403 Forbidden`. Application Service should
forward the candidate's existing `Authorization: Bearer <token>` header when
calling Job Service at its reachable base URL on port `8083`.

To check the application eligibility contract in Postman, send
`GET /api/jobs/{id}` with a valid candidate Bearer token:

| Case | Expected status and response |
|---|---|
| Existing `OPEN` job | `200`; `exists: true`, `status: "OPEN"`, `acceptingApplications: true`. |
| Existing `CLOSED` job | `200`; `exists: true`, `status: "CLOSED"`, `acceptingApplications: false`. |
| Unknown job ID | `404` with the standard error body. |
| Missing, invalid, or expired token | `401 Unauthorized`. |
| Valid recruiter token | `403` for candidate-only job details. |
| Candidate token on create, update, or close | `403 Forbidden`. |

Use the Job Service API as documented above; no delete endpoint is provided
because no deletion policy was established.
