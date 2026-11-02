ALTER TABLE jobs
    ADD COLUMN recruiter_id BIGINT NULL,
    ADD INDEX idx_jobs_recruiter_id (recruiter_id);
