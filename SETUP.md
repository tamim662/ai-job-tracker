# Setup Instructions

## 1. Install Docker

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
docker compose version
```

---

## 2. AWS S3 — Create Bucket + IAM Credentials

**Create the bucket:**
1. Log in to https://s3.console.aws.amazon.com
2. Click **Create bucket**
3. Name it something like `ai-job-tracker-files` (must be globally unique)
4. Region: `ap-southeast-2` (Sydney) or your preferred region
5. Keep **Block all public access** enabled
6. Click **Create bucket**

**Create an IAM user:**
1. Go to https://console.aws.amazon.com/iam
2. **Users → Create user**
3. Username: `ai-job-tracker-s3`
4. **Attach policies directly** → select `AmazonS3FullAccess`
5. Create user, then open the user → **Security credentials** tab
6. Click **Create access key** → choose **Application running outside AWS**
7. Copy the **Access key ID** and **Secret access key** — you only see the secret once

---

## 3. Anthropic API Key

1. Go to https://console.anthropic.com
2. **API Keys → Create Key**
3. Name it `ai-job-tracker`
4. Copy the key (starts with `sk-ant-...`)

---

## 4. Generate a JWT Secret

```bash
openssl rand -base64 48 | tr -d '\n'
```

Copy the output — that's your `JWT_SECRET`.

---

## 5. Create Your `.env` File

From the project root:

```bash
cp .env.example .env
```

Then open `.env` and fill in your values:

```env
# Database (leave as-is for Docker)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jobtracker
DB_USER=jobtracker
DB_PASSWORD=jobtracker

# JWT — paste the output from step 4
JWT_SECRET=your-generated-secret-here

# App login credentials
APP_USERNAME=admin
APP_PASSWORD=your-chosen-password

# AWS S3 — from step 2
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_BUCKET_NAME=ai-job-tracker-files
AWS_REGION=ap-southeast-2

# Anthropic — from step 3
ANTHROPIC_API_KEY=sk-ant-...
```

---

## 6. Start PostgreSQL

```bash
# From project root
docker-compose up -d

# Verify it's running
docker ps
# Should show: jobtracker-postgres   Up
```

---

## 7. Start the Backend

```bash
cd backend

# Load env vars
export $(grep -v '^#' ../.env | xargs)

# Start
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./mvnw spring-boot:run
```

Wait for `Started JobTrackerApplication` in the logs. Flyway will create all DB tables automatically on first run.

---

## 8. Start the Frontend

Open a second terminal:

```bash
cd frontend
npm run dev
```

---

## 9. Verify Everything Works

Open http://localhost:5173 — you should see the login page.

Log in with your `APP_USERNAME` and `APP_PASSWORD` from `.env`.

Check the backend directly:

```bash
curl http://localhost:8080/api/health
# Expected: {"status":"UP"}
```

If all three pass (login page loads, login succeeds, health returns UP) — setup is complete.
