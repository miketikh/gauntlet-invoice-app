package com.invoiceme;

import com.invoiceme.auth.JwtTokenProvider;
import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import com.invoiceme.payment.domain.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests
 * Provides TestContainers PostgreSQL database and common test utilities
 * All integration tests should extend this class
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("invoiceme_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected JpaCustomerRepository customerRepository;

    @Autowired
    protected JpaInvoiceRepository invoiceRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    protected String baseUrl;
    protected String jwtToken;

    /**
     * Configure Spring Boot to use the TestContainers PostgreSQL instance
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Set up method run before each test
     * Clears database and generates JWT token
     */
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // Clear all data before each test
        clearDatabase();

        // Generate JWT token for authenticated requests
        jwtToken = generateToken("testuser");
    }

    /**
     * Clear all database tables
     */
    protected void clearDatabase() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
    }

    /**
     * Generate JWT token for testing
     * @param username Username to include in token
     * @return JWT token string
     */
    protected String generateToken(String username) {
        return jwtTokenProvider.generateToken(username, false);
    }

    /**
     * Get Authorization header with Bearer token
     * @return Authorization header value
     */
    protected String getAuthHeader() {
        return "Bearer " + jwtToken;
    }

    /**
     * Get Authorization header with custom token
     * @param token JWT token
     * @return Authorization header value
     */
    protected String getAuthHeader(String token) {
        return "Bearer " + token;
    }
}
