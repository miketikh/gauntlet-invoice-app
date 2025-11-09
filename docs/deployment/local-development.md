# Local Development Setup

This guide covers running InvoiceMe locally using Docker Compose.

## Prerequisites

- Docker 20.x or higher
- Docker Compose 2.x or higher
- Git
- 8GB+ RAM recommended
- 10GB+ free disk space

## Quick Start

### 1. Clone and Setup

```bash
# Clone the repository (if not already done)
git clone <repository-url>
cd invoice-me

# Copy environment variables
cp .env.example .env

# Review and update .env as needed
nano .env  # or your preferred editor
```

### 2. Start with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f
```

### 3. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **API Documentation**: http://localhost:8080/api/swagger-ui.html
- **Database**: localhost:5432
- **Health Checks**:
  - Backend: http://localhost:8080/api/actuator/health
  - Frontend: http://localhost:3000/api/health

### 4. Default Credentials

Check the database migrations for default user credentials or create a new user through the API.

## Using Helper Scripts

The project includes convenience scripts in the `scripts/` directory:

```bash
# Build all Docker images
./scripts/build-all.sh

# Build backend only
./scripts/build-backend.sh

# Build frontend only
./scripts/build-frontend.sh

# Run with Docker Compose (interactive mode)
./scripts/run-local.sh

# Stop all services
./scripts/stop-local.sh

# Clean up Docker resources
./scripts/clean-docker.sh
```

## Development Workflow

### Making Code Changes

#### Backend Changes

1. Make changes to Java code in `backend/src/`
2. Rebuild backend container:
   ```bash
   docker-compose up -d --build backend
   ```
3. View logs:
   ```bash
   docker-compose logs -f backend
   ```

#### Frontend Changes

1. Make changes to Next.js code
2. Rebuild frontend container:
   ```bash
   docker-compose up -d --build frontend
   ```
3. View logs:
   ```bash
   docker-compose logs -f frontend
   ```

### Database Changes

#### Add a New Migration

1. Create migration file in `backend/src/main/resources/db/migration/`
2. Name it: `V{next_number}__{description}.sql`
3. Example: `V10__add_products_table.sql`
4. Restart backend to apply migration:
   ```bash
   docker-compose restart backend
   ```

#### Reset Database

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Start fresh
docker-compose up --build
```

## Alternative: Running Without Docker

### Backend (Spring Boot)

```bash
cd backend

# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/invoiceme
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export SPRING_PROFILES_ACTIVE=dev

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/invoiceme-1.0.0-SNAPSHOT.jar
```

### Frontend (Next.js)

```bash
# Install dependencies
npm install

# Set environment variable
export NEXT_PUBLIC_API_URL=http://localhost:8080/api

# Run development server
npm run dev

# Build for production
npm run build
npm start
```

### Database (PostgreSQL)

```bash
# Using Docker for just the database
docker run -d \
  --name invoiceme-db \
  -e POSTGRES_DB=invoiceme \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# Or install PostgreSQL locally and create database
createdb invoiceme
```

## Troubleshooting

### Port Conflicts

If ports 3000, 8080, or 5432 are already in use:

1. Edit `docker-compose.yml`
2. Change port mappings:
   ```yaml
   ports:
     - "3001:3000"  # Frontend
     - "8081:8080"  # Backend
     - "5433:5432"  # Database
   ```
3. Update `.env` file accordingly

### Container Won't Start

```bash
# Check logs
docker-compose logs <service-name>

# Example
docker-compose logs backend

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

### Database Connection Issues

```bash
# Verify database is running
docker-compose ps

# Check database logs
docker-compose logs database

# Test connection
docker-compose exec database psql -U postgres -d invoiceme -c '\dt'
```

### Out of Memory

```bash
# Increase Docker memory limit (Docker Desktop)
# Go to Settings > Resources > Memory
# Increase to 8GB or more

# Verify container resources
docker stats
```

### Frontend Build Fails

```bash
# Clear Next.js cache
rm -rf .next node_modules
npm install
docker-compose up --build frontend
```

### Backend Build Fails

```bash
# Clear Maven cache
cd backend
mvn clean
docker-compose up --build backend
```

## Testing the Application

### Backend Tests

```bash
# Run tests inside container
docker-compose exec backend mvn test

# With coverage report
docker-compose exec backend mvn test jacoco:report

# Or run locally
cd backend
mvn test
```

### Frontend Tests

```bash
# Run tests inside container
docker-compose exec frontend npm test

# With coverage
docker-compose exec frontend npm test -- --coverage

# Or run locally
npm test
```

### API Testing

Use the Swagger UI at http://localhost:8080/api/swagger-ui.html or tools like:

```bash
# Using curl
curl http://localhost:8080/api/actuator/health

# Using httpie
http localhost:8080/api/actuator/health

# Using Postman
# Import the OpenAPI spec from http://localhost:8080/api/v3/api-docs
```

## Environment Configuration

### Development Profile

The development profile (`dev`) is used by default:

- SQL logging enabled
- Debug logging enabled
- Swagger UI enabled
- Detailed error messages
- Relaxed security (for testing)

### Docker Profile

When running with Docker Compose, use the `docker` profile:

```yaml
# In docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: docker
```

## Database Management

### Connect to Database

```bash
# Using docker-compose
docker-compose exec database psql -U postgres -d invoiceme

# Using external client
psql -h localhost -p 5432 -U postgres -d invoiceme
```

### Backup Database

```bash
# Export database
docker-compose exec database pg_dump -U postgres invoiceme > backup.sql

# Import database
docker-compose exec -T database psql -U postgres -d invoiceme < backup.sql
```

### View Migration History

```bash
# Check Flyway schema history
docker-compose exec database psql -U postgres -d invoiceme -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

## Performance Tips

1. **Use BuildKit** - Enable Docker BuildKit for faster builds:
   ```bash
   export DOCKER_BUILDKIT=1
   export COMPOSE_DOCKER_CLI_BUILD=1
   ```

2. **Layer Caching** - Dependencies are cached in separate layers
   - Backend: Maven dependencies cached
   - Frontend: npm packages cached

3. **Resource Limits** - Adjust in `docker-compose.yml`:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 2G
   ```

## Next Steps

- Review [Production Checklist](production-checklist.md) before deploying
- Learn about [AWS Deployment](aws.md)
- Learn about [Azure Deployment](azure.md)
