import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { ErrorBoundary } from "@/components/ui/ErrorBoundary";
import { AuthProvider } from "@/stores/authContext";
import { MainLayout } from "@/components/layout/MainLayout";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Smart Attendance System",
  description: "AI-powered attendance management system",
  manifest: "/manifest.json",
  themeColor: "#1e293b",
};

import { AppWrapper } from "@/components/layout/AppWrapper";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-screen bg-gradient-to-br from-slate-900 via-[#1a103c] to-slate-900 text-white antialiased">
        <AppWrapper>
          {children}
        </AppWrapper>
      </body>
    </html>
  );
}

