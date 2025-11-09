# Troubleshooting Guide

## Common Issues and Solutions

### Database Issues

#### Cannot Connect to Database

**Symptoms:**
- Backend fails to start with connection error
- Error: "Connection refused" or "Unknown database host"

**Solutions:**

1. **Check if database is running:**
   ```bash
   docker compose ps
   # Should show postgres container running
   ```

2. **Start database if not running:**
   ```bash
   docker compose up -d
   ```

3. **Verify connection details:**
   ```bash
   # Check environment variables
   echo $DATABASE_URL
   # Should be: jdbc:postgresql://localhost:54322/invoiceme
   ```

4. **Check if port is available:**
   ```bash
   lsof -i :54322
   # If another process is using the port, either stop it or change the port
   ```

5. **Reset database containers:**
   ```bash
   docker compose down -v
   docker compose up -d
   ```

#### Flyway Migration Errors

**Symptoms:**
- Error: "Validate failed: Migrations have failed validation"
- Backend won't start due to migration issues

**Solutions:**

1. **Check migration files:**
   ```bash
   ls backend/src/main/resources/db/migration/
   # Ensure files follow V{number}__{description}.sql pattern
   ```

2. **Clean and rebuild:**
   ```bash
   cd backend
   mvn clean install
   mvn flyway:clean  # WARNING: Deletes all data!
   mvn flyway:migrate
   ```

3. **Manual fix (if needed):**
   ```sql
   -- Connect to database
   psql -h localhost -p 54322 -U postgres -d invoiceme

   -- Check flyway_schema_history
   SELECT * FROM flyway_schema_history;

   -- If needed, delete corrupted entry
   DELETE FROM flyway_schema_history WHERE version = '2';
   ```

---

### Authentication Issues

#### JWT Token Invalid or Expired

**Symptoms:**
- 401 Unauthorized errors
- Error: "Invalid JWT token" or "Token expired"

**Solutions:**

1. **Check token expiration:**
   ```javascript
   // In browser console
   const token = localStorage.getItem('token');
   const payload = JSON.parse(atob(token.split('.')[1]));
   console.log('Expires:', new Date(payload.exp * 1000));
   ```

2. **Refresh token:**
   - Use refresh endpoint: POST `/api/auth/refresh`
   - Or log in again

3. **Verify JWT secret matches:**
   ```bash
   # Backend JWT_SECRET must be consistent
   grep JWT_SECRET backend/src/main/resources/application.yml
   ```

4. **Clear stored tokens:**
   ```javascript
   // Browser console
   localStorage.clear();
   // Then log in again
   ```

#### Cannot Log In

**Symptoms:**
- Login fails with 401
- Error: "Invalid credentials"

**Solutions:**

1. **Verify credentials:**
   - Default: `admin@example.com` / `password`

2. **Check if user exists:**
   ```sql
   SELECT * FROM users;
   ```

3. **Reset user password:**
   ```sql
   -- Generate BCrypt hash for "password"
   -- Use online BCrypt generator or run backend code
   UPDATE users SET password_hash = '$2a$10$...' WHERE username = 'admin@example.com';
   ```

---

### Build Errors

#### Backend Build Fails

**Symptoms:**
- `mvn clean install` fails
- Compilation errors

**Solutions:**

1. **Check Java version:**
   ```bash
   java -version
   # Must be Java 17 or higher
   ```

2. **Clean Maven cache:**
   ```bash
   cd backend
   mvn clean
   rm -rf ~/.m2/repository/com/invoiceme
   mvn install
   ```

3. **Check for dependency conflicts:**
   ```bash
   mvn dependency:tree
   ```

#### Frontend Build Fails

**Symptoms:**
- `npm run build` fails
- TypeScript errors

**Solutions:**

1. **Check Node version:**
   ```bash
   node -v
   # Must be 18.x or higher
   ```

2. **Clear dependencies and reinstall:**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

3. **Clear Next.js cache:**
   ```bash
   rm -rf .next
   npm run build
   ```

4. **Check TypeScript errors:**
   ```bash
   npm run type-check
   # Fix any TypeScript errors
   ```

---

### Runtime Errors

#### Backend Won't Start

**Symptoms:**
- Spring Boot application fails to start
- Port already in use

**Solutions:**

1. **Check if port 8080 is available:**
   ```bash
   lsof -i :8080
   # If something is using it:
   kill -9 <PID>
   # Or change port:
   export SERVER_PORT=8081
   ```

2. **Check logs:**
   ```bash
   cd backend
   mvn spring-boot:run
   # Read error messages carefully
   ```

3. **Verify dependencies:**
   ```bash
   mvn dependency:resolve
   ```

#### Frontend Won't Start

**Symptoms:**
- `npm run dev` fails
- Port 3000 in use

**Solutions:**

1. **Kill process on port 3000:**
   ```bash
   lsof -i :3000
   kill -9 <PID>
   ```

2. **Use different port:**
   ```bash
   PORT=3001 npm run dev
   ```

3. **Check for syntax errors:**
   ```bash
   npm run lint
   ```

#### API Calls Failing (CORS Errors)

**Symptoms:**
- Browser console: "CORS policy blocked"
- Network tab shows preflight OPTIONS request failed

**Solutions:**

1. **Check CORS configuration:**
   ```yaml
   # backend/src/main/resources/application.yml
   cors:
     allowed-origins: http://localhost:3000
   ```

2. **Verify API URL:**
   ```bash
   # .env.local
   NEXT_PUBLIC_API_URL=http://localhost:8080/api
   # NOT https:// if backend is HTTP
   ```

3. **Restart backend after CORS changes:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

---

### Test Failures

#### Backend Tests Failing

**Solutions:**

1. **Run with more details:**
   ```bash
   mvn test -X
   ```

2. **Check TestContainers:**
   ```bash
   # Ensure Docker is running
   docker ps
   ```

3. **Run specific test:**
   ```bash
   mvn test -Dtest=CustomerTest
   ```

#### Frontend Tests Failing

**Solutions:**

1. **Update snapshots:**
   ```bash
   npm test -- -u
   ```

2. **Run with coverage:**
   ```bash
   npm test -- --coverage --verbose
   ```

3. **Check for async issues:**
   - Use `waitFor` from @testing-library/react
   - Ensure all async operations complete

---

### Docker Issues

#### Container Won't Start

**Symptoms:**
- `docker compose up` fails
- Container exits immediately

**Solutions:**

1. **Check logs:**
   ```bash
   docker compose logs
   docker compose logs backend
   docker compose logs postgres
   ```

2. **Rebuild containers:**
   ```bash
   docker compose down
   docker compose build --no-cache
   docker compose up -d
   ```

3. **Check disk space:**
   ```bash
   docker system df
   docker system prune -a  # Remove unused images/containers
   ```

#### Database Container Issues

**Symptoms:**
- PostgreSQL container crashes
- Data not persisting

**Solutions:**

1. **Check volume mounts:**
   ```bash
   docker volume ls
   docker volume inspect invoice-me_postgres-data
   ```

2. **Reset volumes:**
   ```bash
   docker compose down -v  # WARNING: Deletes all data
   docker compose up -d
   ```

---

### Performance Issues

#### Slow API Response

**Solutions:**

1. **Check database indexes:**
   ```sql
   SELECT schemaname, tablename, indexname, idx_scan
   FROM pg_stat_user_indexes
   WHERE idx_scan = 0;
   ```

2. **Enable SQL logging:**
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

3. **Check N+1 query problems:**
   - Use `@EntityGraph` or JOIN FETCH in JPA queries

#### Slow Frontend Rendering

**Solutions:**

1. **Use React DevTools Profiler**

2. **Check for unnecessary re-renders:**
   ```typescript
   // Use React.memo for expensive components
   export const ExpensiveComponent = React.memo(({ data }) => {
     // Component logic
   });
   ```

3. **Optimize API calls:**
   - Implement pagination
   - Add caching with React Query or SWR

---

### Debugging Tips

#### Backend Debugging

**IntelliJ IDEA:**
1. Set breakpoints in code
2. Right-click on Application class → Debug
3. Step through code with F8/F7

**Logging:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.debug("Debug info: {}", variable);
log.error("Error occurred", exception);
```

#### Frontend Debugging

**Browser DevTools:**
1. Open DevTools (F12)
2. Set breakpoints in Sources tab
3. Use console.log() strategically

**React DevTools:**
1. Install React DevTools extension
2. Inspect component state and props
3. Use Profiler to find performance issues

#### Database Debugging

**Connect with psql:**
```bash
psql -h localhost -p 54322 -U postgres -d invoiceme
```

**Use Supabase Studio:**
- Navigate to http://localhost:54323
- Visual query builder and data explorer

---

### Getting Help

If you can't resolve an issue:

1. **Check existing documentation:**
   - [Architecture](architecture.md)
   - [API Documentation](api/README.md)
   - [Database Schema](database/schema.md)

2. **Check logs:**
   ```bash
   # Backend logs
   cd backend && mvn spring-boot:run

   # Frontend logs
   npm run dev

   # Docker logs
   docker compose logs -f
   ```

3. **Search for error message:**
   - Google the exact error message
   - Check Stack Overflow
   - Check GitHub issues for dependencies

4. **Create detailed issue report:**
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details
   - Error logs and screenshots

---

## Diagnostic Commands

### Health Checks

```bash
# Backend health
curl http://localhost:8080/api/actuator/health

# Frontend
curl http://localhost:3000

# Database
docker compose exec postgres pg_isready
```

### System Status

```bash
# Check all services
docker compose ps

# Check ports in use
lsof -i :3000
lsof -i :8080
lsof -i :54322

# Check Java processes
jps -v

# Check Node processes
ps aux | grep node
```

### Database Status

```sql
-- Active connections
SELECT * FROM pg_stat_activity;

-- Database size
SELECT pg_size_pretty(pg_database_size('invoiceme'));

-- Table sizes
SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

**Last Updated:** 2024-11-09
