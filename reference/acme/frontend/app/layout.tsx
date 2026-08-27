import type { Metadata } from "next";
import { AuthShell } from "@/components/auth-shell";
import { branding } from "@/lib/branding";
import "./globals.css";

export const metadata: Metadata = {
  title: `${branding.name} | Customers`,
  description: branding.description,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full">
        <AuthShell>{children}</AuthShell>
      </body>
    </html>
  );
}
