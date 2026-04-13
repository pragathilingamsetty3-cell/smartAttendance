'use client';

import React, { useState, useEffect } from 'react';
import { 
  FileText, 
  Download, 
  CheckCircle2, 
  AlertCircle, 
  Filter,
  Calendar,
  Users,
  Settings,
  ChevronRight
} from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { safeParseInt } from '@/utils/numberUtils';
import { Loading } from '@/components/ui/Loading';
import { userManagementService } from '@/services/userManagement.service';
import { DropdownDTO } from '@/types/user-management';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/utils/cn';
import { useAuthStore } from '@/stores/authStore';
import { Role } from '@/types';

export const AttendanceReport: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  
  // Selection State
  const [selectedDept, setSelectedDept] = useState('');
  const [selectedSection, setSelectedSection] = useState('');
  const [reportMode, setReportMode] = useState<'COMPLETE' | 'THRESHOLD' | 'STUDENT_LIST'>('COMPLETE');
  const [threshold, setThreshold] = useState(75);
  
  // Date State
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  
  // UI State
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  const { user } = useAuthStore();
  const isSuperAdmin = user?.role === Role.SUPER_ADMIN;
  const isAdmin = user?.role === Role.ADMIN;
  const isFaculty = user?.role === Role.FACULTY;

  // Initial load: Departments
  useEffect(() => {
    const fetchDepts = async () => {
      try {
        const depts = await userManagementService.getDepartments();
        setDepartments(depts);
        
        // 🔐 DLAC LOCKDOWN for Faculty and Department Admins
        if (isFaculty || isAdmin || isSuperAdmin) {
          // Priority 1: If there's only one department (Backend-filtered), select it
          if (depts.length === 1) {
            setSelectedDept(depts[0].id);
          } 
          // Priority 2: Try to match user.department (ID or Name) in the list
          else if (user?.department) {
            const matchingDept = depts.find(d => 
              d.id === user.department || 
              d.label.toLowerCase() === user.department.toLowerCase() ||
              d.code?.toLowerCase() === user.department.toLowerCase()
            );
            if (matchingDept) {
              setSelectedDept(matchingDept.id);
            }
          }
        }
      } catch (error) {
        console.error('Failed to fetch departments:', error);
      }
    };
    fetchDepts();
  }, [isAdmin, isFaculty, user]);

  // Cascading Sections
  useEffect(() => {
    if (selectedDept) {
      userManagementService.getSections(selectedDept).then(setSections);
    } else {
      setSections([]);
    }
    setSelectedSection('');
  }, [selectedDept]);

  const handleDownload = async () => {
    if (!selectedSection) return;

    setLoading(true);
    setMessage(null);
    try {
      if (reportMode === 'STUDENT_LIST') {
        await userManagementService.downloadStudentList(selectedSection);
      } else {
        await userManagementService.downloadSectionReport(
          selectedSection,
          reportMode === 'THRESHOLD' ? threshold : undefined,
          startDate || undefined,
          endDate || undefined
        );
      }
      
      setMessage({ 
        type: 'success', 
        text: 'Report generation started. Your download will begin shortly.' 
      });
    } catch (error: any) {
      setMessage({ 
        type: 'error', 
        text: error.message || 'Failed to generate report. Please try again.' 
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 p-6">
      {/* Header section */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight flex items-center gap-3">
            <div className="p-2.5 bg-emerald-600/20 rounded-xl border border-emerald-500/30">
              <FileText className="h-7 w-7 text-emerald-400" />
            </div>
            Attendance Report Architect
          </h1>
          <p className="text-gray-400 mt-2 text-sm uppercase tracking-[0.15em] font-medium opacity-70">
            Generate and export academic compliance reports
          </p>
        </div>
      </div>

      {message && (
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className={cn(
            "p-4 rounded-xl border flex items-center gap-3 mb-6",
            message.type === 'success' 
              ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400" 
              : "bg-red-500/10 border-red-500/30 text-red-400"
          )}
        >
          {message.type === 'success' ? <CheckCircle2 className="h-5 w-5" /> : <AlertCircle className="h-5 w-5" />}
          <span className="font-medium text-sm">{message.text}</span>
        </motion.div>
      )}

      <div className="grid grid-cols-1 gap-8">
        <Card className="overflow-hidden border-white/5 bg-[#0F0F16]/40 backdrop-blur-xl">
          <CardHeader className="bg-white/[0.02] border-b border-white/5 py-6">
            <div className="flex items-center gap-3">
              <div className="h-8 w-8 rounded-full bg-emerald-600/20 flex items-center justify-center text-emerald-400 font-bold border border-emerald-500/30">
                <Filter className="h-4 w-4" />
              </div>
              <h3 className="text-lg font-semibold text-white">Report Constraints</h3>
            </div>
          </CardHeader>
          <CardContent className="p-8 space-y-8">
            {/* Selection Logic */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 text-left">
              <div className="space-y-4">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest px-1">Department</label>
                <div className="relative group">
                  <select 
                    value={selectedDept}
                    onChange={(e) => setSelectedDept(e.target.value)}
                    disabled={isFaculty || (isAdmin && !!selectedDept)}
                    className="w-full bg-[#05050A] border border-white/10 rounded-xl px-4 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/40 transition-all appearance-none cursor-pointer disabled:opacity-60"
                  >
                    <option value="">Select Department</option>
                    {departments.map(d => <option key={d.id} value={d.id}>{d.label}</option>)}
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none opacity-40">
                    <Filter className="h-4 w-4" />
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest px-1">Section</label>
                <div className="relative group">
                  <select 
                    value={selectedSection}
                    onChange={(e) => setSelectedSection(e.target.value)}
                    disabled={!selectedDept}
                    className="w-full bg-[#05050A] border border-white/10 rounded-xl px-4 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/40 transition-all appearance-none cursor-pointer disabled:opacity-40"
                  >
                    <option value="">Select Section</option>
                    {sections.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none opacity-40">
                    <Users className="h-4 w-4" />
                  </div>
                </div>
              </div>
            </div>

            {/* Mode Toggle */}
            <div className="space-y-4 text-left">
              <label className="text-xs font-bold text-gray-500 uppercase tracking-widest px-1">Report Mode</label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  onClick={() => setReportMode('COMPLETE')}
                  className={cn(
                    "p-4 rounded-xl border transition-all text-left group",
                    reportMode === 'COMPLETE' 
                      ? "bg-emerald-600/10 border-emerald-500/40 shadow-[0_0_15px_rgba(16,185,129,0.1)]" 
                      : "bg-white/[0.02] border-white/5 hover:border-white/20"
                  )}
                >
                  <div className="flex items-center gap-3 mb-2">
                    <div className={cn("p-1.5 rounded-lg", reportMode === 'COMPLETE' ? "bg-emerald-600 text-white" : "bg-white/5 text-gray-500")}>
                      <Users className="h-4 w-4" />
                    </div>
                    <span className={cn("text-sm font-bold", reportMode === 'COMPLETE' ? "text-white" : "text-gray-400")}>Complete Section</span>
                  </div>
                  <p className="text-[10px] text-gray-500 leading-relaxed px-1">Generate attendance data for every student in the selected section.</p>
                </button>

                <button
                  onClick={() => setReportMode('THRESHOLD')}
                  className={cn(
                    "p-4 rounded-xl border transition-all text-left group",
                    reportMode === 'THRESHOLD' 
                      ? "bg-violet-600/10 border-violet-500/40 shadow-[0_0_15px_rgba(124,58,237,0.1)]" 
                      : "bg-white/[0.02] border-white/5 hover:border-white/20"
                  )}
                >
                  <div className="flex items-center gap-3 mb-2">
                    <div className={cn("p-1.5 rounded-lg", reportMode === 'THRESHOLD' ? "bg-violet-600 text-white" : "bg-white/5 text-gray-500")}>
                      <Settings className="h-4 w-4" />
                    </div>
                    <span className={cn("text-sm font-bold", reportMode === 'THRESHOLD' ? "text-white" : "text-gray-400")}>Below Threshold</span>
                  </div>
                  <p className="text-[10px] text-gray-500 leading-relaxed px-1">Identifies students falling below a specified attendance percentage.</p>
                </button>

                <button
                  onClick={() => setReportMode('STUDENT_LIST')}
                  className={cn(
                    "p-4 rounded-xl border transition-all text-left group col-span-2",
                    reportMode === 'STUDENT_LIST' 
                      ? "bg-amber-600/10 border-amber-500/40 shadow-[0_0_15px_rgba(245,158,11,0.1)]" 
                      : "bg-white/[0.02] border-white/5 hover:border-white/20"
                  )}
                >
                  <div className="flex items-center gap-3 mb-2">
                    <div className={cn("p-1.5 rounded-lg", reportMode === 'STUDENT_LIST' ? "bg-amber-600 text-white" : "bg-white/5 text-gray-500")}>
                      <FileText className="h-4 w-4" />
                    </div>
                    <span className={cn("text-sm font-bold", reportMode === 'STUDENT_LIST' ? "text-white" : "text-gray-400")}>Student Identification List</span>
                  </div>
                  <p className="text-[10px] text-gray-500 leading-relaxed px-1">Export a comprehensive list of all active students in the selected section (Names, Reg Nos, Emails).</p>
                </button>
              </div>
            </div>

            <AnimatePresence mode="wait">
              {reportMode === 'THRESHOLD' && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-4 text-left overflow-hidden"
                >
                  <div className="p-6 rounded-2xl bg-violet-600/5 border border-violet-500/20">
                    <label className="text-xs font-bold text-violet-400 uppercase tracking-widest block mb-4">Set Attendance Range (%)</label>
                    <div className="flex items-center gap-6">
                      <input 
                        type="range" 
                        min="0" 
                        max="100" 
                        value={threshold} 
                        onChange={(e) => setThreshold(safeParseInt(e.target.value, 75))}
                        className="flex-1 accent-violet-500 h-1.5 bg-white/10 rounded-lg appearance-none cursor-pointer"
                      />
                      <div className="w-16 h-10 bg-[#05050A] border border-violet-500/30 rounded-lg flex items-center justify-center font-bold text-violet-400">
                        {threshold}%
                      </div>
                    </div>
                    <p className="text-[10px] text-violet-500/70 mt-3 italic">Listing students with cumulative attendance less than or equal to {threshold}%</p>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Date Range */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 text-left">
              <div className="space-y-4">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest px-1">Start Date</label>
                <div className="relative">
                  <input 
                    type="date" 
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full bg-[#05050A] border border-white/10 rounded-xl px-4 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/40 transition-all cursor-pointer"
                  />
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none opacity-40">
                    <Calendar className="h-4 w-4" />
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-widest px-1">End Date</label>
                <div className="relative">
                  <input 
                    type="date" 
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    className="w-full bg-[#05050A] border border-white/10 rounded-xl px-4 py-3.5 text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/40 transition-all cursor-pointer"
                  />
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none opacity-40">
                    <Calendar className="h-4 w-4" />
                  </div>
                </div>
              </div>
            </div>

            {/* CTA */}
            <div className="pt-6">
              <Button 
                variant="primary" 
                className={cn(
                  "w-full h-14 group relative overflow-hidden transition-all",
                  reportMode === 'THRESHOLD' ? "bg-violet-600 hover:bg-violet-500 border-violet-400" : 
                  reportMode === 'STUDENT_LIST' ? "bg-amber-600 hover:bg-amber-500 border-amber-400" :
                  "bg-emerald-600 hover:bg-emerald-500 border-emerald-400"
                )}
                disabled={!selectedSection || loading}
                loading={loading}
                onClick={handleDownload}
              >
                <div className="flex items-center justify-center gap-3">
                  <Download className="h-5 w-5 group-hover:-translate-y-1 transition-transform" />
                  <span className="text-base font-bold tracking-tight">
                    Download {reportMode === 'COMPLETE' ? 'Complete' : reportMode === 'THRESHOLD' ? 'Range Filtered' : 'Student List'} Excel Report
                  </span>
                  <ChevronRight className="h-5 w-5 group-hover:translate-x-1 transition-transform opacity-50" />
                </div>
              </Button>
              <p className="text-[10px] text-center text-gray-600 mt-4 uppercase tracking-[0.1em] font-medium opacity-50">
                Data generated in .xlsx format • Real-time database snapshot
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
