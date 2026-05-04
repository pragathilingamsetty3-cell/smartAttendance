'use client';

import React, { useState, useEffect } from 'react';
import { 
  Users, 
  ArrowUp, 
  CheckCircle2, 
  AlertCircle, 
  ChevronRight,
  Search,
  Filter,
  CheckSquare,
  Square
} from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { userManagementService } from '@/services/userManagement.service';
import { DropdownDTO, BulkPromotionRequest } from '@/types/user-management';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/utils/cn';
import { useAuthStore } from '@/stores/authStore';
import { Role } from '@/types';

export const StudentPromotion: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  
  // Source State
  const [sourceDept, setSourceDept] = useState('');
  const [sourceSections, setSourceSections] = useState<DropdownDTO[]>([]);
  const [sourceSection, setSourceSection] = useState('');
  const [students, setStudents] = useState<any[]>([]);
  const [selectedStudentIds, setSelectedStudentIds] = useState<string[]>([]);
  
  // Target State
  const [targetDept, setTargetDept] = useState('');
  const [targetSections, setTargetSections] = useState<DropdownDTO[]>([]);
  const [targetSection, setTargetSection] = useState('');
  const [autoIncrement, setAutoIncrement] = useState(true);
  
  // UI State
  const [step, setStep] = useState(1);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  const { user } = useAuthStore();
  const isAdmin = user?.role === Role.ADMIN;

  // Initial load: Departments
  useEffect(() => {
    const fetchDepts = async () => {
      try {
        const depts = await userManagementService.getDepartments();
        setDepartments(depts);
        
        // 🔐 AGGRESSIVE AUTO-SELECTION for DLAC Admins
        if (isAdmin) {
          // Priority 1: If there's only one department (Backend-filtered), select it
          if (depts.length === 1) {
            setSourceDept(depts[0].id);
            setTargetDept(depts[0].id);
          } 
          // Priority 2: Try to match user.department (ID or Name) in the list
          else if (user?.department) {
            const matchingDept = depts.find(d => 
              d.id === user.department || 
              d.label.toLowerCase() === user.department.toLowerCase() ||
              d.code?.toLowerCase() === user.department.toLowerCase()
            );
            if (matchingDept) {
              setSourceDept(matchingDept.id);
              setTargetDept(matchingDept.id);
            }
          }
        }
      } catch (error) {
        console.error('Failed to fetch departments:', error);
      }
    };
    fetchDepts();
  }, [isAdmin, user]);

  // Cascading Sections for Source
  useEffect(() => {
    if (sourceDept) {
      userManagementService.getSections(sourceDept).then(setSourceSections);
    } else {
      setSourceSections([]);
    }
    setSourceSection('');
  }, [sourceDept]);

  // Cascading Sections for Target
  useEffect(() => {
    if (targetDept) {
      userManagementService.getSections(targetDept).then(setTargetSections);
    } else {
      setTargetSections([]);
    }
    setTargetSection('');
  }, [targetDept]);

  // Fetch Students when Source Section changes
  useEffect(() => {
    if (sourceSection) {
      setLoading(true);
      userManagementService.getSectionStudents(sourceSection)
        .then(data => {
          setStudents(data);
          setSelectedStudentIds([]);
        })
        .finally(() => setLoading(false));
    } else {
      setStudents([]);
    }
  }, [sourceSection]);

  const handleSelectAll = () => {
    if (selectedStudentIds.length === students.length) {
      setSelectedStudentIds([]);
    } else {
      setSelectedStudentIds(students.map(s => s.id));
    }
  };

  const handleToggleStudent = (id: string) => {
    setSelectedStudentIds(prev => 
      prev.includes(id) ? prev.filter(sid => sid !== id) : [...prev, id]
    );
  };

  const handlePromote = async () => {
    if (!targetSection || selectedStudentIds.length === 0) return;

    setLoading(true);
    setMessage(null);
    try {
      const request: BulkPromotionRequest = {
        studentIds: selectedStudentIds,
        targetSectionId: targetSection,
        autoIncrementSemester: autoIncrement
      };
      
      await userManagementService.bulkPromoteStudents(request);
      
      setMessage({ 
        type: 'success', 
        text: `Successfully promoted ${selectedStudentIds.length} students!` 
      });
      
      // Reset after success
      setStep(1);
      setSourceSection('');
      setSelectedStudentIds([]);
      setTargetSection('');
    } catch (error: any) {
      setMessage({ 
        type: 'error', 
        text: error.message || 'Promotion failed. Please try again.' 
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-8 p-6">
      {/* Header section with Sky Blue Glow */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 tracking-tight flex items-center gap-3">
            <div className="p-2.5 bg-primary/10 rounded-xl border border-primary/20">
              <ArrowUp className="h-7 w-7 text-primary" />
            </div>
            Student Promotion Architect
          </h1>
          <p className="text-slate-500 mt-2 text-sm uppercase tracking-[0.15em] font-medium opacity-80">
            Bulk movement and academic advancement system
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
              ? "bg-emerald-500/10 border-emerald-500/20 text-emerald-600" 
              : "bg-red-500/10 border-red-500/20 text-red-600"
          )}
        >
          {message.type === 'success' ? <CheckCircle2 className="h-5 w-5" /> : <AlertCircle className="h-5 w-5" />}
          <span className="font-medium">{message.text}</span>
        </motion.div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Step 1: Source Identification */}
        <div className="lg:col-span-12">
          <Card className="overflow-hidden border-slate-200 bg-white shadow-xl shadow-sky-900/5">
             <CardHeader className="bg-slate-50/50 border-b border-slate-100 py-6">
               <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold border border-primary/20">1</div>
                  <h3 className="text-lg font-semibold text-slate-900">Identify Students to Promote</h3>
               </div>
             </CardHeader>
             <CardContent className="p-8">
               <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                  <div className="space-y-4">
                    <label className="text-xs font-bold text-slate-400 uppercase tracking-widest px-1">Source Department</label>
                    <div className="relative group">
                      <select 
                        value={sourceDept}
                        onChange={(e) => setSourceDept(e.target.value)}
                        disabled={isAdmin}
                        className="w-full bg-white border border-slate-200 rounded-xl px-4 py-3.5 text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all appearance-none cursor-pointer group-hover:border-slate-300 disabled:opacity-60"
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
                    <label className="text-xs font-bold text-slate-400 uppercase tracking-widest px-1">Source Section</label>
                    <div className="relative group">
                      <select 
                        value={sourceSection}
                        onChange={(e) => setSourceSection(e.target.value)}
                        disabled={!sourceDept}
                        className="w-full bg-white border border-slate-200 rounded-xl px-4 py-3.5 text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all appearance-none cursor-pointer disabled:opacity-40 group-hover:border-slate-300"
                      >
                        <option value="">Select Section</option>
                        {sourceSections.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}
                      </select>
                      <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none opacity-40">
                        <Users className="h-4 w-4" />
                      </div>
                    </div>
                  </div>
               </div>

               {loading ? (
                 <div className="py-12 flex justify-center"><Loading /></div>
               ) : students.length > 0 ? (
                 <motion.div 
                    initial={{ opacity: 0, y: 10 }} 
                    animate={{ opacity: 1, y: 0 }}
                    className="space-y-6"
                 >
                   <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                      <h4 className="text-slate-900 font-semibold flex items-center gap-2">
                        Class Registry 
                        <span className="text-xs bg-slate-100 px-2 py-1 rounded-full text-slate-500 font-normal">
                          {students.length} Total
                        </span>
                      </h4>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        onClick={handleSelectAll}
                        className="h-8 text-xs font-bold text-primary hover:text-primary-hover"
                      >
                         {selectedStudentIds.length === students.length ? 'Deselect All' : 'Select All students'}
                      </Button>
                   </div>
                   
                   <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                      {students.map((student) => (
                        <div 
                          key={student.id}
                          onClick={() => handleToggleStudent(student.id)}
                          className={cn(
                            "group cursor-pointer p-3 rounded-xl border transition-all flex items-center justify-between",
                            selectedStudentIds.includes(student.id) 
                              ? "bg-primary/5 border-primary/30 shadow-[0_0_15px_rgba(14,165,233,0.1)]" 
                              : "bg-slate-50/50 border-slate-100 hover:border-slate-200"
                          )}
                        >
                          <div className="flex items-center gap-3 overflow-hidden">
                            <div className={cn(
                              "h-8 w-8 rounded-lg flex items-center justify-center font-bold text-xs uppercase",
                              selectedStudentIds.includes(student.id) ? "bg-primary text-white" : "bg-slate-200 text-slate-500"
                            )}>
                              {student.name.charAt(0)}
                            </div>
                            <div className="overflow-hidden">
                              <p className="text-xs font-bold text-slate-800 truncate">{student.name}</p>
                              <p className="text-[10px] text-slate-500 truncate font-mono tracking-tighter opacity-70 group-hover:opacity-100">{student.registrationNumber}</p>
                            </div>
                          </div>
                          {selectedStudentIds.includes(student.id) ? (
                            <CheckSquare className="h-4 w-4 text-primary flex-shrink-0" />
                          ) : (
                            <Square className="h-4 w-4 text-slate-300 flex-shrink-0" />
                          )}
                        </div>
                      ))}
                   </div>
                 </motion.div>
               ) : sourceSection && !loading ? (
                 <div className="py-12 text-center text-slate-500 border-2 border-dashed border-slate-100 rounded-2xl">
                    No students found in this section.
                 </div>
               ) : (
                 <div className="py-12 text-center text-slate-400 italic">
                    Select a section to load students registry
                 </div>
               )}
             </CardContent>
          </Card>
        </div>

        {/* Step 2: Destination & Execution (Only if students selected) */}
        <AnimatePresence>
          {selectedStudentIds.length > 0 && (
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="lg:col-span-12"
            >
               <Card className="overflow-hidden border-primary/20 bg-white shadow-2xl shadow-sky-900/10 ring-1 ring-primary/5">
                 <CardHeader className="bg-primary/5 border-b border-slate-100 py-6">
                    <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-full bg-primary flex items-center justify-center text-white font-bold shadow-[0_0_15px_rgba(14,165,233,0.4)]">2</div>
                        <h3 className="text-lg font-semibold text-slate-900">Target Destination & Advance</h3>
                    </div>
                 </CardHeader>
                 <CardContent className="p-8">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                      {/* Target Selectors */}
                      <div className="md:col-span-2 space-y-8">
                         <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="space-y-4">
                              <label className="text-xs font-bold text-slate-400 uppercase tracking-widest px-1">Target Department</label>
                              <select 
                                value={targetDept}
                                onChange={(e) => setTargetDept(e.target.value)}
                                disabled={isAdmin}
                                className="w-full bg-white border border-slate-200 rounded-xl px-4 py-3.5 text-slate-900 focus:outline-none focus:border-primary/50 transition-all font-medium disabled:opacity-60"
                              >
                                <option value="">Select Destination Dept</option>
                                {departments.map(d => <option key={d.id} value={d.id}>{d.label}</option>)}
                              </select>
                            </div>

                            <div className="space-y-4">
                              <label className="text-xs font-bold text-slate-400 uppercase tracking-widest px-1">Target Section</label>
                              <select 
                                value={targetSection}
                                onChange={(e) => setTargetSection(e.target.value)}
                                disabled={!targetDept}
                                className="w-full bg-white border border-slate-200 rounded-xl px-4 py-3.5 text-slate-900 focus:outline-none focus:border-primary/50 transition-all font-medium disabled:opacity-40"
                              >
                                <option value="">Select Destination Section</option>
                                {targetSections.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}
                              </select>
                            </div>
                         </div>

                         {/* Options toggle */}
                         <div className="flex items-center gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                            <div className="p-2 bg-primary/10 rounded-lg">
                               <ArrowUp className="h-4 w-4 text-primary" />
                            </div>
                            <div className="flex-1">
                               <p className="text-xs font-bold text-slate-800 uppercase tracking-wider">Auto-increment Semester</p>
                               <p className="text-[10px] text-slate-500 mt-0.5">Automatically advance students to the next semester in system database</p>
                            </div>
                            <button 
                              onClick={() => setAutoIncrement(!autoIncrement)}
                              className={cn(
                                "w-12 h-6 rounded-full p-1 transition-colors duration-300 relative",
                                autoIncrement ? "bg-primary" : "bg-slate-300"
                              )}
                            >
                              <div className={cn(
                                "h-4 w-4 rounded-full bg-white transition-transform duration-300",
                                autoIncrement ? "translate-x-6" : "translate-x-0"
                              )} />
                            </button>
                         </div>
                      </div>

                      {/* Summary & Global Action */}
                      <div className="lg:col-span-1 border-slate-100 lg:border-l lg:pl-8 space-y-6">
                         <div className="space-y-4">
                            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-widest">Operation Summary</h4>
                            <div className="space-y-3">
                               <div className="flex justify-between items-center text-xs">
                                  <span className="text-slate-500">Subject Volume:</span>
                                  <span className="text-slate-900 font-bold">{selectedStudentIds.length} Students</span>
                               </div>
                               <div className="flex justify-between items-center text-xs">
                                  <span className="text-slate-500">Destination:</span>
                                  <span className="text-primary font-bold truncate max-w-[120px]">
                                    {targetSections.find(s => s.id === targetSection)?.label || 'Not Selected'}
                                  </span>
                               </div>
                               <div className="flex justify-between items-center text-xs">
                                  <span className="text-slate-500">Semester Logic:</span>
                                  <span className="text-slate-900 font-bold">{autoIncrement ? '+1 Advancement' : 'Manual Update'}</span>
                               </div>
                            </div>
                         </div>

                         <div className="pt-4 border-t border-slate-100">
                            <Button 
                              variant="primary" 
                              className="w-full h-14 group"
                              disabled={!targetSection || selectedStudentIds.length === 0 || loading}
                              loading={loading}
                              onClick={handlePromote}
                            >
                              <div className="flex items-center justify-center gap-3">
                                 <span className="text-base font-bold tracking-tight">Execute Promotion</span>
                                 <ChevronRight className="h-5 w-5 group-hover:translate-x-1 transition-transform" />
                              </div>
                            </Button>
                            <p className="text-[9px] text-center text-slate-400 mt-4 uppercase tracking-[0.1em] font-medium">
                              Secure transaction verified • Administrator credentials required
                            </p>
                         </div>
                      </div>
                    </div>
                 </CardContent>
               </Card>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 5px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: rgba(14, 165, 233, 0.02);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(14, 165, 233, 0.1);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(14, 165, 233, 0.3);
        }
      `}</style>
    </div>
  );
};
