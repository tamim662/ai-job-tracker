# AI Job Tracker

Looking for a job is exhausting, not because there aren't enough jobs, but because the process around applying is just a lot of repetitive work. Every application needs a slightly different cover letter, a slightly different email to the hiring manager, and if you get to the interview stage you suddenly need to research the company from scratch.

This is a personal, self-hosted tool I built to handle that entire process in one place, specifically for the Australian job market.

The idea is simple: **you bring your own resume and cover letter**. The app does not write them for you from scratch, and it shouldn't, because a generic AI-written CV will get you nowhere. You take the time to write a proper resume and cover letter yourself, upload them once, and from that point the AI works *from your material*, not instead of it. When you want a cover letter for a specific job, it reads the one you uploaded and rewrites it for that role, keeping your voice, your experience, your structure. Same for HR emails and LinkedIn messages: you save a default template in your profile, and for each job it adapts your template rather than hallucinating a new one from nothing.

Everything flows from job search through to interview prep in a single pipeline:

- Search Australian job listings (Adzuna + Tavily web search across Seek, LinkedIn, Indeed, Jora)
- Get an instant ATS match score against your resume for every result
- Click "Want to Apply" and an automatic ATS deep scan runs (Groq / Llama 3.3 70B), then choose your path
- Contact the hiring manager with an HR email or LinkedIn message adapted from your template
- Apply directly with a cover letter generated from your uploaded base, or skip
- Track every application through the hiring pipeline
- When you land an interview, a one-click company research briefing via Tavily and Claude covers what they do, their tech stack, culture, recent news, and things to ask

I built this initially for myself, but it can genuinely help anyone who is looking for a job in Australia and wants a proper job tracking system running on their own machine. You can download the repo and follow the setup instructions along with the required API keys. I tried to use free APIs from different AI tools for most tasks — Groq for ATS scanning, Adzuna and Tavily for job search and research — with Claude API being the only one that requires token cost, used specifically for generating the writing and outreach content.

<video src="https://raw.githubusercontent.com/tamim662/ai-job-tracker/main/ai-job-tacker.mp4" controls width="100%"></video>

---

## Features

### Resume & Cover Letter Management
- Upload your resume (PDF or DOCX) — text is extracted and used for keyword matching and ATS analysis
- Upload a base cover letter — Claude edits this for each job rather than writing one from scratch, preserving your voice and personal details
- One file at a time; uploading a new file automatically replaces the previous one

### Job Search
- Search Australian job listings via **Adzuna API** (structured results with salary, contract type, pagination)
- **Web Results** via **Tavily** search across Seek, LinkedIn, Indeed, Jora, CareerOne, Glassdoor — broader coverage beyond Adzuna's index
- Every Adzuna result shows an instant **keyword match %** against your uploaded resume (local scoring, no API call)
- Paginated results — 10 per page with Prev / page numbers / Next controls

### Apply Modal (full application flow)
When you click **"Want to Apply"** on any search result:
1. Job is automatically saved to **My Jobs**
2. A full-screen modal opens and immediately runs an **ATS scan** (Groq / Llama 3.3 70B) — matched skills, missing skills, and a suggested summary rewrite
3. Choose your path:

   **Contact Hiring Manager**
   - Generate an **HR Email** — adapts your saved default template for this specific job
   - Generate a **LinkedIn Message** — adapts your saved default LinkedIn template for this job
   - Copy and send yourself

   **Apply Directly**
   - Optionally generate a **Cover Letter** — Claude edits your uploaded base cover letter for this role
   - Or skip straight to the job URL
   - **Open Job Application ↗** button in both sub-paths

4. **Mark as Applied ✓** — always visible at the bottom, updates status to `APPLIED`

### My Jobs
- All saved and applied jobs in one tab
- Full status pipeline per job with colour-coded badge
- Status dropdown to manually move a job through the pipeline
- Expand any job to view: ATS results, all generated messages, company research

### Message Generation (from My Jobs)
Generate four message types for any saved job at any time:
- **HR Email** — adapted from your default template
- **LinkedIn Message** — adapted from your default template
- **Follow-up Email** — polite status check after applying
- **Cover Letter** — customised from your uploaded base cover letter

All copy buttons write plain text to clipboard — no background colours carried over when pasting into Gmail or Word.

### Company Research
- Available once a job reaches `INTERVIEW_SCHEDULED` status
- Tavily searches the live web, Claude synthesises a structured briefing:
  - What the company does, tech stack, culture, recent news, suggested interview questions
- Saved to the database — revisit without regenerating

### Profile
Store your details once — used across all AI generation:
- Name, email, phone, LinkedIn URL, GitHub URL
- Target roles, preferred locations, visa / work rights
- Salary expectation, availability / notice period
- **Default HR Email Template** — your preferred email structure; AI adapts it per job
- **Default LinkedIn Message Template** — same approach

### Dashboard
- Live stats: total jobs tracked, applied count and %, active interviews, offers
- Visual pipeline chart by stage
- Recent jobs list
- Setup checklist (prompts to upload resume / cover letter if missing)
- Quick action links

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 17, Spring Security (JWT stateless) |
| Database | PostgreSQL (Docker locally or AWS RDS) |
| Migrations | Flyway |
| Frontend | React 18, Vite, Tailwind CSS |
| AI — messages, cover letters, research | Anthropic Claude (`claude-sonnet-4-6`) |
| AI — ATS scanning | Groq (`llama-3.3-70b-versatile`) — free tier |
| Job search | Adzuna API — free, Australia-specific |
| Web job search + company research | Tavily API — free tier |
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
git clone https://github.com/tamim662/ai-job-tracker.git
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
APP_USERNAME=your-username
APP_PASSWORD=your-strong-password
```

Change both to something only you know. These are the credentials you type into the login page.

---

### 4. Generate a JWT secret

This is **not** something you type when logging in. It is an internal signing key that Spring Security uses to sign and verify session tokens automatically — generate it once, put it in `.env`, and never touch it again.

```bash
openssl rand -base64 64 | tr -d '\n'
```

Copy the output into `.env`:

```env
JWT_SECRET=the-long-random-string-you-just-generated
```

Must be at least 32 characters. The app rejects it on startup if the placeholder is left in.

---

### 5. Database — choose one option

#### Option A: Docker (local, easiest)

```bash
docker-compose up -d
```

Data persists in a Docker volume (`postgres_data`) and survives PC restarts. Only `docker-compose down -v` deletes it.

Keep the `.env` database section as the defaults:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jobtracker
DB_USER=jobtracker
DB_PASSWORD=jobtracker
```

#### Option B: AWS RDS

1. Create a PostgreSQL 15 RDS instance (Free Tier: `db.t3.micro`)
2. Allow inbound port `5432` in the security group
3. Update `.env`:

```env
DB_HOST=your-instance.xxxx.ap-southeast-2.rds.amazonaws.com
DB_PORT=5432
DB_NAME=jobtracker
DB_USER=your_rds_username
DB_PASSWORD=your_rds_password
```

Flyway runs on startup and creates all tables automatically.

---

### 6. AWS S3 (optional — for file uploads)

If you skip this, everything works except resume and cover letter uploads.

1. Create a private S3 bucket
2. Create an IAM user with:
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

#### Anthropic Claude (required — messages, cover letters, company research)
1. Go to [console.anthropic.com](https://console.anthropic.com)
2. Create an API key
3. Add to `.env`: `ANTHROPIC_API_KEY=sk-ant-...`

#### Groq (required — ATS scanning)
1. Go to [console.groq.com](https://console.groq.com)
2. Create an API key (free, generous limits)
3. Add to `.env`: `GROQ_API_KEY=gsk_...`

#### Adzuna (required — structured job search)
1. Register at [developer.adzuna.com](https://developer.adzuna.com)
2. Create an app to get your App ID and App Key (free)
3. Add to `.env`:
   ```env
   ADZUNA_APP_ID=your-app-id
   ADZUNA_APP_KEY=your-app-key
   ```

#### Tavily (required — web job search + company research)
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

Starts on **http://localhost:8080**. Flyway creates all database tables on first run.

### Frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Starts on **http://localhost:5173**.

Open **http://localhost:5173**, log in with your credentials from `.env`, and you are ready.

---

## User Journey

1. **Upload your resume** → `/resumes` — PDF or DOCX. Parsed text is used for matching and ATS.

2. **Upload a base cover letter** → `/cover-letters` — Claude edits this (not replaces it) when generating cover letters for specific jobs.

3. **Fill in your profile** → `/profile` — Visa status, availability, target roles. Add a default HR email and LinkedIn message template so AI has your style to work from.

4. **Search for jobs** → `/jobs` (Search Jobs tab):
   - Adzuna results with keyword match % and pagination
   - Web Results below (Seek, LinkedIn, Indeed, Jora) with a "Browse on source ↗" link

5. **Click "Want to Apply"** on any Adzuna result:
   - Saved to My Jobs automatically
   - ATS scan runs immediately
   - Choose Contact Hiring Manager or Apply Directly

6. **Contact Hiring Manager** → HR Email or LinkedIn Message, adapted from your default template → copy and send

7. **Apply Directly** → optional cover letter → open job URL → Mark as Applied ✓

8. **Track in My Jobs** → update status as things progress; generate follow-up emails; view all past messages

9. **Interview prep** → Company Research button appears at `INTERVIEW_SCHEDULED` → Tavily + Claude briefing saved to DB

---

## Security Notes

- **`.env` is in `.gitignore`** — never commit your real `.env` file
- Single-user by design — no registration endpoint, no public API
- Use a strong JWT secret (`openssl rand -base64 64`)
- If exposing to the internet, use HTTPS and a strong `APP_PASSWORD`

---

## Running Tests

No Docker or `.env` needed — tests use H2 in-memory database.

```bash
cd backend
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw test
```

**68 tests across 11 test classes** — all controllers and key service logic covered.

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
│       ├── security/           # JWT filter + util
│       └── util/               # KeywordMatcher (local resume scoring)
│   └── src/main/resources/
│       └── db/migration/       # Flyway SQL (V1 base, V2 company_research, V3 profile templates)
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

- `RESUME_MATCHED` is set automatically after ATS scan completes
- All other transitions are manual via the status dropdown in My Jobs
- Company Research appears at `INTERVIEW_SCHEDULED` and beyond

---

## License

This project is licensed under the **GNU General Public License v3.0**.

You are free to use, study, modify, and distribute this software. Any modified version you distribute must also be licensed under GPL v3 and made open source.

See the [LICENSE](LICENSE) file for the full terms.
