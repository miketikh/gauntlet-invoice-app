# Deployment Runbook

## Overview

This runbook provides step-by-step procedures for deploying InvoiceMe to production environments. Follow these procedures carefully to ensure successful deployments.

## Pre-Deployment Checklist

Before starting deployment, verify:

- [ ] All tests passing (backend, frontend, E2E)
- [ ] Code review completed and approved
- [ ] Database migrations prepared and tested
- [ ] Environment variables configured
- [ ] Backup of current production database taken
- [ ] Deployment window scheduled and communicated
- [ ] Rollback plan prepared
- [ ] Monitoring and alerts configured

## Deployment Environments

| Environment | Purpose | URL | Database |
|-------------|---------|-----|----------|
| Development | Local development | http://localhost:3000 | Local PostgreSQL |
| Staging | Pre-production testing | https://staging.invoiceme.com | Staging DB |
| Production | Live system | https://invoiceme.com | Production DB |

## Deployment Steps

### 1. Prepare Release

#### Tag Release

```bash
# Ensure you're on main branch with latest code
git checkout main
git pull origin main

# Create release tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

#### Build Artifacts

**Backend:**
```bash
cd backend
mvn clean package -DskipTests

# Verify JAR created
ls -lh target/invoice-me-*.jar
```

**Frontend:**
```bash
npm run build

# Verify build successful
ls -lh .next
```

### 2. Build Docker Images

```bash
# Build backend image
docker build -t invoice-me-backend:v1.0.0 ./backend

# Build frontend image
docker build -t invoice-me-frontend:v1.0.0 .

# Tag images for registry
docker tag invoice-me-backend:v1.0.0 your-registry/invoice-me-backend:v1.0.0
docker tag invoice-me-frontend:v1.0.0 your-registry/invoice-me-frontend:v1.0.0

# Push to registry
docker push your-registry/invoice-me-backend:v1.0.0
docker push your-registry/invoice-me-frontend:v1.0.0
```

### 3. Database Migration

#### Backup Current Database

```bash
# Production backup
pg_dump -h prod-db-host -U prod_user -d invoiceme > backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup
ls -lh backup_*.sql

# Upload backup to secure storage
aws s3 cp backup_*.sql s3://backups/invoiceme/
```

#### Run Migrations

```bash
# Test migrations on staging first
cd backend
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://staging-db-host/invoiceme

# Verify staging migrations successful
# Then run on production
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db-host/invoiceme
```

**Migration Checklist:**
- [ ] Migrations tested on staging
- [ ] Backup completed
- [ ] Migration scripts reviewed
- [ ] Downtime window (if required) scheduled
- [ ] Monitoring active

### 4. Deploy to Cloud Platform

#### AWS ECS Deployment

```bash
# Update task definition with new image
aws ecs register-task-definition \
  --cli-input-json file://task-definition.json

# Update service
aws ecs update-service \
  --cluster invoice-me-cluster \
  --service invoice-me-backend \
  --task-definition invoice-me-backend:latest \
  --desired-count 2

aws ecs update-service \
  --cluster invoice-me-cluster \
  --service invoice-me-frontend \
  --task-definition invoice-me-frontend:latest \
  --desired-count 2

# Monitor deployment
aws ecs describe-services \
  --cluster invoice-me-cluster \
  --services invoice-me-backend invoice-me-frontend
```

#### Azure Container Instances Deployment

```bash
# Deploy backend
az container create \
  --resource-group invoice-me-rg \
  --name invoice-me-backend \
  --image your-registry/invoice-me-backend:v1.0.0 \
  --cpu 2 \
  --memory 4 \
  --environment-variables \
    DATABASE_URL="jdbc:postgresql://..." \
    JWT_SECRET="..." \
  --ports 8080

# Deploy frontend
az container create \
  --resource-group invoice-me-rg \
  --name invoice-me-frontend \
  --image your-registry/invoice-me-frontend:v1.0.0 \
  --cpu 1 \
  --memory 2 \
  --environment-variables \
    NEXT_PUBLIC_API_URL="https://api.invoiceme.com/api" \
  --ports 3000
```

#### Docker Compose (Single Server)

```bash
# SSH to server
ssh user@production-server

# Pull latest code
cd /opt/invoice-me
git pull origin main

# Pull latest images
docker compose pull

# Stop services
docker compose down

# Start with new images
docker compose up -d

# View logs
docker compose logs -f
```

### 5. Post-Deployment Verification

#### Health Checks

```bash
# Backend health
curl https://api.invoiceme.com/api/actuator/health

# Expected: {"status":"UP"}

# Frontend health
curl https://invoiceme.com

# Expected: HTTP 200 response

# Database connection
curl https://api.invoiceme.com/api/actuator/health/db

# Expected: {"status":"UP"}
```

#### Smoke Tests

1. **Authentication Test:**
   ```bash
   # Login
   curl -X POST https://api.invoiceme.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"test@example.com","password":"testpass"}'

   # Should return JWT token
   ```

2. **Create Customer Test:**
   ```bash
   curl -X POST https://api.invoiceme.com/api/customers \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"name":"Test Customer","email":"test@example.com"}'

   # Should return 201 Created
   ```

3. **List Customers Test:**
   ```bash
   curl https://api.invoiceme.com/api/customers \
     -H "Authorization: Bearer <token>"

   # Should return customer list
   ```

4. **UI Test:**
   - Navigate to https://invoiceme.com
   - Login
   - Create test customer
   - Create test invoice
   - Record test payment

#### Monitoring

Check monitoring dashboards:
- Application performance metrics
- Error rates
- Response times
- Database connections
- CPU and memory usage

### 6. Update Documentation

```bash
# Update deployment history
echo "v1.0.0 - $(date) - Deployed successfully" >> docs/deployment/history.md

# Commit and push
git add docs/deployment/history.md
git commit -m "docs: update deployment history for v1.0.0"
git push origin main
```

### 7. Communication

- [ ] Notify team of successful deployment
- [ ] Update status page (if applicable)
- [ ] Send deployment summary email
- [ ] Document any issues encountered

## Rollback Procedure

If deployment fails or critical issues are discovered:

### Quick Rollback

#### Rollback to Previous Image

```bash
# AWS ECS
aws ecs update-service \
  --cluster invoice-me-cluster \
  --service invoice-me-backend \
  --task-definition invoice-me-backend:previous

# Azure
az container create ... --image your-registry/invoice-me-backend:v0.9.0

# Docker Compose
docker compose down
docker compose up -d invoice-me-backend:v0.9.0
```

#### Rollback Database Migration

```bash
# Only if migration is reversible
cd backend

# Review migration to rollback
cat src/main/resources/db/migration/V2__description.sql

# If safe, execute DOWN migration
psql -h prod-db-host -U prod_user -d invoiceme < down_migration.sql

# Or restore from backup
psql -h prod-db-host -U prod_user -d invoiceme < backup_20241109_120000.sql
```

### Rollback Checklist

- [ ] Identify issue and decision to rollback made
- [ ] Rollback deployed to production
- [ ] Health checks passing
- [ ] Smoke tests passing
- [ ] Team notified
- [ ] Post-mortem scheduled

## Troubleshooting

### Deployment Fails

**Problem:** Container fails to start

**Diagnosis:**
```bash
# Check container logs
docker logs <container-id>

# Check events
aws ecs describe-tasks --cluster invoice-me-cluster --tasks <task-arn>
```

**Solutions:**
- Check environment variables are set correctly
- Verify database connection
- Check image was pushed correctly
- Review application logs

### Health Check Fails

**Problem:** Health endpoint returns unhealthy

**Diagnosis:**
```bash
# Check detailed health
curl https://api.invoiceme.com/api/actuator/health | jq

# Check logs
docker compose logs backend
```

**Solutions:**
- Check database connection
- Verify dependencies are healthy
- Review application logs for errors

### High Error Rates

**Problem:** Error rate spike after deployment

**Diagnosis:**
- Check error logs
- Review monitoring dashboards
- Test affected endpoints manually

**Solutions:**
- Roll back if critical
- Hot-fix if minor issue
- Scale up resources if capacity issue

### Database Migration Issues

**Problem:** Migration fails or corrupts data

**Diagnosis:**
```sql
-- Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

-- Check for data issues
SELECT COUNT(*) FROM customers WHERE created_at > NOW() - INTERVAL '1 hour';
```

**Solutions:**
- Restore from backup
- Fix migration script
- Re-run migration

## Monitoring and Alerts

### Key Metrics

Monitor these metrics post-deployment:

| Metric | Threshold | Action |
|--------|-----------|--------|
| Error Rate | > 1% | Investigate immediately |
| Response Time (P95) | > 1000ms | Investigate |
| CPU Usage | > 80% | Scale up |
| Memory Usage | > 85% | Scale up |
| Database Connections | > 80% of pool | Scale database |
| Failed Health Checks | > 2 consecutive | Rollback |

### Alert Configuration

Ensure alerts are configured for:
- Application errors (5xx responses)
- High response times
- Failed health checks
- Database connection issues
- High resource usage

### Log Monitoring

```bash
# Tail logs in real-time
docker compose logs -f backend

# Search for errors
docker compose logs backend | grep ERROR

# Check for specific exceptions
docker compose logs backend | grep "SQLException\|NullPointerException"
```

## Deployment Schedule

### Recommended Deployment Windows

- **Production:** Tuesday or Wednesday, 10:00 AM - 2:00 PM (low traffic)
- **Avoid:** Fridays, weekends, holidays, month-end
- **Maintenance Window:** Schedule downtime during lowest traffic periods

### Deployment Frequency

- **Hotfixes:** As needed, any time
- **Regular Releases:** Weekly or bi-weekly
- **Major Releases:** Monthly, with extended testing

## Security Checklist

Before deploying to production:

- [ ] All secrets rotated and secured
- [ ] HTTPS enabled with valid certificates
- [ ] CORS configured correctly
- [ ] Rate limiting enabled (if applicable)
- [ ] Security headers configured
- [ ] SQL injection prevention verified
- [ ] XSS protection enabled
- [ ] Authentication and authorization tested
- [ ] Dependency security scan passed
- [ ] Container image scanned for vulnerabilities

## References

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [Azure Container Instances](https://docs.microsoft.com/en-us/azure/container-instances/)
- [Docker Compose](https://docs.docker.com/compose/)
- [Flyway Migrations](https://flywaydb.org/documentation/)

---

## Deployment History Template

```
# Deployment History

## v1.0.0 - 2024-11-09
- **Deployed by:** John Doe
- **Duration:** 15 minutes
- **Issues:** None
- **Rollback:** N/A

## v0.9.0 - 2024-11-02
- **Deployed by:** Jane Smith
- **Duration:** 20 minutes
- **Issues:** Database migration took longer than expected
- **Rollback:** N/A
```

---

**Last Updated:** 2024-11-09
**Version:** 1.0
**Owner:** DevOps Team
