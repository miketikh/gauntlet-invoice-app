# InvoiceMe - ERP Invoicing System

A production-quality ERP invoicing system built with **Domain-Driven Design (DDD)**, **CQRS**, and **Vertical Slice Architecture**.

## 🏗️ Architecture

- **Frontend**: Next.js 14 with TypeScript, Tailwind CSS, and Shadcn/ui
- **Backend**: Spring Boot 3.2 with Java 17
- **Database**: PostgreSQL (via Supabase locally)
- **Authentication**: JWT-based
- **State Management**: Zustand
- **API**: RESTful with OpenAPI documentation

## 📋 Features

- **Customer Management**: Full CRUD operations with soft delete
- **Invoice Management**: Create invoices with line items, tax, and discounts
- **Payment Processing**: Record payments and track balances
- **Dashboard**: Real-time metrics and analytics

## 🚀 Quick Start

### Prerequisites

- Node.js >= 18.x
- Java >= 17
- Maven >= 3.9.x
- Docker >= 24.x

### Setup Instructions

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/invoice-me.git
cd invoice-me
```

2. **Set up environment variables**
```bash
cp .env.example .env.local
```

3. **Start the database**
```bash
docker compose up -d
```
This starts PostgreSQL on port 54322 and Supabase Studio on http://localhost:54323

4. **Install frontend dependencies**
```bash
npm install
```

5. **Set up the backend**
```bash
cd backend
mvn clean install
```

6. **Run database migrations** (automatic with Flyway on startup)

7. **Start the applications**

In separate terminals:

```bash
# Terminal 1: Frontend (http://localhost:3000)
npm run dev

# Terminal 2: Backend (http://localhost:8080)
cd backend
mvn spring-boot:run
```

## 🧪 Testing

```bash
# Frontend tests
npm test

# Backend tests
cd backend
mvn test

# E2E tests
npm run test:e2e
```

## 📁 Project Structure

```
invoice-me/
├── app/                    # Next.js app directory
├── components/             # React components
├── lib/                    # Frontend utilities
├── types/                  # TypeScript types
├── backend/                # Spring Boot application
│   └── src/main/java/com/invoiceme/
│       ├── config/         # Configuration classes
│       ├── features/       # Vertical slices (customer, invoice, payment)
│       │   ├── customer/
│       │   │   ├── domain/
│       │   │   ├── commands/
│       │   │   ├── queries/
│       │   │   └── api/
│       │   └── ...
│       └── common/         # Shared utilities
├── docker-compose.yml      # Local development setup
└── docs/                   # Documentation
    ├── prd.md             # Product Requirements
    └── architecture.md     # Technical Architecture
```

## 🔧 Development

### API Documentation

Once the backend is running, access the API documentation at:
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs

### Database Access

- **Supabase Studio**: http://localhost:54323
- **Direct PostgreSQL**: `localhost:54322` (user: postgres, password: postgres)

### Health Check

- Frontend: http://localhost:3000
- Backend: http://localhost:8080/api/actuator/health

## 📚 Documentation

- [Product Requirements Document](docs/prd.md)
- [Architecture Document](docs/architecture.md)
- [API Specification](http://localhost:8080/api/swagger-ui.html)

## 🤝 Contributing

This is an assessment project demonstrating architectural principles and patterns.

## 📄 License

MIT
