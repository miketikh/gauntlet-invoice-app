/**
 * Payment Modal Store (Zustand)
 * Manages global payment modal state for recording payments
 * Allows payment modal to be opened from anywhere in the app
 */

import { create } from 'zustand';

interface PaymentModalState {
  isOpen: boolean;
  invoiceId: string | null;
}

interface PaymentModalActions {
  openPaymentModal: (invoiceId: string) => void;
  closePaymentModal: () => void;
  reset: () => void;
}

type PaymentModalStore = PaymentModalState & PaymentModalActions;

const initialState: PaymentModalState = {
  isOpen: false,
  invoiceId: null,
};

/**
 * Payment Modal Store
 * No persistence needed - this is ephemeral UI state
 */
export const usePaymentModalStore = create<PaymentModalStore>((set) => ({
  // Initial state
  ...initialState,

  // Actions
  openPaymentModal: (invoiceId: string) => {
    set({ isOpen: true, invoiceId });
  },

  closePaymentModal: () => {
    set({ isOpen: false });
    // Don't clear invoiceId immediately to allow for animation/cleanup
    setTimeout(() => {
      set({ invoiceId: null });
    }, 300); // Match dialog close animation duration
  },

  reset: () => {
    set(initialState);
  },
}));

// Convenience selectors for common use cases
export const usePaymentModalActions = () =>
  usePaymentModalStore((state) => ({
    openPaymentModal: state.openPaymentModal,
    closePaymentModal: state.closePaymentModal,
  }));

export const useIsPaymentModalOpen = () =>
  usePaymentModalStore((state) => state.isOpen);

export const usePaymentModalInvoiceId = () =>
  usePaymentModalStore((state) => state.invoiceId);
