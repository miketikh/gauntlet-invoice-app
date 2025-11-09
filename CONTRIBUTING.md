# Contributing to InvoiceMe

## Code of Conduct

This project is an assessment demonstrating architectural principles and best practices. While primarily for evaluation purposes, we welcome contributions that enhance the demonstration value of the project.

## How to Contribute

### Reporting Bugs

If you find a bug, please open an issue with:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, Java version, Node version)
- Screenshots if applicable

### Suggesting Features

For feature suggestions, open an issue describing:
- The feature or improvement
- Use case and value proposition
- Potential implementation approach

### Submitting Pull Requests

1. **Fork the Repository**
   ```bash
   git clone https://github.com/yourusername/invoice-me.git
   cd invoice-me
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make Your Changes**
   - Follow existing code patterns and architecture
   - Write or update tests for your changes
   - Update documentation if needed
   - Follow the coding standards below

4. **Run Tests**
   ```bash
   # Backend tests
   cd backend && mvn test

   # Frontend tests
   npm test

   # E2E tests
   npm run test:e2e
   ```

5. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat(invoice): add email notification support"
   ```

6. **Push and Create Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```

   Then open a Pull Request on GitHub.

## Development Workflow

### Prerequisites

Ensure you have installed:
- Java 17+
- Node.js 18+
- Maven 3.9+
- Docker 24+

See [Developer Setup Guide](docs/developer-setup.md) for detailed setup instructions.

### Local Development

```bash
# Start database
docker compose up -d

# Start backend (terminal 1)
cd backend && mvn spring-boot:run

# Start frontend (terminal 2)
npm run dev
```

### Code Style

#### Backend (Java)

- Follow DDD/CQRS/Vertical Slice Architecture patterns
- Use Java 17 features (records, sealed classes)
- Place business logic in domain entities
- Keep commands and queries separate
- Write JavaDoc for public methods

**Example:**
```java
/**
 * Handles creation of new invoices with line item calculation.
 * Validates customer exists and calculates all totals.
 */
@Service
public class CreateInvoiceCommandHandler {
    // Implementation
}
```

#### Frontend (TypeScript/React)

- Use TypeScript for type safety
- Follow React hooks patterns
- Use Zustand for state management
- Keep components focused and small
- Write JSDoc for complex functions

**Example:**
```typescript
/**
 * Calculates the total amount for an invoice including all line items,
 * discounts, and taxes.
 */
export function calculateInvoiceTotal(lineItems: LineItem[]): number {
  // Implementation
}
```

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Java Classes | PascalCase | `CreateInvoiceCommand` |
| Java Methods | camelCase | `calculateTotal()` |
| React Components | PascalCase | `InvoiceForm.tsx` |
| React Hooks | camelCase with 'use' prefix | `useInvoiceStore()` |
| API Endpoints | kebab-case | `/api/customers` |
| Database Tables | snake_case | `customer_invoices` |

### Commit Message Format

Use conventional commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build/config changes

**Examples:**
```
feat(invoice): add support for partial payments

fix(auth): resolve token refresh race condition

docs(api): update customer endpoint examples

refactor(customer): extract validation logic

test(payment): add payment reconciliation tests
```

### Pull Request Guidelines

A good pull request:

1. **Has a clear title and description**
   - What does it do?
   - Why is it needed?
   - How was it tested?

2. **Is focused and small**
   - One feature or fix per PR
   - Less than 500 lines if possible

3. **Includes tests**
   - Unit tests for logic
   - Integration tests for API endpoints
   - E2E tests for critical flows

4. **Updates documentation**
   - Update README if needed
   - Add/update API docs
   - Update architecture docs if design changes

5. **Passes all checks**
   - All tests pass
   - Lint checks pass
   - Code builds successfully

### Review Process

1. Automated checks run on PR creation
2. Code review by maintainer
3. Address feedback with new commits
4. Maintainer merges when approved

## Architecture Guidelines

### Domain-Driven Design

- Keep domain logic in entities
- Use value objects for immutable concepts
- Define clear bounded contexts
- Use ubiquitous language

### CQRS Pattern

- Separate commands (writes) and queries (reads)
- Commands modify state, queries read state
- Different DTOs for commands and queries
- Never mix command and query logic

### Vertical Slice Architecture

- Group code by feature, not layer
- Each feature is self-contained
- Minimal coupling between features
- Features can evolve independently

## Testing Standards

### Unit Tests

- Test business logic in isolation
- Use mocks for dependencies
- Aim for >80% code coverage
- Use test builders for complex objects

### Integration Tests

- Test API endpoints with real database
- Use TestContainers for PostgreSQL
- Verify request/response contracts
- Test error scenarios

### E2E Tests

- Test critical user journeys
- Use Playwright for browser automation
- Keep tests maintainable and stable
- Run in CI/CD pipeline

## Questions?

- Check existing documentation in `/docs`
- Open an issue for clarification
- Review closed issues and PRs

Thank you for contributing to InvoiceMe!
