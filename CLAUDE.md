# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Personal AI Job Tracker — a full-stack app for job searching in Australia, ATS resume matching, application tracking, HR outreach, and company research before interviews. Single-user (no registration). AI features use Anthropic Claude API (messages/cover letters/research) and Groq API (ATS scanning).

## Commands

### Backend (from `backend/`)

```bash
# Run all tests (uses H2 in-memory DB — no Docker needed)
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw test

# Run a single test class
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw test -Dtest=AtsMatchControllerTest

# Start the dev server (requires Docker PostgreSQL running first)
cd backend && export $(grep -v '^#' ../.env | xargs) && JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw spring-boot:run

# Build JAR
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw package -DskipTests
```

### Frontend (from `frontend/`)

```bash
npm run dev      # dev server on http://localhost:5173
npm run build    # production build
```

### Database

```bash
# From project root
docker-compose up -d    # start PostgreSQL
docker-compose down     # stop
docker-compose down -v  # stop and delete data volume
```

## Environment Variables

All variables live in `.env` at the project root. The backend loads them at startup.

| Variable | Purpose |
|---|---|
| `DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD` | PostgreSQL connection (defaults match docker-compose) |
| `JWT_SECRET` | Must be ≥ 32 characters — internal signing key, never typed by user |
| `APP_USERNAME` / `APP_PASSWORD` | Login credentials (default: admin / changeme) |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_BUCKET_NAME` / `AWS_REGION` | S3 bucket for resume/cover letter file storage (optional) |
| `ANTHROPIC_API_KEY` | Claude API — message generation, cover letters, company research summaries |
| `GROQ_API_KEY` | Groq API (free) — ATS scanning via Llama 3.3 70B |
| `ADZUNA_APP_ID` / `ADZUNA_APP_KEY` | Adzuna job search API (free, Australia-specific) |
| `TAVILY_API_KEY` | Tavily search API (free) — company research + web job search |

Tests override all of these via `src/test/resources/application.properties` using H2 — no `.env` needed for tests.

## Architecture

### Backend (`backend/src/main/java/com/jobtracker/`)

Stateless REST API, Spring Boot 3.3.4, Java 17 (compiled on 21). **68 tests passing.**

**`.env` loading:** `JobTrackerApplication.main()` calls `loadDotEnv()` before Spring starts. Uses `dotenv-java` (`io.github.cdimascio:dotenv-java:3.0.0`) to find `.env` at `../` (project root relative to `backend/`). Sets missing keys as Java system properties so Spring resolves `${VAR}` placeholders correctly. OS environment variables always take precedence — so IntelliJ run configs and production env vars are never overridden. This means the backend works from both IntelliJ (green Run button) and the terminal without needing `export $(...)` first.

**Auth flow:** `POST /api/auth/login` → `AuthController` → `AuthenticationManager` (in-memory single user) → returns JWT → client sends `Authorization: Bearer <token>` on every request → `JwtAuthFilter` validates and sets `SecurityContext`.

Public endpoints: `/api/health`, `/api/auth/**`. Everything else requires a valid JWT.

**Package layout:**
```
config/       — AnthropicConfig, CorsConfig, RestTemplateConfig, S3Config, SecurityConfig
security/     — JwtUtil, JwtAuthFilter
controller/   — REST controllers, one per module
service/      — Business logic, one per module
repository/   — Spring Data JPA repositories
entity/       — JPA entities mirroring DB schema
dto/          — Request/response records (use Java records)
util/         — KeywordMatcher (local keyword scoring, no API)
```

**Database:** Flyway manages schema. **Never modify existing migrations.** Add new changes in new files.
- `V1__init_schema.sql` — all base tables
- `V2__add_company_research.sql` — company_research table
- `V3__add_profile_templates.sql` — default_hr_email, default_linkedin_message columns on users

`ddl-auto=validate` means Hibernate fails on startup if entities don't match the DB.

**External API services:**
- `ClaudeService` — wraps Anthropic Java SDK (`com.anthropic:anthropic-java:2.34.0`). Takes systemPrompt + userMessage, returns text. Uses prompt caching (1h TTL) on the system prompt. Model: `claude-sonnet-4-6`.
- `GroqService` — calls Groq's OpenAI-compatible API via `RestTemplate`. Model: `llama-3.3-70b-versatile`. Used for ATS scanning to save Anthropic tokens.
- `AdzunaService` — calls Adzuna REST API for Australian job listings. Returns `JobSearchPageDto` (paginated) with keyword match score per result via `KeywordMatcher`. Uses `build().encode().toUri()` for proper URL encoding of multi-word searches.
- `TavilyService` — calls Tavily search API. Two methods:
  - `search(query, maxResults)` — used by `CompanyResearchService` for company research
  - `searchJobs(what, where, maxResults, resumeText)` — used by `JobSearchController` for web job search across Seek, LinkedIn, Indeed, Jora etc. via `include_domains`

**File storage:** AWS SDK v2 (`software.amazon.awssdk:s3`). `S3Service` handles upload/delete.

**PDF/DOCX parsing:** Apache PDFBox 3.0.2 for `.pdf`, Apache POI 5.2.5 (`poi-ooxml`) for `.docx`. `FileParserService` handles both.

**Single-file policy:** Uploading a new resume or cover letter auto-deletes the existing one (enforced in `ResumeService` and `CoverLetterService` via `findTopByOrderByCreatedAtDesc()`).

**Message templates:** Profile stores `defaultHrEmail` and `defaultLinkedinMessage`. `MessageGenerationService` passes these as base templates to Claude when generating HR_EMAIL or LINKEDIN messages — Claude adapts them per job rather than writing from scratch. Same pattern as COVER_LETTER which uses the uploaded cover letter file. If no template is set, generation still runs but uses `[placeholders]` for missing details — it never refuses or asks for more information.

**Template warning (frontend):** Both `ApplyModal` and `MessagesSection` in `JobsPage` fetch the user profile on mount and check `defaultHrEmail` / `defaultLinkedinMessage` before generating. If the relevant template is missing, an amber warning banner appears with a link to `/profile` and a "Generate anyway" fallback. This check is in the frontend only — the backend always generates regardless.

### REST API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/health` | Health check (public) |
| GET / PUT | `/api/profile` | User profile (includes defaultHrEmail, defaultLinkedinMessage) |
| GET / POST / DELETE | `/api/resumes` | Resume upload, list, delete (single-file) |
| GET / POST / DELETE | `/api/cover-letters` | Cover letter upload, list, delete (single-file) |
| GET / POST / PUT / DELETE | `/api/jobs` | Job CRUD |
| PUT | `/api/jobs/{id}/status` | Update application status |
| GET / POST | `/api/jobs/{id}/match` | Groq ATS match (GET=latest, POST=run new scan) |
| GET / POST | `/api/jobs/{id}/messages` | Generate/list outreach messages (HR_EMAIL, LINKEDIN, FOLLOWUP, COVER_LETTER) |
| GET | `/api/job-search` | Search jobs via Adzuna — paginated (params: what, where, results, page) |
| GET | `/api/job-search/web` | Web job search via Tavily (params: what, where, results) — returns WebJobResultDto list |
| GET / POST | `/api/jobs/{id}/research` | Company research via Tavily + Claude |

### Application Status Flow

`SAVED` → `RESUME_MATCHED` → `READY_TO_APPLY` → `APPLIED` → `HR_CONTACTED` → `INTERVIEW_SCHEDULED` → `INTERVIEW_DONE` → `OFFER` / `REJECTED` / `CLOSED`

- `RESUME_MATCHED` is set automatically after ATS match runs (no threshold — any completed scan sets it)
- All other transitions are triggered by the user manually via the status dropdown in My Jobs
- Company Research button appears when status reaches `INTERVIEW_SCHEDULED` or beyond
- **Applied date:** `applications.applied_date` is stamped with `LocalDate.now()` whenever status transitions to `APPLIED` — either via `PUT /api/jobs/{id}/status` or at job creation if the initial status is `APPLIED`. Once set it is never overwritten. Returned in `JobDto.appliedDate` and shown as "Applied [date]" in green on the job card.

### Database Schema

```
users (id=1, always — single seeded row)
  — name, email, phone, linkedinUrl, githubUrl
  — targetRoles, preferredLocations, visaNote
  — salaryExpectation, availability
  — defaultHrEmail, defaultLinkedinMessage   ← V3
  └── resumes (user_id → users.id)
  └── cover_letters (user_id → users.id)

jobs (standalone)
  └── job_matches (job_id → jobs.id, resume_id → resumes.id)
  └── applications (job_id → jobs.id)
  └── hr_contacts (job_id → jobs.id)
  └── generated_messages (job_id → jobs.id, type: HR_EMAIL | LINKEDIN | FOLLOWUP | COVER_LETTER)
  └── company_research (job_id → jobs.id)    ← V2
```

**Entity design rule:** Use plain `Long jobId` / `Long resumeId` fields (no `@ManyToOne`) to avoid FK constraint issues with H2 in tests.

### Frontend (`frontend/src/`)

React 18 + Vite + Tailwind CSS. No component library — plain Tailwind only.

**Auth:** `AuthContext` holds the JWT in `localStorage`. `axios.js` attaches it as `Authorization: Bearer` on every request and redirects to `/login` on 401. `PrivateRoute` gates all authenticated routes.

**API calls:** Always use the shared `api` instance from `src/api/axios.js` — never import `axios` directly. Vite proxies `/api/*` to `http://localhost:8080` in dev.

**Copy to clipboard:** Always use `ClipboardItem` with `text/plain` to avoid copying background colours from styled divs:
```js
const blob = new Blob([text], { type: 'text/plain' })
navigator.clipboard.write([new ClipboardItem({ 'text/plain': blob })])
```

**Routing:** React Router v6. Pages in `src/pages/`, wired in `App.jsx`. All authenticated pages use `<Layout>` which provides the nav bar and logout.

**Pages/routes:**
- `/` — Dashboard (live stats: total jobs, applied, interviews, offers; pipeline bar chart; recent jobs; quick actions; setup checklist)
- `/jobs` — Jobs page with two tabs:
  - **My Jobs** — saved/applied jobs with status badge, ATS score, applied date (green, when set); click to expand for: job details (salary, type, description with See more/See less toggle), ATS results, outreach messages, company research, status dropdown
  - **Search Jobs** — Adzuna search (paginated, 10/page) + Web Results section (Tavily, browse-only)
- `/resumes` — Single resume management (upload/replace/remove)
- `/cover-letters` — Single cover letter management (upload/replace/remove)
- `/profile` — User profile including default HR email and LinkedIn InMail templates

**Key components:**
- `ApplyModal` — full-screen modal triggered by "Want to Apply"; runs ATS on mount, then branches to Contact HM or Apply Directly (with optional cover letter generation); checks profile templates before generating HR Email or LinkedIn and shows amber warning if missing
- `MessagesSection` — expanded section within My Jobs job card; fetches profile on mount to check templates before generation; shows amber warning with link to Profile if template missing

### User Flow

1. **Upload resume** → parsed text stored in DB → used for keyword matching and ATS
2. **Upload cover letter** → stored as base template for Claude to edit per job
3. **Fill profile** → visa status, availability, target roles; set default HR email and LinkedIn InMail templates (used by AI generation)
4. **Search Jobs tab** → Adzuna results (paginated, with match %) + Web Results (Tavily, links to Seek/LinkedIn/Indeed)
5. **"Want to Apply"** → saves job to My Jobs → opens Apply Modal → ATS scan runs automatically
   - **Contact Hiring Manager** → HR Email or LinkedIn InMail (adapts your saved template; warns if no template set)
   - **Apply Directly** → optionally generate cover letter → Open Job Application ↗
   - **Mark as Applied ✓** → updates status to APPLIED and stamps applied date
6. **Add Job manually** → "Add Job" form includes an Application Status dropdown (defaults to SAVED); selecting APPLIED stamps the applied date immediately
7. **My Jobs** → track all jobs; click to expand full details; "See more / See less" toggle on long descriptions; update status via dropdown; generate outreach messages; view ATS results; applied date shown in green
8. **Company Research** → appears at INTERVIEW_SCHEDULED+; Tavily web search + Claude briefing; saved to DB
9. **Dashboard** → overview of entire pipeline at a glance

### Testing Conventions

- Tests use H2 in-memory DB with Flyway disabled (`spring.flyway.enabled=false`) and `ddl-auto=create-drop`.
- H2 does not enforce FK constraints with plain `Long` fields — this is intentional.
- Use `@SpringBootTest + @AutoConfigureMockMvc` for all controller tests (full security filter chain must be active, not `@WebMvcTest`).
- Test credentials: `admin` / `changeme` (in `src/test/resources/application.properties`).
- Get JWT in tests: POST to `/api/auth/login`, parse `token` from response, pass as `Authorization: Bearer <token>`.
- External API services must be `@MockBean`'d in tests: `ClaudeService`, `GroqService`, `AdzunaService`, `TavilyService`, `S3Service`, `FileParserService`.
- **Current test count: 68 tests across 11 test classes — all must pass before proceeding to next feature.**
