import type { Metadata } from "next";
import { branding } from "@/lib/branding";

export const metadata: Metadata = {
  title: `${branding.name} | Change password`,
};

export default function ChangePasswordLayout({ children }: { children: React.ReactNode }) {
  return children;
}
