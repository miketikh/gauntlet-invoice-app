"use client";

/**
 * Customer Form Component
 * Form for creating and editing customers
 */

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useCustomerStore, useCustomerActions } from "@/lib/stores/customer-store";
import { toast } from "sonner";
import type { CreateCustomerDTO, UpdateCustomerDTO } from "@/lib/api/types";

// Validation schema matching backend rules
const customerFormSchema = z.object({
  name: z.string().min(1, "Name is required").max(255, "Name must be less than 255 characters"),
  email: z
    .string()
    .min(1, "Email is required")
    .email("Invalid email format")
    .max(255, "Email must be less than 255 characters"),
  phone: z.string().optional(),
  address: z
    .object({
      street: z.string().optional(),
      city: z.string().optional(),
      state: z.string().optional(),
      postalCode: z.string().optional(),
      country: z.string().optional(),
    })
    .optional(),
});

type CustomerFormValues = z.infer<typeof customerFormSchema>;

interface CustomerFormProps {
  mode: "create" | "edit";
  customerId?: string;
  onSuccess?: () => void;
}

export function CustomerForm({ mode, customerId, onSuccess }: CustomerFormProps) {
  const router = useRouter();
  const { createCustomer, updateCustomer } = useCustomerActions();
  const selectedCustomer = useCustomerStore((state) => state.selectedCustomer);
  const loading = useCustomerStore((state) => state.loading);

  const form = useForm<CustomerFormValues>({
    resolver: zodResolver(customerFormSchema),
    defaultValues: {
      name: "",
      email: "",
      phone: "",
      address: {
        street: "",
        city: "",
        state: "",
        postalCode: "",
        country: "",
      },
    },
  });

  // Pre-populate form in edit mode
  useEffect(() => {
    if (mode === "edit" && selectedCustomer) {
      form.reset({
        name: selectedCustomer.name,
        email: selectedCustomer.email,
        phone: selectedCustomer.phone || "",
        address: {
          street: selectedCustomer.address?.street || "",
          city: selectedCustomer.address?.city || "",
          state: selectedCustomer.address?.state || "",
          postalCode: selectedCustomer.address?.postalCode || "",
          country: selectedCustomer.address?.country || "",
        },
      });
    }
  }, [mode, selectedCustomer, form]);

  const onSubmit = async (values: CustomerFormValues) => {
    try {
      // Clean up address if all fields are empty
      const hasAddress =
        values.address &&
        (values.address.street ||
          values.address.city ||
          values.address.state ||
          values.address.postalCode ||
          values.address.country);

      const data: CreateCustomerDTO | UpdateCustomerDTO = {
        name: values.name,
        email: values.email,
        phone: values.phone || undefined,
        address: hasAddress
          ? {
              street: values.address!.street || "",
              city: values.address!.city || "",
              state: values.address!.state || "",
              postalCode: values.address!.postalCode || "",
              country: values.address!.country || "",
            }
          : undefined,
      };

      if (mode === "create") {
        await createCustomer(data);
        toast.success("Customer created successfully");
        form.reset();
        if (onSuccess) {
          onSuccess();
        } else {
          router.push("/customers");
        }
      } else if (mode === "edit" && customerId) {
        await updateCustomer(customerId, data);
        toast.success("Customer updated successfully");
        if (onSuccess) {
          onSuccess();
        } else {
          router.push("/customers");
        }
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error && 'response' in error
          ? ((error as { response?: { data?: { message?: string } } }).response?.data?.message ||
             `Failed to ${mode === "create" ? "create" : "update"} customer`)
          : `Failed to ${mode === "create" ? "create" : "update"} customer`;
      toast.error(errorMessage);
    }
  };

  const handleCancel = () => {
    router.push("/customers");
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Information */}
        <div className="space-y-4">
          <h3 className="text-lg font-medium">Basic Information</h3>

          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>
                  Name <span className="text-red-600">*</span>
                </FormLabel>
                <FormControl>
                  <Input placeholder="John Doe" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>
                  Email <span className="text-red-600">*</span>
                </FormLabel>
                <FormControl>
                  <Input type="email" placeholder="john@example.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="phone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Phone</FormLabel>
                <FormControl>
                  <Input placeholder="+1 (555) 123-4567" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Address Information */}
        <div className="space-y-4">
          <h3 className="text-lg font-medium">Address</h3>

          <FormField
            control={form.control}
            name="address.street"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Street</FormLabel>
                <FormControl>
                  <Input placeholder="123 Main St" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="address.city"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>City</FormLabel>
                  <FormControl>
                    <Input placeholder="New York" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="address.state"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>State</FormLabel>
                  <FormControl>
                    <Input placeholder="NY" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="address.postalCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Postal Code</FormLabel>
                  <FormControl>
                    <Input placeholder="10001" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="address.country"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Country</FormLabel>
                  <FormControl>
                    <Input placeholder="United States" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex gap-4 pt-4">
          <Button type="submit" disabled={loading}>
            {loading ? "Saving..." : mode === "create" ? "Create Customer" : "Update Customer"}
          </Button>
          <Button type="button" variant="outline" onClick={handleCancel} disabled={loading}>
            Cancel
          </Button>
        </div>
      </form>
    </Form>
  );
}
