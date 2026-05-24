# Render Deployment Guide - Hybrid AWS Setup

**Project:** Codebase Q&A Backend  
**Date:** May 24, 2026  
**Architecture:** Render (Backend) + AWS RDS + AWS SQS  
**Estimated Time:** 30-45 minutes

---

## Why This Setup?

✅ **Simpler than EC2:** No SSH, no server management, auto-deploy from Git  
✅ **Cost-effective:** Render free tier + AWS free tier  
✅ **Best of both:** Render's simplicity + AWS's managed services  
✅ **Auto-scaling:** Render handles it automatically  

---

## Prerequisites

- [x] AWS RDS PostgreSQL instance (from your current setup)
- [x] AWS SQS queues (from your current setup)
- [x] AWS credentials (Access Key ID + Secret Access Key)
- [x] GitHub account
- [x] Render account (free - sign up at https://render.com)
- [x] GitHub OAuth App
- [x] Gemini API key

---

## Architecture Overview

```
┌─────────────────┐
│   Render.com    │
│  (Backend API)  │
│   Java 21 App   │
└────────┬────────┘
         │
         ├──────────► AWS RDS PostgreSQL (eu-north-1)
         │            └─ pgvector enabled
         │
         ├──────────► AWS SQS (eu-north-1)
         │            ├─ codebaseqa-indexing-queue
         │            └─ DLQ (if configured)
         │
         └──────────► Redis (Render managed - optional)
```

---

## STEP 1: Prepare Your Repository (10 minutes)

### 1.1 Create Render Configuration File

Create a file to tell Render how to build your app:

**File:** `backend/render.yaml`

```yaml
services:
  - type: web
    name: codebase-qa-backend
    runtime: java
    buildCommand: ./mvnw clean package -DskipTests
    startCommand: java -jar target/backend-0.0.1-SNAPSHOT.jar
    envVars:
      - key: JAVA_VERSION
        value: 21
      - key: MAVEN_OPTS
        value: -Xmx512m
    healthCheckPath: /actuator/health
```

### 1.2 Update application.properties for Render

Render uses `PORT` environment variable (not 8080). Update your `application.properties`:

**File:** `backend/src/main/resources/application.properties`

Add this line:
```properties
server.port=${PORT:8080}
```

This makes your app use Render's `PORT` variable in production, but default to 8080 locally.

### 1.3 Commit and Push to GitHub

```bash
cd d:\Projects\CodeBaseQA
git add backend/render.yaml backend/src/main/resources/application.properties
git commit -m "Add Render deployment configuration"
git push origin main
```

---

## STEP 2: Configure AWS Security for Render (5 minutes)

### 2.1 Update RDS Security Group

Render uses dynamic IPs, so we need to allow connections from anywhere (or use Render's IP ranges).

**Option A: Allow All (Simpler, less secure)**

1. Go to **AWS Console → EC2 → Security Groups**
2. Find your RDS security group: `codebase-qa-db-sg`
3. Edit **Inbound rules**
4. Modify PostgreSQL rule:
   - Type: `PostgreSQL`
   - Port: `5432`
   - Source: `0.0.0.0/0` (anywhere)
   - Description: `Allow Render access`
5. Save

**Option B: Use Render Static IPs (More secure - Paid plan only)**

If you upgrade to Render's paid plan, you get static IPs. Then restrict RDS to those IPs only.

### 2.2 Verify RDS is Publicly Accessible

1. Go to **RDS → Databases → codebase-qa-db**
2. Check **Connectivity & security** tab
3. Ensure **Publicly accessible:** `Yes`

If not, modify the database:
- Actions → Modify
- Connectivity → Public access: `Yes`
- Apply immediately

---

## STEP 3: Create Render Web Service (15 minutes)

### 3.1 Sign Up / Login to Render

1. Go to https://render.com
2. Sign up with GitHub (recommended)
3. Authorize Render to access your repositories

### 3.2 Create New Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub repository: `CodeBaseQA`
3. Configure:
   - **Name:** `codebase-qa-backend`
   - **Region:** Choose closest to your AWS region (Europe for eu-north-1)
   - **Branch:** `main`
   - **Root Directory:** `backend`
   - **Runtime:** `Java`
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/backend-0.0.1-SNAPSHOT.jar`
   - **Instance Type:** `Free` (or `Starter` for better performance)

### 3.3 Configure Environment Variables

Click **Advanced** → **Add Environment Variable** for each:

#### Database Configuration
```
DB_HOST=codebaseqa-db.c9abc123xyz.eu-north-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=codebaseqa
DB_USERNAME=postgres
DB_PASSWORD=your-rds-password
```

#### Redis Configuration
```
REDIS_URL=redis://localhost:6379
```
*(We'll set up Render Redis later, or use Upstash)*

#### JWT Configuration
```
JWT_SECRET=your-super-secret-key-minimum-32-characters-long-change-this-in-production
```

#### GitHub OAuth
```
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GITHUB_REDIRECT_URI=https://codebase-qa-backend.onrender.com/api/auth/github/callback
```
*(Update with your actual Render URL after deployment)*

#### AWS Configuration
```
AWS_REGION=eu-north-1
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
SQS_QUEUE_URL=https://sqs.eu-north-1.amazonaws.com/YOUR-ACCOUNT-ID/codebaseqa-indexing-queue
SQS_DLQ_URL=
```

#### Gemini & Voyage API
```
GEMINI_API_KEY=your-gemini-api-key
VOYAGE_API_KEY=your-voyage-api-key
```

#### App Configuration
```
FRONTEND_URL=https://your-frontend-url.vercel.app
INDEXING_TEMP_DIR=/tmp/codebase-qa
```

### 3.4 Deploy

1. Click **Create Web Service**
2. Render will:
   - Clone your repo
   - Build the JAR
   - Start the application
   - Assign a URL: `https://codebase-qa-backend.onrender.com`

**Wait 5-10 minutes for first deployment.**

---

## STEP 4: Set Up Redis on Render (Optional - 5 minutes)

### Option A: Use Render Redis (Recommended)

1. In Render Dashboard, click **New +** → **Redis**
2. Configure:
   - **Name:** `codebase-qa-redis`
   - **Region:** Same as your web service
   - **Plan:** `Free` (25MB)
3. Click **Create Redis**
4. Copy the **Internal Redis URL** (looks like: `redis://red-xxx:6379`)
5. Update your web service environment variable:
   - `REDIS_URL=redis://red-xxx:6379`

### Option B: Use Upstash Redis (Alternative)

1. Go to https://upstash.com
2. Create free Redis database
3. Copy the Redis URL
4. Update `REDIS_URL` in Render

---

## STEP 5: Update GitHub OAuth Callback (2 minutes)

1. Go to **GitHub → Settings → Developer settings → OAuth Apps**
2. Select your app: `Codebase QA`
3. Update:
   - **Homepage URL:** `https://codebase-qa-backend.onrender.com`
   - **Authorization callback URL:** `https://codebase-qa-backend.onrender.com/api/auth/github/callback`
4. Save changes

5. Update Render environment variable:
   - `GITHUB_REDIRECT_URI=https://codebase-qa-backend.onrender.com/api/auth/github/callback`
   - Click **Save Changes** (this will redeploy)

---

## STEP 6: Verify Deployment (5 minutes)

### 6.1 Check Health Endpoint

Open in browser or use curl:
```bash
curl https://codebase-qa-backend.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 6.2 Test GitHub OAuth

1. Open: `https://codebase-qa-backend.onrender.com/api/auth/github`
2. Should redirect to GitHub
3. Authorize
4. Should return JWT token

### 6.3 Check Render Logs

1. Go to Render Dashboard → Your service
2. Click **Logs** tab
3. Look for:
   - ✅ `Started CodebaseQaApplication`
   - ✅ `Tomcat started on port(s)`
   - ✅ Database connection successful

---

## STEP 7: Update Frontend Configuration (2 minutes)

Update your frontend `.env` file:

**File:** `frontend/.env`

```env
VITE_API_URL=https://codebase-qa-backend.onrender.com
```

Commit and push:
```bash
git add frontend/.env
git commit -m "Update API URL to Render"
git push origin main
```

If frontend is on Vercel, it will auto-deploy.

---

## STEP 8: Terminate EC2 Instance (5 minutes)

Now that Render is working, you can shut down EC2:

### 8.1 Stop EC2 Instance

1. Go to **AWS Console → EC2 → Instances**
2. Select your instance: `codebase-qa-backend`
3. **Instance state** → **Stop instance**

### 8.2 (Optional) Terminate EC2 Instance

If you're sure you don't need it:
1. **Instance state** → **Terminate instance**
2. Confirm termination

### 8.3 Clean Up EC2 Resources

To avoid any charges:

1. **Delete Security Group:**
   - EC2 → Security Groups → `codebase-qa-sg` → Actions → Delete

2. **Delete Key Pair:**
   - EC2 → Key Pairs → `codebase-qa-key` → Actions → Delete

3. **Delete IAM Role:**
   - IAM → Roles → `codebase-qa-ec2-role` → Delete

4. **Delete SSM Parameters (Optional):**
   - Systems Manager → Parameter Store → Delete all `/codebase-qa/*` parameters

---

## Cost Comparison

### Before (EC2 Setup)
- EC2 t2.micro: $0/month (free tier) → $8.50/month after
- RDS db.t3.micro: $0/month (free tier) → $15/month after
- SQS: $0/month (free tier)
- **Total after free tier:** ~$23.50/month

### After (Render Setup)
- Render Free: $0/month (with sleep after inactivity)
- Render Starter: $7/month (no sleep, better performance)
- RDS db.t3.micro: $0/month (free tier) → $15/month after
- SQS: $0/month (free tier)
- **Total after free tier:** $7-22/month

**Savings:** ~$1.50-16.50/month + much simpler management!

---

## Render Free Tier Limitations

⚠️ **Important:** Render free tier has limitations:

1. **Sleeps after 15 minutes of inactivity**
   - First request after sleep takes 30-60 seconds (cold start)
   - Solution: Upgrade to Starter ($7/month) for always-on

2. **750 hours/month limit**
   - Enough for testing, not for production
   - Solution: Upgrade to Starter

3. **Shared CPU**
   - May be slower under load
   - Solution: Upgrade to Standard ($25/month)

**Recommendation:** Start with Free tier for testing, upgrade to Starter ($7/month) for production.

---

## Auto-Deploy from Git

✨ **Best Feature:** Every time you push to GitHub, Render auto-deploys!

```bash
# Make changes
git add .
git commit -m "Update feature"
git push origin main

# Render automatically:
# 1. Detects push
# 2. Builds new JAR
# 3. Deploys with zero downtime
# 4. Notifies you via email
```

---

## Monitoring and Logs

### View Logs
1. Render Dashboard → Your service → **Logs** tab
2. Real-time logs (like `tail -f`)
3. Search and filter

### Metrics
1. Render Dashboard → Your service → **Metrics** tab
2. See:
   - CPU usage
   - Memory usage
   - Request count
   - Response times

### Alerts
1. Render Dashboard → Your service → **Settings** → **Notifications**
2. Configure email alerts for:
   - Deploy failures
   - Service crashes
   - High resource usage

---

## Troubleshooting

### Issue 1: Build Fails

**Check:**
- Render logs for Maven errors
- Ensure `mvnw` has execute permissions
- Verify Java 21 is specified

**Fix:**
```bash
# Make mvnw executable
git update-index --chmod=+x backend/mvnw
git commit -m "Make mvnw executable"
git push
```

### Issue 2: Can't Connect to RDS

**Check:**
- RDS security group allows `0.0.0.0/0`
- RDS is publicly accessible
- Environment variables are correct

**Test connection:**
```bash
# From Render Shell (Dashboard → Shell tab)
nc -zv your-rds-endpoint.rds.amazonaws.com 5432
```

### Issue 3: SQS Access Denied

**Check:**
- AWS credentials are correct
- IAM user has `AmazonSQSFullAccess` policy
- Region matches SQS queue region

**Test:**
```bash
# From Render Shell
aws sqs list-queues --region eu-north-1
```

### Issue 4: App Crashes on Startup

**Check Render logs for:**
- Database connection errors
- Missing environment variables
- Port binding issues

**Common fixes:**
- Ensure `server.port=${PORT:8080}` in application.properties
- Verify all required env vars are set
- Check Redis connection

---

## Performance Optimization

### 1. Enable HTTP/2
Already enabled by default on Render.

### 2. Use Render Redis
Faster than external Redis (same datacenter).

### 3. Optimize JVM Settings

Add to environment variables:
```
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

### 4. Enable Compression

Add to `application.properties`:
```properties
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
```

---

## Security Best Practices

### 1. Use Environment Variables
✅ Already doing this - never commit secrets!

### 2. Rotate AWS Credentials
Create a dedicated IAM user for Render:
```bash
# AWS Console → IAM → Users → Create user
# Attach policies: AmazonSQSFullAccess
# Generate access keys
# Update Render env vars
```

### 3. Enable HTTPS Only
Render provides free SSL - always use `https://` URLs.

### 4. Restrict CORS
Update `CORS_ALLOWED_ORIGINS` to only your frontend domain.

### 5. Use Secrets Management
For sensitive data, consider:
- AWS Secrets Manager
- Render Secret Files (paid feature)

---

## Scaling on Render

### Horizontal Scaling (Multiple Instances)

Render Starter plan and above:
1. Dashboard → Your service → **Settings**
2. **Scaling** → Set number of instances
3. Render load balances automatically

### Vertical Scaling (Bigger Instances)

Upgrade instance type:
- Free: 512MB RAM, 0.1 CPU
- Starter: 512MB RAM, 0.5 CPU
- Standard: 2GB RAM, 1 CPU
- Pro: 4GB RAM, 2 CPU

---

## Backup Strategy

### Database Backups
RDS handles this automatically (if enabled):
1. RDS → Databases → Your DB → Maintenance & backups
2. Enable automated backups
3. Set retention period (7 days recommended)

### Application Backups
Your code is in Git - that's your backup! 🎉

---

## Next Steps

✅ **Backend is now on Render!**

**What's next:**
1. ✅ Terminate EC2 instance (done in Step 8)
2. 🔄 Deploy frontend to Vercel/Netlify
3. 🔄 Update frontend API URL
4. 🔄 Test end-to-end flow
5. 📊 Set up monitoring alerts
6. 🚀 (Optional) Custom domain with SSL

---

## Quick Reference

**Render Dashboard:** https://dashboard.render.com  
**Backend URL:** `https://codebase-qa-backend.onrender.com`  
**Health Check:** `https://codebase-qa-backend.onrender.com/actuator/health`  
**GitHub OAuth:** `https://codebase-qa-backend.onrender.com/api/auth/github`

**View Logs:**
```bash
# Render Dashboard → Logs tab (real-time)
```

**Redeploy:**
```bash
# Option 1: Push to GitHub (auto-deploys)
git push origin main

# Option 2: Manual deploy
# Render Dashboard → Manual Deploy → Deploy latest commit
```

**Shell Access:**
```bash
# Render Dashboard → Shell tab
# Run commands directly on your instance
```

---

## Support Resources

- **Render Docs:** https://render.com/docs
- **Render Community:** https://community.render.com
- **AWS RDS Docs:** https://docs.aws.amazon.com/rds
- **AWS SQS Docs:** https://docs.aws.amazon.com/sqs

---

## Migration Checklist

- [ ] Create `render.yaml` configuration
- [ ] Update `application.properties` for PORT variable
- [ ] Commit and push to GitHub
- [ ] Update RDS security group
- [ ] Create Render web service
- [ ] Configure all environment variables
- [ ] Set up Render Redis (optional)
- [ ] Update GitHub OAuth callback URL
- [ ] Verify deployment and health check
- [ ] Test GitHub OAuth flow
- [ ] Update frontend API URL
- [ ] Test end-to-end functionality
- [ ] Stop/terminate EC2 instance
- [ ] Clean up EC2 resources (security groups, IAM roles)
- [ ] Set up monitoring alerts
- [ ] Document new deployment process

---

**Congratulations! You've simplified your deployment! 🎉**

No more SSH, no more server management, just push to Git and deploy! 🚀
