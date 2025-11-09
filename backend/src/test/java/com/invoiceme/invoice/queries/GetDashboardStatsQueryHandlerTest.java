package com.invoiceme.invoice.queries;

import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import com.invoiceme.invoice.queries.dto.DashboardStatsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardStatsQueryHandlerTest {

    @Mock
    private JpaInvoiceRepository invoiceRepository;

    @Mock
    private JpaCustomerRepository customerRepository;

    @InjectMocks
    private GetDashboardStatsQueryHandler handler;

    @Test
    void shouldCalculateAllStatisticsCorrectly() {
        // Arrange
        when(customerRepository.count()).thenReturn(10L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Draft)).thenReturn(5L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Sent)).thenReturn(3L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Paid)).thenReturn(2L);
        when(invoiceRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("5000.00"));
        when(invoiceRepository.calculateOutstandingAmount()).thenReturn(new BigDecimal("3000.00"));
        when(invoiceRepository.calculateOverdueAmount()).thenReturn(new BigDecimal("1000.00"));

        // Act
        GetDashboardStatsQuery query = new GetDashboardStatsQuery();
        DashboardStatsDTO result = handler.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.totalCustomers()).isEqualTo(10);
        assertThat(result.totalInvoices()).isEqualTo(10);
        assertThat(result.draftInvoices()).isEqualTo(5);
        assertThat(result.sentInvoices()).isEqualTo(3);
        assertThat(result.paidInvoices()).isEqualTo(2);
        assertThat(result.totalRevenue()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.outstandingAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.overdueAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldHandleZeroInvoices() {
        // Arrange
        when(customerRepository.count()).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Draft)).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Sent)).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Paid)).thenReturn(0L);
        when(invoiceRepository.calculateTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.calculateOutstandingAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.calculateOverdueAmount()).thenReturn(BigDecimal.ZERO);

        // Act
        GetDashboardStatsQuery query = new GetDashboardStatsQuery();
        DashboardStatsDTO result = handler.handle(query);

        // Assert
        assertThat(result.totalCustomers()).isZero();
        assertThat(result.totalInvoices()).isZero();
        assertThat(result.draftInvoices()).isZero();
        assertThat(result.sentInvoices()).isZero();
        assertThat(result.paidInvoices()).isZero();
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.outstandingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.overdueAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateRevenueFromPaidInvoices() {
        // Arrange
        when(customerRepository.count()).thenReturn(5L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Draft)).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Sent)).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Paid)).thenReturn(10L);
        when(invoiceRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("25000.00"));
        when(invoiceRepository.calculateOutstandingAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.calculateOverdueAmount()).thenReturn(BigDecimal.ZERO);

        // Act
        DashboardStatsDTO result = handler.handle(new GetDashboardStatsQuery());

        // Assert
        assertThat(result.paidInvoices()).isEqualTo(10);
        assertThat(result.totalRevenue()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    void shouldCalculateOutstandingAndOverdueAmounts() {
        // Arrange
        when(customerRepository.count()).thenReturn(5L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Draft)).thenReturn(0L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Sent)).thenReturn(5L);
        when(invoiceRepository.countByStatus(InvoiceStatus.Paid)).thenReturn(0L);
        when(invoiceRepository.calculateTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.calculateOutstandingAmount()).thenReturn(new BigDecimal("10000.00"));
        when(invoiceRepository.calculateOverdueAmount()).thenReturn(new BigDecimal("2500.00"));

        // Act
        DashboardStatsDTO result = handler.handle(new GetDashboardStatsQuery());

        // Assert
        assertThat(result.sentInvoices()).isEqualTo(5);
        assertThat(result.outstandingAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(result.overdueAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }
}
