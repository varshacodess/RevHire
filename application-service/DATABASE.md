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
