'use client';

import React from 'react';
import {
  BarChart,
  Bar,
  ResponsiveContainer,
  XAxis,
  YAxis,
  Tooltip,
  TooltipProps,
  Cell
} from 'recharts';
import { AnomalyBreakdownDTO } from '@/types/index';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';

interface AnomalyChartProps {
  data: AnomalyBreakdownDTO[];
}

const GlassTooltip = ({ active, payload, label }: { active?: boolean; payload?: unknown[]; label?: string }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-gray-900/60 backdrop-blur-md border border-[#ff007a]/40 p-3 rounded-lg shadow-[0_0_15px_rgba(255,0,122,0.3)]">
        <p className="text-gray-300 text-sm mb-1">{label}</p>
        <p className="text-xl font-bold text-[#ff007a] drop-shadow-[0_0_8px_rgba(255,0,122,0.8)]">
          {`Alerts: ${(payload as Array<{value: number}>)[0].value}`}
        </p>
      </div>
    );
  }
  return null;
};

export const AnomalyChart: React.FC<AnomalyChartProps> = ({ data }) => {
  return (
    <Card glass className="border-[#ff007a]/20 bg-black/20">
      <CardHeader>
        <h3 className="text-lg font-semibold text-white tracking-tight drop-shadow-md">
          Triple-Lock Anomalies Caught
        </h3>
        <p className="text-sm text-gray-400">
          Verification failures mapped by category
        </p>
      </CardHeader>
      <CardContent>
        <div className="h-[300px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={data}
              margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
            >
              <defs>
                <filter id="neonPinkGlow" x="-20%" y="-20%" width="140%" height="140%">
                  <feGaussianBlur stdDeviation="3" result="blur" />
                  <feMerge>
                    <feMergeNode in="blur" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>
              <XAxis
                dataKey="type"
                stroke="#475569"
                fontSize={12}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                stroke="#475569"
                fontSize={12}
                tickLine={false}
                axisLine={false}
                allowDecimals={false}
              />
              <Tooltip cursor={{ fill: 'rgba(255, 0, 122, 0.05)' }} content={<GlassTooltip />} />
              <Bar
                dataKey="count"
                radius={[4, 4, 0, 0]}
                isAnimationActive={true}
                animationDuration={1500}
                animationEasing="ease-out"
              >
                {data.map((entry, index) => (
                  <Cell 
                    key={`cell-${index}`} 
                    fill="#ff007a" 
                    filter="url(#neonPinkGlow)" 
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
};
