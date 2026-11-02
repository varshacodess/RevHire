-- ============================================
-- AUTHENTICATION DATABASE
-- ============================================

USE revhire_authentication;

CREATE TABLE IF NOT EXISTS users (
                                     user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('CANDIDATE', 'RECRUITER', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


-- ============================================
-- PROFILE DATABASE
-- ============================================

USE revhire_profile;

CREATE TABLE IF NOT EXISTS candidate_profiles (
                                                  profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  user_id BIGINT NOT NULL UNIQUE,
                                                  first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    phone VARCHAR(20),
    location VARCHAR(150),
    education VARCHAR(255),
    experience_years INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS recruiter_profiles (
                                                  profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  user_id BIGINT NOT NULL UNIQUE,
                                                  recruiter_name VARCHAR(150) NOT NULL,
    organization_name VARCHAR(200),
    phone VARCHAR(20),
    location VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS skills (
                                      skill_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      skill_name VARCHAR(100) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS candidate_skills (
                                                candidate_id BIGINT NOT NULL,
                                                skill_id BIGINT NOT NULL,
                                                PRIMARY KEY (candidate_id, skill_id),
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id)
    );

CREATE TABLE IF NOT EXISTS resumes (
                                       resume_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       candidate_id BIGINT NOT NULL,
                                       resume_url VARCHAR(500),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


-- ============================================
-- JOB DATABASE
-- ============================================

USE revhire_job;

CREATE TABLE IF NOT EXISTS jobs (
                                    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    recruiter_id BIGINT NOT NULL,
                                    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    location VARCHAR(150),
    employment_type VARCHAR(50),
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS job_skills (
                                          job_id BIGINT NOT NULL,
                                          skill_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (job_id, skill_name)
    );


-- ============================================
-- APPLICATION DATABASE
-- ============================================

USE revhire_application;

CREATE TABLE IF NOT EXISTS applications (
                                            application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            job_id BIGINT NOT NULL,
                                            candidate_id BIGINT NOT NULL,
                                            resume_id BIGINT,
                                            status ENUM(
                                            'APPLIED',
                                            'SHORTLISTED',
                                            'REJECTED',
                                            'INTERVIEW',
                                            'SELECTED'
) DEFAULT 'APPLIED',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE (job_id, candidate_id)
    );

CREATE TABLE IF NOT EXISTS application_status_history (
                                                          history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                          application_id BIGINT NOT NULL,
                                                          status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT
    );