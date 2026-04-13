'use client';

import React, { useState, useEffect } from 'react';
import { X, History, Calendar, Clock, MapPin, CheckCircle2, AlertCircle, Eye } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import userManagementService from '@/services/userManagement.service';
import { UserActivity } from '@/types/user-management';

interface ActivityLogModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
  userName: string;
}

export const ActivityLogModal: React.FC<ActivityLogModalProps> = ({ isOpen, onClose, userId, userName }) => {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      fetchActivity();
    }
  }, [isOpen, userId]);

  const fetchActivity = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await userManagementService.getUserActivity(userId);
      setActivities(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load activity logs');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }} 
          animate={{ opacity: 1 }} 
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm" 
        />
        
        <motion.div
          initial={{ opacity: 0, scale: 0.95, x: 20 }}
          animate={{ opacity: 1, scale: 1, x: 0 }}
          exit={{ opacity: 0, scale: 0.95, x: 20 }}
          className="relative w-full max-w-2xl bg-[#0F0F16] border border-white/10 rounded-3xl overflow-hidden shadow-2xl h-[80vh] flex flex-col"
        >
          {/* Header */}
          <div className="px-8 py-6 border-b border-white/5 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-2xl bg-secondary/10 text-secondary border border-secondary/20 shadow-inner">
                <History size={24} />
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white tracking-tight">Activity Log</h2>
                <p className="text-slate-500 text-sm mt-0.5">Historical verification timeline for {userName}</p>
              </div>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-white/5 rounded-xl transition-colors text-slate-400 hover:text-white"
            >
              <X size={24} />
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
            {loading ? (
              <div className="h-full flex items-center justify-center">
                <Loading size="lg" text="Retrieving identity history..." />
              </div>
            ) : error ? (
              <div className="text-center py-20">
                <AlertCircle className="mx-auto text-accent mb-4" size={48} />
                <p className="text-white font-bold">{error}</p>
                <Button variant="secondary" className="mt-6" onClick={fetchActivity}>
                  Retry Fetch
                </Button>
              </div>
            ) : activities.length === 0 ? (
              <div className="text-center py-20 text-slate-500">
                <History className="mx-auto mb-4 opacity-20" size={64} />
                <p className="text-lg">No historical records found for this identity.</p>
              </div>
            ) : (
              <div className="relative space-y-8 before:absolute before:inset-0 before:ml-5 before:-translate-x-px before:h-full before:w-0.5 before:bg-gradient-to-b before:from-primary/50 before:via-white/5 before:to-transparent">
                {activities.map((activity, idx) => (
                  <motion.div 
                    key={activity.id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: idx * 0.05 }}
                    className="relative pl-12"
                  >
                    {/* Timeline Dot */}
                    <div className={`absolute left-0 top-1.5 h-10 w-10 rounded-xl border-4 border-[#0F0F16] flex items-center justify-center z-10 shadow-lg ${
                      activity.action === 'PRESENT' ? 'bg-emerald-500 text-white' :
                      activity.action === 'WALK_OUT' ? 'bg-accent text-white' :
                      'bg-primary text-white'
                    }`}>
                      {activity.type === 'ATTENDANCE' ? <CheckCircle2 size={16} /> : <Clock size={16} />}
                    </div>

                    <div className="glass-panel p-6 hover:bg-white/[0.04] transition-all group">
                      <div className="flex flex-col sm:flex-row justify-between items-start gap-4 mb-4">
                        <div>
                          <div className="flex items-center gap-3 mb-1">
                            <span className="text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded bg-white/5 text-slate-400 border border-white/5">
                              {activity.type}
                            </span>
                            <span className={`text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded border border-current shadow-sm ${
                              activity.action === 'PRESENT' ? 'bg-emerald-500/10 text-emerald-400' :
                              activity.action === 'WALK_OUT' ? 'bg-accent/10 text-accent' :
                              'bg-primary/10 text-primary'
                            }`}>
                              {activity.action}
                            </span>
                          </div>
                          <h4 className="font-bold text-white group-hover:text-primary transition-colors">{activity.description}</h4>
                        </div>
                        <div className="flex flex-col items-end text-[11px] text-slate-500 font-medium">
                          <div className="flex items-center gap-1.5 ring-1 ring-white/5 px-2 py-1 rounded bg-black/20">
                            <Calendar size={12} className="text-secondary" /> {new Date(activity.timestamp).toLocaleDateString()}
                          </div>
                          <div className="flex items-center gap-1.5 mt-1">
                            <Clock size={12} /> {new Date(activity.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      </div>

                      {activity.metadata && (
                        <div className="flex items-center gap-4 text-xs p-3 rounded-xl bg-black/40 border border-white/5">
                          <div className="flex items-center gap-2 text-slate-400">
                             <MapPin size={14} className="text-primary" />
                             <span>Session: <span className="text-slate-200 font-bold">{activity.metadata}</span></span>
                          </div>
                        </div>
                      )}
                    </div>
                  </motion.div>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="p-6 border-t border-white/5 bg-black/20 flex justify-end">
            <Button variant="secondary" onClick={onClose}>
              Close History
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
