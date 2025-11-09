# InvoiceMe Fullstack Architecture Document

## Introduction

This document outlines the complete fullstack architecture for InvoiceMe, including backend systems, frontend implementation, and their integration. It serves as the single source of truth for AI-driven development, ensuring consistency across the entire technology stack.

This unified approach combines what would traditionally be separate backend and frontend architecture documents, streamlining the development process for modern fullstack applications where these concerns are increasingly intertwined.

### Starter Template or Existing Project

**Existing Setup:** Next.js with Shadcn/ui

The project has an existing Next.js application with Shadcn/ui already configured in the current directory. This provides the frontend foundation with:
- Next.js with TypeScript configured
- Tailwind CSS for styling
- Shadcn/ui component library integrated
- No existing business logic or domain code

The backend will be created from scratch using Spring Initializr for Spring Boot 3.x setup, following the DDD/CQRS/VSA principles outlined in the PRD.

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|---------|
| 2025-11-08 | v1.0 | Initial architecture document based on PRD requirements | Winston (Architect) |

## High Level Architecture

### Technical Summary

InvoiceMe implements a modular monolith architecture with a Spring Boot backend following Domain-Driven Design principles and a Next.js frontend using modern React patterns. The backend exposes RESTful APIs with CQRS separation, while the frontend provides a responsive SPA with server-side rendering capabilities. Integration occurs through well-defined REST endpoints with JWT authentication, deployed as containerized services to cloud platforms (AWS or Azure). This architecture achieves the PRD goals by ensuring clear separation of concerns, maintainability through vertical slices, and scalability through modular design that can evolve into microservices if needed.

### Platform and Infrastructure Choice

**Platform:** Docker containers deployable to AWS or Azure
**Key Services:**
- AWS: ECS/Fargate, RDS PostgreSQL, S3, CloudFront, Route 53
- Azure: Container Instances, PostgreSQL, Blob Storage, CDN, DNS
- Local: Docker Compose with Supabase (PostgreSQL + Studio)

**Deployment Host and Regions:**
- Primary: US-East-1 (AWS) or East US (Azure)
- Local development via Docker Compose

**Rationale:** Container-based deployment provides platform flexibility required for the assessment while maintaining production-grade infrastructure patterns. Docker ensures consistency across development and production environments.

### Repository Structure

**Structure:** Monorepo (Simple)
**Monorepo Tool:** None needed - simple folder structure
**Package Organization:**
- Root directory - Existing Next.js application
- `/backend` - Spring Boot application subdirectory
- `/types` - Shared TypeScript interfaces for DTOs (if needed)
- Root level Docker Compose for local development

### C4 Model Architecture Diagrams

The following diagrams use the C4 model to describe the system architecture at different levels of detail.

#### Level 1: System Context Diagram

```mermaid
C4Context
    title System Context - InvoiceMe

    Person(user, "Business User", "Creates invoices and tracks payments")
    System(invoiceMe, "InvoiceMe System", "Manages customers, invoices, and payment tracking")
    System_Ext(email, "Email System", "Sends invoice notifications (future)")
    System_Ext(payment, "Payment Gateway", "Processes online payments (future)")

    Rel(user, invoiceMe, "Uses", "HTTPS")
    Rel(invoiceMe, email, "Sends emails", "SMTP")
    Rel(invoiceMe, payment, "Processes payments", "API")
```

#### Level 2: Container Diagram

```mermaid
C4Container
    title Container Diagram - InvoiceMe System

    Person(user, "Business User")

    Container_Boundary(invoiceMe, "InvoiceMe System") {
        Container(frontend, "Frontend Application", "Next.js, React, TypeScript", "Provides UI for invoice management")
        Container(backend, "Backend API", "Spring Boot, Java 17", "Provides REST API with CQRS pattern")
        ContainerDb(database, "Database", "PostgreSQL", "Stores customers, invoices, and payments")
    }

    Rel(user, frontend, "Uses", "HTTPS")
    Rel(frontend, backend, "Makes API calls", "JSON/HTTPS")
    Rel(backend, database, "Reads from and writes to", "JDBC")
```

#### Level 3: Component Diagram - Backend

```mermaid
C4Component
    title Component Diagram - Backend API

    Container_Boundary(backend, "Backend API - Spring Boot") {
        Component(auth, "Authentication", "Spring Security, JWT", "Handles authentication and authorization")

        Component_Boundary(customer, "Customer Vertical Slice") {
            Component(custCmd, "Customer Commands", "Command Handlers", "Creates, updates, deletes customers")
            Component(custQry, "Customer Queries", "Query Handlers", "Retrieves customer data")
            Component(custDomain, "Customer Domain", "Domain Entity", "Customer business logic")
            Component(custRepo, "Customer Repository", "JPA Repository", "Customer data access")
        }

        Component_Boundary(invoice, "Invoice Vertical Slice") {
            Component(invCmd, "Invoice Commands", "Command Handlers", "Creates, sends invoices")
            Component(invQry, "Invoice Queries", "Query Handlers", "Retrieves invoice data")
            Component(invDomain, "Invoice Domain", "Aggregate Root", "Invoice business logic with line items")
            Component(invRepo, "Invoice Repository", "JPA Repository", "Invoice data access")
        }

        Component_Boundary(payment, "Payment Vertical Slice") {
            Component(payCmd, "Payment Commands", "Command Handlers", "Records payments")
            Component(payQry, "Payment Queries", "Query Handlers", "Retrieves payment data")
            Component(payDomain, "Payment Domain", "Domain Entity", "Payment business logic")
            Component(payRepo, "Payment Repository", "JPA Repository", "Payment data access")
        }
    }

    ContainerDb(database, "Database", "PostgreSQL")

    Rel(custCmd, custDomain, "Uses")
    Rel(custCmd, custRepo, "Uses")
    Rel(custQry, custRepo, "Uses")
    Rel(custRepo, database, "Reads/Writes")

    Rel(invCmd, invDomain, "Uses")
    Rel(invCmd, invRepo, "Uses")
    Rel(invQry, invRepo, "Uses")
    Rel(invRepo, database, "Reads/Writes")

    Rel(payCmd, payDomain, "Uses")
    Rel(payCmd, payRepo, "Uses")
    Rel(payQry, payRepo, "Uses")
    Rel(payRepo, database, "Reads/Writes")
```

### High Level Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Web Browser]
    end

    subgraph "Frontend - Next.js"
        NextApp[Next.js App<br/>React + TypeScript]
        NextApp --> Pages[Pages/App Router]
        NextApp --> Components[Shadcn/ui Components]
        NextApp --> Services[API Services]
    end

    subgraph "Backend - Spring Boot"
        API[REST API Gateway]
        API --> Auth[Auth Module<br/>JWT]
        API --> CustomerSlice[Customer<br/>Vertical Slice]
        API --> InvoiceSlice[Invoice<br/>Vertical Slice]
        API --> PaymentSlice[Payment<br/>Vertical Slice]

        CustomerSlice --> CustomerCmd[Commands]
        CustomerSlice --> CustomerQry[Queries]

        InvoiceSlice --> InvoiceCmd[Commands]
        InvoiceSlice --> InvoiceQry[Queries]

        PaymentSlice --> PaymentCmd[Commands]
        PaymentSlice --> PaymentQry[Queries]
    end

    subgraph "Data Layer"
        DB[(PostgreSQL<br/>Database)]
        CustomerCmd --> DB
        CustomerQry --> DB
        InvoiceCmd --> DB
        InvoiceQry --> DB
        PaymentCmd --> DB
        PaymentQry --> DB
    end

    Browser --> NextApp
    NextApp --> API
    Services --> API
```

### Sequence Diagrams for Key Flows

#### Authentication Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant API as Spring Boot API
    participant Security as Spring Security
    participant DB as PostgreSQL

    User->>Frontend: Enter credentials
    Frontend->>API: POST /api/auth/login
    API->>Security: Authenticate
    Security->>DB: Query user
    DB-->>Security: User details
    Security->>Security: Validate password
    Security->>Security: Generate JWT
    Security-->>API: JWT token + refresh token
    API-->>Frontend: {token, refreshToken, expiresIn}
    Frontend->>Frontend: Store tokens
    Frontend-->>User: Redirect to dashboard
```

#### Create Invoice Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant API as Invoice API
    participant Handler as CreateInvoiceHandler
    participant Domain as Invoice Entity
    participant Repo as InvoiceRepository
    participant DB as PostgreSQL

    User->>Frontend: Fill invoice form with line items
    Frontend->>Frontend: Calculate totals client-side
    Frontend->>API: POST /api/invoices (with JWT)
    API->>API: Validate JWT
    API->>Handler: CreateInvoiceCommand
    Handler->>Domain: new Invoice(command)
    Domain->>Domain: Validate business rules
    Domain->>Domain: Calculate line item totals
    Domain->>Domain: Calculate invoice totals
    Handler->>Repo: save(invoice)
    Repo->>DB: INSERT invoice
    DB-->>Repo: Invoice saved
    Repo-->>Handler: Invoice entity
    Handler->>Handler: Map to InvoiceResponseDTO
    Handler-->>API: InvoiceResponseDTO
    API-->>Frontend: 201 Created + invoice data
    Frontend-->>User: Show success message
    Frontend->>Frontend: Navigate to invoice detail
```

#### Record Payment Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant PaymentAPI as Payment API
    participant Handler as RecordPaymentHandler
    participant InvoiceRepo as InvoiceRepository
    participant PaymentRepo as PaymentRepository
    participant Invoice as Invoice Entity
    participant DB as PostgreSQL

    User->>Frontend: Enter payment details
    Frontend->>PaymentAPI: POST /api/invoices/{id}/payments (with JWT)
    PaymentAPI->>PaymentAPI: Validate JWT
    PaymentAPI->>Handler: RecordPaymentCommand

    Handler->>InvoiceRepo: findById(invoiceId)
    InvoiceRepo->>DB: SELECT invoice
    DB-->>InvoiceRepo: Invoice data
    InvoiceRepo-->>Handler: Invoice entity

    Handler->>Invoice: validate payment amount <= balance
    Invoice-->>Handler: Validation passed

    Handler->>Handler: Create Payment entity
    Handler->>PaymentRepo: save(payment)
    PaymentRepo->>DB: INSERT payment
    DB-->>PaymentRepo: Payment saved

    Handler->>Invoice: reduceBalance(payment.amount)
    Invoice->>Invoice: Update status if fully paid
    Handler->>InvoiceRepo: save(invoice)
    InvoiceRepo->>DB: UPDATE invoice
    DB-->>InvoiceRepo: Invoice updated

    Handler-->>PaymentAPI: PaymentResponseDTO
    PaymentAPI-->>Frontend: 201 Created + payment data
    Frontend->>Frontend: Refresh invoice balance
    Frontend-->>User: Show success message
```

### Architectural Patterns

- **Domain-Driven Design (DDD):** Three bounded contexts (Customer, Invoice, Payment) with rich domain models encapsulating business logic - *Rationale:* Ensures business logic remains cohesive and maintainable within domain boundaries
- **CQRS Pattern:** Separate command and query models with distinct DTOs and handlers - *Rationale:* Optimizes for different read/write patterns and enables independent scaling
- **Vertical Slice Architecture:** Features organized by domain capability rather than technical layers - *Rationale:* Reduces coupling and enables independent development of features
- **Repository Pattern:** Abstract data access behind interfaces in domain layer - *Rationale:* Enables testing with mocks and potential database migration
- **API Gateway Pattern:** Single REST API entry point with centralized cross-cutting concerns - *Rationale:* Simplifies authentication, validation, and error handling
- **Component-Based UI:** Reusable React components with TypeScript and Shadcn/ui - *Rationale:* Ensures UI consistency and type safety across the application
- **Container-First Deployment:** Docker containers for all services - *Rationale:* Platform-agnostic deployment with consistent environments
- **JWT Authentication:** Stateless token-based auth with refresh tokens - *Rationale:* Scalable authentication without server-side session management

### Architectural Decision Records (ADRs)

#### Why Domain-Driven Design?

**Context:** Need to manage complex business logic for invoicing with line items, tax calculations, and payment reconciliation.

**Decision:** Implement DDD with rich domain models that encapsulate business rules.

**Rationale:**
- Invoice calculations involve complex business logic that belongs in the domain
- Clear bounded contexts (Customer, Invoice, Payment) map naturally to business concepts
- Rich domain entities prevent anemic models and scattered business logic
- Enables ubiquitous language between developers and domain experts

**Consequences:**
- Steeper learning curve for developers unfamiliar with DDD
- More code compared to simple CRUD operations
- Better maintainability and testability of business logic
- Clear separation of concerns

#### Why CQRS?

**Context:** Read and write operations have different requirements and optimization needs.

**Decision:** Separate command and query operations with distinct handlers and DTOs.

**Rationale:**
- Write operations need validation, business rules, and transactional consistency
- Read operations need performance and denormalized data
- Dashboard queries join multiple entities while commands focus on single aggregates
- Enables independent optimization of reads vs writes

**Consequences:**
- More code structure (separate command/query folders)
- Clear separation makes it easier to understand data flow
- Can optimize queries without impacting command logic
- Prepared for event sourcing if needed in the future

#### Why Vertical Slice Architecture?

**Context:** Need to organize code for long-term maintainability and independent feature development.

**Decision:** Organize code by feature (vertical slices) rather than technical layers.

**Rationale:**
- Each feature (customer, invoice, payment) is self-contained
- Reduces coupling between features
- New developers can understand one feature without learning entire codebase
- Easier to extract features into microservices if needed
- Follows "high cohesion, low coupling" principle

**Consequences:**
- Different from traditional layered architecture
- Some code duplication between slices (acceptable trade-off)
- Clear feature boundaries improve team collaboration
- Easier to assign features to different developers

#### Why Monolithic Deployment?

**Context:** Need to choose between monolithic, microservices, or modular monolith architecture.

**Decision:** Deploy as a single monolithic application (modular internally).

**Rationale:**
- Simpler deployment and operations for assessment project
- Lower infrastructure costs and complexity
- Easier debugging and testing
- Modular structure allows future migration to microservices
- YAGNI principle - microservices add complexity without current need

**Consequences:**
- Must scale entire application (can't scale individual features)
- All features share same database and deployment
- Simpler CI/CD pipeline
- Vertical slice architecture provides migration path to microservices

## Tech Stack

### Technology Stack Table

| Category | Technology | Version | Purpose | Rationale |
|----------|------------|---------|---------|-----------|
| Frontend Language | TypeScript | 5.x | Type-safe frontend development | Type safety reduces runtime errors and improves maintainability |
| Frontend Framework | Next.js | 14.x | React framework with SSR/SSG | Already configured, provides routing and optimization out of box |
| UI Component Library | Shadcn/ui | Latest | Accessible component library | Already integrated, provides professional UI components |
| State Management | Zustand | 4.x | Global state management | Lightweight (8kb), simple API, better performance than Context, no providers needed |
| Backend Language | Java | 17 | Backend development language | Modern Java with records, sealed classes for DDD |
| Backend Framework | Spring Boot | 3.2.x | Application framework | Industry standard for enterprise Java applications |
| API Style | REST | - | HTTP API protocol | Simple, well-understood, matches PRD requirements |
| Database | PostgreSQL (Supabase locally) | 15 | Primary data store | PostgreSQL with Supabase Studio for visual DB management in development |
| Cache | None initially | - | - | YAGNI - add caching if performance requires |
| File Storage | Local filesystem | - | Development only | Cloud storage (S3/Azure Blob) for production |
| Authentication | Spring Security + JWT | - | Auth implementation | Integrated with Spring Boot, stateless tokens |
| Frontend Testing | Jest + React Testing Library | 29.x + 14.x | Unit and component tests | Standard React testing tools |
| Backend Testing | JUnit + Mockito | 5.x | Unit and integration tests | Spring Boot default testing framework |
| E2E Testing | Playwright | 1.40.x | End-to-end testing | Modern, fast, reliable cross-browser testing |
| Build Tool | Maven | 3.9.x | Java build tool | Simple, mature, no Gradle complexity |
| Bundler | Next.js built-in (Webpack) | - | Frontend bundling | Configured by Next.js |
| IaC Tool | Docker Compose | 2.x | Local infrastructure | Simple container orchestration for development |
| CI/CD | GitHub Actions | - | Continuous integration | Free for public repos, integrated with GitHub |
| Monitoring | Console logs + Spring Actuator | - | Basic monitoring | Sufficient for assessment, add APM in production |
| Logging | SLF4J/Logback | 1.4.x | Structured logging | Spring Boot default with good flexibility |
| CSS Framework | Tailwind CSS | 3.x | Utility-first CSS | Already configured with Next.js setup |

## Data Models

### Customer

**Purpose:** Represents a business entity that receives invoices and makes payments

**Key Attributes:**
- id: UUID - Unique identifier
- name: string - Company or individual name
- email: string - Primary contact email
- phone: string - Contact phone number
- address: string - Billing address
- createdAt: timestamp - Record creation time
- updatedAt: timestamp - Last modification time
- isDeleted: boolean - Soft delete flag

#### TypeScript Interface
```typescript
interface Customer {
  id: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  createdAt: Date;
  updatedAt: Date;
  isDeleted: boolean;
}

interface CreateCustomerDTO {
  name: string;
  email: string;
  phone: string;
  address: string;
}

interface CustomerResponseDTO extends Customer {
  totalInvoices?: number;
  outstandingBalance?: number;
}
```

#### Relationships
- One-to-Many with Invoice (one customer can have many invoices)
- Indirect relationship with Payment through Invoice

### Invoice

**Purpose:** Represents a billable document with line items that tracks amounts owed and payment status

**Key Attributes:**
- id: UUID - Unique identifier
- invoiceNumber: string - Sequential formatted number (INV-2024-0001)
- customerId: UUID - Foreign key to Customer
- issueDate: date - Date invoice was created
- dueDate: date - Payment due date
- status: enum - Draft | Sent | Paid
- paymentTerms: string - Payment terms (Net 30, Due on receipt, etc.)
- subtotal: decimal - Sum of line items before tax
- totalDiscount: decimal - Sum of all discounts
- totalTax: decimal - Sum of all tax amounts
- totalAmount: decimal - Final amount due (subtotal - discount + tax)
- balance: decimal - Remaining amount after payments
- lineItems: LineItem[] - Array of line items
- notes: string - Optional invoice notes
- createdAt: timestamp - Record creation time
- updatedAt: timestamp - Last modification time

#### TypeScript Interface
```typescript
type InvoiceStatus = 'Draft' | 'Sent' | 'Paid';

interface LineItem {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number; // 0.10 = 10% discount, 0 = no discount
  taxRate: number;         // 0.08 = 8% tax, 0 = no tax

  // Calculated fields (computed in domain logic):
  subtotal: number;        // quantity * unitPrice
  discountAmount: number;  // subtotal * discountPercent
  taxableAmount: number;   // subtotal - discountAmount
  taxAmount: number;       // taxableAmount * taxRate
  total: number;           // taxableAmount + taxAmount
}

interface Invoice {
  id: string;
  invoiceNumber: string;
  customerId: string;
  issueDate: Date;
  dueDate: Date;
  status: InvoiceStatus;
  paymentTerms: string;    // "Net 30", "Due on receipt", etc.

  // Calculated totals from line items:
  subtotal: number;        // Sum of line items subtotal
  totalDiscount: number;   // Sum of line items discountAmount
  totalTax: number;        // Sum of line items taxAmount
  totalAmount: number;     // Sum of line items total
  balance: number;         // totalAmount - sum of payments

  lineItems: LineItem[];
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}

interface CreateInvoiceDTO {
  customerId: string;
  issueDate: Date;
  paymentTerms: string;
  lineItems: Array<{
    description: string;
    quantity: number;
    unitPrice: number;
    discountPercent?: number;
    taxRate?: number;
  }>;
  notes?: string;
}

interface InvoiceResponseDTO extends Invoice {
  customerName?: string;
  customerEmail?: string;
  paymentHistory?: Payment[];
  daysOverdue?: number;    // Calculated if past due date
}
```

#### Relationships
- Many-to-One with Customer (many invoices belong to one customer)
- One-to-Many with LineItem (one invoice has many line items)
- One-to-Many with Payment (one invoice can have multiple payments)

### Payment

**Purpose:** Records payment transactions applied against invoices to track balance reduction

**Key Attributes:**
- id: UUID - Unique identifier
- invoiceId: UUID - Foreign key to Invoice
- paymentDate: date - Date payment was made
- amount: decimal - Payment amount
- paymentMethod: enum - CreditCard | BankTransfer | Check | Cash
- reference: string - Transaction reference or check number
- notes: string - Optional payment notes
- createdAt: timestamp - Record creation time
- createdBy: string - User who recorded payment

#### TypeScript Interface
```typescript
type PaymentMethod = 'CreditCard' | 'BankTransfer' | 'Check' | 'Cash';

interface Payment {
  id: string;
  invoiceId: string;
  paymentDate: Date;
  amount: number;
  paymentMethod: PaymentMethod;
  reference: string;
  notes?: string;
  createdAt: Date;
  createdBy: string;
}

interface RecordPaymentDTO {
  invoiceId: string;
  paymentDate: Date;
  amount: number;
  paymentMethod: PaymentMethod;
  reference: string;
  notes?: string;
}

interface PaymentResponseDTO extends Payment {
  invoiceNumber?: string;
  customerName?: string;
  remainingBalance?: number;
}
```

#### Relationships
- Many-to-One with Invoice (many payments can be applied to one invoice)
- Indirect relationship with Customer through Invoice

## API Specification

### REST API Specification

```yaml
openapi: 3.0.0
info:
  title: InvoiceMe API
  version: 1.0.0
  description: RESTful API for ERP invoicing system with CQRS pattern

servers:
  - url: http://localhost:8080/api
    description: Local development server
  - url: https://api.invoiceme.com
    description: Production server

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    Customer:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        email:
          type: string
        phone:
          type: string
        address:
          type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
        isDeleted:
          type: boolean

    LineItem:
      type: object
      properties:
        id:
          type: string
        description:
          type: string
        quantity:
          type: number
        unitPrice:
          type: number
        discountPercent:
          type: number
        taxRate:
          type: number
        subtotal:
          type: number
        discountAmount:
          type: number
        taxableAmount:
          type: number
        taxAmount:
          type: number
        total:
          type: number

    Invoice:
      type: object
      properties:
        id:
          type: string
        invoiceNumber:
          type: string
        customerId:
          type: string
        status:
          type: string
          enum: [Draft, Sent, Paid]
        # ... other fields

paths:
  # Authentication
  /auth/login:
    post:
      summary: Login with credentials
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                username:
                  type: string
                password:
                  type: string
      responses:
        200:
          description: Login successful
          content:
            application/json:
              schema:
                type: object
                properties:
                  token:
                    type: string
                  refreshToken:
                    type: string
                  expiresIn:
                    type: integer

  /auth/refresh:
    post:
      summary: Refresh access token
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                refreshToken:
                  type: string

  # Customer endpoints (CQRS separated)
  /customers:
    post:
      summary: Create customer (Command)
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateCustomerDTO'
      responses:
        201:
          description: Customer created

    get:
      summary: List customers (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: page
          in: query
          schema:
            type: integer
        - name: size
          in: query
          schema:
            type: integer
        - name: search
          in: query
          schema:
            type: string
      responses:
        200:
          description: List of customers

  /customers/{id}:
    get:
      summary: Get customer by ID (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: Customer details

    put:
      summary: Update customer (Command)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateCustomerDTO'

    delete:
      summary: Delete customer (Command)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

  # Invoice endpoints
  /invoices:
    post:
      summary: Create invoice (Command)
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateInvoiceDTO'

    get:
      summary: List invoices (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: customerId
          in: query
          schema:
            type: string
        - name: status
          in: query
          schema:
            type: string
            enum: [Draft, Sent, Paid]
        - name: startDate
          in: query
          schema:
            type: string
            format: date
        - name: endDate
          in: query
          schema:
            type: string
            format: date

  /invoices/{id}:
    get:
      summary: Get invoice by ID (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

    put:
      summary: Update invoice (Command - Draft only)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

  /invoices/{id}/send:
    post:
      summary: Send invoice (Command - state transition)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

  /invoices/{id}/payments:
    post:
      summary: Record payment (Command)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RecordPaymentDTO'

    get:
      summary: Get payments for invoice (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

  # Payment endpoints
  /payments:
    get:
      summary: List all payments (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: startDate
          in: query
          schema:
            type: string
            format: date
        - name: endDate
          in: query
          schema:
            type: string
            format: date
        - name: customerId
          in: query
          schema:
            type: string

  /payments/{id}:
    get:
      summary: Get payment by ID (Query)
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string

  # Dashboard/Analytics endpoints
  /dashboard/stats:
    get:
      summary: Get dashboard statistics (Query)
      security:
        - bearerAuth: []
      responses:
        200:
          description: Dashboard stats
          content:
            application/json:
              schema:
                type: object
                properties:
                  totalCustomers:
                    type: integer
                  totalInvoices:
                    type: integer
                  draftInvoices:
                    type: integer
                  sentInvoices:
                    type: integer
                  paidInvoices:
                    type: integer
                  totalRevenue:
                    type: number
                  outstandingAmount:
                    type: number
                  overdueAmount:
                    type: number
```

## Components

### Frontend Components

#### Authentication Service
**Responsibility:** Manages JWT tokens, login/logout flow, and authenticated API requests

**Key Interfaces:**
- `login(credentials)` - Authenticates user and stores tokens
- `logout()` - Clears tokens and redirects to login
- `refreshToken()` - Refreshes expired access token
- `getAuthHeader()` - Returns Bearer token header

**Dependencies:** Zustand auth store, Next.js router

**Technology Stack:** TypeScript, Axios interceptors for token refresh

#### Customer Management Module
**Responsibility:** UI components and state management for customer CRUD operations

**Key Interfaces:**
- `CustomerList` - Data table with search/filter/pagination
- `CustomerForm` - Create/edit form with validation
- `CustomerDetail` - Read-only customer view with stats
- `useCustomerStore()` - Zustand store for customer state

**Dependencies:** API Service, Shadcn/ui components

**Technology Stack:** React, TypeScript, Zustand, React Hook Form

#### Invoice Management Module
**Responsibility:** Complex invoice creation, editing, and lifecycle management

**Key Interfaces:**
- `InvoiceList` - Filterable list with status badges
- `InvoiceForm` - Multi-step creation with line items
- `InvoiceDetail` - Print-friendly invoice view
- `LineItemManager` - Dynamic line item add/remove with calculations
- `useInvoiceStore()` - Zustand store for invoice state

**Dependencies:** Customer Module, API Service, date-fns

**Technology Stack:** React, TypeScript, Zustand, React Hook Form, Tailwind CSS

#### Payment Recording Module
**Responsibility:** Payment entry and history tracking

**Key Interfaces:**
- `PaymentForm` - Payment recording with validation
- `PaymentHistory` - List of payments with filters
- `PaymentSummary` - Payment statistics and totals
- `usePaymentStore()` - Zustand store for payment state

**Dependencies:** Invoice Module, API Service

**Technology Stack:** React, TypeScript, Zustand

### Backend Components

#### Authentication Module
**Responsibility:** JWT generation, validation, and Spring Security configuration

**Key Interfaces:**
- `JwtTokenProvider` - Generate and validate tokens
- `AuthenticationController` - Login/refresh endpoints
- `JwtAuthenticationFilter` - Request authentication
- `UserDetailsService` - Load user for authentication

**Dependencies:** Spring Security, JJWT library

**Technology Stack:** Spring Security, JWT, BCrypt

#### Customer Vertical Slice
**Responsibility:** Complete customer domain implementation with CQRS

**Key Interfaces:**
- **Commands:**
  - `CreateCustomerCommand` + `CreateCustomerCommandHandler`
  - `UpdateCustomerCommand` + `UpdateCustomerCommandHandler`
  - `DeleteCustomerCommand` + `DeleteCustomerCommandHandler`
- **Queries:**
  - `GetCustomerByIdQuery` + `GetCustomerByIdQueryHandler`
  - `ListCustomersQuery` + `ListCustomersQueryHandler`
- **Domain:**
  - `Customer` entity with business logic
  - `CustomerRepository` interface
- **API:**
  - `CustomerCommandController` - Command endpoints
  - `CustomerQueryController` - Query endpoints

**Dependencies:** Spring Data JPA, PostgreSQL

**Technology Stack:** Spring Boot, JPA/Hibernate, Java 17

#### Invoice Vertical Slice
**Responsibility:** Invoice aggregate with line items and lifecycle management

**Key Interfaces:**
- **Commands:**
  - `CreateInvoiceCommand` + Handler
  - `UpdateInvoiceCommand` + Handler
  - `SendInvoiceCommand` + Handler
- **Queries:**
  - `GetInvoiceByIdQuery` + Handler
  - `ListInvoicesQuery` + Handler
- **Domain:**
  - `Invoice` aggregate root
  - `LineItem` value object
  - `InvoiceService` - Complex calculations
  - `InvoiceNumberGenerator` - Sequential numbering
- **API:**
  - `InvoiceCommandController`
  - `InvoiceQueryController`

**Dependencies:** Customer Slice, Spring Data JPA

**Technology Stack:** Spring Boot, JPA with JSON column support for line items

#### Payment Vertical Slice
**Responsibility:** Payment recording and invoice balance reconciliation

**Key Interfaces:**
- **Commands:**
  - `RecordPaymentCommand` + Handler
- **Queries:**
  - `GetPaymentByIdQuery` + Handler
  - `ListPaymentsQuery` + Handler
- **Domain:**
  - `Payment` entity
  - `PaymentService` - Balance reconciliation
  - `PaymentRepository` interface
- **API:**
  - `PaymentCommandController`
  - `PaymentQueryController`

**Dependencies:** Invoice Slice, Spring Transactions

**Technology Stack:** Spring Boot, JPA, @Transactional for consistency

#### Cross-Cutting Components

##### Exception Handling
**Responsibility:** Global exception handling and error response formatting

**Key Interfaces:**
- `GlobalExceptionHandler` - @ControllerAdvice
- `ApiErrorResponse` - Standard error format
- `DomainException` - Business rule violations
- `ValidationException` - Input validation errors

**Dependencies:** Spring Web

**Technology Stack:** Spring Boot @ExceptionHandler

##### Audit & Logging
**Responsibility:** Request/response logging and audit trail

**Key Interfaces:**
- `AuditAspect` - @Aspect for command auditing
- `LoggingFilter` - HTTP request/response logging
- `CorrelationIdFilter` - Request tracking

**Dependencies:** SLF4J, Logback, Spring AOP

**Technology Stack:** Spring AOP, MDC for correlation IDs

### Component Interaction Diagram

```mermaid
sequenceDiagram
    participant UI as Next.js UI
    participant Auth as Auth Service
    participant API as Spring API
    participant CMD as Command Handler
    participant QRY as Query Handler
    participant DB as PostgreSQL

    UI->>Auth: Login
    Auth->>API: POST /auth/login
    API-->>Auth: JWT Token
    Auth-->>UI: Store Token

    UI->>API: GET /customers (with JWT)
    API->>QRY: ListCustomersQuery
    QRY->>DB: SELECT
    DB-->>QRY: Results
    QRY-->>API: CustomerListDTO
    API-->>UI: JSON Response

    UI->>API: POST /invoices (with JWT)
    API->>CMD: CreateInvoiceCommand
    CMD->>DB: INSERT
    CMD->>CMD: Calculate totals
    CMD->>DB: UPDATE
    DB-->>CMD: Success
    CMD-->>API: Invoice ID
    API-->>UI: 201 Created
```

## Database Schema

```sql
-- Customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_is_deleted ON customers(is_deleted);

-- Invoices table (line_items stored as JSON)
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('Draft', 'Sent', 'Paid')),
    payment_terms VARCHAR(100),

    -- Line items as JSONB for flexibility
    line_items JSONB NOT NULL DEFAULT '[]',

    -- Calculated totals (denormalized for query performance)
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_discount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_tax DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0,

    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);

-- Payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('CreditCard', 'BankTransfer', 'Check', 'Cash')),
    reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);

-- Users table (simple auth)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Invoice number sequence
CREATE SEQUENCE invoice_number_seq START 1;

-- Function to generate invoice numbers
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS VARCHAR AS $$
BEGIN
    RETURN 'INV-' || EXTRACT(YEAR FROM CURRENT_DATE) || '-' || LPAD(nextval('invoice_number_seq')::TEXT, 4, '0');
END;
$$ LANGUAGE plpgsql;
```

## Unified Project Structure

```plaintext
invoice-me/                           # Root directory (Next.js frontend)
├── app/                              # Next.js App Router
│   ├── (auth)/                       # Auth group
│   │   ├── login/
│   │   │   └── page.tsx
│   │   └── layout.tsx
│   ├── (dashboard)/                  # Protected routes
│   │   ├── layout.tsx                # Dashboard layout
│   │   ├── page.tsx                  # Dashboard home
│   │   ├── customers/
│   │   │   ├── page.tsx              # Customer list
│   │   │   ├── new/
│   │   │   │   └── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── invoices/
│   │   │   ├── page.tsx              # Invoice list
│   │   │   ├── new/
│   │   │   │   └── page.tsx
│   │   │   └── [id]/
│   │   │       ├── page.tsx
│   │   │       └── payment/
│   │   │           └── page.tsx
│   │   └── payments/
│   │       └── page.tsx
│   └── api/                          # Next.js API routes (if needed)
│
├── components/                       # React components
│   ├── ui/                           # Shadcn/ui components
│   ├── customers/
│   │   ├── customer-list.tsx
│   │   ├── customer-form.tsx
│   │   └── customer-detail.tsx
│   ├── invoices/
│   │   ├── invoice-list.tsx
│   │   ├── invoice-form.tsx
│   │   ├── invoice-detail.tsx
│   │   └── line-item-manager.tsx
│   └── payments/
│       ├── payment-form.tsx
│       └── payment-history.tsx
│
├── lib/                              # Frontend utilities
│   ├── api/                          # API client
│   │   ├── client.ts                 # Axios setup
│   │   ├── auth.ts
│   │   ├── customers.ts
│   │   ├── invoices.ts
│   │   └── payments.ts
│   └── stores/                       # Zustand stores
│       ├── auth-store.ts
│       ├── customer-store.ts
│       ├── invoice-store.ts
│       └── payment-store.ts
│
├── types/                            # TypeScript types
│   ├── customer.ts
│   ├── invoice.ts
│   ├── payment.ts
│   └── api.ts
│
├── backend/                          # Spring Boot application
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── invoiceme/
│   │       │           ├── InvoiceMeApplication.java
│   │       │           ├── config/
│   │       │           │   ├── SecurityConfig.java
│   │       │           │   └── JwtConfig.java
│   │       │           ├── common/
│   │       │           │   ├── exceptions/
│   │       │           │   ├── utils/
│   │       │           │   └── dto/
│   │       │           ├── auth/
│   │       │           │   ├── AuthController.java
│   │       │           │   ├── JwtTokenProvider.java
│   │       │           │   └── UserDetailsServiceImpl.java
│   │       │           └── features/
│   │       │               ├── customer/
│   │       │               │   ├── domain/
│   │       │               │   │   ├── Customer.java
│   │       │               │   │   └── CustomerRepository.java
│   │       │               │   ├── commands/
│   │       │               │   │   ├── CreateCustomerCommand.java
│   │       │               │   │   ├── CreateCustomerCommandHandler.java
│   │       │               │   │   └── ...
│   │       │               │   ├── queries/
│   │       │               │   │   ├── GetCustomerByIdQuery.java
│   │       │               │   │   ├── GetCustomerByIdQueryHandler.java
│   │       │               │   │   └── ...
│   │       │               │   └── api/
│   │       │               │       ├── CustomerCommandController.java
│   │       │               │       └── CustomerQueryController.java
│   │       │               ├── invoice/
│   │       │               │   └── ... (same structure)
│   │       │               └── payment/
│   │       │                   └── ... (same structure)
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/
│   │               └── migration/
│   │                   └── V1__initial_schema.sql
│   └── pom.xml
│
├── docker-compose.yml                # Local development setup
├── .env.example                      # Environment variables template
├── package.json                      # Frontend dependencies
└── README.md                         # Project documentation
```

## Development Workflow

### Local Development Setup

#### Prerequisites
```bash
# Required tools
node >= 18.x
npm >= 9.x
java >= 17
maven >= 3.9.x
docker >= 24.x
```

#### Initial Setup
```bash
# 1. Clone repository and install frontend dependencies
npm install

# 2. Setup backend
cd backend
mvn clean install

# 3. Setup database (Supabase local)
docker compose up -d

# 4. Copy environment variables
cp .env.example .env.local

# 5. Run database migrations
cd backend
mvn flyway:migrate
```

#### Development Commands
```bash
# Start all services
docker compose up -d        # Database
npm run dev                 # Frontend (http://localhost:3000)
cd backend && mvn spring-boot:run  # Backend (http://localhost:8080)

# Or use single command (if configured)
npm run dev:all

# Run tests
npm test                    # Frontend tests
cd backend && mvn test      # Backend tests
npm run test:e2e            # E2E tests with Playwright
```

### Environment Configuration

```bash
# Frontend (.env.local)
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_NAME=InvoiceMe

# Backend (application.yml or .env)
DATABASE_URL=postgresql://postgres:password@localhost:5432/invoiceme
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=3600000  # 1 hour

# Supabase (docker-compose.yml provides defaults)
POSTGRES_PASSWORD=postgres
POSTGRES_DB=invoiceme
```

## Coding Standards

### Critical Fullstack Rules

- **API Contracts First:** Always define TypeScript interfaces that match Java DTOs exactly
- **Vertical Slice Isolation:** Each feature (customer, invoice, payment) must be completely self-contained in its folder
- **CQRS Separation:** Never mix command and query logic - separate controllers, separate handlers
- **Domain Logic in Entities:** Business rules belong in domain objects, not services or controllers
- **Immutable State Updates:** In React, never mutate state directly - use Zustand's set function properly
- **Error Boundaries:** Every major UI section needs an error boundary to prevent full app crashes
- **Transaction Boundaries:** Use @Transactional only at command handler level, never at controller level
- **DTO Pattern:** Never expose domain entities directly through APIs - always use DTOs
- **Validation Layers:** Validate in frontend (UX), controller (security), and domain (business rules)
- **Consistent Naming:** Commands end with "Command", Queries with "Query", Handlers with "Handler"

### Naming Conventions

| Element | Frontend | Backend | Example |
|---------|----------|---------|---------|
| Components | PascalCase | - | `CustomerList.tsx` |
| Hooks | camelCase with 'use' | - | `useCustomerStore.ts` |
| API Routes | - | kebab-case | `/api/customers` |
| Java Classes | - | PascalCase | `CustomerCommandHandler.java` |
| Database Tables | - | snake_case | `customer_invoices` |
| Environment Vars | SCREAMING_SNAKE | SCREAMING_SNAKE | `DATABASE_URL` |

### TypeScript/Java Alignment

```typescript
// Frontend: types/customer.ts
interface CreateCustomerDTO {
  name: string;
  email: string;
  phone: string;
  address: string;
}
```

```java
// Backend: dto/CreateCustomerDTO.java
public record CreateCustomerDTO(
    String name,
    String email,
    String phone,
    String address
) {}
```

### Git Commit Convention
```
feat(invoice): add line item tax calculation
fix(auth): resolve token refresh race condition
refactor(customer): extract validation logic
test(payment): add reconciliation tests
docs(api): update endpoint documentation
```