CREATE TABLE company_research (
    id           BIGSERIAL PRIMARY KEY,
    job_id       BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    briefing     TEXT NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
