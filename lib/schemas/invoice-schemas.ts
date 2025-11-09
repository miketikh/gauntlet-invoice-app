/**
 * Invoice Form Validation Schemas using Zod
 * Matches backend validation rules
 */

import { z } from 'zod';

// Line item schema
export const lineItemSchema = z.object({
  id: z.string().optional(), // temporary UI id
  description: z.string().min(1, 'Description is required'),
  quantity: z.number().positive('Quantity must be greater than 0'),
  unitPrice: z.number().positive('Unit price must be greater than 0'),
  discountPercent: z
    .number()
    .min(0, 'Discount percent must be at least 0')
    .max(1, 'Discount percent must be at most 1 (100%)'),
  taxRate: z.number().min(0, 'Tax rate must be at least 0'),
  // Calculated fields are optional since they're computed
  subtotal: z.number().optional(),
  discountAmount: z.number().optional(),
  taxableAmount: z.number().optional(),
  taxAmount: z.number().optional(),
  total: z.number().optional(),
});

// Invoice form schema with cross-field validation
export const invoiceFormSchema = z
  .object({
    customerId: z.string().min(1, 'Customer is required'),
    issueDate: z.date({
      message: 'Issue date must be a valid date',
    }),
    dueDate: z.date({
      message: 'Due date must be a valid date',
    }),
    paymentTerms: z.string().min(1, 'Payment terms are required'),
    lineItems: z
      .array(lineItemSchema)
      .min(1, 'At least one line item is required'),
    notes: z.string().optional(),
  })
  .refine((data) => data.dueDate > data.issueDate, {
    message: 'Due date must be after issue date',
    path: ['dueDate'],
  });

// Type inference from schemas
export type LineItemFormValues = z.infer<typeof lineItemSchema>;
export type InvoiceFormValues = z.infer<typeof invoiceFormSchema>;
