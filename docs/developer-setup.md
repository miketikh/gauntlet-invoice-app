# Developer Setup Guide

## Introduction

This guide provides comprehensive setup instructions for new developers joining the InvoiceMe project.

## Prerequisites

### Required Software

Install the following before proceeding:

1. **Java Development Kit (JDK) 17+**
   - Download from [Adoptium](https://adoptium.net/)
   - Verify: `java -version` should show 17 or higher

2. **Node.js 18+**
   - Download from [nodejs.org](https://nodejs.org/)
   - Verify: `node -v` should show 18 or higher
   - npm comes bundled with Node.js

3. **Maven 3.9+**
   - Download from [maven.apache.org](https://maven.apache.org/)
   - Verify: `mvn -v` should show 3.9 or higher

4. **Docker Desktop 24+**
   - Download from [docker.com](https://www.docker.com/)
   - Verify: `docker --version` and `docker compose version`

5. **Git**
   - Usually pre-installed on Mac/Linux
   - Windows: Download from [git-scm.com](https://git-scm.com/)
   - Verify: `git --version`

### Recommended Software

1. **IDE**
   - **IntelliJ IDEA** (recommended for backend)
     - Install Java plugin
     - Install Lombok plugin
   - **VS Code** (recommended for frontend)
     - Install ESLint extension
     - Install Prettier extension
     - Install Tailwind CSS IntelliSense

2. **Database Tools**
   - **DBeaver** or **pgAdmin** for PostgreSQL management
   - **Supabase Studio** (included with docker compose)

3. **API Testing**
   - **Postman** or **Insomnia**
   - Or use built-in Swagger UI

## Initial Setup

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/invoice-me.git
cd invoice-me
```

### 2. Backend Setup

```bash
# Navigate to backend directory
cd backend

# Install dependencies and build
mvn clean install

# This will:
# - Download dependencies
# - Compile code
# - Run tests
# - Create JAR file

cd ..
```

### 3. Frontend Setup

```bash
# Install Node.js dependencies
npm install

# Or if using pnpm
pnpm install
```

### 4. Environment Configuration

```bash
# Copy example environment file
cp .env.example .env.local

# Edit .env.local if needed
# Default values work for local development
```

### 5. Database Setup

```bash
# Start PostgreSQL and Supabase Studio
docker compose up -d

# Verify containers are running
docker compose ps

# Check database is accessible
docker compose exec postgres psql -U postgres -d invoiceme -c "SELECT version();"
```

The database schema will be automatically created via Flyway migrations when the backend starts.

### 6. Start Applications

**Terminal 1 - Backend:**
```bash
cd backend
mvn spring-boot:run

# Backend starts on http://localhost:8080
# Wait for "Started InvoiceMeApplication" message
```

**Terminal 2 - Frontend:**
```bash
npm run dev

# Frontend starts on http://localhost:3000
```

### 7. Verify Setup

Open your browser and navigate to:
- **Frontend:** http://localhost:3000
- **Backend API Docs:** http://localhost:8080/swagger-ui.html
- **Supabase Studio:** http://localhost:54323
- **Health Check:** http://localhost:8080/api/actuator/health

**Default Login Credentials:**
- Email: `admin@example.com`
- Password: `password`

## IDE Configuration

### IntelliJ IDEA (Backend)

1. **Open Project:**
   - File → Open → Select `invoice-me/backend` directory
   - Wait for Maven to import dependencies

2. **Configure JDK:**
   - File → Project Structure → Project
   - Set SDK to JDK 17+

3. **Install Lombok Plugin:**
   - File → Settings → Plugins
   - Search for "Lombok"
   - Install and restart

4. **Enable Annotation Processing:**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

5. **Code Style:**
   - File → Settings → Editor → Code Style → Java
   - Import coding style (if provided)

6. **Run Configuration:**
   - Create new "Spring Boot" configuration
   - Main class: `com.invoiceme.InvoiceMeApplication`
   - Working directory: `backend` module
   - Active profiles: `dev`

### VS Code (Frontend)

1. **Open Project:**
   - File → Open Folder → Select `invoice-me` directory

2. **Install Extensions:**
   - ESLint
   - Prettier - Code formatter
   - Tailwind CSS IntelliSense
   - TypeScript Vue Plugin (Volar)

3. **Configure Settings:**
   Create `.vscode/settings.json`:
   ```json
   {
     "editor.formatOnSave": true,
     "editor.defaultFormatter": "esbenp.prettier-vscode",
     "editor.codeActionsOnSave": {
       "source.fixAll.eslint": true
     },
     "typescript.tsdk": "node_modules/typescript/lib"
   }
   ```

4. **Debugging:**
   Create `.vscode/launch.json`:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "name": "Next.js: debug server-side",
         "type": "node-terminal",
         "request": "launch",
         "command": "npm run dev"
       }
     ]
   }
   ```

## Development Workflow

### Daily Workflow

1. **Pull latest changes:**
   ```bash
   git pull origin main
   ```

2. **Start services:**
   ```bash
   # Start database
   docker compose up -d

   # Start backend
   cd backend && mvn spring-boot:run

   # Start frontend (new terminal)
   npm run dev
   ```

3. **Make changes:**
   - Follow coding standards
   - Write tests
   - Test locally

4. **Run tests:**
   ```bash
   # Backend tests
   cd backend && mvn test

   # Frontend tests
   npm test
   ```

5. **Commit and push:**
   ```bash
   git add .
   git commit -m "feat(feature): description"
   git push
   ```

### Creating a Feature Branch

```bash
# Create and switch to new branch
git checkout -b feature/my-feature

# Make changes, commit
git add .
git commit -m "feat: add my feature"

# Push to remote
git push -u origin feature/my-feature

# Create pull request on GitHub
```

### Running Tests

```bash
# Backend unit tests
cd backend
mvn test

# Backend with coverage
mvn test jacoco:report
open target/site/jacoco/index.html

# Frontend tests
npm test

# Frontend with coverage
npm test -- --coverage

# E2E tests
npm run test:e2e

# Specific test
mvn test -Dtest=CustomerTest
npm test customer-form.test.tsx
```

### Code Quality

```bash
# Lint frontend code
npm run lint

# Fix linting issues
npm run lint:fix

# Format code
npm run format

# Type check
npm run type-check
```

## Debugging

### Backend Debugging

**IntelliJ IDEA:**
1. Set breakpoints by clicking line number gutter
2. Right-click `InvoiceMeApplication.java` → Debug
3. Use debugger controls: Step Over (F8), Step Into (F7), Continue (F9)

**Logging:**
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.debug("Variable value: {}", variable);
log.error("Error occurred", exception);
```

### Frontend Debugging

**Browser DevTools:**
1. Open DevTools (F12 or Cmd+Option+I)
2. Set breakpoints in Sources tab
3. Use Console for logging

**React DevTools:**
- Install React DevTools browser extension
- Inspect component hierarchy and state

**VS Code Debugging:**
- Set breakpoints in code
- Press F5 or Run → Start Debugging
- Use Debug Console

### Database Debugging

**Supabase Studio:**
- Navigate to http://localhost:54323
- Visual query builder and table editor

**psql:**
```bash
psql -h localhost -p 54322 -U postgres -d invoiceme

# List tables
\dt

# Describe table
\d customers

# Query
SELECT * FROM customers;

# Exit
\q
```

## Common Issues

See [Troubleshooting Guide](troubleshooting.md) for solutions to common problems.

**Quick Fixes:**

- **Port already in use:** Kill process or change port
- **Database connection error:** Ensure Docker is running and postgres container is up
- **Module not found:** Run `npm install` or `mvn clean install`
- **Tests failing:** Check TestContainers requires Docker running

## Best Practices

### Code Style

- Follow existing patterns in the codebase
- Use TypeScript types, avoid `any`
- Write self-documenting code
- Add comments only when necessary to explain "why", not "what"

### Testing

- Write tests for new features
- Aim for >80% coverage
- Test edge cases and error scenarios
- Keep tests maintainable

### Git

- Write descriptive commit messages
- Keep commits focused and small
- Pull before push to avoid conflicts
- Don't commit secrets or `.env` files

### Code Review

- Review your own code first
- Run tests before pushing
- Respond to review comments promptly
- Be respectful and constructive

## Resources

### Documentation

- [Architecture](architecture.md)
- [Project Structure](project-structure.md)
- [API Documentation](api/README.md)
- [Testing Strategy](testing/README.md)
- [Troubleshooting](troubleshooting.md)

### Learning Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Next.js Documentation](https://nextjs.org/docs)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)

### Team Communication

- Check project documentation first
- Ask questions in team chat
- Open GitHub issues for bugs
- Submit PRs for features

## Getting Help

If you're stuck:

1. Check documentation in `/docs`
2. Search existing GitHub issues
3. Ask team members
4. Create new GitHub issue with details

Welcome to the team!

---

**Last Updated:** 2024-11-09
