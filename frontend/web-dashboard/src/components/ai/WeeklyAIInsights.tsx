'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Sparkles, RefreshCw, AlertCircle, CheckCircle2, TrendingUp, Calendar } from 'lucide-react';
import { aiAnalyticsService } from '@/services/aiAnalytics.service';
import { Loading } from '@/components/ui/Loading';

export const WeeklyAIInsights: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [insights, setInsights] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [generatedAt, setGeneratedAt] = useState<string | null>(null);

  useEffect(() => {
    fetchInsights();
  }, []);

  const fetchInsights = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await aiAnalyticsService.getWeeklyInsights();
      setInsights(data.insights);
      setGeneratedAt(data.generatedAt);
    } catch (err: any) {
      setError(err.message || 'Failed to generate AI insights');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchInsights();
    setRefreshing(false);
  };

  const formatInsights = (text: string) => {
    if (!text) return null;
    
    // Split by newlines or bullet points
    const lines = text.split('\n').filter(line => line.trim().length > 0);
    
    return (
      <ul className="space-y-4">
        {lines.map((line, index) => {
          // Determine icon based on content or just use a default sparkle
          const icon = index === 0 ? <TrendingUp className="h-5 w-5 text-blue-400 mt-1" /> :
                       index === 1 ? <AlertCircle className="h-5 w-5 text-orange-400 mt-1" /> :
                       <CheckCircle2 className="h-5 w-5 text-green-400 mt-1" />;
                       
          return (
            <li key={index} className="flex items-start space-x-3 p-3 rounded-lg bg-gray-800/20 border border-gray-700/30 hover:border-purple-500/30 transition-colors">
              <span className="flex-shrink-0">{icon}</span>
              <p className="text-gray-300 text-sm leading-relaxed">
                {line.replace(/^[•\s*-]+/, '')}
              </p>
            </li>
          );
        })}
      </ul>
    );
  };

  if (loading && !refreshing) {
    return (
      <Card glass className="border-purple-500/20">
        <CardContent className="h-64 flex items-center justify-center">
          <Loading text="AI is analyzing attendance data..." />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card glass className="border-purple-500/20 overflow-hidden relative">
      <div className="absolute top-0 right-0 p-4 opacity-5 pointer-events-none">
        <Sparkles size={120} className="text-purple-500" />
      </div>
      
      <CardHeader className="flex flex-row items-center justify-between border-b border-gray-700/50 pb-4">
        <div className="flex items-center space-x-2">
          <div className="p-2 bg-purple-500/20 rounded-lg">
            <Sparkles className="h-5 w-5 text-purple-400" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">AI Executive Summary</h3>
            <div className="flex items-center text-xs text-gray-400 mt-0.5">
              <Calendar className="h-3 w-3 mr-1" />
              <span>Generated {generatedAt ? new Date(generatedAt).toLocaleDateString() : 'Today'}</span>
            </div>
          </div>
        </div>
        
        <Button 
          variant="glass" 
          size="sm" 
          onClick={handleRefresh}
          disabled={refreshing}
          className="hover:bg-purple-500/10"
        >
          <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
          {refreshing ? 'Analyzing...' : 'Refresh'}
        </Button>
      </CardHeader>

      <CardContent className="pt-6">
        {error ? (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <AlertCircle className="h-12 w-12 text-red-400 mb-2 opacity-50" />
            <p className="text-red-400 font-medium">{error}</p>
            <Button variant="ghost" onClick={handleRefresh} className="mt-2">Try again</Button>
          </div>
        ) : (
          <div className="space-y-6">
            <p className="text-sm text-gray-400 italic">
              "Based on the latest data patterns, here are your strategic insights for the current week."
            </p>
            
            {formatInsights(insights)}
            
            <div className="pt-4 mt-2 border-t border-gray-700/50 flex justify-between items-center text-[10px] text-gray-500 uppercase tracking-widest font-bold">
              <span>Smart Attendance AI Engine</span>
              <span className="flex items-center text-purple-400/70">
                <Sparkles className="h-3 w-3 mr-1" />
                Live Analysis Active
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default WeeklyAIInsights;
