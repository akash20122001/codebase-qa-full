# AWS Deployment Checklist

Use this checklist to track your deployment progress.

---

## Pre-Deployment

- [ ] AWS Account created
- [ ] GitHub OAuth App credentials ready
- [ ] Gemini API key obtained
- [ ] AWS CLI installed and configured

---

## AWS Resources

### RDS PostgreSQL
- [ ] RDS instance created (`codebase-qa-db`)
- [ ] Security group configured (port 5432 open)
- [ ] pgvector extension enabled
- [ ] RDS endpoint saved: `_______________________________`
- [ ] Database password saved securely

### SQS Queues
- [ ] Dead Letter Queue created (`codebase-qa-indexing-dlq`)
- [ ] Main queue created (`codebase-qa-indexing`)
- [ ] DLQ configured on main queue (max receives: 3)
- [ ] Main queue URL saved: `_______________________________`
- [ ] DLQ URL saved: `_______________________________`

### IAM Role
- [ ] EC2 role created (`codebase-qa-ec2-role`)
- [ ] SQS permissions attached
- [ ] SSM read permissions attached

### SSM Parameter Store
- [ ] `/codebase-qa/db-password` created
- [ ] `/codebase-qa/jwt-secret` created
- [ ] `/codebase-qa/github-client-id` created
- [ ] `/codebase-qa/github-client-secret` created
- [ ] `/codebase-qa/gemini-api-key` created

### EC2 Instance
- [ ] Instance launched (`codebase-qa-backend`)
- [ ] Instance type: t2.micro or t3.micro
- [ ] IAM role attached
- [ ] Security group configured (ports 22, 8080)
- [ ] Key pair downloaded and saved
- [ ] Public IP saved: `_______________________________`

---

## EC2 Setup

- [ ] Connected to EC2 via SSH
- [ ] Java 21 installed
- [ ] Git installed
- [ ] Application directory created (`/opt/codebase-qa`)
- [ ] Temp directory created (`/tmp/codebase-qa`)
- [ ] Redis installed and running

---

## Application Deployment

- [ ] JAR built locally (`mvn clean package`)
- [ ] JAR copied to EC2 (`/opt/codebase-qa/app.jar`)
- [ ] Startup script created (`/opt/codebase-qa/start.sh`)
- [ ] Startup script configured with correct values:
  - [ ] RDS endpoint
  - [ ] EC2 public IP
  - [ ] AWS account ID
  - [ ] SQS queue URLs
- [ ] Startup script made executable
- [ ] Systemd service created (`/etc/systemd/system/codebase-qa.service`)
- [ ] Service enabled and started

---

## Verification

- [ ] Service is running: `sudo systemctl status codebase-qa`
- [ ] Health endpoint responds: `curl http://YOUR-IP:8080/actuator/health`
- [ ] GitHub OAuth flow works
- [ ] GitHub OAuth callback URL updated
- [ ] Can authenticate and get JWT token
- [ ] Protected endpoints work with JWT
- [ ] Can connect a repository
- [ ] Indexing job created in SQS
- [ ] Worker processes indexing job
- [ ] Can query the repository

---

## Post-Deployment

- [ ] Application logs reviewed (no errors)
- [ ] All endpoints tested with Postman
- [ ] Frontend updated to point to EC2 backend
- [ ] End-to-end flow tested
- [ ] Documentation updated with EC2 IP

---

## Optional Enhancements

- [ ] Custom domain configured
- [ ] HTTPS/SSL certificate installed
- [ ] CloudWatch monitoring set up
- [ ] CloudWatch alarms configured
- [ ] Automated backups configured
- [ ] CI/CD pipeline set up

---

## Important URLs

**Backend:** `http://YOUR-EC2-IP:8080`  
**Health:** `http://YOUR-EC2-IP:8080/actuator/health`  
**OAuth:** `http://YOUR-EC2-IP:8080/api/auth/github`  
**API Docs:** `http://YOUR-EC2-IP:8080/api`

---

## Important Commands

**SSH to EC2:**
```bash
ssh -i codebase-qa-key.pem ec2-user@YOUR-EC2-IP
```

**View logs:**
```bash
sudo journalctl -u codebase-qa -f
```

**Restart service:**
```bash
sudo systemctl restart codebase-qa
```

**Check service status:**
```bash
sudo systemctl status codebase-qa
```

**Test SSM access:**
```bash
aws ssm get-parameter --name /codebase-qa/db-password --with-decryption --region us-east-1
```

---

## Troubleshooting

If something doesn't work:

1. **Check logs:** `sudo journalctl -u codebase-qa -f`
2. **Verify environment variables** in `/opt/codebase-qa/start.sh`
3. **Check security groups** (ports 22, 8080, 5432)
4. **Test RDS connection** from EC2
5. **Verify IAM role** has correct permissions
6. **Check SSM parameters** are accessible

---

**Deployment Date:** _______________  
**Deployed By:** _______________  
**EC2 Instance ID:** _______________  
**RDS Instance ID:** _______________
