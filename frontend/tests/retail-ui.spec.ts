import { test, expect } from '@playwright/test';

test('retail mvp renders core panels', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Markets')).toBeVisible();
  await expect(page.getByText('Order Ticket')).toBeVisible();
  await expect(page.getByText('Chart')).toBeVisible();
});
