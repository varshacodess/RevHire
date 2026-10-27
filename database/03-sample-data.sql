USE revhire_authentication;

INSERT INTO users (username, email, password, role)
VALUES
    ('testcandidate', 'candidate@test.com', 'TEMP_PASSWORD', 'CANDIDATE'),
    ('testrecruiter', 'recruiter@test.com', 'TEMP_PASSWORD', 'RECRUITER');


USE revhire_profile;

INSERT INTO skills (skill_name)
VALUES
    ('Java'),
    ('Spring Boot'),
    ('SQL'),
    ('Angular'),
    ('Python');


USE revhire_job;

INSERT INTO jobs
(recruiter_id, title, description, requirements, location, employment_type, salary_min, salary_max)
VALUES
    (2,
     'Java Developer',
     'Develop and maintain Java applications.',
     'Java, Spring Boot, SQL',
     'Hyderabad',
     'FULL_TIME',
     400000,
     800000);