"use client";

import { ReactNode } from "react";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex-1 w-full h-full">
      {children}
    </div>
  );
}

