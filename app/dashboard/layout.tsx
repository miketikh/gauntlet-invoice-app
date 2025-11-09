"use client";

import { useRouter, usePathname } from "next/navigation";
import { useAuth, useAuthActions } from "@/lib/stores/auth-store";
import { useRequireAuth } from "@/lib/hooks/useRequireAuth";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { LogOut, Menu, User } from "lucide-react";
import { useState } from "react";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuth();
  const { logout } = useAuthActions();
  const { isLoading } = useRequireAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // Helper function to check if path is active
  const isActive = (path: string) => {
    if (path === "/dashboard") {
      return pathname === "/dashboard";
    }
    return pathname.startsWith(path);
  };

  // Handle logout
  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  // Show loading state while checking auth
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 dark:border-slate-100 mx-auto mb-4"></div>
          <p className="text-slate-600 dark:text-slate-400">
            Authenticating...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex flex-col">
      {/* Header Navigation */}
      <header className="bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo/Brand */}
            <div className="flex items-center">
              <button
                onClick={() => router.push("/dashboard")}
                className="flex items-center gap-2 hover:opacity-80 transition-opacity"
              >
                <div className="flex items-center justify-center w-8 h-8 bg-slate-900 dark:bg-slate-100 rounded-md">
                  <span className="text-white dark:text-slate-900 font-bold text-sm">
                    IM
                  </span>
                </div>
                <span className="text-xl font-bold text-slate-900 dark:text-slate-50">
                  InvoiceMe
                </span>
              </button>
            </div>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-6">
              <button
                onClick={() => router.push("/dashboard")}
                className={`transition-colors font-medium ${
                  isActive("/dashboard")
                    ? "text-slate-900 dark:text-slate-50 border-b-2 border-slate-900 dark:border-slate-50 pb-1"
                    : "text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-50"
                }`}
              >
                Dashboard
              </button>
              <button
                onClick={() => router.push("/invoices")}
                className={`transition-colors font-medium ${
                  isActive("/invoices")
                    ? "text-slate-900 dark:text-slate-50 border-b-2 border-slate-900 dark:border-slate-50 pb-1"
                    : "text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-50"
                }`}
              >
                Invoices
              </button>
              <button
                onClick={() => router.push("/customers")}
                className={`transition-colors font-medium ${
                  isActive("/customers")
                    ? "text-slate-900 dark:text-slate-50 border-b-2 border-slate-900 dark:border-slate-50 pb-1"
                    : "text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-50"
                }`}
              >
                Customers
              </button>
              <button
                onClick={() => router.push("/payments")}
                className={`transition-colors font-medium ${
                  isActive("/payments")
                    ? "text-slate-900 dark:text-slate-50 border-b-2 border-slate-900 dark:border-slate-50 pb-1"
                    : "text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-50"
                }`}
              >
                Payments
              </button>
              <button
                onClick={() => router.push("/settings")}
                className={`transition-colors font-medium ${
                  isActive("/settings")
                    ? "text-slate-900 dark:text-slate-50 border-b-2 border-slate-900 dark:border-slate-50 pb-1"
                    : "text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-50"
                }`}
              >
                Settings
              </button>
            </nav>

            {/* User Menu */}
            <div className="flex items-center gap-4">
              {/* Mobile Menu Toggle */}
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="md:hidden p-2 rounded-md text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
              >
                <Menu className="h-5 w-5" />
              </button>

              {/* User Dropdown */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    className="flex items-center gap-2 border-slate-200 dark:border-slate-700"
                  >
                    <User className="h-4 w-4" />
                    <span className="hidden sm:inline">{user?.username}</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-48">
                  <DropdownMenuLabel>
                    <div className="flex flex-col">
                      <span className="text-sm font-medium">
                        {user?.username}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400 font-normal">
                        User Account
                      </span>
                    </div>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={() => router.push("/settings")}
                    className="cursor-pointer"
                  >
                    Settings
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={handleLogout}
                    className="cursor-pointer text-red-600 dark:text-red-400 focus:text-red-600 dark:focus:text-red-400"
                  >
                    <LogOut className="h-4 w-4 mr-2" />
                    Logout
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>

          {/* Mobile Navigation Menu */}
          {isMobileMenuOpen && (
            <div className="md:hidden border-t border-slate-200 dark:border-slate-700 py-4">
              <nav className="flex flex-col gap-2">
                <button
                  onClick={() => {
                    router.push("/dashboard");
                    setIsMobileMenuOpen(false);
                  }}
                  className={`text-left px-4 py-2 rounded-md transition-colors font-medium ${
                    isActive("/dashboard")
                      ? "bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-slate-50"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  Dashboard
                </button>
                <button
                  onClick={() => {
                    router.push("/invoices");
                    setIsMobileMenuOpen(false);
                  }}
                  className={`text-left px-4 py-2 rounded-md transition-colors font-medium ${
                    isActive("/invoices")
                      ? "bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-slate-50"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  Invoices
                </button>
                <button
                  onClick={() => {
                    router.push("/customers");
                    setIsMobileMenuOpen(false);
                  }}
                  className={`text-left px-4 py-2 rounded-md transition-colors font-medium ${
                    isActive("/customers")
                      ? "bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-slate-50"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  Customers
                </button>
                <button
                  onClick={() => {
                    router.push("/payments");
                    setIsMobileMenuOpen(false);
                  }}
                  className={`text-left px-4 py-2 rounded-md transition-colors font-medium ${
                    isActive("/payments")
                      ? "bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-slate-50"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  Payments
                </button>
                <button
                  onClick={() => {
                    router.push("/settings");
                    setIsMobileMenuOpen(false);
                  }}
                  className={`text-left px-4 py-2 rounded-md transition-colors font-medium ${
                    isActive("/settings")
                      ? "bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-slate-50"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  Settings
                </button>
              </nav>
            </div>
          )}
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white dark:bg-slate-800 border-t border-slate-200 dark:border-slate-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <p className="text-center text-sm text-slate-600 dark:text-slate-400">
            &copy; {new Date().getFullYear()} InvoiceMe. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
