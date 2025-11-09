# Testing Strategy

## Overview

InvoiceMe employs a comprehensive testing strategy covering unit, integration, and end-to-end tests to ensure code quality and reliability.

## Testing Pyramid

```
         /\
        /E2E\      8 scenarios - Critical user flows
       /------\
      /  API  \    30+ tests - API integration tests
     /--------\
    / Unit Tests\  1,600+ tests - Business logic and components
   /--------------\
```

## Test Coverage Goals

- **Backend Unit Tests:** >80% line coverage (Target: 85%)
- **Frontend Unit Tests:** >75% line coverage (Target: 80%)
- **Integration Tests:** All API endpoints
- **E2E Tests:** Critical user journeys

## Backend Testing

### Unit Tests (JUnit 5 + Mockito)

**Location:** `backend/src/test/java/`

**Run Tests:**
```bash
cd backend
mvn test
```

**Coverage Report:**
```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

**Test Structure:**
```java
@ExtendWith(MockitoExtension.class)
class CreateInvoiceCommandHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CreateInvoiceCommandHandler handler;

    @Test
    void shouldCreateInvoiceWithCalculatedTotals() {
        // Arrange
        var command = CreateInvoiceCommand.builder()
            .customerId(UUID.randomUUID())
            .lineItems(List.of(/* line items */))
            .build();

        // Act
        var result = handler.handle(command);

        // Assert
        assertThat(result.getTotalAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(1620.00));
    }
}
```

### Integration Tests (Spring Boot Test + TestContainers)

**Run Integration Tests:**
```bash
mvn verify
```

**Test Structure:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CustomerControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateCustomerAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/customers")
            .header("Authorization", "Bearer " + getToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Test Customer",
                    "email": "test@example.com"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

## Frontend Testing

### Unit Tests (Jest + React Testing Library)

**Location:** `__tests__/` and `*.test.tsx` files

**Run Tests:**
```bash
npm test
```

**Watch Mode:**
```bash
npm test -- --watch
```

**Coverage:**
```bash
npm test -- --coverage
```

**Test Structure:**
```typescript
describe('CustomerForm', () => {
  it('should submit form with valid data', async () => {
    const onSubmit = jest.fn();
    render(<CustomerForm onSubmit={onSubmit} />);

    await userEvent.type(screen.getByLabelText(/name/i), 'Test Customer');
    await userEvent.type(screen.getByLabelText(/email/i), 'test@example.com');
    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: 'Test Customer',
        email: 'test@example.com'
      });
    });
  });
});
```

### Component Testing Best Practices

1. **Test behavior, not implementation**
2. **Use user-centric queries** (getByRole, getByLabelText)
3. **Test accessibility**
4. **Mock API calls**
5. **Test error states**

## E2E Testing

### Playwright Tests

**Location:** `e2e/`

**Run E2E Tests:**
```bash
npm run test:e2e
```

**Run in UI Mode:**
```bash
npm run test:e2e:ui
```

**Test Structure:**
```typescript
test('should create invoice and record payment', async ({ page }) => {
  await page.goto('http://localhost:3000');

  // Login
  await page.fill('[name=email]', 'admin@example.com');
  await page.fill('[name=password]', 'password');
  await page.click('button[type=submit]');

  // Create invoice
  await page.click('text=Create Invoice');
  // ... fill form

  // Record payment
  await page.click('text=Record Payment');
  // ... verify balance update
});
```

## Test Data Management

### Test Builders

Use test builders for complex objects:

```java
public class InvoiceTestBuilder {
    private UUID id = UUID.randomUUID();
    private String invoiceNumber = "INV-2024-0001";
    private BigDecimal totalAmount = BigDecimal.valueOf(1000);

    public InvoiceTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public Invoice build() {
        return new Invoice(id, invoiceNumber, /* ... */);
    }
}

// Usage
var invoice = new InvoiceTestBuilder()
    .withId(customId)
    .withTotalAmount(BigDecimal.valueOf(5000))
    .build();
```

## Continuous Integration

Tests run automatically on:
- Every pull request
- Every commit to main branch
- Nightly builds

**CI Pipeline:**
```yaml
- name: Run Backend Tests
  run: cd backend && mvn test

- name: Run Frontend Tests
  run: npm test -- --coverage

- name: Run E2E Tests
  run: npm run test:e2e
```

## Writing New Tests

### When to Write Tests

- **Always:** For new features and bug fixes
- **Unit Tests:** For business logic and calculations
- **Integration Tests:** For new API endpoints
- **E2E Tests:** For critical user flows

### Test Naming Conventions

**Backend:**
```java
// Pattern: should{ExpectedBehavior}When{Condition}
shouldCalculateTotalWhenLineItemsProvided()
shouldThrowExceptionWhenInvoiceNotFound()
```

**Frontend:**
```typescript
// Pattern: "should {behavior} when {condition}"
it('should display error when email is invalid')
it('should disable submit button while loading')
```

## Running Specific Tests

### Backend

```bash
# Run single test class
mvn test -Dtest=CustomerCommandHandlerTest

# Run single test method
mvn test -Dtest=CustomerCommandHandlerTest#shouldCreateCustomer

# Run tests matching pattern
mvn test -Dtest="*CustomerTest"
```

### Frontend

```bash
# Run single test file
npm test customer-form.test.tsx

# Run tests matching pattern
npm test -- --testNamePattern="should submit"
```

## Debugging Tests

### Backend

1. Add breakpoint in IntelliJ IDEA
2. Right-click test → Debug
3. Step through code

### Frontend

```typescript
// Add debug statement
screen.debug(); // Prints DOM

// Use Chrome DevTools
npm test -- --detectOpenHandles
```

## Test Maintenance

### Keep Tests Fast

- Use mocks for external dependencies
- Use in-memory database for unit tests
- Run E2E tests in parallel

### Keep Tests Reliable

- Avoid timing dependencies
- Use proper waits (waitFor, await)
- Clean up test data

### Keep Tests Readable

- Use descriptive test names
- Follow Arrange-Act-Assert pattern
- Extract common setup to helper functions

## Common Patterns

### Testing Async Operations

```typescript
await waitFor(() => {
  expect(screen.getByText(/success/i)).toBeInTheDocument();
});
```

### Testing Forms

```typescript
const { result } = renderHook(() => useForm());

act(() => {
  result.current.setValue('name', 'Test');
});

expect(result.current.getValues()).toEqual({ name: 'Test' });
```

### Testing API Calls

```typescript
const mockCreate = jest.fn().mockResolvedValue({ id: '123' });
jest.spyOn(api, 'createCustomer').mockImplementation(mockCreate);
```

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Playwright Documentation](https://playwright.dev/)

---

**Last Updated:** 2024-11-09
