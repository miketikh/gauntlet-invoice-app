# InvoiceMe Deployment Guide

This directory contains comprehensive deployment documentation for the InvoiceMe application.

## Documentation Structure

- **[Local Development](local-development.md)** - Running the application locally with Docker
- **[AWS Deployment](aws.md)** - Deploying to Amazon Web Services (ECS, RDS, ALB)
- **[Azure Deployment](azure.md)** - Deploying to Microsoft Azure (Container Instances, PostgreSQL)
- **[Production Checklist](production-checklist.md)** - Pre-deployment security and configuration checklist

## Quick Start

### Local Development with Docker Compose

```bash
# Copy environment variables
cp .env.example .env

# Start all services
docker-compose up --build

# Or use the convenience script
./scripts/run-local.sh
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- API Documentation: http://localhost:8080/api/swagger-ui.html
- Database: localhost:5432

### Building Docker Images

```bash
# Build all images
./scripts/build-all.sh

# Or build individually
./scripts/build-backend.sh
./scripts/build-frontend.sh
```

## Architecture Overview

InvoiceMe uses a multi-tier architecture:

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│   Next.js       │
│   Frontend      │ :3000
│  (Node.js)      │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  Spring Boot    │
│    Backend      │ :8080
│   (Java 21)     │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   PostgreSQL    │
│    Database     │ :5432
└─────────────────┘
```

## Deployment Targets

### Development
- Docker Compose for local development
- In-memory or local PostgreSQL database
- Hot reloading enabled
- Debug logging enabled

### Staging/Testing
- Containerized deployment (ECS, AKS, or Container Instances)
- Managed database (RDS or Azure Database)
- Application load balancer
- Environment-specific configuration

### Production
- Auto-scaling container orchestration
- High-availability database with backups
- CDN for static assets (optional)
- SSL/TLS encryption
- Monitoring and logging
- Backup and disaster recovery

## Environment Variables

All required environment variables are documented in `.env.example`. Key variables include:

### Frontend
- `NEXT_PUBLIC_API_URL` - Backend API URL
- `NODE_ENV` - Environment (development/production)

### Backend
- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - Database user
- `DATABASE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret (MUST be changed in production)
- `SPRING_PROFILES_ACTIVE` - Active Spring profile (dev/docker/prod)

## Container Images

### Backend
- Base: Eclipse Temurin 21 JRE Alpine
- Size: ~512MB
- Non-root user: `appuser`
- Health check: `/api/actuator/health`
- Port: 8080

### Frontend
- Base: Node.js 18 Alpine
- Size: ~200MB
- Non-root user: `nextjs`
- Port: 3000

## Health Checks

Both containers include health check endpoints:

### Backend
```bash
curl http://localhost:8080/api/actuator/health
```

### Frontend
```bash
curl http://localhost:3000/api/health
```

## Database Migrations

The application uses Flyway for database migrations:

1. Migrations are located in `backend/src/main/resources/db/migration/`
2. Migrations run automatically on application startup
3. Naming convention: `V{version}__{description}.sql`
4. Current migrations:
   - V1: Initial schema (users, roles)
   - V2-V8: Various feature additions
   - V9: Performance indexes

## Security Considerations

1. **Change default credentials** - Update all default passwords
2. **Use strong JWT secret** - Generate with `openssl rand -hex 32`
3. **Enable HTTPS** - Always use TLS in production
4. **Configure CORS** - Restrict to your domain
5. **Update dependencies** - Regularly scan and update
6. **Enable firewalls** - Restrict database access
7. **Use secrets management** - AWS Secrets Manager or Azure Key Vault
8. **Enable logging** - CloudWatch or Azure Monitor

## Monitoring

Recommended monitoring setup:

- **Application Metrics**: Spring Boot Actuator + Prometheus
- **Container Metrics**: CloudWatch or Azure Monitor
- **Database Metrics**: RDS/Azure Database built-in monitoring
- **Logs**: Centralized logging (CloudWatch Logs, Azure Log Analytics)
- **Alerts**: Set up alerts for errors, high CPU, memory, database connections

## Backup Strategy

### Database Backups
- Automated daily backups (RDS/Azure Database)
- Point-in-time recovery enabled
- Retention period: 7-30 days

### Application Backups
- Docker images stored in container registry
- Git repository as source of truth
- Infrastructure as Code (optional)

## Scaling

### Horizontal Scaling
- Frontend: Scale based on CPU/memory usage
- Backend: Scale based on request rate and response time
- Database: Read replicas for read-heavy workloads

### Vertical Scaling
- Increase container CPU/memory allocation
- Upgrade database instance size

## Troubleshooting

### Common Issues

1. **Container fails to start**
   - Check logs: `docker logs <container-name>`
   - Verify environment variables
   - Ensure database is accessible

2. **Database connection errors**
   - Verify DATABASE_URL is correct
   - Check network connectivity
   - Ensure database is running

3. **Frontend can't reach backend**
   - Verify NEXT_PUBLIC_API_URL
   - Check CORS configuration
   - Ensure backend is running

## Support

For issues or questions:
1. Check the relevant deployment guide (AWS/Azure)
2. Review the production checklist
3. Check application logs
4. Consult the troubleshooting section

## Next Steps

- [Set up local development environment](local-development.md)
- [Deploy to AWS](aws.md)
- [Deploy to Azure](azure.md)
- [Complete production checklist](production-checklist.md)
