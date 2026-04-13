'use client';

import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { FileText, Download, Shield, AlertCircle, ChevronDown, Archive, Layers } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';
import { SectionReportActions } from '@/components/attendance/SectionReportActions';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { attendanceService } from '@/services/attendance.service';
import { Role } from '@/types';

const containerVars = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVars = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 }
};

export default function AttendanceReportsPage() {
  const { user } = useAuthStore();
  const [sections, setSections] = useState<{ id: string; name: string }[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [bulkDownloading, setBulkDownloading] = useState(false);

  const isAdmin = user?.role === Role.SUPER_ADMIN || user?.role === Role.ADMIN;

  useEffect(() => {
    const fetchSections = async () => {
      try {
        const data = await attendanceService.getFacultySections();
        setSections(data);
        if (data.length > 0) {
          setSelectedSectionId(data[0].id);
        }
      } catch (error) {
        console.error('Failed to load sections:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchSections();
  }, []);

  const handleBulkDownload = async () => {
    setBulkDownloading(true);
    try {
      // Logic for bulk ZIP download
      await attendanceService.downloadBulkReport();
    } catch (error) {
      console.error('Failed to download bulk report:', error);
    } finally {
      setBulkDownloading(false);
    }
  };

  return (
    <div className="p-8 space-y-8">
      <motion.div 
        variants={containerVars} 
        initial="hidden" 
        animate="show"
        className="max-w-5xl mx-auto space-y-8"
      >
        {/* Header Section */}
        <motion.div variants={itemVars} className="flex flex-col gap-2">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-secondary/20 text-secondary rounded-lg">
              <FileText size={24} />
            </div>
            <h1 className="text-3xl font-bold text-white tracking-tight">Attendance Reports</h1>
          </div>
          <p className="text-slate-400">Generate and download comprehensive attendance registry and student data.</p>
        </motion.div>

        {/* 🏢 ADMINISTRATIVE BULK CONTROLS */}
        {isAdmin && (
          <motion.div variants={itemVars}>
            <Card glass className="p-6 border-secondary/20 bg-secondary/5 space-y-4">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-secondary/20 text-secondary rounded-2xl shadow-lg shadow-secondary/10">
                    <Archive size={24} />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-white">Institutional Bulk Reporting</h2>
                    <p className="text-sm text-slate-400">
                      Generate a comprehensive ZIP archive containing summaries and section-wise reports 
                      for {user?.role === Role.SUPER_ADMIN ? 'all departments' : 'your department'}.
                    </p>
                  </div>
                </div>
                
                <Button 
                  variant="secondary"
                  onClick={handleBulkDownload}
                  loading={bulkDownloading}
                  className="bg-secondary text-obsidian-900 font-bold px-8 shadow-xl shadow-secondary/20 hover:scale-105 active:scale-95 transition-all"
                >
                  <Download className="mr-2" size={18} />
                  Download Bulk ZIP Report
                </Button>
              </div>
            </Card>
          </motion.div>
        )}

        {/* Section Selector */}
        {!loading && sections.length > 0 && (
          <motion.div variants={itemVars} className="flex items-center gap-4 bg-obsidian-800/50 p-4 rounded-2xl border border-white/5">
            <label className="text-sm font-bold text-slate-500 uppercase tracking-widest pl-2">Select Class / Section</label>
            <div className="relative">
              <select 
                value={selectedSectionId}
                onChange={(e) => setSelectedSectionId(e.target.value)}
                className="bg-obsidian-900 text-white text-sm font-bold px-4 py-2 rounded-xl border border-white/10 appearance-none min-w-[200px] cursor-pointer focus:outline-none focus:border-secondary transition-colors"
              >
                {sections.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-2.5 text-slate-500 pointer-events-none" size={16} />
            </div>
          </motion.div>
        )}

        {/* Reports Content */}
        {!loading && selectedSectionId ? (
          <motion.div variants={itemVars}>
            <SectionReportActions sectionId={selectedSectionId} />
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-8">
              <Card glass className="p-6 border-white/5 space-y-4">
                <div className="flex items-center gap-3 text-secondary">
                  <Download size={20} />
                  <h3 className="font-bold text-white">Master Attendance Report</h3>
                </div>
                <p className="text-sm text-slate-400">
                  This report includes total classes held, individual student presence, 
                  percentage-based calculations, and flags students below your selected threshold.
                </p>
              </Card>

              <Card glass className="p-6 border-white/5 space-y-4">
                <div className="flex items-center gap-3 text-primary">
                  <Shield size={20} />
                  <h3 className="font-bold text-white">Student Registry Export</h3>
                </div>
                <p className="text-sm text-slate-400">
                  A complete export of all students currently enrolled in your section, 
                  including registration numbers and contact details for administrative use.
                </p>
              </Card>
            </div>
          </motion.div>
        ) : !loading && (
          <motion.div variants={itemVars} className="glass-panel p-12 text-center space-y-4">
            <div className="mx-auto w-16 h-16 bg-amber-500/10 rounded-full flex items-center justify-center text-amber-500 mb-4">
              <AlertCircle size={32} />
            </div>
            <h2 className="text-xl font-bold text-white">No Section Found</h2>
            <p className="text-slate-400 max-w-md mx-auto">
              We couldn't find any sections assigned to your department. Please contact the administrator 
              to ensure your Faculty profile is correctly mapped.
            </p>
          </motion.div>
        )}
      </motion.div>
    </div>
  );
}
