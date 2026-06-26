# AI Job Tracker

A personal, self-hosted job search assistant built for the Australian market. It combines ATS resume scanning, AI-generated outreach messages, company research, and application tracking — all in one place. No SaaS subscription. No data sent to third parties beyond the APIs you configure yourself.

Designed for a **single user** — there is no sign-up or registration. You run it, you own it.

---

## Features

### Resume & Cover Letter Management
- Upload your resume (PDF or DOCX) — parsed text is extracted automatically
- Upload a base cover letter — used as a template so Claude preserves your voice and personal details
- One file at a time; uploading a new file automatically replaces the existing one

### Job Search
- Search Australian job listings via the **Adzuna API**
- Each result shows an instant **keyword match %** against your uploaded resume (no API call — local scoring)
- Filter by keyword and location

### Apply Modal (full application flow)
When you click **"Want to Apply"** on any search result:
1. The job is automatically saved to **My Jobs**
2. A full-screen modal opens and immediately runs an **ATS scan** (Groq / Llama 3.3 70B) — side-by-side matched skills, missing skills, and a suggested summary rewrite
3. You choose your path:

   **Contact Hiring Manager**
   - Generate an **HR Email** — cold outreach email with subject line, tailored to the job
   - Generate a **LinkedIn Message** — 280-character connection request, tailored to the job
   - Both use your saved default templates (if set in Profile) as a base — Claude adapts them, not rewrites them

   **Apply Directly**
   - Optionally generate a **Cover Letter** — Claude edits your uploaded base cover letter for this specific job
   - Or skip straight to opening the job URL
   - **Open Job Application ↗** button appears in both sub-paths

4. **Mark as Applied** button is always visible at the bottom — updates the job status to `APPLIED`

### My Jobs
- All saved and applied jobs in one tab
- Status badge per job: `SAVED → RESUME_MATCHED → READY_TO_APPLY → APPLIED → HR_CONTACTED → INTERVIEW_SCHEDULED → INTERVIEW_DONE → OFFER / REJECTED / CLOSED`
- Status dropdown to manually move a job through the pipeline
- Expand any job to view ATS results, generated messages, and company research

### Message Generation (from My Jobs)
Generate four message types for any saved job:
- **HR Email** — cold outreach email
- **LinkedIn Message** — connection request
- **Follow-up Email** — polite status check after applying
- **Cover Letter** — customised version of your base cover letter

### Company Research
- Available once a job reaches `INTERVIEW_SCHEDULED` status
- Searches the web via **Tavily** then synthesises a structured briefing via **Claude**:
  - What the company does, tech stack, culture, recent news, interview tips
- Result is saved to the database — no need to regenerate on every visit

### Profile
Store your details once — Claude uses them when generating messages:
- Name, email, phone, LinkedIn URL, GitHub URL
- Target roles, preferred locations, visa / work rights
- Salary expectation, availability / notice period
- **Default HR Email Template** — your go-to email structure; Claude adapts it per job
- **Default LinkedIn Message Template** — same approach

### Dashboard
- Stats: total jobs tracked, applied, interviews, offers
- Visual application pipeline (bar chart by stage)
- Recent jobs list
- Setup checklist (prompts you to upload resume / cover letter if missing)
- Quick action links

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 17, Spring Security (JWT stateless) |
| Database | PostgreSQL (Docker locally or AWS RDS) |
| Migrations | Flyway |
| Frontend | React 18, Vite, Tailwind CSS |
| AI — messages & research | Anthropic Claude (`claude-sonnet-4-6`) |
| AI — ATS scanning | Groq (`llama-3.3-70b-versatile`) — free tier |
| Job search | Adzuna API — free, Australia-specific |
| Web search | Tavily API — free tier |
| File storage | AWS S3 (optional) |

---

## Prerequisites

- **Java 17+** (`java -version`)
- **Node.js 18+** (`node -v`)
- **Docker** (for local PostgreSQL) — or an AWS RDS instance
- API keys for: Anthropic, Groq, Adzuna, Tavily (all have free tiers)
- An AWS S3 bucket (optional — required only for file uploads)

---

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/ai-job-tracker.git
cd ai-job-tracker
```

### 2. Create your environment file

```bash
cp .env.example .env
```

Then open `.env` and fill in each value. The sections below explain each one.

---

### 3. Set your login credentials

There is no registration. You log in with the username and password you set here:

```env
APP_USERNAME=admin
APP_PASSWORD=your-strong-password
```

Change both to something only you know. These are the credentials you type into the login page — that is all you need to do.

---

### 4. Generate a JWT secret

This is **not** something you type when logging in. It is an internal signing key that Spring Security uses to sign and verify session tokens automatically — you generate it once, put it in `.env`, and never touch it again.

```bash
openssl rand -base64 64 | tr -d '\n'
```

Copy the output into `.env`:

```env
JWT_SECRET=the-long-random-string-you-just-generated
```

Must be at least 32 characters. If you skip this or leave the placeholder, the app will reject it on startup.

---

### 5. Database — choose one option

#### Option A: Docker (local, easiest)

Make sure Docker is running, then:

```bash
docker-compose up -d
```

This starts a PostgreSQL 15 container. Your data persists in a Docker volume (`postgres_data`) — it survives PC restarts. Only `docker-compose down -v` deletes it.

Your `.env` database section stays as the defaults:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jobtracker
DB_USER=jobtracker
DB_PASSWORD=jobtracker
```

#### Option B: AWS RDS

1. Create a PostgreSQL 15 RDS instance (Free Tier: `db.t3.micro`)
2. Set **Public access: Yes** and allow inbound port `5432` in the security group
3. Update `.env`:

```env
DB_HOST=your-instance.xxxx.ap-southeast-2.rds.amazonaws.com
DB_PORT=5432
DB_NAME=jobtracker
DB_USER=your_rds_username
DB_PASSWORD=your_rds_password
```

Flyway runs automatically on startup and creates all tables — no manual SQL needed.

---

### 6. AWS S3 (optional — for file uploads)

If you skip this, the app runs fine but resume and cover letter uploads will fail. You can still use all other features if you are comfortable without file storage.

**To set up S3:**

1. Create a private S3 bucket in your preferred region
2. Create an IAM user with programmatic access and attach a policy with at minimum:
   ```json
   {
     "Effect": "Allow",
     "Action": ["s3:PutObject", "s3:DeleteObject"],
     "Resource": "arn:aws:s3:::your-bucket-name/*"
   }
   ```
3. Update `.env`:
   ```env
   AWS_ACCESS_KEY_ID=your-iam-access-key
   AWS_SECRET_ACCESS_KEY=your-iam-secret-key
   AWS_BUCKET_NAME=your-bucket-name
   AWS_REGION=ap-southeast-2
   ```

---

### 7. API Keys

All services below have free tiers sufficient for personal use.

#### Anthropic Claude (required for message generation and company research)
1. Go to [console.anthropic.com](https://console.anthropic.com)
2. Create an API key
3. Add to `.env`: `ANTHROPIC_API_KEY=sk-ant-...`

#### Groq (required for ATS scanning)
1. Go to [console.groq.com](https://console.groq.com)
2. Create an API key (free — generous limits)
3. Add to `.env`: `GROQ_API_KEY=gsk_...`

#### Adzuna (required for job search)
1. Register at [developer.adzuna.com](https://developer.adzuna.com)
2. Create an app to get your App ID and App Key (free)
3. Add to `.env`:
   ```env
   ADZUNA_APP_ID=your-app-id
   ADZUNA_APP_KEY=your-app-key
   ```

#### Tavily (required for company research)
1. Go to [tavily.com](https://tavily.com)
2. Sign up and get your API key (free tier available)
3. Add to `.env`: `TAVILY_API_KEY=tvly-...`

---

## Running the App

### Backend

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**. On first run, Flyway creates all database tables automatically.

### Frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

Open **http://localhost:5173** in your browser, log in with the credentials you set in `.env`, and you are ready to go.

---

## User Journey

Here is the typical workflow from first use to interview:

1. **Upload your resume** → `/resumes` — PDF or DOCX. Text is extracted and used for keyword matching and ATS analysis.

2. **Upload a base cover letter** → `/cover-letters` — Claude will edit this (not rewrite it) when generating cover letters for specific jobs.

3. **Fill in your profile** → `/profile` — Add your visa status, availability, target roles, and optionally your default HR email and LinkedIn message templates.

4. **Search for jobs** → `/jobs` (Search Jobs tab) — Enter a keyword (e.g. "Java Developer") and location (e.g. "Sydney"). Each result shows a keyword match % against your resume instantly.

5. **Click "Want to Apply"** on any result:
   - Job is saved to My Jobs automatically
   - ATS analysis runs (takes a few seconds — Groq scans your resume vs the job description)
   - Choose: **Contact Hiring Manager** or **Apply Directly**

6. **Contact Hiring Manager path:**
   - Generate HR Email or LinkedIn Message
   - Claude uses your saved default template as a base and adapts it for this specific job
   - Copy the result and send it yourself

7. **Apply Directly path:**
   - Optionally generate a cover letter (Claude edits your base template)
   - Click **Open Job Application ↗** to open the job URL
   - Click **Mark as Applied ✓** once done

8. **Track progress** → `/jobs` (My Jobs tab):
   - Update status via the dropdown as things progress
   - View ATS results and all generated messages for each job

9. **Prepare for interviews** — once a job reaches `INTERVIEW_SCHEDULED`, a **Company Research** button appears:
   - Click it to run a web search (Tavily) + Claude briefing
   - Covers: what they do, tech stack, culture, recent news, interview tips
   - Saved to the database so you can revisit it anytime

---

## Security Notes

- **`.env` is in `.gitignore`** — it will not be committed. Never commit your real `.env` file.
- The app is single-user by design. There is no multi-user support, no registration endpoint, and no public API.
- The JWT secret you generate is what keeps your session secure. Use a strong one (`openssl rand -base64 64`).
- If you expose the app to the internet (e.g. via a reverse proxy), make sure to use HTTPS and change `APP_PASSWORD` to something strong.
- For local use only, the defaults are fine — the app is only accessible on your machine.

---

## Running Tests

Tests use an H2 in-memory database — no Docker or `.env` required.

```bash
cd backend
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw test
```

64 tests across 10 test classes covering all controllers and key service logic.

---

## Project Structure

```
ai-job-tracker/
├── backend/                    # Spring Boot REST API
│   └── src/main/java/com/jobtracker/
│       ├── config/             # Security, CORS, AWS, Anthropic config
│       ├── controller/         # REST endpoints
│       ├── service/            # Business logic + AI integrations
│       ├── entity/             # JPA entities
│       ├── dto/                # Request / response records
│       ├── repository/         # Spring Data JPA
│       └── security/           # JWT filter + util
│   └── src/main/resources/
│       └── db/migration/       # Flyway SQL migrations (V1, V2, V3)
├── frontend/                   # React 18 + Vite + Tailwind CSS
│   └── src/
│       ├── pages/              # Dashboard, Jobs, Resumes, CoverLetters, Profile
│       ├── components/         # Layout, ApplyModal
│       ├── api/                # Axios instance with JWT interceptor
│       └── contexts/           # AuthContext
├── docker-compose.yml          # Local PostgreSQL
├── .env.example                # Template — copy to .env and fill in
└── README.md
```

---

## Application Status Flow

```
SAVED → RESUME_MATCHED → READY_TO_APPLY → APPLIED → HR_CONTACTED
     → INTERVIEW_SCHEDULED → INTERVIEW_DONE → OFFER / REJECTED / CLOSED
```

- `RESUME_MATCHED` is set automatically after ATS scan runs
- All other transitions are manual via the status dropdown in My Jobs
- Company Research button appears at `INTERVIEW_SCHEDULED` and beyond

---

## License

MIT — use it, fork it, adapt it. If you build something cool on top of it, a star on the repo would be appreciated.
