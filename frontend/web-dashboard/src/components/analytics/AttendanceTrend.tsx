'use client';

import React from 'react';
import {
  AreaChart,
  Area,
  ResponsiveContainer,
  XAxis,
  YAxis,
  Tooltip,
  TooltipProps,
} from 'recharts';
import { AnalyticsTrendDTO } from '@/types/index';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';

interface AttendanceTrendProps {
  data: AnalyticsTrendDTO[];
}

const GlassTooltip = ({ active, payload, label }: { active?: boolean; payload?: unknown[]; label?: string }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-gray-900/40 backdrop-blur-md border border-gray-700/50 p-3 rounded-lg shadow-[0_0_15px_rgba(155,81,224,0.3)]">
        <p className="text-gray-300 text-sm mb-1">{`Time: ${label}`}</p>
        <p className="text-xl font-bold text-white drop-shadow-[0_0_8px_rgba(155,81,224,0.8)]">
          {`Velocity: ${(payload as Array<{value: number}>)[0].value}`}
        </p>
      </div>
    );
  }
  return null;
};

export const AttendanceTrend: React.FC<AttendanceTrendProps> = ({ data }) => {
  return (
    <Card glass className="border-gray-800 bg-black/20">
      <CardHeader>
        <h3 className="text-lg font-semibold text-white tracking-tight drop-shadow-md">
          Live Heartbeat Velocity
        </h3>
        <p className="text-sm text-gray-400">
          Real-time tracking of active validations
        </p>
      </CardHeader>
      <CardContent>
        <div className="h-[300px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart
              data={data}
              margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
            >
              <defs>
                <linearGradient id="neonViolet" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#9b51e0" stopOpacity={1} />
                  <stop offset="100%" stopColor="#9b51e0" stopOpacity={0} />
                </linearGradient>
                <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                  <feGaussianBlur stdDeviation="4" result="blur" />
                  <feMerge>
                    <feMergeNode in="blur" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>
              <XAxis
                dataKey="time"
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
              />
              <Tooltip content={<GlassTooltip />} cursor={{ stroke: '#9b51e0', strokeWidth: 1, strokeDasharray: '4 4' }} />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#9b51e0"
                strokeWidth={3}
                fillOpacity={1}
                fill="url(#neonViolet)"
                filter="url(#glow)"
                isAnimationActive={true}
                animationDuration={1500}
                animationEasing="ease-out"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
};
