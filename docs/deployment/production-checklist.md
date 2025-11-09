# Production Deployment Checklist

Complete this checklist before deploying InvoiceMe to production.

## Security Checklist

### Authentication & Authorization

- [ ] **Change default JWT secret**
  - Generate a new secret: `openssl rand -hex 32`
  - Store in environment variables or secrets manager
  - Update `JWT_SECRET` in production configuration
  - Verify secret is at least 256 bits (32 bytes)

- [ ] **Use strong database password**
  - Minimum 16 characters
  - Include uppercase, lowercase, numbers, special characters
  - Store in secrets manager (AWS Secrets Manager, Azure Key Vault)
  - Never commit passwords to version control

- [ ] **Configure CORS properly**
  - Update `CORS_ORIGINS` to production domain(s)
  - Remove wildcards (`*`)
  - Use specific domain: `https://yourdomain.com`
  - Test cross-origin requests

- [ ] **Disable debug endpoints**
  - Set `SWAGGER_ENABLED=false` in production
  - Verify Swagger UI is not accessible
  - Disable detailed error messages (`show-details: never`)
  - Remove or protect development endpoints

### Network Security

- [ ] **Enable HTTPS/TLS**
  - Obtain SSL/TLS certificate (Let's Encrypt, ACM, etc.)
  - Configure HTTPS on load balancer/gateway
  - Force HTTPS redirect (HTTP → HTTPS)
  - Verify certificate validity and expiration

- [ ] **Configure security headers**
  - X-Frame-Options: DENY or SAMEORIGIN
  - X-Content-Type-Options: nosniff
  - X-XSS-Protection: 1; mode=block
  - Content-Security-Policy (CSP)
  - Strict-Transport-Security (HSTS)

- [ ] **Restrict database access**
  - Database not publicly accessible
  - Security groups/firewall rules configured
  - Only application containers can connect
  - Use VPC/VNet private subnets

- [ ] **Configure network segmentation**
  - Frontend in public subnet (or behind CDN)
  - Backend in private subnet
  - Database in isolated subnet
  - Proper routing tables configured

### Container Security

- [ ] **Run as non-root user**
  - Backend: `appuser` (UID 1001)
  - Frontend: `nextjs` (UID 1001)
  - Verify in Dockerfile: `USER <non-root-user>`

- [ ] **Use specific image versions**
  - No `latest` tags in production
  - Pin base image versions
  - Document image versions
  - Track image updates

- [ ] **Scan for vulnerabilities**
  - Run `docker scan` or Trivy
  - Address HIGH and CRITICAL vulnerabilities
  - Set up automated scanning
  - Monitor security advisories

- [ ] **No secrets in images**
  - Verify no secrets in Dockerfile
  - Check build logs for exposed secrets
  - Use .dockerignore properly
  - Environment variables for all secrets

### Application Security

- [ ] **Enable security logging**
  - Log authentication attempts
  - Log authorization failures
  - Log suspicious activities
  - Configure log retention

- [ ] **Implement rate limiting**
  - Protect API endpoints
  - Prevent brute force attacks
  - Configure appropriate limits
  - Test rate limiting

- [ ] **Input validation**
  - Validate all user inputs
  - Sanitize data before storage
  - Use parameterized queries (JPA)
  - Test for SQL injection

## Performance Checklist

### Database Optimization

- [ ] **Configure connection pooling**
  - Set appropriate pool size (`DB_POOL_SIZE`)
  - Configure minimum idle connections
  - Set connection timeout
  - Monitor connection usage

- [ ] **Verify indexes are applied**
  - Run V9 migration (performance indexes)
  - Verify indexes exist: `\di` in psql
  - Check index usage in slow queries
  - Monitor query performance

- [ ] **Enable query optimization**
  - Disable SQL logging in production
  - Enable Hibernate batch operations
  - Configure second-level cache (if needed)
  - Monitor slow queries

- [ ] **Database maintenance**
  - Configure automated backups
  - Set retention period (7-30 days)
  - Test restore procedure
  - Schedule vacuum and analyze

### Application Performance

- [ ] **Enable response compression**
  - Gzip enabled in backend
  - Compression enabled in frontend/nginx
  - Verify compressed responses
  - Test compression ratio

- [ ] **Configure caching**
  - HTTP cache headers set
  - Static assets cached (1 year)
  - API responses cached appropriately
  - CDN configured (optional)

- [ ] **Optimize resource allocation**
  - Backend: 1-2 CPU, 1-2GB RAM minimum
  - Frontend: 0.5-1 CPU, 512MB-1GB RAM minimum
  - Database: appropriate instance size
  - Monitor resource usage

- [ ] **Load balancing configured**
  - Health checks enabled
  - Multiple instances running (HA)
  - Load balancer properly configured
  - Session affinity if needed

### Monitoring & Observability

- [ ] **Configure application logs**
  - Centralized logging (CloudWatch, Azure Monitor)
  - Appropriate log levels (INFO/WARN)
  - Structured logging format
  - Log rotation configured

- [ ] **Enable health checks**
  - Backend: `/api/actuator/health`
  - Frontend: health endpoint configured
  - Health check intervals set
  - Unhealthy instance handling

- [ ] **Set up monitoring**
  - CPU usage alerts
  - Memory usage alerts
  - Database connection alerts
  - Error rate alerts
  - Response time monitoring

- [ ] **Application metrics**
  - Spring Boot Actuator metrics
  - Custom business metrics
  - Prometheus/CloudWatch integration
  - Grafana dashboards (optional)

### Auto-Scaling

- [ ] **Configure scaling policies**
  - CPU-based scaling configured
  - Memory-based scaling configured
  - Minimum instance count: 2
  - Maximum instance count defined

- [ ] **Test scaling**
  - Verify scale-up triggers
  - Verify scale-down triggers
  - Test under load
  - Monitor scaling events

## Deployment Checklist

### Environment Configuration

- [ ] **Environment variables set**
  - All required variables configured
  - No default/development values
  - Secrets stored securely
  - Variables documented

- [ ] **Spring profile configured**
  - `SPRING_PROFILES_ACTIVE=prod`
  - Production configuration loaded
  - Development features disabled
  - Verify active profile in logs

- [ ] **Database configuration**
  - Connection string correct
  - Credentials secured
  - SSL/TLS enabled for connections
  - Connection pool configured

### Pre-Deployment

- [ ] **Run all tests**
  - Backend tests pass: `mvn test`
  - Frontend tests pass: `npm test`
  - Integration tests pass
  - E2E tests pass (if available)

- [ ] **Build Docker images**
  - Backend image builds successfully
  - Frontend image builds successfully
  - Images tagged with version
  - Images pushed to registry

- [ ] **Database migrations ready**
  - All migrations tested locally
  - Migration scripts reviewed
  - Rollback plan prepared
  - Backup taken before migration

- [ ] **Deployment plan documented**
  - Step-by-step deployment guide
  - Rollback procedure documented
  - Contact information listed
  - Maintenance window scheduled

### Post-Deployment

- [ ] **Verify deployment**
  - Frontend accessible via URL
  - Backend API responding
  - Database connected
  - No errors in logs

- [ ] **Smoke testing**
  - User can log in
  - Core features working
  - API endpoints responding
  - Health checks passing

- [ ] **Monitor initial traffic**
  - Check error rates
  - Monitor response times
  - Verify no memory leaks
  - Watch database connections

- [ ] **Document deployment**
  - Deployment timestamp
  - Version deployed
  - Any issues encountered
  - Resolution steps taken

## Backup & Recovery

### Backup Configuration

- [ ] **Database backups enabled**
  - Automated daily backups
  - Retention period set (7-30 days)
  - Point-in-time recovery enabled
  - Backup storage geo-redundant

- [ ] **Test restore procedure**
  - Restore backup to test environment
  - Verify data integrity
  - Document restore time
  - Update runbook

- [ ] **Application backups**
  - Docker images versioned in registry
  - Git repository backed up
  - Configuration backed up
  - Infrastructure as Code stored

### Disaster Recovery

- [ ] **Recovery plan documented**
  - RTO (Recovery Time Objective) defined
  - RPO (Recovery Point Objective) defined
  - Contact information updated
  - Escalation procedures defined

- [ ] **Failover testing**
  - Test database failover
  - Test application failover
  - Verify data consistency
  - Document failover time

## Compliance & Legal

### Data Protection

- [ ] **Data encryption**
  - Data encrypted at rest
  - Data encrypted in transit (TLS)
  - Database encryption enabled
  - Backup encryption enabled

- [ ] **Privacy compliance**
  - Privacy policy in place
  - Terms of service defined
  - User consent mechanisms
  - Data retention policy

- [ ] **Access controls**
  - Role-based access control (RBAC)
  - Principle of least privilege
  - Admin access logged
  - Regular access reviews

### Audit & Compliance

- [ ] **Audit logging**
  - All actions logged
  - Logs tamper-proof
  - Log retention policy
  - Audit trail available

- [ ] **Compliance requirements**
  - GDPR compliance (if EU users)
  - CCPA compliance (if CA users)
  - Industry-specific compliance
  - Regular compliance audits

## Final Verification

### Security

- [ ] All security items completed
- [ ] No HIGH/CRITICAL vulnerabilities
- [ ] Secrets properly managed
- [ ] HTTPS enforced

### Performance

- [ ] Load testing completed
- [ ] Performance targets met
- [ ] Scaling configured
- [ ] Monitoring in place

### Reliability

- [ ] High availability configured
- [ ] Backups verified
- [ ] Disaster recovery tested
- [ ] Health checks passing

### Documentation

- [ ] Deployment guide complete
- [ ] Runbook updated
- [ ] Architecture documented
- [ ] Contact information current

## Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| DevOps Engineer | | | |
| Security Engineer | | | |
| Project Manager | | | |

## Additional Resources

- [AWS Deployment Guide](aws.md)
- [Azure Deployment Guide](azure.md)
- [Local Development Guide](local-development.md)
- [Main Deployment README](README.md)

## Notes

_Add any deployment-specific notes or exceptions here._

---

**Last Updated**: [Date]
**Next Review**: [Date]
