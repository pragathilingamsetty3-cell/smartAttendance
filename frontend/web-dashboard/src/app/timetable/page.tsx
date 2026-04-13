'use client';

import React, { useState, useEffect } from 'react';
import { useAuth } from '@/stores/authContext';
import { timetableService } from '@/services/timetable.service';
import { roomManagementService } from '@/services/roomManagement.service';
import { userManagementService } from '@/services/userManagement.service';
import { TimetableGrid } from '@/components/timetable/TimetableGrid';
import { ScheduleModal } from '@/components/timetable/ScheduleModal';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { Plus, Calendar, Filter, User, Building } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';

export default function TimetablePage() {
  const { user, getUserRole } = useAuth();
  const userRole = getUserRole();
  const [loading, setLoading] = useState(true);
  const [entries, setEntries] = useState<any[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<any>(null);
  
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
      if (userRole === 'STUDENT' && user.sectionId) {
        setSelectedSection(user.sectionId);
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
      const isStudent = String(userRole).includes('STUDENT');
      
      if (selectedSection) {
        data = await timetableService.getTimetablesForSection(selectedSection);
        if (selectedFaculty) {
          data = data.filter((e: any) => e.faculty?.id === selectedFaculty);
        }
      } else if (selectedFaculty) {
        data = await timetableService.getTimetablesForFaculty(selectedFaculty);
      } else if (isFaculty && user.id) {
        data = await timetableService.getTimetablesForFaculty(user.id);
      } else if (isStudent && user.sectionId) {
        data = await timetableService.getTimetablesForSection(user.sectionId);
      } else {
        data = [];
      }
      
      console.log('✅ TIMETABLE DATA RECEIVED:', data?.length || 0, 'entries');
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
      fetchTimetable();
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
            {entries.length > 0 && (
              <span className="ml-3 px-2 py-0.5 rounded-full bg-[#7C3AED]/20 text-[#7C3AED] text-xs font-bold border border-[#7C3AED]/30">
                {entries.length} Slots Loaded
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
            Showing active 2025-2026 Academic Year
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
    </div>
  );
}
