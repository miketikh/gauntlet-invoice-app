/**
 * E2E Test: Invoice PDF Download
 * Tests complete user journey for downloading invoice PDFs
 */

import { test, expect } from '@playwright/test';
import type { Download } from '@playwright/test';

test.describe('Invoice PDF Download', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('should download PDF when Export PDF button is clicked', async ({ page }) => {
    // Navigate to invoices
    await page.goto('/invoices');

    // Click on the first invoice in the list
    await page.click('tbody tr:first-child');

    // Wait for invoice detail page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Setup download handler before clicking
    const downloadPromise = page.waitForEvent('download');

    // Click Export PDF button
    await page.click('button:has-text("Export PDF")');

    // Wait for the download
    const download = await downloadPromise;

    // Verify the filename format
    const fileName = download.suggestedFilename();
    expect(fileName).toMatch(/^Invoice-INV-\d{4}-\d{4}\.pdf$/);

    // Verify the file is a PDF (magic bytes)
    const path = await download.path();
    if (path) {
      const fs = await import('fs/promises');
      const buffer = await fs.readFile(path);
      const header = buffer.toString('utf8', 0, 5);
      expect(header).toBe('%PDF-');
    }
  });

  test('should show loading state while generating PDF', async ({ page }) => {
    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Click Export PDF button
    const exportButton = page.locator('button:has-text("Export PDF")');
    await exportButton.click();

    // Should show loading state
    await expect(page.locator('button:has-text("Generating PDF...")')).toBeVisible({
      timeout: 2000,
    });

    // Button should be disabled during download
    await expect(exportButton).toBeDisabled();

    // Wait for download to complete and button to return to normal state
    await expect(page.locator('button:has-text("Export PDF")')).toBeEnabled({
      timeout: 10000,
    });
  });

  test('should display success toast after PDF download', async ({ page }) => {
    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Setup download handler
    const downloadPromise = page.waitForEvent('download');

    // Click Export PDF button
    await page.click('button:has-text("Export PDF")');

    // Wait for download
    await downloadPromise;

    // Verify success toast appears
    await expect(page.locator('text=/PDF downloaded successfully/i')).toBeVisible({
      timeout: 5000,
    });
  });

  test('should allow PDF export for Draft invoices', async ({ page }) => {
    // Navigate to invoices
    await page.goto('/invoices');

    // Click on a draft invoice
    await page.click('tr:has-text("Draft"):first-child');

    // Wait for invoice detail page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Export PDF button should be visible and enabled
    const exportButton = page.locator('button:has-text("Export PDF")');
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();
  });

  test('should allow PDF export for Sent invoices', async ({ page }) => {
    // Navigate to invoices
    await page.goto('/invoices');

    // Click on a sent invoice
    await page.click('tr:has-text("Sent"):first-child');

    // Wait for invoice detail page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Export PDF button should be visible and enabled
    const exportButton = page.locator('button:has-text("Export PDF")');
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();
  });

  test('should allow PDF export for Paid invoices', async ({ page }) => {
    // Navigate to invoices
    await page.goto('/invoices');

    // Click on a paid invoice
    await page.click('tr:has-text("Paid"):first-child');

    // Wait for invoice detail page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Export PDF button should be visible and enabled
    const exportButton = page.locator('button:has-text("Export PDF")');
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();
  });

  test('should be keyboard accessible', async ({ page }) => {
    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Tab to the Export PDF button
    const exportButton = page.locator('button:has-text("Export PDF")');
    await exportButton.focus();

    // Verify button has focus
    await expect(exportButton).toBeFocused();

    // Setup download handler
    const downloadPromise = page.waitForEvent('download');

    // Press Enter to trigger download
    await page.keyboard.press('Enter');

    // Wait for download
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.pdf$/);
  });

  test('should handle multiple downloads sequentially', async ({ page }) => {
    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // First download
    let downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Export PDF")');
    const download1 = await downloadPromise;
    expect(download1.suggestedFilename()).toMatch(/\.pdf$/);

    // Wait for button to be enabled again
    await expect(page.locator('button:has-text("Export PDF")')).toBeEnabled({
      timeout: 5000,
    });

    // Second download
    downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Export PDF")');
    const download2 = await downloadPromise;
    expect(download2.suggestedFilename()).toMatch(/\.pdf$/);
  });

  test('should display error toast when PDF generation fails', async ({ page }) => {
    // Mock a server error by intercepting the API call
    await page.route('**/api/invoices/*/pdf', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'PDF generation failed',
        }),
      });
    });

    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Click Export PDF button
    await page.click('button:has-text("Export PDF")');

    // Verify error toast appears
    await expect(
      page.locator('text=/PDF generation failed. Please contact support./i')
    ).toBeVisible({
      timeout: 5000,
    });

    // Verify retry button is present
    await expect(page.locator('button:has-text("Retry")')).toBeVisible();
  });

  test('should display error toast when invoice not found', async ({ page }) => {
    // Mock a 404 error
    await page.route('**/api/invoices/*/pdf', (route) => {
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Invoice not found',
        }),
      });
    });

    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Click Export PDF button
    await page.click('button:has-text("Export PDF")');

    // Verify error toast appears
    await expect(page.locator('text=/Invoice not found./i')).toBeVisible({
      timeout: 5000,
    });
  });

  test('should retry download when retry button clicked', async ({ page }) => {
    let apiCallCount = 0;

    // Mock error on first call, success on second
    await page.route('**/api/invoices/*/pdf', (route) => {
      apiCallCount++;
      if (apiCallCount === 1) {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'PDF generation failed',
          }),
        });
      } else {
        // Return a mock PDF on retry
        route.continue();
      }
    });

    // Navigate to invoice detail page
    await page.goto('/invoices');
    await page.click('tbody tr:first-child');

    // Wait for page to load
    await expect(page.locator('text=/Invoice INV-/i')).toBeVisible();

    // Click Export PDF button (will fail)
    await page.click('button:has-text("Export PDF")');

    // Wait for error toast
    await expect(
      page.locator('text=/PDF generation failed. Please contact support./i')
    ).toBeVisible({
      timeout: 5000,
    });

    // Setup download handler for retry
    const downloadPromise = page.waitForEvent('download');

    // Click retry button
    await page.click('button:has-text("Retry")');

    // Wait for successful download
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.pdf$/);

    // Verify success toast
    await expect(page.locator('text=/PDF downloaded successfully/i')).toBeVisible({
      timeout: 5000,
    });
  });
});
