# Project Structure Documentation

## Overview

InvoiceMe follows a monorepo structure with clear separation between frontend (Next.js) and backend (Spring Boot). The project uses Vertical Slice Architecture for the backend and component-based architecture for the frontend.

## Repository Layout

```
invoice-me/
├── app/                    # Next.js App Router (Frontend Routes)
├── components/             # React Components
├── lib/                    # Frontend Libraries and Utilities
├── types/                  # TypeScript Type Definitions
├── backend/                # Spring Boot Application
├── e2e/                    # Playwright E2E Tests
├── __tests__/              # Jest Unit Tests
├── docs/                   # Documentation
├── public/                 # Static Assets
├── scripts/                # Build and Deployment Scripts
├── .bmad-core/             # BMAD Development Framework
├── .claude/                # Claude AI Configuration
└── docker-compose.yml      # Local Development Setup
```

## Frontend Structure

### App Router (`app/`)

Next.js 14 App Router with route groups:

```
app/
├── (auth)/                 # Authentication Routes (Public)
│   ├── layout.tsx          # Auth layout (no sidebar)
│   └── login/
│       └── page.tsx        # Login page
│
├── (dashboard)/            # Protected Dashboard Routes
│   ├── layout.tsx          # Dashboard layout (with sidebar, nav)
│   ├── page.tsx            # Dashboard home
│   │
│   ├── customers/
│   │   ├── page.tsx        # Customer list
│   │   ├── new/
│   │   │   └── page.tsx    # Create customer
│   │   └── [id]/
│   │       └── page.tsx    # Customer detail/edit
│   │
│   ├── invoices/
│   │   ├── page.tsx        # Invoice list
│   │   ├── new/
│   │   │   └── page.tsx    # Create invoice
│   │   └── [id]/
│   │       ├── page.tsx    # Invoice detail
│   │       └── payment/
│   │           └── page.tsx # Record payment
│   │
│   └── payments/
│       └── page.tsx        # Payment history
│
├── api/                    # Next.js API routes (if needed)
├── globals.css             # Global styles
└── layout.tsx              # Root layout
```

### Components (`components/`)

Organized by feature with shared UI components:

```
components/
├── ui/                     # Shadcn/ui Base Components
│   ├── button.tsx
│   ├── input.tsx
│   ├── card.tsx
│   ├── dialog.tsx
│   ├── form.tsx
│   └── ...                 # Other Shadcn components
│
├── customers/              # Customer-specific Components
│   ├── customer-list.tsx
│   ├── customer-form.tsx
│   ├── customer-detail.tsx
│   └── customer-stats.tsx
│
├── invoices/               # Invoice-specific Components
│   ├── invoice-list.tsx
│   ├── invoice-form.tsx
│   ├── invoice-detail.tsx
│   ├── line-item-manager.tsx
│   └── invoice-status-badge.tsx
│
├── payments/               # Payment-specific Components
│   ├── payment-form.tsx
│   ├── payment-history.tsx
│   └── payment-summary.tsx
│
└── layout/                 # Layout Components
    ├── dashboard-layout.tsx
    ├── sidebar.tsx
    └── navbar.tsx
```

### Libraries (`lib/`)

Frontend utilities and services:

```
lib/
├── api/                    # API Client Services
│   ├── client.ts           # Axios configuration with interceptors
│   ├── auth.ts             # Authentication API calls
│   ├── customers.ts        # Customer API calls
│   ├── invoices.ts         # Invoice API calls
│   └── payments.ts         # Payment API calls
│
├── stores/                 # Zustand State Management
│   ├── auth-store.ts       # Authentication state
│   ├── customer-store.ts   # Customer data state
│   ├── invoice-store.ts    # Invoice data state
│   └── payment-store.ts    # Payment data state
│
└── utils/                  # Utility Functions
    ├── formatters.ts       # Date, currency formatters
    ├── validators.ts       # Validation functions
    └── calculations.ts     # Invoice calculations
```

### Type Definitions (`types/`)

TypeScript interfaces matching backend DTOs:

```
types/
├── customer.ts             # Customer types
├── invoice.ts              # Invoice and LineItem types
├── payment.ts              # Payment types
├── api.ts                  # API response types
└── auth.ts                 # Authentication types
```

## Backend Structure

### Spring Boot Application (`backend/`)

Vertical Slice Architecture organized by feature:

```
backend/src/main/java/com/invoiceme/
│
├── InvoiceMeApplication.java   # Main application class
│
├── config/                     # Configuration Classes
│   ├── SecurityConfig.java     # Spring Security configuration
│   ├── JwtConfig.java          # JWT configuration
│   ├── CorsConfig.java         # CORS configuration
│   └── OpenApiConfig.java      # Swagger/OpenAPI configuration
│
├── auth/                       # Authentication Module
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── AuthController.java
│
├── common/                     # Shared Components
│   ├── exceptions/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── DomainException.java
│   │   ├── ValidationException.java
│   │   └── ResourceNotFoundException.java
│   │
│   ├── dto/
│   │   ├── PagedResponse.java
│   │   └── ApiErrorResponse.java
│   │
│   └── utils/
│       ├── DateUtils.java
│       └── ValidationUtils.java
│
└── features/                   # Vertical Slices (CQRS + DDD)
    │
    ├── customer/               # Customer Feature
    │   ├── domain/
    │   │   ├── Customer.java   # Domain entity
    │   │   └── CustomerRepository.java
    │   │
    │   ├── commands/           # Write Operations
    │   │   ├── CreateCustomerCommand.java
    │   │   ├── CreateCustomerCommandHandler.java
    │   │   ├── UpdateCustomerCommand.java
    │   │   ├── UpdateCustomerCommandHandler.java
    │   │   ├── DeleteCustomerCommand.java
    │   │   └── DeleteCustomerCommandHandler.java
    │   │
    │   ├── queries/            # Read Operations
    │   │   ├── GetCustomerByIdQuery.java
    │   │   ├── GetCustomerByIdQueryHandler.java
    │   │   ├── ListCustomersQuery.java
    │   │   └── ListCustomersQueryHandler.java
    │   │
    │   ├── api/                # REST Controllers
    │   │   ├── CustomerCommandController.java
    │   │   └── CustomerQueryController.java
    │   │
    │   └── dto/                # Data Transfer Objects
    │       ├── CreateCustomerDTO.java
    │       ├── UpdateCustomerDTO.java
    │       └── CustomerResponseDTO.java
    │
    ├── invoice/                # Invoice Feature (Same structure)
    │   ├── domain/
    │   │   ├── Invoice.java
    │   │   ├── LineItem.java   # Value object
    │   │   ├── InvoiceStatus.java  # Enum
    │   │   ├── InvoiceRepository.java
    │   │   └── InvoiceService.java
    │   ├── commands/
    │   ├── queries/
    │   ├── api/
    │   └── dto/
    │
    └── payment/                # Payment Feature (Same structure)
        ├── domain/
        ├── commands/
        ├── queries/
        ├── api/
        └── dto/
```

### Resources (`backend/src/main/resources/`)

Configuration and database migrations:

```
resources/
├── application.yml             # Main configuration
├── application-dev.yml         # Development profile
├── application-prod.yml        # Production profile
│
└── db/migration/               # Flyway Migrations
    ├── V1__initial_schema.sql
    ├── V2__add_payment_notes.sql
    └── V3__add_indexes.sql
```

### Tests (`backend/src/test/`)

Test organization mirrors main source structure:

```
test/java/com/invoiceme/
├── features/
│   ├── customer/
│   │   ├── domain/
│   │   │   └── CustomerTest.java
│   │   ├── commands/
│   │   │   └── CreateCustomerCommandHandlerTest.java
│   │   ├── queries/
│   │   │   └── ListCustomersQueryHandlerTest.java
│   │   └── api/
│   │       └── CustomerControllerIntegrationTest.java
│   │
│   ├── invoice/
│   └── payment/
│
└── testutils/
    ├── TestDataBuilder.java
    └── TestContainersConfig.java
```

## Testing Structure

### Frontend Tests

```
__tests__/
├── components/
│   ├── customers/
│   │   ├── customer-form.test.tsx
│   │   └── customer-list.test.tsx
│   ├── invoices/
│   └── payments/
│
├── lib/
│   ├── api/
│   │   └── customers.test.ts
│   └── utils/
│       └── calculations.test.ts
│
└── setup.ts
```

### E2E Tests

```
e2e/
├── auth/
│   └── login.spec.ts
├── customers/
│   └── customer-crud.spec.ts
├── invoices/
│   └── invoice-flow.spec.ts
└── payments/
    └── payment-recording.spec.ts
```

## Documentation Structure

```
docs/
├── architecture.md             # Technical architecture
├── prd.md                      # Product requirements
├── ai-development-report.md    # AI tools usage
│
├── api/
│   └── README.md               # API usage guide
│
├── database/
│   └── schema.md               # Database schema with ERD
│
├── deployment/
│   ├── README.md               # Deployment overview
│   ├── aws.md                  # AWS deployment
│   ├── azure.md                # Azure deployment
│   └── runbook.md              # Deployment procedures
│
├── testing/
│   └── README.md               # Testing strategy
│
├── configuration/
│   └── environment-variables.md
│
└── stories/                    # Development stories
    ├── 1.2.authentication.story.md
    ├── 1.3.customer-domain-commands.story.md
    └── ...
```

## Configuration Files

### Root Level

- **package.json**: Frontend dependencies and scripts
- **tsconfig.json**: TypeScript configuration
- **next.config.ts**: Next.js configuration
- **tailwind.config.ts**: Tailwind CSS configuration
- **jest.config.js**: Jest test configuration
- **playwright.config.ts**: Playwright E2E configuration
- **docker-compose.yml**: Local development services
- **Dockerfile**: Production container image
- **.env.example**: Environment variable template
- **.gitignore**: Git ignore rules

### Backend

- **pom.xml**: Maven dependencies and build configuration
- **application.yml**: Spring Boot configuration

## File Naming Conventions

### Frontend

| Type | Convention | Example |
|------|------------|---------|
| React Components | PascalCase | `CustomerForm.tsx` |
| Hooks | camelCase with 'use' prefix | `useCustomerStore.ts` |
| Utilities | camelCase | `formatCurrency.ts` |
| Types | PascalCase | `Customer.ts` |
| Tests | Component name + `.test.tsx` | `customer-form.test.tsx` |

### Backend

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `CreateCustomerCommand.java` |
| Interfaces | PascalCase | `CustomerRepository.java` |
| Commands | Noun + `Command` | `CreateCustomerCommand.java` |
| Queries | Noun + `Query` | `GetCustomerByIdQuery.java` |
| Handlers | Command/Query + `Handler` | `CreateCustomerCommandHandler.java` |
| Controllers | Feature + `Controller` | `CustomerCommandController.java` |
| Tests | Class + `Test` | `CustomerTest.java` |

## Guidelines for New Features

### Adding a New Feature

1. **Backend (Vertical Slice)**
   ```
   backend/src/main/java/com/invoiceme/features/newfeature/
   ├── domain/          # Create domain entity
   ├── commands/        # Add command handlers
   ├── queries/         # Add query handlers
   ├── api/             # Add controllers
   └── dto/             # Add DTOs
   ```

2. **Frontend**
   ```
   components/newfeature/    # Create components
   lib/api/newfeature.ts     # Add API client
   lib/stores/newfeature-store.ts  # Add state management
   types/newfeature.ts       # Add types
   app/(dashboard)/newfeature/  # Add routes
   ```

3. **Tests**
   ```
   backend/src/test/          # Add unit & integration tests
   __tests__/components/      # Add component tests
   e2e/                       # Add E2E test if critical flow
   ```

4. **Documentation**
   ```
   docs/                      # Update relevant docs
   ```

## Directory Organization Principles

1. **Feature Co-location**: Keep related files together
2. **Clear Separation**: Frontend and backend are separate
3. **Vertical Slices**: Backend organized by feature, not layer
4. **Test Proximity**: Tests mirror source structure
5. **Explicit Dependencies**: Clear import paths

## Common Pitfalls to Avoid

1. **Don't**: Mix business logic in controllers
   - **Do**: Keep logic in domain entities

2. **Don't**: Create shared "services" folder
   - **Do**: Keep services within feature slices

3. **Don't**: Put reusable components in feature folders
   - **Do**: Extract to `components/ui/` or `components/shared/`

4. **Don't**: Create deeply nested folder structures
   - **Do**: Keep maximum 3-4 levels deep

## Finding Files

### Backend

| You need... | Look in... |
|-------------|-----------|
| Domain entity | `features/{feature}/domain/` |
| Business logic | Domain entities |
| API endpoint | `features/{feature}/api/` |
| Data access | `features/{feature}/domain/{Feature}Repository.java` |
| Configuration | `config/` |

### Frontend

| You need... | Look in... |
|-------------|-----------|
| Page route | `app/(dashboard)/{feature}/` |
| Component | `components/{feature}/` |
| API call | `lib/api/{feature}.ts` |
| State management | `lib/stores/{feature}-store.ts` |
| Types | `types/{feature}.ts` |

## Resources

- Architecture decisions: [docs/architecture.md](architecture.md)
- API endpoints: [docs/api/README.md](api/README.md)
- Testing guide: [docs/testing/README.md](testing/README.md)

---

**Last Updated:** 2024-11-09
