package com.invoiceme.invoice.api;

import com.invoiceme.invoice.queries.GetDashboardStatsQuery;
import com.invoiceme.invoice.queries.GetDashboardStatsQueryHandler;
import com.invoiceme.invoice.queries.dto.DashboardStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Dashboard statistics
 * Provides aggregate data for dashboard display
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and analytics")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final GetDashboardStatsQueryHandler getDashboardStatsQueryHandler;

    public DashboardController(GetDashboardStatsQueryHandler getDashboardStatsQueryHandler) {
        this.getDashboardStatsQueryHandler = getDashboardStatsQueryHandler;
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves aggregate statistics including invoice counts by status, revenue, and outstanding amounts"
    )
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        GetDashboardStatsQuery query = new GetDashboardStatsQuery();
        DashboardStatsDTO response = getDashboardStatsQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }
}
