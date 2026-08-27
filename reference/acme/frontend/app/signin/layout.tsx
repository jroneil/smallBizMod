import type { Metadata } from "next";
import { branding } from "@/lib/branding";

export const metadata: Metadata = {
  title: `${branding.name} | Sign in`,
};

export default function SignInLayout({ children }: { children: React.ReactNode }) {
  return children;
}
