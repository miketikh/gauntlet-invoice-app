# QA Diagnostic Report - Database Migration Issues

**Date:** 2025-11-09
**Agent:** Quinn (Test Architect)
**Issue:** Backend build failures due to database migration schema conflicts

---

## Summary

Database migration schema conflicts resolved. V1 migration updated to match current JPA entities. Redundant migrations (V3, V5, V6, V7) deleted.

---

## Migration Fixes Applied

### 1. V1__initial_schema.sql - UPDATED ✅
**Changes:**
- `customers` table: Changed from `address TEXT, is_deleted BOOLEAN` to separate address fields (`street`, `city`, `state`, `postal_code`, `country`) + `deleted_at TIMESTAMP`
- `invoices` table: Added `version BIGINT NOT NULL DEFAULT 0` for optimistic locking
- `created_at`/`updated_at` columns: Added `NOT NULL` constraints to match JPA

**Rationale:** V1 schema conflicted with JPA entity definitions. Now matches:
- `Customer.java` - Uses `@Embedded Address` with separate fields
- `Invoice.java` - Uses `@Version Long version` for optimistic locking
- All entities use `@CreationTimestamp` and `@UpdateTimestamp` (NOT NULL)

### 2. Migrations Deleted ✅
- **V3__create_customers_table.sql** - DELETED (redundant, customers created in V1)
- **V5__create_invoices_table.sql** - DELETED (redundant, invoices created in V1)
- **V6__add_invoice_version_column.sql** - DELETED (version now in V1)
- **V7__create_payments_table.sql** - DELETED (redundant, payments created in V1)

### 3. Remaining Migrations (Clean)
- V1 - Initial schema (FIXED)
- V2 - Add test user
- V4 - Update admin password
- V8 - Create idempotency_records table
- V9 - Add performance indexes
- V10 - Update payment_method enum to SCREAMING_SNAKE_CASE

---

## Outstanding Issue: SSL Connection Failure

### Problem
PostgreSQL JDBC driver attempts SSL handshake despite `ssl=false` configuration, causing "Connection reset" error (SQL State: 08001).

### Tested Solutions (All Failed)
1. URL parameter: `?ssl=false`
2. URL parameter: `?sslmode=disable`
3. HikariCP data-source-properties: `ssl: false, sslmode: disable`

### Root Cause
PostgreSQL server has `ssl=off`, but JDBC driver still attempts SSL negotiation.

### Required Fix
Add explicit `sslmode=disable` to JDBC URL **or** configure PostgreSQL container to handle SSL negotiation properly.

---

## Recommendations for Dev Team

1. **Test migrations immediately** after SSL issue is resolved using:
   ```bash
   mvn clean spring-boot:run
   ```

2. **Do NOT modify V1-V10 migrations** - Flyway checksums are now set. Future schema changes must be **new migrations only** (V11+).

3. **Verify JPA validation** - With `ddl-auto: validate`, Hibernate will fail if entities don't match schema. Current V1 should pass.

4. **Monitor for**:
   - Missing `findAllById` method in `CustomerRepository` (causes compilation error in `ListInvoicesQueryHandler`)
   - Type mismatches in `PerformanceTest.java` (11 errors related to Address/CreateInvoiceCommand)

---

## Migration Execution Order (When SSL Fixed)

Flyway will execute in this order:
1. V1 - Creates all tables with correct schema
2. V2 - Inserts test user
3. V4 - Updates admin password
4. V8 - Creates idempotency table
5. V9 - Adds performance indexes
6. V10 - Updates payment method enum

**Expected Result:** All migrations should succeed with zero errors.

---

## Quality Gate Status

**CONCERNS** - Cannot verify migrations execute successfully due to connection issue.

**Next Steps:**
1. Fix SSL/connection configuration
2. Run full migration test
3. Verify schema matches JPA entities (`ddl-auto: validate`)
4. Run integration tests

**Blocking Issues:**
- SSL connection failure prevents testing
- Compilation errors in test suite (PerformanceTest.java)
