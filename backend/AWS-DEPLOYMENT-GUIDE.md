# AWS Deployment Guide - Step by Step

**Project:** Codebase Q&A Backend  
**Date:** May 24, 2026  
**Estimated Time:** 2-3 hours

---

## Prerequisites

Before starting, ensure you have:
- [ ] AWS Account (free tier eligible)
- [ ] GitHub OAuth App credentials
- [ ] Gemini API key (from https://aistudio.google.com/app/apikey)
- [ ] Basic understanding of AWS console

---

## STEP 1: Install AWS CLI (5 minutes)

### Windows Installation:

**Option A: Using MSI Installer (Recommended)**
1. Download: https://awscli.amazonaws.com/AWSCLIV2.msi
2. Run the installer
3. Restart your terminal
4. Verify: `aws --version`

**Option B: Using Chocolatey**
```powershell
choco install awscli
```

### Configure AWS CLI:

```bash
aws configure
```

You'll be prompted for:
- **AWS Access Key ID:** Get from AWS Console → IAM → Users → Security credentials
- **AWS Secret Access Key:** Get from same place
- **Default region:** `us-east-1` (recommended for free tier)
- **Default output format:** `json`

---

## STEP 2: Create RDS PostgreSQL Instance (15 minutes)

### 2.1 Go to AWS Console

1. Login to https://console.aws.amazon.com
2. Search for "RDS" in the top search bar
3. Click "Create database"

### 2.2 Database Configuration

**Engine options:**
- Engine type: `PostgreSQL`
- Engine version: `15.4` or latest 15.x
- Templates: `Free tier` ✅

**Settings:**
- DB instance identifier: `codebase-qa-db`
- Master username: `postgres`
- Master password: `[Create a strong password]` (Save this!)
- Confirm password: `[Same password]`

**Instance configuration:**
- DB instance class: `db.t3.micro` (Free tier)

**Storage:**
- Storage type: `General Purpose SSD (gp2)`
- Allocated storage: `20 GiB`
- ❌ Uncheck "Enable storage autoscaling"

**Connectivity:**
- Compute resource: `Don't connect to an EC2 compute resource`
- VPC: `Default VPC`
- Public access: `Yes` ✅ (Important!)
- VPC security group: `Create new`
- New VPC security group name: `codebase-qa-db-sg`

**Database authentication:**
- Password authentication ✅

**Additional configuration:**
- Initial database name: `codebaseqa`
- ❌ Uncheck "Enable automated backups" (to stay in free tier)
- ❌ Uncheck "Enable encryption"

**Click "Create database"** (Takes 5-10 minutes)

### 2.3 Configure Security Group

While RDS is creating:

1. Go to **EC2 → Security Groups**
2. Find `codebase-qa-db-sg`
3. Click **Edit inbound rules**
4. Add rule:
   - Type: `PostgreSQL`
   - Port: `5432`
   - Source: `0.0.0.0/0` (Allow from anywhere - for testing)
   - Description: `PostgreSQL access`
5. **Save rules**

⚠️ **Note:** For production, restrict to your EC2 security group only!

### 2.4 Enable pgvector Extension

Once RDS is available:

1. Get the endpoint: RDS → Databases → `codebase-qa-db` → Connectivity & security → Endpoint
2. Connect using any PostgreSQL client (DBeaver, pgAdmin, or psql):

```bash
psql -h your-rds-endpoint.rds.amazonaws.com -U postgres -d codebaseqa
```

3. Run this SQL:

```sql
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify
SELECT * FROM pg_extension WHERE extname = 'vector';
```

**Save the RDS endpoint!** You'll need it later.

---

## STEP 3: Create SQS Queues (10 minutes)

### 3.1 Create Dead Letter Queue (DLQ)

1. Go to **SQS** in AWS Console
2. Click **Create queue**
3. Configuration:
   - Type: `Standard`
   - Name: `codebase-qa-indexing-dlq`
   - Configuration: Leave defaults
4. **Create queue**
5. **Copy the Queue URL** (you'll need it)

### 3.2 Create Main Queue

1. Click **Create queue** again
2. Configuration:
   - Type: `Standard`
   - Name: `codebase-qa-indexing`
   - Visibility timeout: `600 seconds` (10 minutes)
   - Message retention period: `1 day`
   - Delivery delay: `0 seconds`
   - Maximum message size: `256 KB`
   - Receive message wait time: `0 seconds`

3. **Dead-letter queue:**
   - ✅ Enable
   - Choose queue: `codebase-qa-indexing-dlq`
   - Maximum receives: `3`

4. **Create queue**
5. **Copy the Queue URL** (you'll need it)

**Save both URLs:**
- Main Queue: `https://sqs.us-east-1.amazonaws.com/YOUR-ACCOUNT-ID/codebase-qa-indexing`
- DLQ: `https://sqs.us-east-1.amazonaws.com/YOUR-ACCOUNT-ID/codebase-qa-indexing-dlq`

---

## STEP 4: Create IAM Role for EC2 (10 minutes)

### 4.1 Create Role

1. Go to **IAM → Roles**
2. Click **Create role**
3. Trusted entity type: `AWS service`
4. Use case: `EC2`
5. Click **Next**

### 4.2 Attach Policies

Search and select these policies:
- ✅ `AmazonSQSFullAccess` (for SQS operations)
- ✅ `AmazonSSMReadOnlyAccess` (for reading secrets)

Click **Next**

### 4.3 Name and Create

- Role name: `codebase-qa-ec2-role`
- Description: `Role for Codebase QA EC2 instance to access SQS and SSM`
- Click **Create role**

---

## STEP 5: Store Secrets in SSM Parameter Store (10 minutes)

Go to **Systems Manager → Parameter Store**

Create these parameters (click "Create parameter" for each):

### 5.1 Database Password

- Name: `/codebase-qa/db-password`
- Type: `SecureString`
- Value: `[Your RDS password from Step 2]`
- Click **Create parameter**

### 5.2 JWT Secret

- Name: `/codebase-qa/jwt-secret`
- Type: `SecureString`
- Value: `[Generate a random 32+ character string]`
- Click **Create parameter**

**Generate JWT secret:**
```bash
# Windows PowerShell
-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | % {[char]$_})
```

### 5.3 GitHub OAuth Credentials

**First, create GitHub OAuth App:**
1. Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Application name: `Codebase QA`
3. Homepage URL: `http://localhost:8080` (we'll update this later)
4. Authorization callback URL: `http://YOUR-EC2-IP:8080/api/auth/github/callback` (we'll update this later)
5. Click **Register application**
6. Copy **Client ID** and generate **Client Secret**

**Now store in SSM:**

- Name: `/codebase-qa/github-client-id`
- Type: `SecureString`
- Value: `[Your GitHub Client ID]`

- Name: `/codebase-qa/github-client-secret`
- Type: `SecureString`
- Value: `[Your GitHub Client Secret]`

### 5.4 Gemini API Key

- Name: `/codebase-qa/gemini-api-key`
- Type: `SecureString`
- Value: `[Your Gemini API key]`

### 5.5 Voyage API Key (Optional)

- Name: `/codebase-qa/voyage-api-key`
- Type: `SecureString`
- Value: `[Your Voyage API key if you have one]`

---

## STEP 6: Launch EC2 Instance (15 minutes)

### 6.1 Go to EC2

1. Go to **EC2** in AWS Console
2. Click **Launch instance**

### 6.2 Configure Instance

**Name and tags:**
- Name: `codebase-qa-backend`

**Application and OS Images (Amazon Machine Image):**
- Quick Start: `Amazon Linux`
- Amazon Machine Image (AMI): `Amazon Linux 2023 AMI` (Free tier eligible)

**Instance type:**
- Instance type: `t2.micro` or `t3.micro` (Free tier eligible)

**Key pair (login):**
- Click **Create new key pair**
- Key pair name: `codebase-qa-key`
- Key pair type: `RSA`
- Private key file format: `.pem` (for SSH) or `.ppk` (for PuTTY)
- Click **Create key pair**
- **Save the downloaded file!** You'll need it to SSH

**Network settings:**
- Click **Edit**
- VPC: `Default VPC`
- Subnet: `No preference`
- Auto-assign public IP: `Enable`
- Firewall (security groups): `Create security group`
- Security group name: `codebase-qa-sg`
- Description: `Security group for Codebase QA backend`

**Inbound security group rules:**
- Rule 1: SSH (Port 22) - Source: `My IP` (for SSH access)
- Click **Add security group rule**
- Rule 2: Custom TCP (Port 8080) - Source: `0.0.0.0/0` (for API access)
- Click **Add security group rule**
- Rule 3: HTTPS (Port 443) - Source: `0.0.0.0/0` (optional, for future)

**Configure storage:**
- Size: `8 GiB` (Free tier)
- Volume type: `gp2`

**Advanced details:**
- IAM instance profile: `codebase-qa-ec2-role` ✅ (Important!)

### 6.3 Launch Instance

1. Click **Launch instance**
2. Wait for instance to be in `Running` state
3. **Copy the Public IPv4 address** (you'll need it)

---

## STEP 7: Connect to EC2 and Setup (20 minutes)

### 7.1 Connect via SSH

**Windows (using PowerShell):**

```powershell
# Navigate to where you saved the key
cd Downloads

# Set permissions (if needed)
icacls codebase-qa-key.pem /inheritance:r
icacls codebase-qa-key.pem /grant:r "%username%:R"

# Connect
ssh -i codebase-qa-key.pem ec2-user@YOUR-EC2-PUBLIC-IP
```

**Or use EC2 Instance Connect:**
1. Go to EC2 → Instances
2. Select your instance
3. Click **Connect** → **EC2 Instance Connect** → **Connect**

### 7.2 Install Java 21

```bash
# Update system
sudo yum update -y

# Install Java 21 (Amazon Corretto)
sudo yum install -y java-21-amazon-corretto-devel

# Verify
java -version
# Should show: openjdk version "21.x.x"

# Install Git (needed for JGit)
sudo yum install -y git

# Verify
git --version
```

### 7.3 Create Application Directory

```bash
# Create app directory
sudo mkdir -p /opt/codebase-qa
sudo chown ec2-user:ec2-user /opt/codebase-qa

# Create temp directory for repo cloning
sudo mkdir -p /tmp/codebase-qa
sudo chown ec2-user:ec2-user /tmp/codebase-qa

# Verify
ls -la /opt/codebase-qa
ls -la /tmp/codebase-qa
```

---

## STEP 8: Build and Deploy Application (15 minutes)

### 8.1 Build JAR Locally

On your **local machine** (Windows):

```powershell
cd d:\Projects\CodeBaseQA\backend

# Build the JAR
.\mvnw.cmd clean package -DskipTests

# Verify JAR was created
ls target\backend-0.0.1-SNAPSHOT.jar
```

### 8.2 Copy JAR to EC2

```powershell
# Still on your local machine
scp -i Downloads\codebase-qa-key.pem target\backend-0.0.1-SNAPSHOT.jar ec2-user@YOUR-EC2-PUBLIC-IP:/opt/codebase-qa/app.jar
```

### 8.3 Create Startup Script on EC2

SSH back into EC2 and create the startup script:

```bash
nano /opt/codebase-qa/start.sh
```

Paste this content (replace placeholders):

```bash
#!/bin/bash

# Fetch secrets from SSM Parameter Store
export DATABASE_URL="jdbc:postgresql://YOUR-RDS-ENDPOINT:5432/codebaseqa"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD=$(aws ssm get-parameter --name /codebase-qa/db-password --with-decryption --query Parameter.Value --output text --region us-east-1)

export JWT_SECRET=$(aws ssm get-parameter --name /codebase-qa/jwt-secret --with-decryption --query Parameter.Value --output text --region us-east-1)

export GITHUB_CLIENT_ID=$(aws ssm get-parameter --name /codebase-qa/github-client-id --with-decryption --query Parameter.Value --output text --region us-east-1)
export GITHUB_CLIENT_SECRET=$(aws ssm get-parameter --name /codebase-qa/github-client-secret --with-decryption --query Parameter.Value --output text --region us-east-1)
export GITHUB_REDIRECT_URI="http://YOUR-EC2-PUBLIC-IP:8080/api/auth/github/callback"

export GEMINI_API_KEY=$(aws ssm get-parameter --name /codebase-qa/gemini-api-key --with-decryption --query Parameter.Value --output text --region us-east-1)

export AWS_REGION="us-east-1"
export SQS_QUEUE_URL="https://sqs.us-east-1.amazonaws.com/YOUR-ACCOUNT-ID/codebase-qa-indexing"
export SQS_DLQ_URL="https://sqs.us-east-1.amazonaws.com/YOUR-ACCOUNT-ID/codebase-qa-indexing-dlq"

export REDIS_URL="redis://127.0.0.1:6379"
export CORS_ALLOWED_ORIGINS="http://YOUR-EC2-PUBLIC-IP:8080,http://localhost:5173"
export FRONTEND_URL="http://YOUR-EC2-PUBLIC-IP:8080"

# Start the application
java -jar /opt/codebase-qa/app.jar
```

**Replace these placeholders:**
- `YOUR-RDS-ENDPOINT` - From Step 2.4
- `YOUR-EC2-PUBLIC-IP` - From Step 6.3
- `YOUR-ACCOUNT-ID` - Your AWS account ID (12 digits)

Save and exit (Ctrl+X, Y, Enter)

Make it executable:

```bash
chmod +x /opt/codebase-qa/start.sh
```

---

## STEP 9: Install and Configure Redis (10 minutes)

```bash
# Install Redis
sudo yum install -y redis6

# Start Redis
sudo systemctl start redis6
sudo systemctl enable redis6

# Verify
redis-cli ping
# Should return: PONG
```

---

## STEP 10: Create Systemd Service (10 minutes)

```bash
sudo nano /etc/systemd/system/codebase-qa.service
```

Paste this content:

```ini
[Unit]
Description=Codebase QA Backend
After=network.target redis6.service

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/codebase-qa
ExecStart=/bin/bash /opt/codebase-qa/start.sh
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Save and exit (Ctrl+X, Y, Enter)

### Enable and Start Service

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service (start on boot)
sudo systemctl enable codebase-qa

# Start service
sudo systemctl start codebase-qa

# Check status
sudo systemctl status codebase-qa

# View logs
sudo journalctl -u codebase-qa -f
```

---

## STEP 11: Verify Deployment (10 minutes)

### 11.1 Check Health Endpoint

From your local machine:

```bash
curl http://YOUR-EC2-PUBLIC-IP:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 11.2 Test GitHub OAuth

1. Open browser: `http://YOUR-EC2-PUBLIC-IP:8080/api/auth/github`
2. Should redirect to GitHub OAuth page
3. Authorize the app
4. Should redirect back and return JWT token

### 11.3 Update GitHub OAuth App

1. Go to GitHub → Settings → Developer settings → OAuth Apps
2. Edit your app
3. Update **Authorization callback URL** to: `http://YOUR-EC2-PUBLIC-IP:8080/api/auth/github/callback`
4. Save changes

### 11.4 Test API Endpoints

```bash
# Get JWT token first (from browser OAuth flow)
export JWT_TOKEN="your-jwt-token-here"

# Test protected endpoint
curl -H "Authorization: Bearer $JWT_TOKEN" http://YOUR-EC2-PUBLIC-IP:8080/api/auth/me

# Test repos endpoint
curl -H "Authorization: Bearer $JWT_TOKEN" http://YOUR-EC2-PUBLIC-IP:8080/api/repos
```

---

## STEP 12: Monitor and Troubleshoot

### View Application Logs

```bash
# Real-time logs
sudo journalctl -u codebase-qa -f

# Last 100 lines
sudo journalctl -u codebase-qa -n 100

# Logs from last hour
sudo journalctl -u codebase-qa --since "1 hour ago"
```

### Common Issues

**Issue 1: Service won't start**
```bash
# Check if JAR exists
ls -la /opt/codebase-qa/app.jar

# Check if start script is executable
ls -la /opt/codebase-qa/start.sh

# Test start script manually
/opt/codebase-qa/start.sh
```

**Issue 2: Can't connect to RDS**
```bash
# Test RDS connection
psql -h YOUR-RDS-ENDPOINT -U postgres -d codebaseqa

# Check security group allows EC2 IP
```

**Issue 3: SSM parameters not found**
```bash
# Test IAM role
aws sts get-caller-identity

# Test SSM access
aws ssm get-parameter --name /codebase-qa/db-password --with-decryption --region us-east-1
```

### Restart Service

```bash
sudo systemctl restart codebase-qa
sudo systemctl status codebase-qa
```

---

## STEP 13: Cost Optimization

### Free Tier Limits

- **EC2:** 750 hours/month (t2.micro or t3.micro)
- **RDS:** 750 hours/month (db.t3.micro), 20GB storage
- **SQS:** 1 million requests/month
- **Data Transfer:** 15GB/month outbound

### Stop Resources When Not Using

```bash
# Stop EC2 instance (from AWS Console or CLI)
aws ec2 stop-instances --instance-ids i-YOUR-INSTANCE-ID

# Stop RDS (from AWS Console)
# RDS → Databases → Select → Actions → Stop
```

---

## Next Steps

✅ Backend is deployed and running on AWS!

**What's next:**
1. Deploy frontend to Vercel (Task 3.5)
2. Update frontend to point to EC2 backend
3. Test end-to-end flow
4. (Optional) Set up custom domain with HTTPS
5. (Optional) Set up CloudWatch monitoring

---

## Quick Reference

**EC2 Public IP:** `YOUR-EC2-PUBLIC-IP`  
**Backend URL:** `http://YOUR-EC2-PUBLIC-IP:8080`  
**Health Check:** `http://YOUR-EC2-PUBLIC-IP:8080/actuator/health`  
**GitHub OAuth:** `http://YOUR-EC2-PUBLIC-IP:8080/api/auth/github`

**SSH Command:**
```bash
ssh -i codebase-qa-key.pem ec2-user@YOUR-EC2-PUBLIC-IP
```

**View Logs:**
```bash
sudo journalctl -u codebase-qa -f
```

**Restart Service:**
```bash
sudo systemctl restart codebase-qa
```

---

## Support

If you encounter issues:
1. Check application logs: `sudo journalctl -u codebase-qa -f`
2. Verify all environment variables are set correctly
3. Check security groups allow traffic on port 8080
4. Verify IAM role has correct permissions
5. Test RDS connectivity from EC2

**Good luck with your deployment! 🚀**
