# Application uniqueness constraint

`Application` declares the database unique constraint
`uk_application_candidate_job` over `candidate_id` and `job_id`. With the
configured Hibernate `ddl-auto=update`, schema update can create this constraint
on an existing `applications` table, but it will fail if duplicate pairs are
already present. Existing rows are not deleted or changed automatically.

Before deploying against an existing database, inspect conflicting rows with:

```sql
SELECT candidate_id, job_id, COUNT(*) AS application_count,
       GROUP_CONCAT(application_id ORDER BY application_id) AS application_ids
FROM applications
GROUP BY candidate_id, job_id
HAVING COUNT(*) > 1;
```

No live database records were inspected as part of this change. Any returned
groups must be reviewed and resolved by the database owner before applying the
constraint; preserve the records and decide the resolution explicitly.

## Candidate application summary

GET /api/applications/me/summary returns counts for the authenticated candidate. Candidate identity is resolved through the configured CandidateIdentityProvider; clients must not supply a candidate ID.

The total application count includes all applications belonging to the candidate, regardless of status. The pending and reviewing counts include only applications with the respective PENDING and REVIEWING statuses.

Example request:

GET /api/applications/me/summary
Authorization: Bearer <access-token>

Example response:

{
"totalApplications": 12,
"pendingApplications": 8,
"reviewingApplications": 3
}

Counts are computed in a candidate-scoped database aggregate query. A candidate without applications receives zero for all three counts. Missing authentication returns 401, unavailable candidate identity returns 503. This endpoint has no request parameters or resource ID, so parameter-validation (400), not-found (404), and duplicate-submission (409) responses do not apply.