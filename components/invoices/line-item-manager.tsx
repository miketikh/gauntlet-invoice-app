/**
 * LineItemManager Component
 * Manages dynamic list of invoice line items
 */

'use client';

import { useFieldArray, Control, UseFormWatch } from 'react-hook-form';
import { Trash2, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
} from '@/components/ui/form';
import { Card, CardContent } from '@/components/ui/card';
import { calculateLineItemTotals, formatCurrency } from '@/lib/utils/invoice-calculations';
import type { InvoiceFormValues } from '@/lib/schemas/invoice-schemas';
import type { LineItemFormData } from '@/lib/api/types';

interface LineItemManagerProps {
  control: Control<InvoiceFormValues>;
  watch: UseFormWatch<InvoiceFormValues>;
}

export function LineItemManager({ control, watch }: LineItemManagerProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lineItems',
  });

  const lineItems = watch('lineItems') || [];

  const addLineItem = () => {
    append({
      description: '',
      quantity: 1,
      unitPrice: 0,
      discountPercent: 0,
      taxRate: 0,
    });
  };

  const removeLineItem = (index: number) => {
    const item = lineItems[index];
    // Confirm if line item has data
    if (item && (item.description || item.quantity > 1 || item.unitPrice > 0)) {
      if (window.confirm('Are you sure you want to remove this line item?')) {
        remove(index);
      }
    } else {
      remove(index);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Line Items</h3>
        <Button type="button" onClick={addLineItem} variant="outline" size="sm">
          <Plus className="h-4 w-4 mr-2" />
          Add Line Item
        </Button>
      </div>

      {fields.length === 0 && (
        <div className="text-center py-8 text-muted-foreground border-2 border-dashed rounded-lg">
          No line items yet. Click &quot;Add Line Item&quot; to get started.
        </div>
      )}

      <div className="space-y-4 max-h-[600px] overflow-y-auto">
        {fields.map((field, index) => {
          const lineItem = lineItems[index];
          let calculated: LineItemFormData | null = null;

          // Calculate totals if line item has valid data
          if (
            lineItem &&
            lineItem.quantity > 0 &&
            lineItem.unitPrice > 0
          ) {
            calculated = calculateLineItemTotals(lineItem);
          }

          return (
            <Card key={field.id} className={index % 2 === 0 ? 'bg-muted/30' : ''}>
              <CardContent className="pt-6">
                <div className="space-y-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Description */}
                      <FormField
                        control={control}
                        name={`lineItems.${index}.description`}
                        render={({ field }) => (
                          <FormItem className="md:col-span-2">
                            <FormLabel>Description *</FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                placeholder="Item description"
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      {/* Quantity */}
                      <FormField
                        control={control}
                        name={`lineItems.${index}.quantity`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Quantity *</FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                type="number"
                                step="0.01"
                                min="0"
                                placeholder="0"
                                onChange={(e) =>
                                  field.onChange(parseFloat(e.target.value) || 0)
                                }
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      {/* Unit Price */}
                      <FormField
                        control={control}
                        name={`lineItems.${index}.unitPrice`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Unit Price *</FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                type="number"
                                step="0.01"
                                min="0"
                                placeholder="0.00"
                                onChange={(e) =>
                                  field.onChange(parseFloat(e.target.value) || 0)
                                }
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      {/* Discount Percent */}
                      <FormField
                        control={control}
                        name={`lineItems.${index}.discountPercent`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>
                              Discount % (0-1)
                            </FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                type="number"
                                step="0.01"
                                min="0"
                                max="1"
                                placeholder="0.00"
                                onChange={(e) =>
                                  field.onChange(parseFloat(e.target.value) || 0)
                                }
                                title="Enter as decimal: 0.10 = 10%"
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      {/* Tax Rate */}
                      <FormField
                        control={control}
                        name={`lineItems.${index}.taxRate`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>
                              Tax Rate (0-1)
                            </FormLabel>
                            <FormControl>
                              <Input
                                {...field}
                                type="number"
                                step="0.01"
                                min="0"
                                placeholder="0.00"
                                onChange={(e) =>
                                  field.onChange(parseFloat(e.target.value) || 0)
                                }
                                title="Enter as decimal: 0.08 = 8%"
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => removeLineItem(index)}
                      className="mt-8"
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>

                  {/* Calculated Totals - Read-only Display */}
                  {calculated && (
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-2 pt-4 border-t text-sm">
                      <div>
                        <Label className="text-xs text-muted-foreground">Subtotal</Label>
                        <div className="font-semibold">{formatCurrency(calculated.subtotal || 0)}</div>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">Discount</Label>
                        <div className="font-semibold">{formatCurrency(calculated.discountAmount || 0)}</div>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">Taxable</Label>
                        <div className="font-semibold">{formatCurrency(calculated.taxableAmount || 0)}</div>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">Tax</Label>
                        <div className="font-semibold">{formatCurrency(calculated.taxAmount || 0)}</div>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">Total</Label>
                        <div className="font-semibold text-primary">{formatCurrency(calculated.total || 0)}</div>
                      </div>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
