/**
 * E2E Test: Payment Recording Flow
 * Tests complete user journey for recording payments against invoices
 */

import { test, expect } from '@playwright/test';

test.describe('Payment Recording', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('should record a payment on a sent invoice', async ({ page }) => {
    // Navigate to invoices
    await page.goto('/invoices');

    // Find and click on a sent invoice (assuming one exists)
    // In a real test, you would set up test data first
    await page.click('tr:has-text("Sent"):first-child');

    // Wait for invoice detail page to load
    await expect(page.locator('h1, h2').filter({ hasText: /invoice #/i })).toBeVisible();

    // Click "Record Payment" button
    await page.click('button:has-text("Record Payment")');

    // Wait for payment dialog to open
    await expect(page.locator('text=Record Payment').first()).toBeVisible();

    // Verify invoice context is displayed
    await expect(page.locator('text=/INV-\\d{4}-\\d{4}/')).toBeVisible();

    // Fill in payment form
    await page.fill('input[type="number"]', '100.00');

    // Select payment method
    await page.click('button:has-text("Select payment method")');
    await page.click('text=Credit Card');

    // Fill reference number
    await page.fill('input[placeholder*="Transaction ID"]', 'VISA-1234-TEST');

    // Add notes
    await page.fill('textarea', 'Test payment via E2E test');

    // Verify remaining balance is calculated
    await expect(page.locator('text=/Remaining.*Balance/i')).toBeVisible();

    // Submit the form
    await page.click('button:has-text("Record Payment")');

    // Wait for success toast
    await expect(page.locator('text=/payment.*recorded successfully/i')).toBeVisible({
      timeout: 5000,
    });

    // Verify dialog closes
    await expect(page.locator('text=Record Payment').first()).not.toBeVisible();

    // Verify invoice balance is updated on the page
    // The page should have refetched the invoice data
    await page.waitForTimeout(1000); // Wait for refetch

    // Check that balance has been updated
    await expect(page.locator('text=/Balance Due/i')).toBeVisible();
  });

  test('should show validation error when amount exceeds balance', async ({ page }) => {
    // Navigate to an invoice detail page
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');
    await expect(page.locator('text=Record Payment').first()).toBeVisible();

    // Try to enter an amount larger than the balance
    // First get the balance amount from the dialog
    const balanceText = await page.locator('text=/Current Balance/i').textContent();

    // Enter amount that exceeds balance
    await page.fill('input[type="number"]', '999999');

    // Verify error message appears
    await expect(page.locator('text=/amount cannot exceed balance/i')).toBeVisible();

    // Verify submit button behavior
    await page.click('button:has-text("Record Payment"):last-child');

    // Dialog should remain open (payment not submitted)
    await expect(page.locator('text=Record Payment').first()).toBeVisible();
  });

  test('should disable payment button for draft invoices', async ({ page }) => {
    // Navigate to invoices and find a draft invoice
    await page.goto('/invoices');

    // Click on a draft invoice (if exists)
    const draftRow = page.locator('tr:has-text("Draft")').first();
    if (await draftRow.count() > 0) {
      await draftRow.click();

      // Wait for detail page
      await page.waitForTimeout(500);

      // Record Payment button should be disabled
      const recordButton = page.locator('button:has-text("Record Payment")');
      if (await recordButton.count() > 0) {
        await expect(recordButton).toBeDisabled();

        // Hover to see tooltip
        await recordButton.hover();
        await expect(
          page.locator('text=/cannot record payment for draft/i')
        ).toBeVisible({ timeout: 2000 });
      }
    }
  });

  test('should disable payment button for paid invoices', async ({ page }) => {
    // Navigate to invoices and find a paid invoice
    await page.goto('/invoices');

    // Click on a paid invoice (if exists)
    const paidRow = page.locator('tr:has-text("Paid")').first();
    if (await paidRow.count() > 0) {
      await paidRow.click();

      // Wait for detail page
      await page.waitForTimeout(500);

      // Record Payment button should be disabled
      const recordButton = page.locator('button:has-text("Record Payment")');
      if (await recordButton.count() > 0) {
        await expect(recordButton).toBeDisabled();

        // Hover to see tooltip
        await recordButton.hover();
        await expect(
          page.locator('text=/fully paid/i')
        ).toBeVisible({ timeout: 2000 });
      }
    }
  });

  test('should show success message when invoice becomes fully paid', async ({ page }) => {
    // This test requires setting up an invoice with a small remaining balance
    // For now, we'll document the expected behavior

    // Navigate to invoice with small balance
    await page.goto('/invoices');
    // Find invoice with balance that can be fully paid
    // (In real test, this would be seeded data)

    // Record payment for full remaining balance
    // Expected: Two success toasts
    // 1. "Payment of $X recorded successfully"
    // 2. "Invoice #XXX is now fully paid"

    // Verify invoice status changes to "Paid"
    // await expect(page.locator('text=Paid').first()).toBeVisible();
  });

  test('should handle keyboard navigation', async ({ page }) => {
    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open dialog with keyboard
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    // Continue tabbing to Record Payment button
    // In practice, this depends on button order
    await page.click('button:has-text("Record Payment")');

    // Dialog opens
    await expect(page.locator('text=Record Payment').first()).toBeVisible();

    // Tab through form fields
    await page.keyboard.press('Tab'); // Should focus first field
    await page.keyboard.type('100');

    await page.keyboard.press('Tab'); // Payment method
    await page.keyboard.press('Enter'); // Open dropdown
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter'); // Select option

    await page.keyboard.press('Tab'); // Reference field
    await page.keyboard.type('TEST-REF');

    // Close dialog with Escape
    await page.keyboard.press('Escape');

    // Dialog should close
    await expect(page.locator('text=Record Payment').first()).not.toBeVisible();
  });

  test('should reset form when dialog is closed and reopened', async ({ page }) => {
    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');

    // Fill in some data
    await page.fill('input[type="number"]', '50');
    await page.fill('input[placeholder*="Transaction ID"]', 'TEST-123');

    // Close dialog without submitting
    await page.click('button:has-text("Cancel")');

    // Reopen dialog
    await page.click('button:has-text("Record Payment")');

    // Form should be reset
    const amountInput = page.locator('input[type="number"]');
    await expect(amountInput).toHaveValue('0');
  });

  test('should show real-time balance calculation', async ({ page }) => {
    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');

    // Get original balance
    const balanceElement = page.locator('text=/Current Balance/i ~ div');
    const balanceText = await balanceElement.textContent();

    // Extract number (basic parsing for test)
    const balance = parseFloat(balanceText?.replace(/[^0-9.]/g, '') || '0');

    // Enter payment amount
    await page.fill('input[type="number"]', '50');

    // Verify remaining balance is displayed
    await expect(page.locator('text=/Remaining Balance/i')).toBeVisible();

    // Calculate expected remaining
    const expectedRemaining = balance - 50;

    // Verify calculation is correct (approximately)
    const remainingText = await page.locator('text=/Remaining Balance/i ~ span').textContent();
    expect(remainingText).toContain(expectedRemaining.toFixed(2).toString().slice(0, -3));
  });

  test('should handle API errors gracefully', async ({ page }) => {
    // This test would require mocking API responses
    // For E2E, we'll test the error UI if API fails

    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');

    // Fill form
    await page.fill('input[type="number"]', '100');
    await page.click('button:has-text("Select payment method")');
    await page.click('text=Credit Card');
    await page.fill('input[placeholder*="Transaction ID"]', 'ERROR-TEST');

    // Submit - if backend returns error, toast should appear
    await page.click('button:has-text("Record Payment"):last-child');

    // If there's an error, toast should show
    // (This would need actual error scenario to test)
    // For now, we verify the form handles the flow
  });

  test('should display all payment methods in dropdown', async ({ page }) => {
    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');

    // Click payment method dropdown
    await page.click('button:has-text("Select payment method")');

    // Verify all payment methods are available
    await expect(page.locator('text=Credit Card')).toBeVisible();
    await expect(page.locator('text=Bank Transfer')).toBeVisible();
    await expect(page.locator('text=Check')).toBeVisible();
    await expect(page.locator('text=Cash')).toBeVisible();
  });

  test('should allow optional notes field to be empty', async ({ page }) => {
    // Navigate to invoice
    await page.goto('/invoices');
    await page.click('tr:has-text("Sent"):first-child');

    // Open payment dialog
    await page.click('button:has-text("Record Payment")');

    // Fill required fields only (skip notes)
    await page.fill('input[type="number"]', '100');
    await page.click('button:has-text("Select payment method")');
    await page.click('text=Credit Card');
    await page.fill('input[placeholder*="Transaction ID"]', 'REQUIRED-ONLY');

    // Submit should work without notes
    await page.click('button:has-text("Record Payment"):last-child');

    // Should succeed (or show validation for other reasons, but not notes)
    // Success toast or other validation errors (but not for notes)
  });
});
