# API Usage Guide

## Overview

The InvoiceMe API is a RESTful API built with Spring Boot that follows the CQRS pattern. All endpoints require JWT authentication except for the `/auth/login` endpoint.

**Base URL (Development):** `http://localhost:8080/api`
**Base URL (Production):** `https://api.invoiceme.com/api`

**Interactive Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
**OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Authentication

### Login

Obtain a JWT token by authenticating with credentials.

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "username": "admin@example.com",
  "password": "password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600000,
  "username": "admin@example.com"
}
```

### Using the Token

Include the JWT token in the `Authorization` header for all protected endpoints:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Example with curl:**
```bash
curl -H "Authorization: Bearer <your-token>" \
  http://localhost:8080/api/customers
```

**Example with JavaScript/Axios:**
```javascript
axios.get('/api/customers', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

### Token Refresh

When the access token expires, use the refresh token to obtain a new access token.

**Endpoint:** `POST /api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## Customer Endpoints

### Create Customer (Command)

**Endpoint:** `POST /api/customers`
**Authentication:** Required

**Request:**
```json
{
  "name": "Acme Corporation",
  "email": "billing@acme.com",
  "phone": "+1-555-0100",
  "address": "123 Business St, New York, NY 10001"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Acme Corporation",
  "email": "billing@acme.com",
  "phone": "+1-555-0100",
  "address": "123 Business St, New York, NY 10001",
  "isDeleted": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### List Customers (Query)

**Endpoint:** `GET /api/customers`
**Authentication:** Required

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `search` (optional): Search by name or email

**Example:**
```
GET /api/customers?page=0&size=20&search=acme
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Acme Corporation",
      "email": "billing@acme.com",
      "totalInvoices": 5,
      "outstandingBalance": 2500.00
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### Get Customer by ID (Query)

**Endpoint:** `GET /api/customers/{id}`
**Authentication:** Required

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Acme Corporation",
  "email": "billing@acme.com",
  "phone": "+1-555-0100",
  "address": "123 Business St, New York, NY 10001",
  "totalInvoices": 5,
  "outstandingBalance": 2500.00,
  "isDeleted": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Update Customer (Command)

**Endpoint:** `PUT /api/customers/{id}`
**Authentication:** Required

**Request:** Same as Create Customer

### Delete Customer (Command)

**Endpoint:** `DELETE /api/customers/{id}`
**Authentication:** Required

**Response:** `204 No Content`

**Note:** This is a soft delete. The customer remains in the database with `isDeleted = true`.

---

## Invoice Endpoints

### Create Invoice (Command)

**Endpoint:** `POST /api/invoices`
**Authentication:** Required

**Request:**
```json
{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "issueDate": "2024-01-15",
  "paymentTerms": "Net 30",
  "lineItems": [
    {
      "description": "Web Development Services",
      "quantity": 40,
      "unitPrice": 100.00,
      "discountPercent": 0.10,
      "taxRate": 0.08
    },
    {
      "description": "Design Services",
      "quantity": 20,
      "unitPrice": 75.00,
      "discountPercent": 0,
      "taxRate": 0.08
    }
  ],
  "notes": "Thank you for your business"
}
```

**Response:** `201 Created`
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "invoiceNumber": "INV-2024-0001",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "customerName": "Acme Corporation",
  "issueDate": "2024-01-15",
  "dueDate": "2024-02-14",
  "status": "Draft",
  "paymentTerms": "Net 30",
  "subtotal": 5500.00,
  "totalDiscount": 400.00,
  "totalTax": 408.00,
  "totalAmount": 5508.00,
  "balance": 5508.00,
  "lineItems": [ /* calculated line items */ ],
  "notes": "Thank you for your business"
}
```

### List Invoices (Query)

**Endpoint:** `GET /api/invoices`
**Authentication:** Required

**Query Parameters:**
- `page` (optional): Page number
- `size` (optional): Page size
- `customerId` (optional): Filter by customer
- `status` (optional): Filter by status (Draft, Sent, Paid)
- `startDate` (optional): Filter by issue date >= startDate
- `endDate` (optional): Filter by issue date <= endDate

**Example:**
```
GET /api/invoices?status=Sent&customerId=550e8400-e29b-41d4-a716-446655440000
```

### Get Invoice by ID (Query)

**Endpoint:** `GET /api/invoices/{id}`
**Authentication:** Required

### Update Invoice (Command)

**Endpoint:** `PUT /api/invoices/{id}`
**Authentication:** Required

**Note:** Only Draft invoices can be updated. Sent or Paid invoices cannot be modified.

### Send Invoice (Command)

**Endpoint:** `POST /api/invoices/{id}/send`
**Authentication:** Required

**Response:** `200 OK`

Transitions invoice from Draft to Sent status.

---

## Payment Endpoints

### Record Payment (Command)

**Endpoint:** `POST /api/invoices/{invoiceId}/payments`
**Authentication:** Required

**Request:**
```json
{
  "paymentDate": "2024-01-20",
  "amount": 1000.00,
  "paymentMethod": "BankTransfer",
  "reference": "TXN-20240120-001",
  "notes": "Partial payment"
}
```

**Payment Methods:** `CreditCard`, `BankTransfer`, `Check`, `Cash`

**Response:** `201 Created`
```json
{
  "id": "9b9f8e47-2f3d-4e7d-9c4a-1e5f6d7c8b9a",
  "invoiceId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "invoiceNumber": "INV-2024-0001",
  "customerName": "Acme Corporation",
  "paymentDate": "2024-01-20",
  "amount": 1000.00,
  "paymentMethod": "BankTransfer",
  "reference": "TXN-20240120-001",
  "remainingBalance": 4508.00,
  "createdAt": "2024-01-20T14:30:00Z",
  "createdBy": "admin@example.com"
}
```

### List Payments for Invoice (Query)

**Endpoint:** `GET /api/invoices/{invoiceId}/payments`
**Authentication:** Required

### List All Payments (Query)

**Endpoint:** `GET /api/payments`
**Authentication:** Required

**Query Parameters:**
- `startDate` (optional): Filter payments >= startDate
- `endDate` (optional): Filter payments <= endDate
- `customerId` (optional): Filter by customer

### Get Payment by ID (Query)

**Endpoint:** `GET /api/payments/{id}`
**Authentication:** Required

---

## Dashboard Endpoints

### Get Dashboard Statistics (Query)

**Endpoint:** `GET /api/dashboard/stats`
**Authentication:** Required

**Response:** `200 OK`
```json
{
  "totalCustomers": 45,
  "totalInvoices": 152,
  "draftInvoices": 8,
  "sentInvoices": 32,
  "paidInvoices": 112,
  "totalRevenue": 425000.00,
  "outstandingAmount": 87500.00,
  "overdueAmount": 12300.00
}
```

---

## Error Handling

### Error Response Format

All API errors return a consistent error response:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'email': must be a valid email address",
  "path": "/api/customers"
}
```

### Common Error Codes

| Status Code | Description | Common Causes |
|-------------|-------------|---------------|
| 400 Bad Request | Invalid request data | Validation failure, malformed JSON |
| 401 Unauthorized | Missing or invalid token | Token expired, not provided |
| 403 Forbidden | Access denied | Insufficient permissions |
| 404 Not Found | Resource not found | Invalid ID, resource deleted |
| 409 Conflict | Business rule violation | Invoice already sent, payment exceeds balance |
| 500 Internal Server Error | Server error | Database error, unexpected exception |

### Validation Errors

Validation errors provide field-level details:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "name",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

---

## Pagination

List endpoints support pagination using page and size query parameters.

**Default Values:**
- `page`: 0 (first page)
- `size`: 20 items per page

**Response Format:**
```json
{
  "content": [ /* array of items */ ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## Rate Limiting

**Current:** No rate limiting implemented (suitable for internal applications)

**Production Consideration:** Implement rate limiting (e.g., 100 requests per minute per user) using Spring Cloud Gateway or API Gateway service.

---

## Best Practices

### 1. Always Include Authorization Header

```javascript
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Authorization': `Bearer ${getToken()}`
  }
});
```

### 2. Handle Token Expiration

Implement token refresh logic when receiving 401 responses:

```javascript
apiClient.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      await refreshToken();
      return apiClient.request(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 3. Validate on Client Side

Perform client-side validation before API calls to improve UX:

```javascript
if (!email.includes('@')) {
  setError('Invalid email format');
  return;
}

await createCustomer({ name, email, phone, address });
```

### 4. Use Pagination for Large Lists

Always paginate when fetching lists:

```javascript
const customers = await fetchCustomers({ page: 0, size: 20 });
```

### 5. Include Correlation IDs

For debugging, include correlation ID in headers:

```javascript
headers: {
  'X-Correlation-ID': uuid()
}
```

---

## Testing the API

### Using curl

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@example.com","password":"password"}' \
  | jq -r '.token')

# Create customer
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Customer","email":"test@example.com"}'

# List customers
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/customers
```

### Using Postman

1. Import OpenAPI spec from `http://localhost:8080/v3/api-docs`
2. Set up environment variable for token
3. Add Authorization header to collection

### Using Swagger UI

1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Click "Authorize" button
3. Enter `Bearer <your-token>`
4. Try out endpoints interactively

---

## Support

For API issues or questions:
- Check [Troubleshooting Guide](../troubleshooting.md)
- Review [Architecture Documentation](../architecture.md)
- Inspect logs: `docker compose logs backend`

---

**Last Updated:** 2024-11-09
**API Version:** 1.0
