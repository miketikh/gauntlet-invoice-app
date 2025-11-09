/**
 * Invoice Store - Zustand state management for invoices
 */

import { create } from 'zustand';
import type {
  InvoiceResponseDTO,
  InvoiceListItemDTO,
  InvoiceFormData,
  InvoiceListFilters,
  InvoicePagination,
  InvoiceSorting,
} from '@/lib/api/types';
import {
  createInvoice as apiCreateInvoice,
  updateInvoice as apiUpdateInvoice,
  getInvoiceById as apiGetInvoiceById,
  getInvoices as apiGetInvoices,
  sendInvoice as apiSendInvoice,
  deleteInvoice as apiDeleteInvoice,
  copyInvoice as apiCopyInvoice,
} from '@/lib/api/invoices';

interface InvoiceState {
  // State
  currentInvoice: InvoiceResponseDTO | null;
  invoices: InvoiceListItemDTO[];
  loading: boolean;
  error: string | null;

  // Filters, Pagination, and Sorting
  filters: InvoiceListFilters;
  pagination: InvoicePagination;
  sorting: InvoiceSorting;

  // Bulk selection
  selectedInvoiceIds: string[];

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
  deleteInvoice: (id: string) => Promise<void>;
  copyInvoice: (sourceInvoiceId: string) => Promise<InvoiceResponseDTO>;
  setFilters: (filters: Partial<InvoiceListFilters>) => void;
  clearFilters: () => void;
  setPagination: (pagination: Partial<InvoicePagination>) => void;
  setSorting: (sorting: Partial<InvoiceSorting>) => void;
  toggleSelectInvoice: (id: string) => void;
  selectAllInvoices: () => void;
  deselectAllInvoices: () => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useInvoiceStore = create<InvoiceState>((set, get) => ({
  // Initial state
  currentInvoice: null,
  invoices: [],
  loading: false,
  error: null,

  // Filters, Pagination, and Sorting
  filters: {},
  pagination: {
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  },
  sorting: {
    sortBy: 'issueDate',
    sortDirection: 'DESC',
  },

  // Bulk selection
  selectedInvoiceIds: [],

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
      const { filters, pagination, sorting } = get();
      const response = await apiGetInvoices(filters, pagination, sorting);
      set({
        invoices: response.content,
        pagination: {
          page: response.page,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages,
        },
        loading: false,
      });
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
      // Refresh invoice list
      await get().fetchInvoices();
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

  deleteInvoice: async (id) => {
    set({ loading: true, error: null });
    try {
      await apiDeleteInvoice(id);
      set({ loading: false });
      // Refresh invoice list
      await get().fetchInvoices();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to delete invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  copyInvoice: async (sourceInvoiceId) => {
    set({ loading: true, error: null });
    try {
      const invoice = await apiCopyInvoice(sourceInvoiceId);
      set({ currentInvoice: invoice, loading: false });
      return invoice;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage =
        err.response?.data?.message ||
        err.message ||
        'Failed to copy invoice';
      set({ error: errorMessage, loading: false });
      throw error;
    }
  },

  setFilters: (newFilters) => {
    set((state) => ({
      filters: { ...state.filters, ...newFilters },
      pagination: { ...state.pagination, page: 0 }, // Reset to first page when filters change
    }));
    get().fetchInvoices();
  },

  clearFilters: () => {
    set({
      filters: {},
      pagination: { ...get().pagination, page: 0 },
    });
    get().fetchInvoices();
  },

  setPagination: (newPagination) => {
    set((state) => ({
      pagination: { ...state.pagination, ...newPagination },
    }));
    get().fetchInvoices();
  },

  setSorting: (newSorting) => {
    set((state) => ({
      sorting: { ...state.sorting, ...newSorting },
    }));
    get().fetchInvoices();
  },

  toggleSelectInvoice: (id) => {
    set((state) => ({
      selectedInvoiceIds: state.selectedInvoiceIds.includes(id)
        ? state.selectedInvoiceIds.filter((invoiceId) => invoiceId !== id)
        : [...state.selectedInvoiceIds, id],
    }));
  },

  selectAllInvoices: () => {
    set((state) => ({
      selectedInvoiceIds: state.invoices.map((invoice) => invoice.id),
    }));
  },

  deselectAllInvoices: () => {
    set({ selectedInvoiceIds: [] });
  },

  setError: (error) => set({ error }),

  clearError: () => set({ error: null }),
}));
