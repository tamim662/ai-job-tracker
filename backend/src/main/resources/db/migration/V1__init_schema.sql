-- Full schema for AI Job Tracker
-- All tables created upfront to avoid migration ordering issues later

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(50),
    linkedin_url        VARCHAR(500),
    github_url          VARCHAR(500),
    target_roles        TEXT,
    preferred_locations TEXT,
    visa_note           TEXT,
    salary_expectation  VARCHAR(100),
    availability        VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resumes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    file_name   VARCHAR(255) NOT NULL,
    file_url    VARCHAR(1000) NOT NULL,
    parsed_text TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cover_letters (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    file_name   VARCHAR(255) NOT NULL,
    file_url    VARCHAR(1000) NOT NULL,
    parsed_text TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jobs (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    company      VARCHAR(255),
    location     VARCHAR(255),
    platform     VARCHAR(100),
    job_url      VARCHAR(1000),
    description  TEXT,
    salary       VARCHAR(100),
    job_type     VARCHAR(100),
    posted_date  DATE,
    closing_date DATE,
    saved_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_matches (
    id                   BIGSERIAL PRIMARY KEY,
    job_id               BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    resume_id            BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    ats_score            INTEGER,
    matched_skills       TEXT,
    missing_skills       TEXT,
    suggested_summary    TEXT,
    suggested_skills     TEXT,
    suggested_experience TEXT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE applications (
    id                    BIGSERIAL PRIMARY KEY,
    job_id                BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    status                VARCHAR(50) NOT NULL DEFAULT 'SAVED',
    applied_date          DATE,
    resume_version        VARCHAR(255),
    cover_letter_version  VARCHAR(255),
    notes                 TEXT,
    follow_up_date        DATE,
    interview_date        DATE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hr_contacts (
    id             BIGSERIAL PRIMARY KEY,
    job_id         BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    name           VARCHAR(255),
    email          VARCHAR(255),
    linkedin_url   VARCHAR(500),
    contacted      BOOLEAN DEFAULT FALSE,
    contact_date   DATE,
    follow_up_date DATE,
    reply_status   VARCHAR(100),
    notes          TEXT
);

CREATE TABLE generated_messages (
    id         BIGSERIAL PRIMARY KEY,
    job_id     BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    content    TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed the single user profile (empty — filled in via the profile page)
INSERT INTO users (name, email) VALUES ('', '');
