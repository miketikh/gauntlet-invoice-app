/**
 * Tests for Login Page
 * Tests form rendering, validation, submission, and error handling
 */

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginPage from '../page';
import { useAuthStore } from '@/lib/stores/auth-store';
import { useRouter } from 'next/navigation';
import { AuthError } from '@/lib/api/auth';

// Mock next/navigation
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

// Mock auth store
jest.mock('@/lib/stores/auth-store');

const mockPush = jest.fn();
const mockLogin = jest.fn();

describe('LoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      prefetch: jest.fn(),
      back: jest.fn(),
    });

    (useAuthStore as unknown as jest.Mock).mockReturnValue(mockLogin);
  });

  describe('Form Rendering', () => {
    it('should render login form with all fields', () => {
      render(<LoginPage />);

      expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    it('should render form title and description', () => {
      render(<LoginPage />);

      expect(screen.getByText('Login')).toBeInTheDocument();
      expect(screen.getByText(/enter your credentials/i)).toBeInTheDocument();
    });

    it('should show test credentials hint in development', () => {
      const originalEnv = process.env.NODE_ENV;
      process.env.NODE_ENV = 'development';

      render(<LoginPage />);

      expect(screen.getByText(/test credentials/i)).toBeInTheDocument();
      expect(screen.getByText(/username:/i)).toBeInTheDocument();
      expect(screen.getByText(/password:/i)).toBeInTheDocument();

      process.env.NODE_ENV = originalEnv;
    });
  });

  describe('Form Validation', () => {
    it('should show error for short username', async () => {
      const user = userEvent.setup();
      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);

      await user.type(usernameInput, 'ab');
      await user.type(passwordInput, 'validpassword');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/username must be at least 3 characters/i)).toBeInTheDocument();
      });
    });

    it('should show error for short password', async () => {
      const user = userEvent.setup();
      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);

      await user.type(usernameInput, 'validuser');
      await user.type(passwordInput, '12345');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/password must be at least 6 characters/i)).toBeInTheDocument();
      });
    });

    it('should not show errors for valid input', async () => {
      const user = userEvent.setup();
      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);

      await user.type(usernameInput, 'validuser');
      await user.type(passwordInput, 'validpassword');

      expect(screen.queryByText(/username must be/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/password must be/i)).not.toBeInTheDocument();
    });
  });

  describe('Form Submission', () => {
    it('should call login and redirect on successful login', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce(undefined);

      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(usernameInput, 'admin');
      await user.type(passwordInput, 'admin123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockLogin).toHaveBeenCalledWith('admin', 'admin123');
        expect(mockPush).toHaveBeenCalledWith('/dashboard');
      });
    });

    it('should show error message on login failure', async () => {
      const user = userEvent.setup();
      const error = new AuthError('Invalid username or password', 401);
      mockLogin.mockRejectedValueOnce(error);

      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(usernameInput, 'wrong');
      await user.type(passwordInput, 'password');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid username or password/i)).toBeInTheDocument();
      });

      expect(mockPush).not.toHaveBeenCalled();
    });

    it('should show generic error for unknown errors', async () => {
      const user = userEvent.setup();
      mockLogin.mockRejectedValueOnce(new Error('Unknown error'));

      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(usernameInput, 'admin');
      await user.type(passwordInput, 'admin123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/an unexpected error occurred/i)).toBeInTheDocument();
      });
    });
  });

  describe('Loading State', () => {
    it('should show loading state during login', async () => {
      const user = userEvent.setup();
      let resolveLogin: () => void;
      const loginPromise = new Promise<void>((resolve) => {
        resolveLogin = resolve;
      });
      mockLogin.mockReturnValueOnce(loginPromise);

      render(<LoginPage />);

      const usernameInput = screen.getByLabelText(/username/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(usernameInput, 'admin');
      await user.type(passwordInput, 'admin123');
      await user.click(submitButton);

      // Should show loading state
      await waitFor(() => {
        expect(screen.getByText(/signing in/i)).toBeInTheDocument();
      });

      // Button should be disabled
      expect(submitButton).toBeDisabled();

      // Inputs should be disabled
      expect(usernameInput).toBeDisabled();
      expect(passwordInput).toBeDisabled();

      // Resolve the login
      resolveLogin!();
    });
  });
});
