# Environment Variables Configuration

## Overview

This document describes all environment variables used in the InvoiceMe application for both frontend and backend.

## Frontend Environment Variables

Frontend variables are prefixed with `NEXT_PUBLIC_` for client-side access or kept private for server-side only.

### Configuration File

Create `.env.local` in the project root (copy from `.env.example`):

```bash
cp .env.example .env.local
```

### Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | Yes | `http://localhost:8080/api` | Backend API base URL |
| `NEXT_PUBLIC_APP_NAME` | No | `InvoiceMe` | Application display name |
| `NODE_ENV` | Auto | `development` | Environment mode (development/production) |

### Example `.env.local`

```bash
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# App Configuration
NEXT_PUBLIC_APP_NAME=InvoiceMe
```

### Production Frontend Variables

```bash
# Production API URL
NEXT_PUBLIC_API_URL=https://api.invoiceme.com/api
```

---

## Backend Environment Variables

Backend variables are configured in `application.yml` or via environment variables.

### Database Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | `jdbc:postgresql://localhost:54322/invoiceme` | Database connection URL |
| `DATABASE_USERNAME` | Yes | `postgres` | Database username |
| `DATABASE_PASSWORD` | Yes | `postgres` | Database password |
| `SPRING_DATASOURCE_URL` | Alternative | - | Alternative to DATABASE_URL |

### JWT Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes | `your-secret-key-change-in-production` | Secret key for signing JWT tokens |
| `JWT_EXPIRATION` | No | `3600000` | Token expiration in milliseconds (1 hour) |
| `JWT_REFRESH_EXPIRATION` | No | `86400000` | Refresh token expiration (24 hours) |

### Server Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SERVER_PORT` | No | `8080` | Port for Spring Boot application |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile (dev/prod) |

### Swagger/OpenAPI Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SWAGGER_ENABLED` | No | `true` | Enable/disable Swagger UI |

### CORS Configuration

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Allowed CORS origins (comma-separated) |

### Example `application.yml`

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:54322/invoiceme}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: ${SHOW_SQL:false}

jwt:
  secret: ${JWT_SECRET:your-secret-key-change-in-production}
  expiration: ${JWT_EXPIRATION:3600000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:86400000}

springdoc:
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:true}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

### Production Backend Variables

```bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db-host:5432/invoiceme
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=<secure-password>

# JWT (use strong secret!)
JWT_SECRET=<generate-strong-random-secret-minimum-256-bits>
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Swagger (disable in production)
SWAGGER_ENABLED=false

# CORS
CORS_ALLOWED_ORIGINS=https://invoiceme.com,https://www.invoiceme.com
```

---

## Docker Compose Variables

Variables for local development with Docker Compose.

### `.env` File (for docker-compose.yml)

```bash
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=invoiceme
POSTGRES_PORT=54322

# Supabase Studio
SUPABASE_PORT=54323
```

---

## CI/CD Environment Variables

### GitHub Actions Secrets

Configure these as repository secrets in GitHub:

- `DATABASE_URL`: Production database URL
- `JWT_SECRET`: Production JWT secret
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password
- `AWS_ACCESS_KEY_ID`: AWS credentials (if deploying to AWS)
- `AWS_SECRET_ACCESS_KEY`: AWS credentials
- `AZURE_CREDENTIALS`: Azure credentials (if deploying to Azure)

---

## Security Best Practices

### 1. Never Commit Secrets

Add to `.gitignore`:
```
.env
.env.local
.env.production
```

### 2. Use Strong Secrets in Production

Generate secure JWT secret:
```bash
# Generate 256-bit random secret
openssl rand -base64 32
```

### 3. Rotate Secrets Regularly

- Rotate JWT secret every 90 days
- Rotate database passwords quarterly
- Update secrets in all environments simultaneously

### 4. Use Different Secrets Per Environment

- Development: Can use default/simple values
- Staging: Use production-like but separate secrets
- Production: Use strong, unique secrets

### 5. Store Secrets Securely

**Development:**
- `.env.local` (gitignored)

**Production:**
- AWS Secrets Manager / Parameter Store
- Azure Key Vault
- HashiCorp Vault
- Kubernetes Secrets

### 6. Validate Environment Variables

Backend validation (example):
```java
@Configuration
public class EnvironmentValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        if ("your-secret-key-change-in-production".equals(jwtSecret)) {
            throw new IllegalStateException(
                "JWT secret must be changed in production!"
            );
        }
    }
}
```

---

## Troubleshooting

### Problem: "Cannot connect to database"

**Check:**
1. Database is running: `docker compose ps`
2. DATABASE_URL is correct
3. Credentials are correct
4. Port is not blocked by firewall

### Problem: "JWT token invalid"

**Check:**
1. JWT_SECRET matches between environments
2. Token hasn't expired
3. Clock skew between servers

### Problem: "CORS error in browser"

**Check:**
1. CORS_ALLOWED_ORIGINS includes frontend URL
2. Credentials are included in requests
3. HTTP vs HTTPS protocol mismatch

### Problem: "Environment variable not loading"

**Frontend:**
- Must be prefixed with `NEXT_PUBLIC_` for client-side
- Rebuild required after changing `.env.local`
- Check `next.config.ts` for custom env loading

**Backend:**
- Check `application.yml` syntax
- Verify Spring profile is active
- Check environment variable name matches

---

## Environment Variable Templates

### Development `.env.local`

```bash
# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=InvoiceMe

# Backend (if needed)
DATABASE_URL=jdbc:postgresql://localhost:54322/invoiceme
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=dev-secret-key-not-for-production
```

### Production `.env.production`

```bash
# Frontend
NEXT_PUBLIC_API_URL=https://api.invoiceme.com/api
NEXT_PUBLIC_APP_NAME=InvoiceMe

# Backend
DATABASE_URL=jdbc:postgresql://<prod-host>:5432/invoiceme
DATABASE_USERNAME=<prod-user>
DATABASE_PASSWORD=<secure-password>
JWT_SECRET=<generated-secure-secret>
SPRING_PROFILES_ACTIVE=prod
SWAGGER_ENABLED=false
CORS_ALLOWED_ORIGINS=https://invoiceme.com
```

---

## Reference

- [Next.js Environment Variables](https://nextjs.org/docs/basic-features/environment-variables)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App Config](https://12factor.net/config)

---

**Last Updated:** 2024-11-09
