# Render Migration Checklist

**From:** EC2 + RDS + SQS  
**To:** Render + RDS + SQS  
**Date:** May 24, 2026

---

## Pre-Migration (Already Done ✅)

- [x] RDS PostgreSQL instance running (eu-north-1)
- [x] SQS queues created (codebaseqa-indexing-queue)
- [x] AWS credentials (Access Key + Secret Key)
- [x] GitHub OAuth App configured
- [x] Gemini & Voyage API keys

---

## Migration Steps

### Phase 1: Prepare Code (5 minutes)

- [ ] **1.1** Verify `render.yaml` exists in `backend/` folder
- [ ] **1.2** Verify `application.yml` has `port: ${PORT:8080}`
- [ ] **1.3** Commit changes to Git:
  ```bash
  cd d:\Projects\CodeBaseQA
  git add backend/render.yaml backend/src/main/resources/application.yml
  git commit -m "Add Render deployment configuration"
  git push origin main
  ```

### Phase 2: Configure AWS (5 minutes)

- [ ] **2.1** Update RDS Security Group:
  - Go to AWS Console → EC2 → Security Groups
  - Find: `codebase-qa-db-sg`
  - Edit Inbound Rules → PostgreSQL (5432)
  - Change Source to: `0.0.0.0/0`
  - Save

- [ ] **2.2** Verify RDS is publicly accessible:
  - RDS → Databases → codebase-qa-db
  - Check: "Publicly accessible: Yes"

### Phase 3: Deploy to Render (15 minutes)

- [ ] **3.1** Sign up at https://render.com (use GitHub login)

- [ ] **3.2** Create New Web Service:
  - Click: New + → Web Service
  - Connect repository: CodeBaseQA
  - Name: `codebase-qa-backend`
  - Region: Europe (closest to eu-north-1)
  - Branch: `main`
  - Root Directory: `backend`
  - Runtime: `Java`
  - Build Command: `./mvnw clean package -DskipTests`
  - Start Command: `java -jar target/backend-0.0.1-SNAPSHOT.jar`
  - Instance Type: `Free` (or `Starter` for $7/month)

- [ ] **3.3** Add Environment Variables (copy from your `.env`):

**Database:**
```
DB_HOST=codebaseqa-db.c9abc123xyz.eu-north-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=codebaseqa
DB_USERNAME=postgres
DB_PASSWORD=your-actual-rds-password
```

**Redis:**
```
REDIS_URL=redis://localhost:6379
```
*(Will set up Render Redis in next step)*

**JWT:**
```
JWT_SECRET=your-super-secret-key-minimum-32-characters-long-change-this-in-production
```

**GitHub OAuth:**
```
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GITHUB_REDIRECT_URI=https://codebase-qa-backend.onrender.com/api/auth/github/callback
```
*(Update URL after deployment)*

**AWS:**
```
AWS_REGION=eu-north-1
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
SQS_QUEUE_URL=https://sqs.eu-north-1.amazonaws.com/YOUR-ACCOUNT-ID/codebaseqa-indexing-queue
SQS_DLQ_URL=
```

**APIs:**
```
GEMINI_API_KEY=your-gemini-api-key
VOYAGE_API_KEY=your-voyage-api-key
```

**App:**
```
FRONTEND_URL=http://localhost:5173
INDEXING_TEMP_DIR=/tmp/codebase-qa
```

- [ ] **3.4** Click "Create Web Service"
- [ ] **3.5** Wait 5-10 minutes for deployment
- [ ] **3.6** Copy your Render URL: `https://codebase-qa-backend.onrender.com`

### Phase 4: Set Up Redis (5 minutes)

**Option A: Render Redis (Recommended)**
- [ ] **4.1** Render Dashboard → New + → Redis
- [ ] **4.2** Name: `codebase-qa-redis`, Region: Same as web service
- [ ] **4.3** Plan: Free (25MB)
- [ ] **4.4** Copy Internal Redis URL: `redis://red-xxx:6379`
- [ ] **4.5** Update web service env var: `REDIS_URL=redis://red-xxx:6379`

**Option B: Upstash Redis**
- [ ] **4.1** Go to https://upstash.com
- [ ] **4.2** Create free Redis database
- [ ] **4.3** Copy Redis URL
- [ ] **4.4** Update Render env var: `REDIS_URL=<upstash-url>`

### Phase 5: Update GitHub OAuth (2 minutes)

- [ ] **5.1** Go to GitHub → Settings → Developer settings → OAuth Apps
- [ ] **5.2** Select: `Codebase QA`
- [ ] **5.3** Update:
  - Homepage URL: `https://codebase-qa-backend.onrender.com`
  - Callback URL: `https://codebase-qa-backend.onrender.com/api/auth/github/callback`
- [ ] **5.4** Save changes
- [ ] **5.5** Update Render env var:
  - `GITHUB_REDIRECT_URI=https://codebase-qa-backend.onrender.com/api/auth/github/callback`
  - Save (will trigger redeploy)

### Phase 6: Verify Deployment (5 minutes)

- [ ] **6.1** Test health endpoint:
  ```bash
  curl https://codebase-qa-backend.onrender.com/actuator/health
  ```
  Expected: `{"status":"UP"}`

- [ ] **6.2** Test GitHub OAuth:
  - Open: `https://codebase-qa-backend.onrender.com/api/auth/github`
  - Should redirect to GitHub
  - Authorize
  - Should return JWT token

- [ ] **6.3** Check Render logs:
  - Dashboard → Logs tab
  - Look for: "Started CodebaseQaApplication"

### Phase 7: Update Frontend (2 minutes)

- [ ] **7.1** Update `frontend/.env`:
  ```env
  VITE_API_URL=https://codebase-qa-backend.onrender.com
  ```

- [ ] **7.2** Commit and push:
  ```bash
  git add frontend/.env
  git commit -m "Update API URL to Render"
  git push origin main
  ```

- [ ] **7.3** If on Vercel, wait for auto-deploy

### Phase 8: Test End-to-End (5 minutes)

- [ ] **8.1** Open frontend in browser
- [ ] **8.2** Test login with GitHub
- [ ] **8.3** Connect a repository
- [ ] **8.4** Ask a question
- [ ] **8.5** Verify answer is generated

### Phase 9: Terminate EC2 (5 minutes)

⚠️ **Only do this after confirming Render works!**

- [ ] **9.1** AWS Console → EC2 → Instances
- [ ] **9.2** Select: `codebase-qa-backend`
- [ ] **9.3** Instance state → Stop instance (or Terminate)

**Optional: Clean up EC2 resources**
- [ ] **9.4** Delete Security Group: `codebase-qa-sg`
- [ ] **9.5** Delete Key Pair: `codebase-qa-key`
- [ ] **9.6** Delete IAM Role: `codebase-qa-ec2-role`
- [ ] **9.7** Delete SSM Parameters: `/codebase-qa/*`

---

## Post-Migration

### Monitoring
- [ ] Set up Render email alerts (Dashboard → Settings → Notifications)
- [ ] Monitor first 24 hours for any issues
- [ ] Check RDS connections are stable

### Documentation
- [ ] Update README with new deployment URL
- [ ] Document Render deployment process
- [ ] Update team on new deployment workflow

### Optional Upgrades
- [ ] Consider Render Starter plan ($7/month) for no-sleep
- [ ] Set up custom domain
- [ ] Configure SSL certificate (free on Render)
- [ ] Set up monitoring with Sentry/DataDog

---

## Rollback Plan (If Needed)

If something goes wrong:

1. **Keep EC2 running** until Render is fully verified
2. **Revert frontend `.env`** to EC2 URL
3. **Revert GitHub OAuth** callback to EC2 URL
4. **Debug Render logs** to identify issue
5. **Contact Render support** if needed

---

## Cost Savings

**Before (EC2):**
- EC2: $0-8.50/month
- RDS: $0-15/month
- SQS: $0/month
- **Total:** $0-23.50/month

**After (Render):**
- Render Free: $0/month (with sleep)
- Render Starter: $7/month (no sleep)
- RDS: $0-15/month
- SQS: $0/month
- **Total:** $0-22/month

**Savings:** $1.50-16.50/month + much simpler!

---

## Support

**Render:**
- Docs: https://render.com/docs
- Community: https://community.render.com
- Support: support@render.com

**AWS:**
- RDS Docs: https://docs.aws.amazon.com/rds
- SQS Docs: https://docs.aws.amazon.com/sqs

---

## Notes

- Render free tier sleeps after 15 min inactivity (30-60s cold start)
- Upgrade to Starter ($7/month) for always-on
- Auto-deploy on every Git push
- Zero-downtime deployments
- Free SSL certificates
- Automatic health checks

---

**Good luck with your migration! 🚀**
