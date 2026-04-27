'use client';

import React, { useState, useEffect } from 'react';
import { useAuth } from '@/stores/authContext';
import { timetableService } from '@/services/timetable.service';
import { roomManagementService } from '@/services/roomManagement.service';
import { userManagementService } from '@/services/userManagement.service';
import dynamic from 'next/dynamic';
const TimetableGrid = dynamic(
  () => import('@/components/timetable/TimetableGrid').then(mod => mod.TimetableGrid),
  { 
    ssr: false, 
    loading: () => <div className="h-[500px] w-full bg-white/5 animate-pulse rounded-xl flex items-center justify-center text-gray-500 italic">Formatting Timetable Architect...</div>
  }
);
import { ScheduleModal } from '@/components/timetable/ScheduleModal';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { Plus, Calendar, Filter, User, Building, CheckCircle } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { motion, AnimatePresence } from 'framer-motion';

export default function TimetablePage() {
  const { user, getUserRole } = useAuth();
  const userRole = getUserRole();
  const [loading, setLoading] = useState(true);
  const [entries, setEntries] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<any>(null);
  const [showSuccess, setShowSuccess] = useState(false);
  
  // Selection state for filtering
  const [selectedSection, setSelectedSection] = useState('');
  const [selectedFaculty, setSelectedFaculty] = useState('');
  
  // Data for Selects
  const [sections, setSections] = useState<any[]>([]);
  const [facultyList, setFacultyList] = useState<any[]>([]);
  const [rooms, setRooms] = useState<any[]>([]);

  useEffect(() => {
    if (userRole && userRole !== 'STUDENT') {
      fetchInitialData();
    } else if (userRole === 'STUDENT') {
      setLoading(false);
    }
  }, [userRole]);

  useEffect(() => {
    if (user && userRole) {
      // 🎓 FOR STUDENTS: Automatically set their assigned section
      const isStudent = userRole === 'STUDENT' || userRole === 'CR' || userRole === 'LR' || String(userRole).includes('STUDENT');
      
      if (isStudent) {
        // Use type casting to check for variations without crashing the build
        const u = user as any;
        const detectedSectionId = u.sectionId || u.section_id || u.section?.id;
        
        if (detectedSectionId) {
          console.log('🎓 DETECTED STUDENT SECTION:', detectedSectionId);
          setSelectedSection(detectedSectionId);
        } else {
          console.warn('⚠️ WARNING: No Section ID found for student!', user);
        }
      }
      fetchTimetable();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, userRole, selectedSection, selectedFaculty]);

  const fetchInitialData = async () => {
    try {
      // Fetch rooms
      const data = await roomManagementService.getAllRooms();
      // Extract rooms from the structured response and map roomId to id for the modal
      const roomsList = (data.rooms || []).map(r => ({ ...r, id: r.roomId }));
      setRooms(roomsList);

      // Fetch departments to get faculty and sections
      const departments = await userManagementService.getDepartments();
      
      let allSections: any[] = [];
      let allFaculty: any[] = [];

      for (const dept of departments) {
        const [deptSections, deptFaculty] = await Promise.all([
          userManagementService.getSections(dept.id),
          userManagementService.getDepartmentFaculty(dept.id)
        ]);
        allSections = [...allSections, ...deptSections];
        allFaculty = [...allFaculty, ...deptFaculty];
      }

      setSections(allSections);
      setFacultyList(allFaculty);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch initial timetable data', error);
      setLoading(false);
    }
  };

  const fetchTimetable = async () => {
    if (!user) return;
    setLoading(true);
    try {
      let data = [];
      const isFaculty = String(userRole).includes('FACULTY');
      const isStudent = ['STUDENT', 'CR', 'LR'].includes(String(userRole)) || String(userRole).includes('STUDENT');
      
      if (selectedSection) {
        data = await timetableService.getTimetablesForSection(selectedSection);
        if (selectedFaculty) {
          data = data.filter((e: any) => e.faculty?.id === selectedFaculty);
        }
      } else if (selectedFaculty) {
        data = await timetableService.getTimetablesForFaculty(selectedFaculty);
      } else if (isFaculty && user.id) {
        data = await timetableService.getTimetablesForFaculty(user.id);
      } else if (isStudent) {
        // 🎓 FOR STUDENTS/CRs/LRs: Call the dedicated student endpoint.
        // The backend will automatically handle sectionId recovery if it's missing from the token.
        data = await timetableService.getTimetablesForSection(user.sectionId || '');
      } else {
        data = [];
      }
      
      console.log('✅ TIMETABLE DATA RECEIVED:', data?.length || 0, 'entries', data);
      setEntries(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('❌ FAILED TO FETCH TIMETABLE:', error);
      setEntries([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (formData: any) => {
    try {
      if (editingEntry) {
        await timetableService.updateTimetable(editingEntry.id, formData);
      } else {
        await timetableService.createTimetable(formData);
      }
      setIsModalOpen(false);
      setEditingEntry(null);
      // ✨ Trigger Success Animation
      setShowSuccess(true);
      setTimeout(() => {
        setShowSuccess(false);
        fetchTimetable();
      }, 500);
    } catch (error: any) {
      const errorMessage = error.response?.data?.error || error.message || 'Failed to save timetable entry.';
      alert(errorMessage);
    }
  };

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this schedule slot?')) {
      await timetableService.deleteTimetable(id);
      fetchTimetable();
    }
  };

  if (loading && entries.length === 0) return <Loading className="min-h-screen" text="Syncing Schedule..." size="lg" />;

  return (
    <div className="space-y-6 pb-20">
      {/* Header Area */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold gradient-text flex items-center">
            <Calendar className="w-8 h-8 mr-3 text-[#7C3AED]" />
            {userRole === 'FACULTY' || userRole === 'STUDENT' ? 'My Class Schedule' : 'Timetable Architect'}
          </h1>
          <div className="flex items-center mt-1 text-gray-400">
            <p>
              {userRole === 'FACULTY' || userRole === 'STUDENT'
                ? 'Your assigned weekly class sessions and room allocations.' 
                : 'Manage recurring class schedules and smart breaks.'}
            </p>
            {entries.length > 0 ? (
              <span className="ml-3 px-2 py-0.5 rounded-full bg-[#7C3AED]/20 text-[#7C3AED] text-xs font-bold border border-[#7C3AED]/30">
                {entries.length} Slots Loaded
              </span>
            ) : (
              <span className="ml-3 px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 text-xs font-bold border border-red-500/30">
                0 Slots Found {selectedSection ? `[Section: ${selectedSection.substring(0,8)}...]` : '[Select Section]'}
              </span>
            )}
          </div>
        </div>

        {(userRole === 'ADMIN' || userRole === 'SUPER_ADMIN') && (
          <Button 
            variant="primary" 
            onClick={() => { setEditingEntry(null); setIsModalOpen(true); }}
            className="shadow-[0_0_20px_rgba(124,58,237,0.3)]"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add Schedule Slot
          </Button>
        )}
      </div>

      {/* Filters Area - Only for Admins/Faculty Coordinators */}
      {userRole !== 'FACULTY' && userRole !== 'STUDENT' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card glass className="bg-white/5">
            <CardContent className="p-4 flex items-center space-x-4">
              <Filter className="text-[#7C3AED] w-5 h-5" />
              <select 
                className="bg-transparent text-sm font-medium text-white focus:outline-none w-full"
                value={selectedSection}
                onChange={e => setSelectedSection(e.target.value)}
              >
                <option value="" className="bg-[#0F0F16]">Filter by Section</option>
                {sections.map(s => (
                  <option key={s.id} value={s.id} className="bg-[#0F0F16]">{s.label || s.name}</option>
                ))}
              </select>
            </CardContent>
          </Card>

          <Card glass className="bg-white/5">
            <CardContent className="p-4 flex items-center space-x-4">
              <User className="text-[#7C3AED] w-5 h-5" />
              <select 
                className="bg-transparent text-sm font-medium text-white focus:outline-none w-full"
                value={selectedFaculty}
                onChange={e => setSelectedFaculty(e.target.value)}
              >
                <option value="" className="bg-[#0F0F16]">Filter by Faculty</option>
                {facultyList.map(f => (
                  <option key={f.id} value={f.id} className="bg-[#0F0F16]">{f.name || f.label}</option>
                ))}
              </select>
            </CardContent>
          </Card>

          <div className="flex items-center justify-end text-xs text-gray-500 italic">
            <Building className="w-4 h-4 mr-1" />
            Displaying Weekly Schedule
          </div>
        </div>
      )}

      {/* Grid View */}
      <TimetableGrid 
        entries={entries} 
        userRole={userRole ? String(userRole) : undefined}
        onEdit={(entry) => { setEditingEntry(entry); setIsModalOpen(true); }}
        onDelete={handleDelete}
      />

      {/* Modal */}
      <ScheduleModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSave={handleSave}
        editingEntry={editingEntry}
        rooms={rooms}
        faculties={facultyList}
        sections={sections}
      />

      {/* ✨ Success Animation Notification */}
      <AnimatePresence>
        {showSuccess && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            className="fixed bottom-10 right-10 z-[100] flex items-center bg-[#10B981] text-white px-6 py-4 rounded-2xl shadow-[0_20px_50px_rgba(16,185,129,0.3)] border border-white/20"
          >
            <div className="bg-white/20 p-2 rounded-full mr-4">
              <CheckCircle className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="font-bold text-lg leading-none">Schedule Secured!</p>
              <p className="text-sm text-white/80 mt-1">Timetable has been updated successfully.</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* 🔍 DIAGNOSTIC PANEL (Always useful for debugging) */}
      <div className="mt-10 p-4 rounded-xl border border-white/5 bg-black/20">
        <details className="cursor-pointer">
          <summary className="text-[10px] font-black uppercase tracking-widest text-slate-600 hover:text-[#7C3AED] transition-colors">
            Data Architect Diagnostics
          </summary>
          <div className="mt-4 p-4 rounded-lg bg-black/40 font-mono text-[10px] text-gray-500 overflow-auto max-h-60">
            <p className="mb-2 text-[#7C3AED]">User Role: {userRole}</p>
            <p className="mb-2 text-[#7C3AED]">User Profile Section ID: {(user as any)?.sectionId || (user as any)?.section_id || (user as any)?.section?.id || 'NOT FOUND'}</p>
            <p className="mb-2 text-[#7C3AED]">Current Filter Section ID: {selectedSection || 'NONE'}</p>
            <p className="mb-2 text-[#7C3AED]">Total Entries Received: {entries.length}</p>
            <pre>{JSON.stringify(entries, null, 2)}</pre>
          </div>
        </details>
      </div>
    </div>
  );
}
