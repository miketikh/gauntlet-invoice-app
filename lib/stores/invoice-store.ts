/**
 * Invoice Store - Zustand state management for invoices
 */

import { create } from 'zustand';
import type {
  InvoiceResponseDTO,
  InvoiceListItemDTO,
  InvoiceFormData,
} from '@/lib/api/types';
import {
  createInvoice as apiCreateInvoice,
  updateInvoice as apiUpdateInvoice,
  getInvoiceById as apiGetInvoiceById,
  getInvoices as apiGetInvoices,
  sendInvoice as apiSendInvoice,
} from '@/lib/api/invoices';

interface InvoiceState {
  // State
  currentInvoice: InvoiceResponseDTO | null;
  invoices: InvoiceListItemDTO[];
  loading: boolean;
  error: string | null;

  // Actions
  setCurrentInvoice: (invoice: InvoiceResponseDTO | null) => void;
  clearCurrentInvoice: () => void;
  createInvoice: (formData: InvoiceFormData) => Promise<InvoiceResponseDTO>;
  updateInvoice: (
    id: string,
    formData: InvoiceFormData,
    version: number
  ) => Promise<InvoiceResponseDTO>;
  fetchInvoice: (id: string) => Promise<InvoiceResponseDTO>;
  fetchInvoices: () => Promise<void>;
  sendInvoice: (id: string) => Promise<InvoiceResponseDTO>;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useInvoiceStore = create<InvoiceState>((set) => ({
  // Initial state
  currentInvoice: null,
  invoices: [],
  loading: false,
  error: null,

  // Actions
  setCurrentInvoice: (invoice) => set({ currentInvoice: invoice }),

  clearCurrentInvoice: () => set({ currentInvoice: null }),

  createInvoice: async (formData) => {
    set({ loading: true, error: null });
    try {
      const invoice = await apiCreateInvoice(formData);
      set({ currentInvoice: invoice, loading: false });
      return invoice;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to create invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  updateInvoice: async (id, formData, version) => {
    set({ loading: true, error: null });
    try {
      const invoice = await apiUpdateInvoice(id, formData, version);
      set({ currentInvoice: invoice, loading: false });
      return invoice;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to update invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  fetchInvoice: async (id) => {
    set({ loading: true, error: null });
    try {
      const invoice = await apiGetInvoiceById(id);
      set({ currentInvoice: invoice, loading: false });
      return invoice;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to fetch invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  fetchInvoices: async () => {
    set({ loading: true, error: null });
    try {
      const invoices = await apiGetInvoices();
      set({ invoices, loading: false });
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to fetch invoices';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  sendInvoice: async (id) => {
    set({ loading: true, error: null });
    try {
      const invoice = await apiSendInvoice(id);
      set({ currentInvoice: invoice, loading: false });
      return invoice;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to send invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  setError: (error) => set({ error }),

  clearError: () => set({ error: null }),
}));
