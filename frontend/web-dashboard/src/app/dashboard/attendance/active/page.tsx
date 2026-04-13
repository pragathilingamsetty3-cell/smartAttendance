'use client';

import React from 'react';
import { TripleLockScanner } from '@/components/attendance/TripleLockScanner';

export default function ActiveSessionPage() {
  return (
    <div className="space-y-6 max-w-[1600px] mx-auto min-h-[calc(100vh-8rem)] flex flex-col">
      <div className="flex justify-between items-end mb-2">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">Active Surveillance Network</h1>
          <p className="text-slate-400 mt-1">Live Identity Verification Node Alpha-7</p>
        </div>
      </div>
      
      {/* The Triple Lock Hero Feature */}
      <TripleLockScanner />
    </div>
  );
}
