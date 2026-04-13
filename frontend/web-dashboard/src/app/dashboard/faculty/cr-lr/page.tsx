'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Users, 
  UserPlus, 
  UserMinus, 
  Search, 
  ShieldCheck, 
  ShieldAlert,
  ChevronRight,
  Filter,
  CheckCircle2
} from 'lucide-react';
import { roleAssignmentService, CRLRAssignmentRequest } from '@/services/roleAssignment.service';
import { userManagementService } from '@/services/userManagement.service';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { DropdownDTO } from '@/types/user-management';

export default function CRLRAssignments() {
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [selectedDept, setSelectedDept] = useState<string>('');
  const [selectedSection, setSelectedSection] = useState<string>('');
  const [students, setStudents] = useState<any[]>([]);
  const [currentAssignments, setCurrentAssignments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [assigning, setAssigning] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const init = async () => {
      try {
        const depts = await userManagementService.getDepartments();
        setDepartments(depts);
      } catch (err) {
        console.error('Failed to load departments');
      }
    };
    init();
  }, []);

  useEffect(() => {
    if (selectedDept) {
      userManagementService.getSections(selectedDept).then(setSections);
      setSelectedSection('');
      setStudents([]);
    }
  }, [selectedDept]);

  const loadData = async (sectionId: string) => {
    try {
      setLoading(true);
      const [studentList, assignments] = await Promise.all([
        userManagementService.getSectionStudents(sectionId),
        roleAssignmentService.getSectionAssignments(sectionId) as any
      ]);
      setStudents(studentList);
      setCurrentAssignments(assignments.assignments || []);
    } catch (err) {
      alert('Failed to load section data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSection) {
      loadData(selectedSection);
    }
  }, [selectedSection]);

  const handleAssign = async (studentId: string, role: string) => {
    try {
      setAssigning(studentId);
      await roleAssignmentService.assignCRLR({
        studentId,
        sectionId: selectedSection,
        roleType: role
      });
      loadData(selectedSection);
    } catch (err: any) {
      alert(err.message || 'Failed to assign role');
    } finally {
      setAssigning(null);
    }
  };

  const handleRevoke = async (assignmentId: string) => {
    if (!confirm('Are you sure you want to revoke this role?')) return;
    try {
      setLoading(true);
      await roleAssignmentService.revokeCRLR(assignmentId, 'Revoked by faculty coordinator');
      loadData(selectedSection);
    } catch (err: any) {
      alert(err.message || 'Failed to revoke role');
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = students.filter(s => 
    s.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    s.registrationNumber.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="p-8 space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white tracking-tight flex items-center gap-3">
          <ShieldCheck className="text-violet-500" /> CR/LR Governance
        </h1>
        <p className="text-slate-400 mt-1">Appoint and manage Class Representatives (CR) and Lady Representatives (LR)</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="md:col-span-1 space-y-6">
          <Card glass className="p-6">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
              <Filter size={14} className="text-violet-500" /> Control Filters
            </h3>
            <div className="space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase mb-1.5 block">Department</label>
                <select 
                  className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-violet-500/50"
                  value={selectedDept}
                  onChange={(e) => setSelectedDept(e.target.value)}
                >
                  <option value="" className="bg-[#0F0F16]">Select Department</option>
                  {departments.map(d => <option key={d.id} value={d.id} className="bg-[#0F0F16]">{d.label}</option>)}
                </select>
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase mb-1.5 block">Section</label>
                <select 
                  className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-violet-500/50"
                  value={selectedSection}
                  onChange={(e) => setSelectedSection(e.target.value)}
                  disabled={!selectedDept}
                >
                  <option value="" className="bg-[#0F0F16]">Select Section</option>
                  {sections.map(s => <option key={s.id} value={s.id} className="bg-[#0F0F16]">{s.label}</option>)}
                </select>
              </div>
            </div>
          </Card>

          <Card glass className="p-6">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Current Assignments</h3>
            <div className="space-y-3">
              {currentAssignments.length === 0 ? (
                <p className="text-xs text-slate-500 italic text-center py-4">No active appointments</p>
              ) : (
                currentAssignments.map(asgn => (
                  <div key={asgn.assignmentId} className="flex items-center justify-between p-2 rounded-lg bg-white/5 border border-white/5">
                    <div>
                      <p className="text-xs font-bold text-white">{asgn.studentName}</p>
                      <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${asgn.roleType === 'CR' ? 'bg-blue-500/20 text-blue-400' : 'bg-pink-500/20 text-pink-400'}`}>
                        {asgn.roleType}
                      </span>
                    </div>
                    <Button 
                      variant="glass" 
                      size="sm" 
                      className="h-7 w-7 p-0 text-red-500 hover:bg-red-500/10 border-none"
                      onClick={() => handleRevoke(asgn.assignmentId)}
                    >
                      <UserMinus size={12} />
                    </Button>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>

        <div className="md:col-span-3 space-y-6">
          <div className="flex items-center gap-4 bg-white/5 border border-white/10 p-4 rounded-2xl">
            <Search className="text-slate-500" size={20} />
            <input 
              type="text" 
              placeholder="Search students by name or registration ID..."
              className="bg-transparent border-none outline-none text-white w-full text-lg font-medium placeholder:text-slate-600"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <AnimatePresence mode="wait">
            {loading ? (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex justify-center py-20">
                <Loading size="lg" text="Analyzing student registry..." />
              </motion.div>
            ) : !selectedSection ? (
              <div className="py-20 text-center glass-card border-dashed">
                <Users className="mx-auto h-12 w-12 text-slate-700 mb-4" />
                <h3 className="text-xl font-bold text-slate-500">Select a section to begin appointments</h3>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredStudents.map(student => (
                  <motion.div 
                    key={student.id}
                    layout
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className={`glass-card p-4 flex items-center justify-between group hover:border-violet-500/30 transition-all ${
                      currentAssignments.some(a => a.studentId === student.id) ? 'opacity-50 pointer-events-none grayscale' : ''
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-slate-800 flex items-center justify-center font-bold text-white border border-white/5 group-hover:border-violet-500/50">
                        {student.name.charAt(0)}
                      </div>
                      <div>
                        <p className="text-sm font-bold text-white">{student.name}</p>
                        <p className="text-[10px] text-slate-500 font-mono">{student.registrationNumber}</p>
                      </div>
                    </div>
                    
                    <div className="flex gap-2">
                       <Button 
                         variant="glass" 
                         size="sm" 
                         className="text-[10px] font-bold h-8 border-blue-500/20 hover:bg-blue-500/10 text-blue-400"
                         onClick={() => handleAssign(student.id, 'CR')}
                         disabled={!!assigning}
                       >
                         {assigning === student.id ? <RefreshCw className="animate-spin h-3 w-3" /> : 'SET CR'}
                       </Button>
                       <Button 
                         variant="glass" 
                         size="sm" 
                         className="text-[10px] font-bold h-8 border-pink-500/20 hover:bg-pink-500/10 text-pink-400"
                         onClick={() => handleAssign(student.id, 'LR')}
                         disabled={!!assigning}
                       >
                         {assigning === student.id ? <RefreshCw className="animate-spin h-3 w-3" /> : 'SET LR'}
                       </Button>
                    </div>
                  </motion.div>
                ))}
              </div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}

function RefreshCw(props: any) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
      <path d="M21 3v5h-5" />
      <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
      <path d="M3 21v-5h5" />
    </svg>
  )
}
