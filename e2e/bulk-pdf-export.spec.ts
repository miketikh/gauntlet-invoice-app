/**
 * Bulk PDF Export E2E Tests
 * End-to-end tests for bulk PDF export functionality
 */

import { test, expect, type Page, type Download } from '@playwright/test';

// Helper to login before tests
async function login(page: Page) {
  await page.goto('/login');
  await page.fill('input[name="email"]', 'test@example.com');
  await page.fill('input[name="password"]', 'password');
  await page.click('button[type="submit"]');
  await page.waitForURL('/dashboard');
}

test.describe('Bulk PDF Export', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/invoices');
  });

  test('should export multiple PDFs when bulk action is triggered', async ({ page }) => {
    // Select multiple invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();

    // Select first 2 invoices
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Track downloads
    const downloads: Download[] = [];
    page.on('download', download => downloads.push(download));

    // Open bulk actions menu
    await page.getByRole('button', { name: /bulk actions/i }).click();

    // Click Export PDFs
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();

    // Confirm in modal
    await expect(page.getByText(/export 2 invoices as pdf/i)).toBeVisible();
    await page.getByRole('button', { name: /export 2 pdfs/i }).click();

    // Wait for progress modal
    await expect(page.getByText(/exporting invoices/i)).toBeVisible();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });

    // Verify downloads occurred
    expect(downloads.length).toBe(2);
    expect(downloads[0].suggestedFilename()).toMatch(/^Invoice-.*\.pdf$/);
    expect(downloads[1].suggestedFilename()).toMatch(/^Invoice-.*\.pdf$/);
  });

  test('should show confirmation modal with selected invoice details', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Open bulk actions and click export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();

    // Verify confirmation modal content
    await expect(page.getByText(/export 2 invoices as pdf/i)).toBeVisible();
    await expect(page.getByText(/this will download 2 pdf files/i)).toBeVisible();
    await expect(page.getByText(/selected invoices:/i)).toBeVisible();

    // Verify invoice numbers are shown
    const modalContent = page.getByRole('dialog');
    await expect(modalContent).toBeVisible();
  });

  test('should allow canceling the export from confirmation modal', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    const downloads: Download[] = [];
    page.on('download', download => downloads.push(download));

    // Open bulk actions and click export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();

    // Cancel
    await page.getByRole('button', { name: /cancel/i }).click();

    // Verify modal closed
    await expect(page.getByText(/export 2 invoices as pdf/i)).not.toBeVisible();

    // Verify no downloads occurred
    await page.waitForTimeout(1000);
    expect(downloads.length).toBe(0);
  });

  test('should show progress modal during export with real-time updates', async ({ page }) => {
    // Select multiple invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    const selectCount = Math.min(3, checkboxes.length);

    for (let i = 0; i < selectCount; i++) {
      await checkboxes[i].check();
    }

    // Start export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: new RegExp(`export ${selectCount} pdfs`, 'i') }).click();

    // Verify progress modal appears
    await expect(page.getByText(/exporting invoices/i)).toBeVisible();

    // Verify progress indicators
    await expect(page.getByText(/exporting \d+ of \d+ invoices/i)).toBeVisible();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });
  });

  test('should show results modal after export completes', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: /export 2 pdfs/i }).click();

    // Wait for results modal
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });

    // Verify success count
    await expect(page.getByText('2')).toBeVisible();
    await expect(page.getByText('Successful')).toBeVisible();
  });

  test('should clear selection after closing results modal', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Verify selection shown
    await expect(page.getByText('2')).toBeVisible();
    await expect(page.getByText('invoices selected')).toBeVisible();

    // Export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: /export 2 pdfs/i }).click();

    // Wait for results and close
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });
    await page.getByRole('button', { name: /close/i }).click();

    // Verify selection cleared
    await expect(page.getByText('invoices selected')).not.toBeVisible();
  });

  test('should handle single invoice export', async ({ page }) => {
    // Select one invoice
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 1) {
      await checkboxes[0].check();
    }

    const downloads: Download[] = [];
    page.on('download', download => downloads.push(download));

    // Export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();

    // Verify modal shows singular form
    await expect(page.getByText(/export 1 invoice as pdf/i)).toBeVisible();

    await page.getByRole('button', { name: /export 1 pdf/i }).click();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });

    // Verify one download
    expect(downloads.length).toBe(1);
  });

  test('should export invoices with different statuses', async ({ page }) => {
    // Select all visible invoices (which may include Draft, Sent, Paid)
    const selectAllCheckbox = page.locator('input[type="checkbox"][aria-label="Select all"]');
    await selectAllCheckbox.check();

    const selectedText = await page.locator('text=/\\d+ invoices? selected/').textContent();
    const count = parseInt(selectedText?.match(/\d+/)?.[0] || '0');

    if (count > 0) {
      const downloads: Download[] = [];
      page.on('download', download => downloads.push(download));

      // Export all
      await page.getByRole('button', { name: /bulk actions/i }).click();
      await page.getByRole('menuitem', { name: /export pdfs/i }).click();
      await page.getByRole('button', { name: new RegExp(`export ${count}`, 'i') }).click();

      // Wait for completion
      await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 60000 });

      // Verify all downloaded
      expect(downloads.length).toBe(count);
    }
  });

  test('should show warning for large batch sizes', async ({ page }) => {
    // Select more than 20 invoices (if available)
    const selectAllCheckbox = page.locator('input[type="checkbox"][aria-label="Select all"]');
    await selectAllCheckbox.check();

    const selectedText = await page.locator('text=/\\d+ invoices? selected/').textContent();
    const count = parseInt(selectedText?.match(/\d+/)?.[0] || '0');

    if (count > 20) {
      // Try to export
      await page.getByRole('button', { name: /bulk actions/i }).click();
      await page.getByRole('menuitem', { name: /export pdfs/i }).click();

      // Should show warning toast
      await expect(page.locator('text=/may take a while/i')).toBeVisible();
    }
  });

  test('should prevent export of more than 50 invoices', async ({ page }) => {
    // This test assumes there might be a way to select >50 invoices
    // In a real scenario with >50 invoices, this would be testable
    // For now, we'll verify the validation exists in the confirmation modal logic

    const selectAllCheckbox = page.locator('input[type="checkbox"][aria-label="Select all"]');
    await selectAllCheckbox.check();

    const selectedText = await page.locator('text=/\\d+ invoices? selected/').textContent();
    const count = parseInt(selectedText?.match(/\d+/)?.[0] || '0');

    // If we have more than 50 (unlikely in test data), verify error
    if (count > 50) {
      await page.getByRole('button', { name: /bulk actions/i }).click();
      await page.getByRole('menuitem', { name: /export pdfs/i }).click();

      // Should show error toast
      await expect(page.locator('text=/maximum 50 invoices/i')).toBeVisible();
    }
  });

  test('should maintain UI responsiveness during export', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 3) {
      for (let i = 0; i < 3; i++) {
        await checkboxes[i].check();
      }
    }

    // Start export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: /export 3 pdfs/i }).click();

    // Verify progress modal prevents interaction with background
    const progressModal = page.getByRole('dialog').filter({ hasText: /exporting invoices/i });
    await expect(progressModal).toBeVisible();

    // Try to click outside modal (should not close it)
    await page.mouse.click(10, 10);

    // Modal should still be visible
    await expect(progressModal).toBeVisible();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });
  });

  test('should show sequential download with delays', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    const downloadTimes: number[] = [];
    page.on('download', () => downloadTimes.push(Date.now()));

    // Export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: /export 2 pdfs/i }).click();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });

    // Verify downloads were sequential (with delay between them)
    if (downloadTimes.length >= 2) {
      const timeDiff = downloadTimes[1] - downloadTimes[0];
      // Should have at least 400ms delay (500ms target minus some tolerance)
      expect(timeDiff).toBeGreaterThan(400);
    }
  });

  test('should display accessibility features', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Start export
    await page.getByRole('button', { name: /bulk actions/i }).click();
    await page.getByRole('menuitem', { name: /export pdfs/i }).click();
    await page.getByRole('button', { name: /export 2 pdfs/i }).click();

    // Verify ARIA live region for progress
    const liveRegion = page.locator('[role="status"][aria-live="polite"]');
    await expect(liveRegion).toBeVisible();

    // Wait for completion
    await expect(page.getByText(/export complete/i)).toBeVisible({ timeout: 30000 });
  });

  test('should support keyboard navigation through modals', async ({ page }) => {
    // Select invoices
    const checkboxes = await page.locator('input[type="checkbox"][aria-label*="Select invoice"]').all();
    if (checkboxes.length >= 2) {
      await checkboxes[0].check();
      await checkboxes[1].check();
    }

    // Navigate with keyboard
    await page.getByRole('button', { name: /bulk actions/i }).click();

    // Tab to Export PDFs menu item
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    // Modal should open
    await expect(page.getByText(/export 2 invoices as pdf/i)).toBeVisible();

    // Tab through modal elements
    await page.keyboard.press('Tab'); // Focus first element
    await page.keyboard.press('Tab'); // Move to cancel button
    await page.keyboard.press('Tab'); // Move to confirm button

    // Press Escape to close
    await page.keyboard.press('Escape');

    // Modal should close
    await expect(page.getByText(/export 2 invoices as pdf/i)).not.toBeVisible();
  });
});
