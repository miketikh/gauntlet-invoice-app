import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Users, DollarSign, TrendingUp, BarChart3 } from "lucide-react";

export default function LandingPage() {
  return (
    <div className="min-h-screen flex flex-col relative">
      {/* Enhanced gradient background with more depth */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary/80 via-info/70 to-primary/60 -z-10">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_30%,rgba(255,255,255,0.25),transparent_40%)]"></div>
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_70%,rgba(255,255,255,0.2),transparent_40%)]"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-primary/30 via-transparent to-transparent"></div>
      </div>

      {/* Header with better contrast */}
      <header className="border-b border-white/30 bg-gradient-to-b from-black/20 to-black/10 backdrop-blur-md shadow-lg">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white shadow-xl">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="h-5 w-5 text-primary"
                >
                  <rect width="20" height="14" x="2" y="5" rx="2" />
                  <line x1="2" x2="22" y1="10" y2="10" />
                </svg>
              </div>
              <span className="text-xl font-bold text-white drop-shadow-md">InvoiceMe</span>
            </div>
            <Link href="/login">
              <Button
                variant="outline"
                size="sm"
                className="bg-white text-primary border-0 hover:bg-white/95 hover:text-primary hover:scale-105 shadow-xl transition-all duration-200"
              >
                Sign In
              </Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center py-12 sm:py-16">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto">
            {/* Hero section */}
            <div className="text-center mb-16">
              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-white mb-6 text-balance drop-shadow-lg">
                Invoicing that
                <br />
                just works
              </h1>
              <p className="text-lg md:text-xl text-white/95 mb-10 text-pretty max-w-2xl mx-auto leading-relaxed drop-shadow-md">
                Professional invoicing made simple. Track customers, payments, and stay organized.
              </p>
              <Link href="/login">
                <Button
                  size="lg"
                  className="text-lg shadow-2xl bg-success text-white hover:bg-success/90 hover:scale-105 h-14 px-12 font-semibold transition-all duration-200"
                >
                  Get Started
                </Button>
              </Link>
            </div>

            {/* Features grid - 3 on top row */}
            <div className="space-y-5 max-w-6xl mx-auto">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                {/* Feature 1 - Customer Management */}
                <Card className="p-6 bg-white/95 backdrop-blur-sm shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 border-0">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-primary/80 text-white mb-4 shadow-lg">
                    <Users className="h-7 w-7" />
                  </div>
                  <h3 className="text-xl font-bold text-card-foreground mb-3">Customer Management</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Organize clients with complete contact history and invoice tracking.
                  </p>
                </Card>

                {/* Feature 2 - Invoice Creation */}
                <Card className="p-6 bg-white/95 backdrop-blur-sm shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 border-0">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-info to-info/80 text-white mb-4 shadow-lg">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className="h-7 w-7"
                    >
                      <rect width="20" height="14" x="2" y="5" rx="2" />
                      <line x1="2" x2="22" y1="10" y2="10" />
                    </svg>
                  </div>
                  <h3 className="text-xl font-bold text-card-foreground mb-3">Invoice Creation</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Build detailed invoices with automatic calculations and professional templates.
                  </p>
                </Card>

                {/* Feature 3 - Payment Tracking */}
                <Card className="p-6 bg-white/95 backdrop-blur-sm shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 border-0">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-success to-success/80 text-white mb-4 shadow-lg">
                    <DollarSign className="h-7 w-7" />
                  </div>
                  <h3 className="text-xl font-bold text-card-foreground mb-3">Payment Tracking</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Record payments and reconcile balances with complete payment history.
                  </p>
                </Card>
              </div>

              {/* Bottom row - 2 centered cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 max-w-4xl mx-auto">
                {/* Feature 4 - Real-time Updates */}
                <Card className="p-6 bg-white/95 backdrop-blur-sm shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 border-0">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-warning to-warning/80 text-white mb-4 shadow-lg">
                    <TrendingUp className="h-7 w-7" />
                  </div>
                  <h3 className="text-xl font-bold text-card-foreground mb-3">Real-time Updates</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    Always know what's outstanding with live invoice status tracking.
                  </p>
                </Card>

                {/* Feature 5 - Clean Dashboard */}
                <Card className="p-6 bg-white/95 backdrop-blur-sm shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all duration-300 border-0">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-info/90 text-white mb-4 shadow-lg">
                    <BarChart3 className="h-7 w-7" />
                  </div>
                  <h3 className="text-xl font-bold text-card-foreground mb-3">Clean Dashboard</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    See your business health at a glance with invoice statistics and insights.
                  </p>
                </Card>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
