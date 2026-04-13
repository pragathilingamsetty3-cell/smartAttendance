import React from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';

export const PulseSkeleton = () => {
  return (
    <Card glass className="border-gray-800 bg-black/20 animate-pulse">
      <CardHeader>
        <div className="h-6 w-1/3 bg-gray-700/50 rounded drop-shadow-md mb-2"></div>
        <div className="h-4 w-1/2 bg-gray-800/50 rounded"></div>
      </CardHeader>
      <CardContent>
        <div className="h-[300px] w-full flex items-end justify-between space-x-2 pb-4">
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[40%]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[60%]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[80%]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[30%]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[50%]"></div>
          <div className="w-1/12 bg-[var(--color-primary)]/20 rounded-t h-[90%] blur-sm shadow-[0_0_15px_rgba(155,81,224,0.3)]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[40%]"></div>
          <div className="w-1/12 bg-[#ff007a]/20 rounded-t h-[70%] blur-sm shadow-[0_0_15px_rgba(255,0,122,0.3)]"></div>
          <div className="w-1/12 bg-gray-700/30 rounded-t h-[55%]"></div>
        </div>
      </CardContent>
    </Card>
  );
};
