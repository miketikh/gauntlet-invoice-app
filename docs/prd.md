# InvoiceMe Product Requirements Document (PRD)

## Goals and Background Context

### Goals
- Implement a production-quality ERP-style invoicing system demonstrating modern software architecture principles
- Build a fully functional system managing Customers, Invoices, and Payments with proper domain modeling
- Demonstrate mastery of Domain-Driven Design (DDD), CQRS, and Vertical Slice Architecture
- Create a scalable, maintainable solution that mirrors real-world SaaS ERP development
- Leverage AI tools effectively as development accelerators while maintaining architectural integrity
- Deliver a complete solution with RESTful APIs, responsive UI, and integration testing

### Background Context

This project addresses the challenge of building enterprise-grade software in an AI-assisted development environment. While AI tools can accelerate code generation, they don't inherently guarantee sound system design. InvoiceMe proves that architectural guidance and proper design principles are essential for creating maintainable, scalable applications, with AI serving as an accelerator rather than the primary designer.

The system focuses on core ERP business domains - managing customers, creating and tracking invoices with line items, and processing payments. It implements a complete invoice lifecycle (Draft → Sent → Paid) with proper balance calculations and payment applications, providing a realistic simulation of production SaaS requirements.

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|---------|
| 2025-11-08 | v1.0 | Initial PRD creation based on assessment requirements | John (PM) |

## Requirements

### Functional

- **FR1:** The system must provide complete CRUD operations for Customer entities (Create, Read, Update, Delete)
- **FR2:** The system must support Invoice creation with multiple Line Items including service/product descriptions, quantities, and unit prices
- **FR3:** The system must implement Invoice lifecycle state transitions: Draft → Sent → Paid
- **FR4:** The system must calculate and maintain accurate Invoice balances considering all line items and applied payments
- **FR5:** The system must record and apply Payments against specific Invoices with proper balance reconciliation
- **FR6:** The system must provide authentication functionality to secure access to application data
- **FR7:** The system must expose RESTful APIs for all domain operations following CQRS principles
- **FR8:** The system must provide Query operations to retrieve entities by ID and list entities by various filters (status, customer, etc.)
- **FR9:** The system must maintain referential integrity between Customers, Invoices, and Payments

### Non Functional

- **NFR1:** API response times for standard CRUD operations must be under 200ms in local testing environment
- **NFR2:** The UI must provide smooth, responsive interactions without noticeable lag
- **NFR3:** The system must implement Domain-Driven Design with rich domain objects encapsulating business logic
- **NFR4:** The system must maintain clear separation between Commands (write) and Queries (read) operations
- **NFR5:** The codebase must follow Vertical Slice Architecture organizing code around features rather than technical layers
- **NFR6:** The system must maintain clear boundaries between Domain, Application, and Infrastructure layers (Clean Architecture)
- **NFR7:** The system must use DTOs and mappers for data transfer across layer boundaries
- **NFR8:** The system must include comprehensive integration tests verifying end-to-end functionality
- **NFR9:** The system must be deployable to either AWS or Azure cloud platforms
- **NFR10:** The codebase must follow consistent naming conventions and maintain high readability standards

## User Interface Design Goals

### Overall UX Vision
Modern, clean ERP interface following established business application patterns. The design should prioritize efficiency and data clarity, with intuitive navigation between customer management, invoice creation, and payment processing workflows. Focus on reducing cognitive load through consistent patterns and clear visual hierarchy typical of professional accounting software.

### Key Interaction Paradigms
- **Command-Query Separation in UI:** Distinct visual patterns for read-only views (queries) versus action forms (commands)
- **Progressive Disclosure:** Complex invoice creation flows broken into logical steps with clear progress indication
- **Inline Validation:** Real-time feedback for data entry, particularly for financial calculations
- **Contextual Actions:** Action buttons positioned relative to their data context (row-level actions in tables)
- **Master-Detail Pattern:** List views with expandable detail panels for quick information access

### Core Screens and Views
- **Login Screen** - Authentication gateway with basic credential validation
- **Main Dashboard** - Overview of key metrics: outstanding invoices, recent payments, customer summary
- **Customer List View** - Searchable/filterable grid with inline actions for edit/delete
- **Customer Detail/Edit View** - Form for creating/updating customer information
- **Invoice List View** - Filterable by status (Draft/Sent/Paid), customer, date range
- **Invoice Creation/Edit View** - Multi-step form with line item management and running total calculation
- **Invoice Detail View** - Read-only presentation format suitable for printing/PDF export
- **Payment Recording View** - Modal or dedicated form for applying payments to invoices
- **Payment History View** - Transaction log showing all payments with invoice associations

### Accessibility: WCAG AA
Ensuring Level AA compliance for enterprise accessibility requirements, including proper ARIA labels, keyboard navigation support, and sufficient color contrast ratios for financial data presentation.

### Branding
Clean, professional aesthetic appropriate for B2B financial software. Neutral color palette with accent colors for status indicators (draft=gray, sent=blue, paid=green). Typography should prioritize readability of numerical data with clear distinction between labels and values.

### Target Device and Platforms: Web Responsive
Primary focus on desktop browsers for data entry workflows, with responsive design ensuring usability on tablets. Mobile phone support for read-only views and quick status checks. Full feature parity across Chrome, Firefox, Safari, and Edge browsers.

## Technical Assumptions

### Repository Structure: Monorepo
Single repository containing both backend (Java/Spring Boot) and frontend (TypeScript/React) codebases. This simplifies assessment submission and enables shared configuration, while maintaining clear separation between backend and frontend modules through directory structure.

### Service Architecture
**Modular Monolith with Vertical Slices** - Single deployable backend service organized around vertical feature slices (per domain entity). Each slice contains its own Commands, Queries, DTOs, and controllers. This aligns with VSA principles while avoiding microservices complexity for an assessment project. Clear module boundaries prepare for potential future extraction to microservices if needed.

### Testing Requirements
**Full Testing Pyramid** implementation:
- **Unit Tests:** Domain logic and individual component testing
- **Integration Tests:** MANDATORY - End-to-end flows across modules (Customer→Invoice→Payment)
- **API Tests:** REST endpoint contract validation
- **UI Component Tests:** React component behavior and rendering
- **E2E Tests (optional but recommended):** Critical user journeys through the full stack

### Additional Technical Assumptions and Requests
- **Backend Framework:** Spring Boot 3.x with Java 17+ for modern features and long-term support
- **Frontend Framework:** Next.js (preferred over plain React) for better developer experience, routing, and SSR capabilities
- **State Management:** React Context API for simple state, with option for Zustand/Redux if complexity grows
- **UI Component Library:** Shadcn/ui with Tailwind CSS for rapid, accessible, customizable components
- **Database Access:** Spring Data JPA with Hibernate for domain persistence, native queries for complex read models (CQRS)
- **API Documentation:** OpenAPI/Swagger for REST API specification and testing
- **Build Tools:** Maven for Java, npm/yarn for frontend, with clear build scripts
- **Deployment:** Containerized with Docker for cloud-agnostic deployment to AWS/Azure
- **AI Development Tools:** Explicit documentation of prompts and patterns used with Cursor/Copilot for code generation

## Epic List

**Epic 1: Foundation & Customer Management**
Establish project infrastructure with Spring Boot backend, Next.js frontend, database setup, authentication, and full CRUD operations for Customer domain including API and UI integration.

**Epic 2: Invoice Domain & Core Workflows**
Implement complete Invoice entity with line items, lifecycle states (Draft→Sent→Paid), balance calculations, and full UI for invoice creation, editing, and management with CQRS command/query separation.

**Epic 3: Payment Processing & Reconciliation**
Build Payment recording system with invoice application logic, balance reconciliation, payment history tracking, and complete integration between all three domains (Customer→Invoice→Payment).

**Epic 4: Production Readiness & Quality Assurance**
Comprehensive integration testing, API documentation, error handling, performance optimization, Docker containerization, and cloud deployment preparation with monitoring setup.

## Epic 1: Foundation & Customer Management

**Epic Goal:** Establish the complete project foundation with both backend and frontend infrastructure, implement authentication, and deliver full Customer domain functionality. This epic creates the architectural patterns and development workflows that all subsequent epics will follow, while also providing immediate business value through customer management capabilities.

### Story 1.1: Project Setup & Architecture Foundation
**As a** developer,
**I want** to initialize the monorepo with Spring Boot backend and Next.js frontend scaffolding,
**so that** we have a working foundation following DDD, CQRS, and VSA principles.

#### Acceptance Criteria
1. Monorepo structure created with `/backend` (Spring Boot) and `/frontend` (Next.js) directories
2. Spring Boot 3.x application initialized with Maven, web, JPA, H2 database dependencies
3. Package structure follows VSA: `com.invoiceme.features.{feature}.{commands|queries|domain|api}`
4. Next.js application initialized with TypeScript, Tailwind CSS, and Shadcn/ui configured
5. Both applications start successfully with health check endpoints working
6. Git repository initialized with appropriate .gitignore files for Java and Node.js
7. README.md documents project structure and how to run both applications
8. Docker Compose file for local development with PostgreSQL database

### Story 1.2: Authentication Implementation
**As a** user,
**I want** to log in with credentials to access the system,
**so that** my data is secured and isolated.

#### Acceptance Criteria
1. Spring Security configured with JWT-based authentication
2. `/api/auth/login` endpoint accepts username/password and returns JWT token
3. `/api/auth/refresh` endpoint for token refresh functionality
4. Protected endpoints return 401 for unauthorized requests
5. Next.js login page with form validation and error handling
6. Client-side token storage and automatic inclusion in API requests
7. Protected routes redirect to login when unauthenticated
8. Logout functionality clears tokens and redirects to login

### Story 1.3: Customer Domain Model & Commands
**As a** developer,
**I want** to implement the Customer domain entity with command operations,
**so that** we can create, update, and delete customers following DDD principles.

#### Acceptance Criteria
1. Customer domain entity with fields: id, name, email, phone, address, createdAt, updatedAt
2. CreateCustomerCommand with handler implementing business validation
3. UpdateCustomerCommand with handler for modifying customer details
4. DeleteCustomerCommand with soft-delete implementation
5. CustomerRepository interface in domain layer, JPA implementation in infrastructure
6. Command DTOs with validation annotations
7. REST endpoints: POST /api/customers, PUT /api/customers/{id}, DELETE /api/customers/{id}
8. Unit tests for domain logic and command handlers

### Story 1.4: Customer Queries & API Integration
**As a** developer,
**I want** to implement Customer query operations with separate read models,
**so that** we maintain CQRS separation and optimize for read performance.

#### Acceptance Criteria
1. GetCustomerByIdQuery with dedicated handler and read model DTO
2. ListCustomersQuery with pagination, sorting, and filtering support
3. CustomerQueryRepository using Spring Data JPA projections or native queries
4. REST endpoints: GET /api/customers/{id}, GET /api/customers
5. Separate CustomerResponseDTO optimized for UI consumption
6. Query results include computed fields (e.g., total invoices count)
7. Integration tests verifying query operations
8. Swagger/OpenAPI documentation for all customer endpoints

### Story 1.5: Customer UI Implementation
**As a** user,
**I want** to manage customers through a responsive web interface,
**so that** I can perform all CRUD operations efficiently.

#### Acceptance Criteria
1. Customer list page with data table showing all customers
2. Search, filter, and sort functionality on customer list
3. Create customer form with client-side validation matching backend rules
4. Edit customer form pre-populated with existing data
5. Delete confirmation dialog with success/error notifications
6. Responsive design working on desktop and tablet
7. Loading states and error handling for all API operations
8. Navigation menu with active state highlighting

## Epic 2: Invoice Domain & Core Workflows

**Epic Goal:** Implement the complete Invoice domain with rich business logic including line items, lifecycle state management, and balance calculations. This epic establishes the core business value of the system with proper CQRS separation and complex domain modeling that demonstrates advanced DDD principles.

### Story 2.1: Invoice Domain Model & Line Items
**As a** developer,
**I want** to implement the Invoice aggregate with Line Items,
**so that** we can model complex invoice structures following DDD aggregate patterns.

#### Acceptance Criteria
1. Invoice aggregate root with fields: id, customerID, invoiceNumber, issueDate, dueDate, status (Draft/Sent/Paid), totalAmount, balance
2. LineItem value object with: description, quantity, unitPrice, amount
3. Invoice aggregate methods: addLineItem(), removeLineItem(), calculateTotal()
4. Domain invariants enforced: line items cannot be modified after status=Sent
5. InvoiceRepository interface with aggregate persistence
6. Domain events: InvoiceCreated, InvoiceStatusChanged, LineItemAdded
7. Unit tests for all domain logic and invariant enforcement
8. Invoice number generation with unique sequential format (e.g., INV-2024-0001)

### Story 2.2: Invoice Commands & State Transitions
**As a** developer,
**I want** to implement Invoice commands with proper state management,
**so that** invoices follow the correct lifecycle workflow.

#### Acceptance Criteria
1. CreateInvoiceCommand creates invoice in Draft status with line items
2. UpdateInvoiceCommand allows modifications only in Draft status
3. SendInvoiceCommand transitions from Draft to Sent with validation
4. Command handlers enforce business rules and state transitions
5. REST endpoints: POST /api/invoices, PUT /api/invoices/{id}, POST /api/invoices/{id}/send
6. Optimistic locking to prevent concurrent modifications
7. Command validation ensures minimum one line item before sending
8. Integration tests for all state transitions and edge cases

### Story 2.3: Invoice Queries & Calculations
**As a** developer,
**I want** to implement Invoice query models with computed fields,
**so that** we can efficiently retrieve invoice data with calculations.

#### Acceptance Criteria
1. GetInvoiceByIdQuery returns full invoice with line items and calculations
2. ListInvoicesQuery with filters: by customer, status, date range
3. InvoiceResponseDTO includes computed: subtotal, tax, total, balance due
4. Query projections optimize for common read patterns
5. REST endpoints: GET /api/invoices/{id}, GET /api/invoices
6. Dashboard query for invoice statistics (count by status, total amounts)
7. Export-friendly query format for invoice details
8. Performance: invoice list query under 100ms for 1000 records

### Story 2.4: Invoice Creation UI
**As a** user,
**I want** to create and edit invoices with line items,
**so that** I can generate professional invoices for customers.

#### Acceptance Criteria
1. Invoice creation form with customer selection dropdown
2. Dynamic line items management: add, edit, remove rows
3. Real-time total calculation as line items change
4. Date picker for issue and due dates with validation
5. Auto-save draft functionality every 30 seconds
6. Form validation matching backend business rules
7. Success redirect to invoice detail view after creation
8. Responsive layout maintaining usability on tablets

### Story 2.5: Invoice Management UI
**As a** user,
**I want** to view and manage all invoices with their lifecycle states,
**so that** I can track invoice status and take appropriate actions.

#### Acceptance Criteria
1. Invoice list page with status badges (Draft=gray, Sent=blue, Paid=green)
2. Filters for status, customer, date range with URL persistence
3. Bulk actions menu (future-proofing for batch operations)
4. Invoice detail view with print-friendly layout
5. Send invoice action button with confirmation dialog
6. Edit button disabled for Sent/Paid invoices
7. Copy invoice function to create new draft from existing
8. Export to PDF placeholder (button visible, shows "Coming Soon")

## Epic 3: Payment Processing & Reconciliation

**Epic Goal:** Build the Payment domain to complete the invoice lifecycle, implementing payment recording, invoice application, and balance reconciliation. This epic demonstrates complex domain interactions and cross-aggregate consistency while maintaining proper bounded context separation.

### Story 3.1: Payment Domain Model
**As a** developer,
**I want** to implement the Payment entity with invoice application logic,
**so that** we can track payments and properly reconcile invoice balances.

#### Acceptance Criteria
1. Payment entity with fields: id, invoiceId, paymentDate, amount, paymentMethod, reference, notes
2. PaymentMethod enum: CreditCard, BankTransfer, Check, Cash
3. Payment validation: amount must be positive, cannot exceed invoice balance
4. PaymentRepository interface following repository pattern
5. Domain service for payment-invoice reconciliation logic
6. Unit tests for payment validation and business rules
7. Database constraints ensuring referential integrity with invoices
8. Audit fields: createdAt, createdBy for payment tracking

### Story 3.2: Payment Recording Commands
**As a** developer,
**I want** to implement payment recording with invoice state updates,
**so that** payments properly affect invoice status and balance.

#### Acceptance Criteria
1. RecordPaymentCommand creates payment and updates invoice balance
2. Payment application triggers invoice status change to Paid when balance=0
3. Partial payment support with remaining balance calculation
4. Command validation prevents payment on Draft invoices
5. REST endpoint: POST /api/invoices/{id}/payments
6. Transactional consistency between payment creation and invoice update
7. Domain event: PaymentRecorded with invoice and payment details
8. Idempotency handling to prevent duplicate payment recording

### Story 3.3: Payment Queries & History
**As a** developer,
**I want** to implement payment queries and history tracking,
**so that** users can view payment details and transaction history.

#### Acceptance Criteria
1. GetPaymentByIdQuery returns payment with invoice details
2. ListPaymentsByInvoiceQuery shows all payments for an invoice
3. PaymentHistoryQuery with date range and customer filters
4. PaymentResponseDTO includes invoice number, customer name
5. REST endpoints: GET /api/payments/{id}, GET /api/invoices/{id}/payments
6. Dashboard payment statistics (total collected, by method, by period)
7. Query includes running balance calculation per payment
8. Export-ready format for accounting reconciliation

### Story 3.4: Payment Recording UI
**As a** user,
**I want** to record payments against sent invoices,
**so that** I can track payment collection and update invoice status.

#### Acceptance Criteria
1. Payment recording modal/form accessible from invoice detail view
2. Form shows invoice details: number, customer, total, balance due
3. Payment amount field with validation (cannot exceed balance)
4. Payment method dropdown and reference number field
5. Optional notes field for payment details
6. Real-time balance calculation showing remaining after payment
7. Success notification and invoice status update after recording
8. Payment button disabled for Draft or fully Paid invoices

### Story 3.5: Payment History & Reconciliation UI
**As a** user,
**I want** to view payment history and reconciliation details,
**so that** I can track all payments and verify account balances.

#### Acceptance Criteria
1. Payment history tab on invoice detail showing all payments
2. Global payments page with searchable payment list
3. Payment details modal showing full payment information
4. Balance reconciliation view per customer showing all invoices/payments
5. Payment method filter and summary statistics
6. Date range picker for payment history filtering
7. Export button for payment data (CSV format placeholder)
8. Visual indicators for partial vs full payments

## Epic 4: Production Readiness & Quality Assurance

**Epic Goal:** Ensure the application meets production quality standards with comprehensive testing, proper error handling, performance optimization, and deployment readiness. This epic transforms the functional system into a production-grade application ready for cloud deployment and real-world usage.

### Story 4.1: Comprehensive Integration Testing
**As a** developer,
**I want** to implement end-to-end integration tests,
**so that** we verify the complete business workflows function correctly.

#### Acceptance Criteria
1. Integration test suite covering Customer→Invoice→Payment flow
2. Test data builders following builder pattern for test setup
3. Database test containers (TestContainers) for realistic testing
4. API integration tests for all REST endpoints with auth
5. CQRS verification tests ensuring command-query separation
6. State transition tests for invoice lifecycle
7. Negative test cases for error conditions and validation
8. Test coverage report showing >80% coverage on business logic

### Story 4.2: Error Handling & Validation Framework
**As a** developer,
**I want** to implement comprehensive error handling,
**so that** the system gracefully handles failures and provides meaningful feedback.

#### Acceptance Criteria
1. Global exception handler for REST API with standard error format
2. Domain validation exceptions with detailed field-level errors
3. Custom exception types for business rule violations
4. Client-side error interceptor with retry logic
5. User-friendly error messages in UI with actionable guidance
6. Logging framework integration (SLF4J/Logback) with correlation IDs
7. Error boundary components in React for graceful UI failures
8. 404, 403, 500 error pages with consistent design

### Story 4.3: Performance Optimization
**As a** developer,
**I want** to optimize application performance,
**so that** the system meets the required response time benchmarks.

#### Acceptance Criteria
1. Database indexes on frequently queried fields (customerId, status, dates)
2. Query optimization using EXPLAIN plans and query hints
3. Lazy loading strategy for invoice line items
4. API response caching for read-heavy endpoints
5. Frontend bundle optimization with code splitting
6. Image and asset optimization with CDN configuration
7. Performance tests verifying <200ms API response times
8. React memo and useMemo optimization for expensive renders

### Story 4.4: Deployment & Infrastructure
**As a** developer,
**I want** to containerize and prepare the application for cloud deployment,
**so that** we can deploy to AWS or Azure seamlessly.

#### Acceptance Criteria
1. Multi-stage Dockerfile for backend with minimal JRE image
2. Dockerfile for frontend with nginx serving optimized build
3. Docker Compose configuration for full stack local deployment
4. Environment-specific configuration with Spring profiles
5. Health check endpoints for container orchestration
6. Database migration scripts using Flyway or Liquibase
7. CI/CD pipeline configuration (GitHub Actions or similar)
8. Deployment documentation with cloud-specific instructions

### Story 4.5: Documentation & AI Development Report
**As a** developer,
**I want** to document the architecture and AI tool usage,
**so that** the assessment demonstrates both technical quality and AI effectiveness.

#### Acceptance Criteria
1. Technical architecture document with diagrams (C4 model recommended)
2. API documentation auto-generated from OpenAPI specs
3. README with setup, run, and test instructions
4. Database schema documentation with ERD
5. AI tools usage report documenting prompts and patterns
6. Code comments explaining complex business logic
7. Developer setup guide for new team members
8. Deployment runbook with troubleshooting guide

## Checklist Results Report

### Executive Summary
- **Overall PRD Completeness:** 94%
- **MVP Scope Appropriateness:** Just Right - Well-scoped for 5-7 day assessment
- **Readiness for Architecture Phase:** Ready
- **Most Critical Gaps:** Minor gaps in data migration strategy and monitoring specifics

### Category Analysis

| Category | Status | Critical Issues |
|----------|--------|-----------------|
| 1. Problem Definition & Context | PASS | None - Clear assessment objectives and business context |
| 2. MVP Scope Definition | PASS | None - Appropriate scope for assessment timeframe |
| 3. User Experience Requirements | PASS | None - Comprehensive UI/UX specifications |
| 4. Functional Requirements | PASS | None - Complete CRUD operations and business logic |
| 5. Non-Functional Requirements | PASS | None - Performance, security, and quality standards defined |
| 6. Epic & Story Structure | PASS | None - Logical progression with sized stories |
| 7. Technical Guidance | PASS | None - Clear architectural principles and tech stack |
| 8. Cross-Functional Requirements | PARTIAL | Missing detailed monitoring/alerting specifications |
| 9. Clarity & Communication | PASS | None - Well-structured and consistent documentation |

### Top Issues by Priority

**BLOCKERS:** None identified

**HIGH:**
- Monitoring and alerting strategy needs more detail for production deployment
- Data migration approach for moving from H2 to PostgreSQL not specified

**MEDIUM:**
- Export functionality (PDF/CSV) marked as placeholder - implementation strategy unclear
- Multi-tenancy considerations not addressed (single-tenant assumed)

**LOW:**
- Batch operations mentioned but not fully specified
- Internationalization/localization not considered

### MVP Scope Assessment

**Appropriately Scoped Features:**
- Core CRUD operations for all three domains
- Essential invoice lifecycle management
- Basic authentication and authorization
- Fundamental CQRS/DDD implementation

**Potential Additions (if time permits):**
- Basic reporting dashboard
- Email notifications for invoice status changes
- Bulk invoice operations

**Complexity Appropriate:**
- 20 stories across 4 epics is manageable for 5-7 days
- Each story sized for 2-4 hour AI-assisted implementation
- Progressive complexity from setup to optimization

### Technical Readiness

**Clear Technical Constraints:**
- Spring Boot 3.x with Java 17+
- Next.js with TypeScript and Shadcn/ui
- PostgreSQL for production, H2 for development
- Docker containerization required
- CQRS and VSA architecture mandated

**Identified Technical Risks:**
- Performance requirement of <200ms may be challenging with complex queries
- State management complexity in frontend with multiple domains
- Transaction boundary management across aggregates

**Areas for Architect Investigation:**
- Optimal aggregate boundaries for Invoice/Payment relationship
- Query model denormalization strategy
- Caching strategy for read-heavy operations

### Recommendations

1. **Proceed to Architecture Phase** - PRD is comprehensive and ready
2. **During Architecture Design:**
   - Define specific monitoring metrics and alerting thresholds
   - Create data migration strategy from H2 to PostgreSQL
   - Design caching layer for query optimization
3. **Early Implementation Focus:**
   - Establish CI/CD pipeline in Story 1.1
   - Implement comprehensive logging from the start
   - Create integration test framework early

### Final Decision

**READY FOR ARCHITECT** ✅

The PRD and epics are comprehensive, properly structured, and ready for architectural design. All core requirements are clearly defined with appropriate scope for the assessment timeframe.

## Next Steps

### UX Expert Prompt
Please review this PRD and create comprehensive UI/UX designs for the InvoiceMe invoicing system, focusing on the user workflows for customer management, invoice creation with line items, and payment recording. Ensure the design follows modern ERP patterns with clear visual hierarchy and efficient data entry flows.

### Architect Prompt
Please create a detailed technical architecture document for InvoiceMe based on this PRD, implementing Domain-Driven Design with CQRS and Vertical Slice Architecture. Focus on establishing clear bounded contexts for Customer, Invoice, and Payment domains, defining the command/query separation, and specifying the integration patterns between the Spring Boot backend and Next.js frontend.
