# InvoiceMe - ERP Invoicing System

A production-quality ERP invoicing system demonstrating enterprise software development best practices with **Domain-Driven Design (DDD)**, **CQRS**, and **Vertical Slice Architecture**.

## Overview

InvoiceMe is a comprehensive invoicing application built to showcase modern software architecture patterns and full-stack development expertise. The system enables businesses to manage customers, create detailed invoices with line items (including taxes and discounts), and track payment history with automated reconciliation.

This project demonstrates:
- Clean architecture with clear separation of concerns
- Domain-driven design principles with rich domain models
- CQRS pattern for optimized read/write operations
- Vertical slice architecture for maintainable feature development
- Production-ready error handling and validation
- Comprehensive testing strategy (unit, integration, E2E)
- Docker-based deployment with cloud platform support

## 🏗️ Architecture

### Technology Stack

- **Frontend**: Next.js 14 with TypeScript, Tailwind CSS, and Shadcn/ui
- **Backend**: Spring Boot 3.2 with Java 17
- **Database**: PostgreSQL 15 (via Supabase locally)
- **Authentication**: JWT-based with Spring Security
- **State Management**: Zustand
- **API**: RESTful with OpenAPI/Swagger documentation
- **Testing**: Jest, React Testing Library, JUnit 5, Playwright
- **Deployment**: Docker containers (AWS/Azure compatible)

### Architectural Patterns

- **Domain-Driven Design (DDD)**: Business logic encapsulated in rich domain entities
- **CQRS**: Separate command and query models for optimal performance
- **Vertical Slice Architecture**: Features organized by business capability
- **Repository Pattern**: Abstracted data access layer
- **API Gateway Pattern**: Centralized cross-cutting concerns

For detailed architecture documentation, see [docs/architecture.md](docs/architecture.md).

## 📋 Features

### Customer Management
- Create, update, and soft-delete customers
- Search and filter customer list
- View customer statistics (total invoices, outstanding balance)
- Full CRUD operations with validation

### Invoice Management
- Create invoices with multiple line items
- Support for item-level discounts and taxes
- Automatic calculation of subtotals, taxes, and totals
- Invoice lifecycle management (Draft → Sent → Paid)
- Sequential invoice numbering (INV-2024-0001)
- Payment terms configuration (Net 30, Due on receipt, etc.)

### Payment Processing
- Record payments against invoices
- Multiple payment methods (Credit Card, Bank Transfer, Check, Cash)
- Automatic balance calculation and reconciliation
- Payment history tracking with date ranges
- Support for partial and full payments

### Dashboard & Analytics
- Real-time metrics and statistics
- Invoice status breakdown (Draft, Sent, Paid)
- Outstanding and overdue amounts
- Revenue tracking

## 🚀 Quick Start

### Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js**: >= 18.x ([Download](https://nodejs.org/))
- **Java**: >= 17 (JDK 17 recommended) ([Download](https://adoptium.net/))
- **Maven**: >= 3.9.x ([Download](https://maven.apache.org/download.cgi))
- **Docker**: >= 24.x ([Download](https://www.docker.com/products/docker-desktop/))
- **pnpm** (optional): >= 8.x (`npm install -g pnpm`)

### Local Development Setup

Follow these steps to set up the project locally:

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/invoice-me.git
cd invoice-me
```

#### 2. Install Dependencies

```bash
# Frontend dependencies
npm install
# or if using pnpm
pnpm install

# Backend dependencies
cd backend
mvn clean install
cd ..
```

#### 3. Configure Environment Variables

```bash
# Copy the example environment file
cp .env.example .env.local

# Edit .env.local and configure the following:
# - Database connection (defaults work for local development)
# - JWT secret (use a strong secret for production)
# - API URLs
```

See [docs/configuration/environment-variables.md](docs/configuration/environment-variables.md) for detailed configuration options.

#### 4. Start the Database

```bash
# Start PostgreSQL and Supabase Studio using Docker Compose
docker compose up -d

# Verify the database is running:
# - PostgreSQL: localhost:54322
# - Supabase Studio: http://localhost:54323
```

The database schema will be automatically created via Flyway migrations when the backend starts.

#### 5. Start the Applications

Open two terminal windows:

**Terminal 1 - Backend (Spring Boot):**
```bash
cd backend
mvn spring-boot:run

# Backend will be available at http://localhost:8080
# Health check: http://localhost:8080/api/actuator/health
# API docs: http://localhost:8080/swagger-ui.html
```

**Terminal 2 - Frontend (Next.js):**
```bash
npm run dev
# or
pnpm dev

# Frontend will be available at http://localhost:3000
```

#### 6. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Supabase Studio**: http://localhost:54323
- **Health Check**: http://localhost:8080/api/actuator/health

### Running with Docker Compose

To run the entire application stack with Docker:

```bash
# Build and start all services
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

This starts:
- PostgreSQL database
- Spring Boot backend
- Next.js frontend
- Nginx reverse proxy (if configured)

## 🧪 Testing

The project includes comprehensive testing at multiple levels:

### Frontend Tests

```bash
# Run all frontend tests
npm test

# Run tests in watch mode
npm test -- --watch

# Run tests with coverage
npm test -- --coverage

# Run specific test file
npm test customer-form.test.tsx
```

### Backend Tests

```bash
cd backend

# Run all backend tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerCommandHandlerTest

# Run tests with coverage report
mvn test jacoco:report

# View coverage report at:
# backend/target/site/jacoco/index.html
```

### E2E Tests

```bash
# Run end-to-end tests with Playwright
npm run test:e2e

# Run E2E tests in UI mode
npm run test:e2e:ui

# Run E2E tests in headed mode
npm run test:e2e:headed
```

### Test Coverage

Current test coverage:
- **Backend**: >80% line coverage (target: 80%+)
- **Frontend**: >75% line coverage (target: 75%+)
- **E2E**: Critical user flows covered

See [docs/testing/README.md](docs/testing/README.md) for detailed testing strategy and guidelines.

## 📁 Project Structure

The project follows a monorepo structure with clear separation between frontend and backend:

```
invoice-me/
├── app/                              # Next.js App Router (Frontend)
│   ├── (auth)/                       # Authentication routes
│   │   └── login/                    # Login page
│   └── (dashboard)/                  # Protected dashboard routes
│       ├── page.tsx                  # Dashboard home
│       ├── customers/                # Customer management
│       ├── invoices/                 # Invoice management
│       └── payments/                 # Payment tracking
│
├── components/                       # React Components
│   ├── ui/                           # Shadcn/ui base components
│   ├── customers/                    # Customer-specific components
│   ├── invoices/                     # Invoice-specific components
│   └── payments/                     # Payment-specific components
│
├── lib/                              # Frontend Utilities
│   ├── api/                          # API client services
│   │   ├── client.ts                 # Axios configuration
│   │   ├── customers.ts              # Customer API calls
│   │   ├── invoices.ts               # Invoice API calls
│   │   └── payments.ts               # Payment API calls
│   └── stores/                       # Zustand state management
│       ├── auth-store.ts
│       ├── customer-store.ts
│       ├── invoice-store.ts
│       └── payment-store.ts
│
├── types/                            # TypeScript Type Definitions
│   ├── customer.ts
│   ├── invoice.ts
│   ├── payment.ts
│   └── api.ts
│
├── backend/                          # Spring Boot Application
│   ├── src/main/java/com/invoiceme/
│   │   ├── InvoiceMeApplication.java # Main application class
│   │   ├── config/                   # Configuration classes
│   │   │   ├── SecurityConfig.java
│   │   │   └── JwtConfig.java
│   │   ├── auth/                     # Authentication module
│   │   ├── common/                   # Shared utilities
│   │   │   ├── exceptions/
│   │   │   ├── dto/
│   │   │   └── utils/
│   │   └── features/                 # Vertical Slices (DDD + CQRS)
│   │       ├── customer/
│   │       │   ├── domain/           # Domain entities
│   │       │   ├── commands/         # Write operations
│   │       │   ├── queries/          # Read operations
│   │       │   └── api/              # REST controllers
│   │       ├── invoice/              # Same structure
│   │       └── payment/              # Same structure
│   ├── src/main/resources/
│   │   ├── application.yml           # Application configuration
│   │   └── db/migration/             # Flyway database migrations
│   └── pom.xml                       # Maven dependencies
│
├── docs/                             # Documentation
│   ├── architecture.md               # Architecture overview
│   ├── prd.md                        # Product requirements
│   ├── api/                          # API documentation
│   ├── database/                     # Database schema docs
│   ├── deployment/                   # Deployment guides
│   ├── testing/                      # Testing guidelines
│   └── stories/                      # Development stories
│
├── e2e/                              # Playwright E2E tests
├── __tests__/                        # Jest unit tests
├── docker-compose.yml                # Local development setup
├── Dockerfile                        # Production container image
└── README.md                         # This file
```

For detailed project structure documentation, see [docs/project-structure.md](docs/project-structure.md).

## 🔧 Development

### Development Tools

#### API Documentation
Once the backend is running, access interactive API documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **API Guide**: [docs/api/README.md](docs/api/README.md)

#### Database Tools
- **Supabase Studio**: http://localhost:54323 (visual database management)
- **Direct PostgreSQL**: `localhost:54322` (user: `postgres`, password: `postgres`)
- **Schema Documentation**: [docs/database/schema.md](docs/database/schema.md)

#### Health Checks
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080/api/actuator/health
- **Database**: Check Supabase Studio

### Building for Production

#### Frontend Build
```bash
# Build optimized production bundle
npm run build

# Start production server
npm start

# Build statistics
npm run build -- --analyze
```

#### Backend Build
```bash
cd backend

# Build JAR file
mvn clean package

# Run JAR
java -jar target/invoice-me-0.0.1-SNAPSHOT.jar

# Skip tests during build
mvn clean package -DskipTests
```

#### Docker Build
```bash
# Build Docker images
docker build -t invoice-me-frontend .
docker build -t invoice-me-backend ./backend

# Run with Docker Compose
docker compose up -d
```

### Code Quality

```bash
# Frontend linting
npm run lint
npm run lint:fix

# Frontend formatting
npm run format
npm run format:check

# Backend code style (if configured)
cd backend
mvn checkstyle:check
mvn spotless:check
mvn spotless:apply
```

## 📚 Documentation

### Core Documentation
- **[Product Requirements Document](docs/prd.md)** - Business requirements and features
- **[Architecture Document](docs/architecture.md)** - Technical architecture and design decisions
- **[Project Structure](docs/project-structure.md)** - Codebase organization
- **[AI Development Report](docs/ai-development-report.md)** - AI tools usage and effectiveness

### Developer Guides
- **[Developer Setup Guide](docs/developer-setup.md)** - Detailed setup instructions for new developers
- **[API Usage Guide](docs/api/README.md)** - API endpoints and usage examples
- **[Database Schema](docs/database/schema.md)** - Database design and relationships
- **[Testing Strategy](docs/testing/README.md)** - Testing approach and guidelines

### Operations
- **[Deployment Guide](docs/deployment/README.md)** - Deployment instructions
- **[Deployment Runbook](docs/deployment/runbook.md)** - Step-by-step deployment procedures
- **[Environment Variables](docs/configuration/environment-variables.md)** - Configuration reference
- **[Troubleshooting Guide](docs/troubleshooting.md)** - Common issues and solutions

## 🤝 Contributing

While this is primarily an assessment project demonstrating architectural principles and patterns, contributions that improve the demonstration value are welcome.

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code of conduct
- Development workflow
- Code style guidelines
- Pull request process

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Next.js](https://nextjs.org/) and [Spring Boot](https://spring.io/projects/spring-boot)
- UI components from [Shadcn/ui](https://ui.shadcn.com/)
- Database powered by [PostgreSQL](https://www.postgresql.org/)
- Deployed with [Docker](https://www.docker.com/)

---

**Project Status**: Active Development

For questions or issues, please open an issue on GitHub or contact the development team.
