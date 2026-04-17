'use client';

import React, { useState, useEffect } from 'react';
import { 
  Building, 
  Plus, 
  Edit, 
  Trash2, 
  ChevronRight, 
  Search, 
  Layers, 
  Users, 
  MoreVertical,
  CheckCircle2,
  XCircle,
  AlertCircle,
  LayoutGrid,
  List as ListIcon
} from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Loading } from '@/components/ui/Loading';
import { safeParseInt } from '@/utils/numberUtils';

// --- Types ---

interface Department {
  id: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  studentCount?: number;
  facultyCount?: number;
}

interface Section {
  id: string;
  name: string;
  program: string;
  capacity: number;
  batchYear: number;
  totalAcademicYears: string;
  currentSemester: number;
  description?: string;
  isActive: boolean;
  studentCount?: number;
}

interface DropdownDTO {
  id: string;
  label: string;
  value: string;
}

interface Faculty {
  id: string;
  name: string;
  email: string;
  role: string;
}

interface Student {
  id: string;
  name: string;
  email: string;
  registrationNumber: string;
}

// --- Modals ---

interface DeptModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string, code: string, description: string, isActive: boolean }) => void;
  initialData?: Department;
  loading: boolean;
  error?: string | null;
}

const DeptModal: React.FC<DeptModalProps> = ({ isOpen, onClose, onSubmit, initialData, loading, error }) => {
  const [name, setName] = useState(initialData?.name || '');
  const [code, setCode] = useState(initialData?.code || '');
  const [description, setDescription] = useState(initialData?.description || '');
  const [isActive, setIsActive] = useState(initialData?.isActive !== false);

  useEffect(() => {
    if (isOpen) {
      setName(initialData?.name || '');
      setCode(initialData?.code || '');
      setDescription(initialData?.description || '');
      setIsActive(initialData?.isActive !== false);
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md bg-[#1a1c1e] border border-gray-800 rounded-2xl shadow-2xl p-6">
        <h3 className="text-xl font-bold text-white mb-6">
          {initialData ? 'Edit Department' : 'New Department'}
        </h3>
        
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1.5">Department Name</label>
            <Input 
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Computer Science & Engineering"
              glass
              className="w-full"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1.5">Department Code</label>
            <Input 
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="e.g. CSE"
              glass
              className="w-full"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1.5">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-[#0f1113] border border-gray-800 rounded-xl p-3 text-white focus:outline-none focus:border-blue-500/50 transition-colors min-h-[100px]"
              placeholder="Enter department description..."
            />
          </div>
          <div className="flex items-center space-x-3 p-3 bg-white/5 rounded-xl border border-white/10">
            <input 
              type="checkbox"
              id="dept-active"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 rounded border-gray-700 bg-gray-900 text-blue-500 focus:ring-blue-500"
            />
            <label htmlFor="dept-active" className="text-sm text-gray-300 cursor-pointer">Mark as Active</label>
          </div>

          {error && (
            <div className="flex items-center space-x-2 text-red-400 bg-red-400/10 p-3 rounded-xl border border-red-400/20 text-sm">
              <AlertCircle size={14} />
              <span>{error}</span>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end space-x-3 mt-8">
          <Button variant="glass" onClick={onClose} disabled={loading}>Cancel</Button>
          <Button 
            variant="primary" 
            onClick={() => onSubmit({ name, code, description, isActive })} 
            disabled={loading || !name || !code}
          >
            {loading ? <Loading size="sm" /> : (initialData ? 'Save Changes' : 'Create Department')}
          </Button>
        </div>
      </div>
    </div>
  );
};

interface SectionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { 
    name: string, 
    program: string, 
    capacity: number,
    batchYear: number,
    totalAcademicYears: string,
    currentSemester: number,
    description: string,
    isActive: boolean
  }) => void;
  initialData?: Section;
  loading: boolean;
  error?: string | null;
}

const SectionModal: React.FC<SectionModalProps> = ({ isOpen, onClose, onSubmit, initialData, loading, error }) => {
  const [name, setName] = useState(initialData?.name || '');
  const [program, setProgram] = useState(initialData?.program || '');
  const [capacity, setCapacity] = useState(initialData?.capacity || 60);
  const [batchYear, setBatchYear] = useState(initialData?.batchYear || new Date().getFullYear());
  const [totalAcademicYears, setTotalAcademicYears] = useState(initialData?.totalAcademicYears || '4');
  const [currentSemester, setCurrentSemester] = useState(initialData?.currentSemester || 1);
  const [description, setDescription] = useState(initialData?.description || '');
  const [isActive, setIsActive] = useState(initialData?.isActive !== false);

  useEffect(() => {
    if (isOpen) {
      setName(initialData?.name || '');
      setProgram(initialData?.program || '');
      setCapacity(initialData?.capacity || 60);
      setBatchYear(initialData?.batchYear || new Date().getFullYear());
      setTotalAcademicYears(initialData?.totalAcademicYears || '4');
      setCurrentSemester(initialData?.currentSemester || 1);
      setDescription(initialData?.description || '');
      setIsActive(initialData?.isActive !== false);
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md bg-[#1a1c1e] border border-gray-800 rounded-2xl shadow-2xl p-6">
        <h3 className="text-xl font-bold text-white mb-6">
          {initialData ? 'Edit Section' : 'New Section'}
        </h3>
        
        <div className="space-y-4 max-h-[60vh] overflow-y-auto px-1 pr-2">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5">Section Name</label>
              <Input 
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Section A"
                glass
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5">Batch Year</label>
              <Input 
                type="number"
                value={batchYear}
                onChange={(e) => setBatchYear(safeParseInt(e.target.value))}
                placeholder="2024"
                glass
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1.5">Program / Degree</label>
            <Input 
              value={program}
              onChange={(e) => setProgram(e.target.value)}
              placeholder="e.g. B.Tech (Honours)"
              glass
            />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5">Capacity</label>
              <Input 
                type="number"
                value={capacity}
                onChange={(e) => setCapacity(safeParseInt(e.target.value))}
                placeholder="60"
                glass
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5">Total Years</label>
              <Input 
                value={totalAcademicYears}
                onChange={(e) => setTotalAcademicYears(e.target.value)}
                placeholder="4"
                glass
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5">Semester</label>
              <Input 
                type="number"
                value={currentSemester}
                onChange={(e) => setCurrentSemester(safeParseInt(e.target.value))}
                placeholder="1"
                glass
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-1.5">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-[#0f1113] border border-gray-800 rounded-xl p-3 text-white focus:outline-none focus:border-blue-500/50 transition-colors min-h-[80px]"
              placeholder="Enter section description..."
            />
          </div>
          <div className="flex items-center space-x-3 p-3 bg-white/5 rounded-xl border border-white/10">
            <input 
              type="checkbox"
              id="section-active"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 rounded border-gray-700 bg-gray-900 text-blue-500 focus:ring-blue-500"
            />
            <label htmlFor="section-active" className="text-sm text-gray-300 cursor-pointer">Mark as Active</label>
          </div>

          {error && (
            <div className="flex items-center space-x-2 text-red-400 bg-red-400/10 p-3 rounded-xl border border-red-400/20 text-sm">
              <AlertCircle size={14} />
              <span>{error}</span>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end space-x-3 mt-8">
          <Button variant="glass" onClick={onClose} disabled={loading}>Cancel</Button>
          <Button 
            variant="primary" 
            onClick={() => onSubmit({ name, program, capacity, batchYear, totalAcademicYears, currentSemester, description, isActive })} 
            disabled={loading || !name || !program}
          >
            {loading ? <Loading size="sm" /> : (initialData ? 'Save Changes' : 'Create Section')}
          </Button>
        </div>
      </div>
    </div>
  );
};

interface FacultyListModalProps {
  isOpen: boolean;
  onClose: () => void;
  deptName: string;
  faculty: Faculty[];
  loading: boolean;
}

const FacultyListModal: React.FC<FacultyListModalProps> = ({ isOpen, onClose, deptName, faculty, loading }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-2xl bg-[#1a1c1e] border border-gray-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[80vh]">
        <div className="p-6 border-b border-white/5 flex items-center justify-between bg-gradient-to-r from-purple-500/10 to-transparent">
          <div>
            <h3 className="text-xl font-bold text-white leading-tight">Department Faculty</h3>
            <p className="text-sm text-gray-400 mt-1">{deptName}</p>
          </div>
          <button 
            onClick={onClose}
            className="p-2 text-gray-500 hover:text-white hover:bg-white/5 rounded-xl transition-colors"
          >
            <XCircle size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="py-20 flex flex-col items-center justify-center space-y-4">
              <Loading size="lg" />
              <p className="text-gray-500 animate-pulse">Fetching department experts...</p>
            </div>
          ) : faculty.length === 0 ? (
            <div className="py-20 text-center">
              <Users className="mx-auto text-gray-700 mb-4 opacity-20" size={64} />
              <p className="text-gray-500 font-medium text-lg">No faculty mapped to this department yet.</p>
              <p className="text-sm text-gray-600 mt-2 italic">Assign faculty members to see them here.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              {faculty.map((member, idx) => (
                <div 
                  key={member.id || idx}
                  className="p-4 bg-white/5 border border-white/5 rounded-2xl flex items-center justify-between hover:border-purple-500/30 transition-all group"
                >
                  <div className="flex items-center space-x-4">
                    <div className="w-10 h-10 rounded-full bg-purple-500/10 flex items-center justify-center text-purple-400 group-hover:bg-purple-500/20 transition-colors">
                      <span className="font-bold text-sm uppercase">{member.name?.charAt(0) || 'F'}</span>
                    </div>
                    <div>
                      <h4 className="text-white font-semibold">{member.name}</h4>
                      <p className="text-xs text-gray-500">{member.email}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className="text-[10px] uppercase font-bold tracking-tighter px-2 py-0.5 bg-purple-500/10 text-purple-400 border border-purple-500/20 rounded-full">
                      {member.role?.replace('ROLE_', '') || 'FACULTY'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-white/5 bg-gray-900/40 flex justify-end">
          <Button variant="glass" onClick={onClose}>Close Directory</Button>
        </div>
      </div>
    </div>
  );
};

interface StudentListModalProps {
  isOpen: boolean;
  onClose: () => void;
  sectionName: string;
  students: Student[];
  loading: boolean;
}

const StudentListModal: React.FC<StudentListModalProps> = ({ isOpen, onClose, sectionName, students, loading }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-2xl bg-[#1a1c1e] border border-gray-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[80vh]">
        <div className="p-6 border-b border-white/5 flex items-center justify-between bg-gradient-to-r from-emerald-500/10 to-transparent">
          <div>
            <h3 className="text-xl font-bold text-white leading-tight">Section Students</h3>
            <p className="text-sm text-gray-400 mt-1">{sectionName}</p>
          </div>
          <button 
            onClick={onClose}
            className="p-2 text-gray-500 hover:text-white hover:bg-white/5 rounded-xl transition-colors"
          >
            <XCircle size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {loading ? (
            <div className="py-20 flex flex-col items-center justify-center space-y-4">
              <Loading size="lg" />
              <p className="text-gray-500 animate-pulse">Fetching class list...</p>
            </div>
          ) : students.length === 0 ? (
            <div className="py-20 text-center">
              <Users className="mx-auto text-gray-700 mb-4 opacity-20" size={64} />
              <p className="text-gray-500 font-medium text-lg">No students assigned to this section yet.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3">
              {students.map((student, idx) => (
                <div 
                  key={student.id || idx}
                  className="p-4 bg-white/5 border border-white/5 rounded-2xl flex items-center justify-between hover:border-emerald-500/30 transition-all group"
                >
                  <div className="flex items-center space-x-4">
                    <div className="w-10 h-10 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400 group-hover:bg-emerald-500/20 transition-colors">
                      <span className="font-bold text-sm uppercase">{student.name?.charAt(0) || 'S'}</span>
                    </div>
                    <div>
                      <h4 className="text-white font-semibold">{student.name}</h4>
                      <p className="text-xs text-gray-500">{student.email}</p>
                      <p className="text-[10px] text-gray-600 font-mono mt-1">{student.registrationNumber}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 border-t border-white/5 bg-gray-900/40 flex justify-end">
          <Button variant="glass" onClick={onClose}>Close Directory</Button>
        </div>
      </div>
    </div>
  );
};

// --- Main Component ---

export const DepartmentSectionManagement: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [selectedDeptId, setSelectedDeptId] = useState<string | null>(null);
  const [sections, setSections] = useState<Section[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'details'>('grid');

  // Modals state
  const [deptModal, setDeptModal] = useState<{ open: boolean, data?: Department }>({ open: false });
  const [sectionModal, setSectionModal] = useState<{ open: boolean, data?: Section }>({ open: false });
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [facultyModal, setFacultyModal] = useState<{ open: boolean, dept?: Department | null, list: Faculty[] }>({ 
    open: false, 
    dept: null, 
    list: [] 
  });
  const [studentModal, setStudentModal] = useState<{ open: boolean, section?: Section | null, list: Student[] }>({ 
    open: false, 
    section: null, 
    list: [] 
  });

  useEffect(() => {
    fetchDepartments();
  }, []);

  useEffect(() => {
    if (selectedDeptId) {
      fetchSections(selectedDeptId);
    } else {
      setSections([]);
    }
  }, [selectedDeptId]);

  const fetchDepartments = async () => {
    setLoading(true);
    try {
      const { data } = await apiClient.get<Array<{
        id?: string;
        departmentId?: string;
        name?: string;
        label?: string;
        code?: string;
        value?: string;
        description?: string;
        isActive?: boolean;
        studentCount?: number;
        facultyCount?: number;
      }>>('/api/v1/admin/departments');
      // For some reason this endpoint returns different structures based on logic
      // We will handle it by checking the properties
      const mapped = data.map(d => ({
        id: d.id || d.departmentId || '',
        name: d.name || d.label || '',
        code: d.code || d.value || '',
        description: d.description || '',
        isActive: d.isActive !== false,
        studentCount: d.studentCount || 0,
        facultyCount: d.facultyCount || 0
      }));
      setDepartments(mapped);
    } catch (error) {
      console.error('Failed to fetch departments:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchSections = async (deptId: string) => {
    try {
      const { data } = await apiClient.get<Section[]>(`/api/v1/admin/departments/${deptId}/sections/details`);
      const processed = Array.isArray(data) ? data : [];
      const mapped = processed.map(s => ({
        id: s.id,
        name: s.name,
        program: s.program || 'Standard',
        capacity: s.capacity || 60,
        batchYear: s.batchYear || 2024,
        totalAcademicYears: s.totalAcademicYears || '4',
        currentSemester: s.currentSemester || 1,
        description: s.description || '',
        isActive: s.isActive !== false,
        studentCount: s.studentCount || 0
      }));
      setSections(mapped);
    } catch (error) {
      console.error('Failed to fetch sections:', error);
      setSections([]);
    }
  };

  const handleViewStudents = async (section: Section) => {
    setStudentModal({ open: true, section, list: [] });
    setModalLoading(true);
    try {
      const { data } = await apiClient.get(`/api/v1/admin/sections/${section.id}/students`);
      setStudentModal(prev => ({ ...prev, list: Array.isArray(data) ? data : [] }));
    } catch (error) {
      console.error('Failed to fetch students:', error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleCreateDept = async (data: { name: string, code: string, description: string, isActive: boolean }) => {
    setModalLoading(true);
    setModalError(null);
    try {
      await apiClient.post('/api/v1/admin/departments', data);
      await fetchDepartments();
      setDeptModal({ open: false });
    } catch (error: unknown) {
      const axiosError = error as any;
      const errorMessage = axiosError.response?.data?.error || axiosError.response?.data?.message || 'Failed to create department';
      setModalError(errorMessage);
      console.error('Failed to create department:', error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleUpdateDept = async (data: { name: string, code: string, description: string, isActive: boolean }) => {
    if (!deptModal.data) return;
    setModalLoading(true);
    setModalError(null);
    try {
      await apiClient.put(`/api/v1/admin/departments/${deptModal.data.id}`, data);
      await fetchDepartments();
      setDeptModal({ open: false });
    } catch (error: unknown) {
      const axiosError = error as any;
      const errorMessage = axiosError.response?.data?.error || axiosError.response?.data?.message || 'Failed to update department';
      setModalError(errorMessage);
      console.error('Failed to update department:', error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteDept = async (id: string) => {
    if (!confirm('Are you sure you want to delete this department? All associated sections will be affected.')) return;
    try {
      await apiClient.delete(`/api/v1/admin/departments/${id}`);
      if (selectedDeptId === id) setSelectedDeptId(null);
      await fetchDepartments();
    } catch (error) {
      console.error('Failed to delete department:', error);
    }
  };

  const handleCreateSection = async (data: Omit<Section, 'id' | 'studentCount' | 'isActive'> & { isActive: boolean }) => {
    if (!selectedDeptId) return;
    setModalLoading(true);
    setModalError(null);
    try {
      await apiClient.post('/api/v1/admin/sections', { ...data, departmentId: selectedDeptId });
      await fetchSections(selectedDeptId);
      setSectionModal({ open: false });
    } catch (error: unknown) {
      const axiosError = error as any;
      const errorMessage = axiosError.response?.data?.error || axiosError.response?.data?.message || 'Failed to create section';
      setModalError(errorMessage);
      console.error('Failed to create section:', error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleUpdateSection = async (data: Omit<Section, 'id' | 'studentCount' | 'isActive'> & { isActive: boolean }) => {
    const sectionId = sectionModal.data?.id;
    if (!sectionId || !selectedDeptId) {
      setModalError("Missing required identifiers for update");
      return;
    }
    
    setModalLoading(true);
    setModalError(null);
    try {
      // Ensure we send the correct payload structure
      const payload = {
        ...data,
        departmentId: selectedDeptId
      };
      
      await apiClient.put(`/api/v1/admin/sections/${sectionId}`, payload);
      await fetchSections(selectedDeptId);
      setSectionModal({ open: false, data: undefined });
    } catch (error: unknown) {
      const axiosError = error as any;
      const errorMessage = axiosError.response?.data?.error || axiosError.response?.data?.message || 'Failed to update section';
      setModalError(errorMessage);
      console.error('Failed to update section:', error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteSection = async (id: string) => {
    if (!confirm('Are you sure you want to delete this section?')) return;
    try {
      await apiClient.delete(`/api/v1/admin/sections/${id}`);
      if (selectedDeptId) await fetchSections(selectedDeptId);
    } catch (error) {
      console.error('Failed to delete section:', error);
    }
  };

  const handleViewFaculty = async (dept: Department) => {
    setFacultyModal(prev => ({ ...prev, open: true, dept, list: [] }));
    setModalLoading(true);
    try {
      const { data } = await apiClient.get(`/api/v1/admin/departments/${dept.id}/faculty`);
      setFacultyModal(prev => ({ ...prev, list: Array.isArray(data) ? data : [] }));
    } catch (error) {
      console.error('Failed to fetch faculty:', error);
    } finally {
      setModalLoading(false);
    }
  };


  const filteredDepts = departments.filter(d => 
    d.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    d.code.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredSections = sections.filter(s =>
    s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.program.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6 pb-20">
      {viewMode === 'grid' ? (
        <div className="space-y-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h1 className="text-3xl font-extrabold text-white tracking-tight">Academic Departments</h1>
              <p className="text-gray-400 mt-1">Manage institutional departments, faculty allocations, and student statistics</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={16} />
                <Input 
                  placeholder="Search departments..." 
                  className="pl-10 h-10 w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  glass
                />
              </div>
              <Button 
                variant="primary" 
                className="bg-blue-600 hover:bg-blue-500 shadow-lg shadow-blue-600/20"
                onClick={() => setDeptModal({ open: true })}
              >
                <Plus size={18} className="mr-2" />
                Add Department
              </Button>
            </div>
          </div>

          {loading ? (
            <div className="py-20 flex flex-col items-center justify-center space-y-4">
              <Loading size="lg" />
              <p className="text-gray-500 animate-pulse">Synchronizing academic data...</p>
            </div>
          ) : filteredDepts.length === 0 ? (
            <Card glass className="py-20 flex flex-col items-center justify-center text-center border-dashed">
              <Building className="text-gray-700 mb-4 opacity-20" size={64} />
              <h3 className="text-xl font-bold text-white">No Departments Found</h3>
              <p className="text-gray-500 mt-2 max-w-sm">Start by creating your first academic department to manage sections and students.</p>
              <Button variant="glass" className="mt-6" onClick={() => setDeptModal({ open: true })}>
                Create Department
              </Button>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredDepts.map((dept) => (
                <div 
                  key={dept.id}
                  className="group relative bg-gray-900/40 backdrop-blur-xl border border-white/5 hover:border-blue-500/30 rounded-3xl p-6 transition-all duration-300 hover:shadow-2xl hover:shadow-blue-500/10"
                >
                  <div className="flex items-start justify-between mb-6">
                    <div className="p-3 bg-blue-500/10 rounded-2xl text-blue-400 group-hover:scale-110 transition-transform">
                      <Building size={24} />
                    </div>
                    <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => setDeptModal({ open: true, data: dept })}
                        className="p-2 text-gray-500 hover:text-white hover:bg-white/5 rounded-xl transition-colors"
                      >
                        <Edit size={16} />
                      </button>
                      <button 
                        onClick={() => handleDeleteDept(dept.id)}
                        className="p-2 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-colors"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>

                  <div className="space-y-1 mb-8">
                    <h3 className="text-xl font-bold text-white group-hover:text-blue-400 transition-colors line-clamp-1">{dept.name}</h3>
                    <div className="flex items-center space-x-2">
                      <span className="text-[10px] uppercase font-bold tracking-widest text-gray-500 bg-gray-800/50 px-2 py-0.5 rounded border border-white/5">
                        {dept.code}
                      </span>
                      {!dept.isActive && (
                        <span className="text-[9px] uppercase font-bold px-1.5 py-0.5 bg-red-500/10 text-red-500 border border-red-500/20 rounded">
                          Inactive
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 mb-8">
                    <div className="bg-white/5 rounded-2xl p-4 border border-white/5 flex flex-col items-center justify-center text-center">
                      <span className="text-[10px] text-gray-500 uppercase font-black tracking-widest mb-1">Students</span>
                      <span className="text-2xl font-black text-white">{dept.studentCount || 0}</span>
                    </div>
                    <div 
                      onClick={() => handleViewFaculty(dept)}
                      className="bg-white/5 rounded-2xl p-4 border border-white/5 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-purple-500/10 hover:border-purple-500/30 transition-all group/stat"
                    >
                      <span className="text-[10px] text-gray-500 uppercase font-black tracking-widest mb-1 group-hover/stat:text-purple-300">Faculty</span>
                      <span className="text-2xl font-black text-purple-400 group-hover/stat:scale-110 transition-transform">{dept.facultyCount || 0}</span>
                    </div>
                  </div>

                  <button 
                    onClick={() => { setSelectedDeptId(dept.id); setViewMode('details'); }}
                    className="w-full py-4 bg-white/5 hover:bg-blue-600/20 text-[11px] font-black uppercase tracking-[0.2em] text-gray-400 hover:text-blue-400 border border-white/5 hover:border-blue-500/30 rounded-2xl transition-all flex items-center justify-center space-x-2"
                  >
                    <LayoutGrid size={14} />
                    <span>Explore Sections</span>
                  </button>
                  
                  {dept.isActive && (
                    <div className="absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-[#0a0a0a] shadow-lg shadow-blue-500/50" />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <div className="flex items-center justify-between">
            <button 
              onClick={() => setViewMode('grid')}
              className="flex items-center space-x-2 text-gray-400 hover:text-white transition-colors group"
            >
              <div className="p-2 bg-white/5 rounded-xl group-hover:bg-white/10">
                <ChevronRight size={18} className="rotate-180" />
              </div>
              <span className="text-sm font-bold">Back to Departments</span>
            </button>
            
            <div className="flex items-center space-x-3">
              <div className="text-right">
                <h2 className="text-xl font-bold text-white">{departments.find(d => d.id === selectedDeptId)?.name}</h2>
                <p className="text-[10px] text-blue-400 font-mono tracking-widest uppercase">{departments.find(d => d.id === selectedDeptId)?.code} Operations</p>
              </div>
              <div className="p-3 bg-blue-600/20 text-blue-400 rounded-2xl">
                <Building size={24} />
              </div>
            </div>
          </div>

          <Card glass className="overflow-hidden flex flex-col border-white/5">
            <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-white/5 py-6">
              <div className="flex items-center space-x-4">
                <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-2xl">
                  <Layers size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">Academic Sections</h3>
                  <p className="text-xs text-gray-500">Managing capacity and curriculum flow</p>
                </div>
              </div>
              <div className="flex flex-col sm:flex-row items-center gap-3">
                <Input 
                  placeholder="Filter sections..." 
                  className="w-full sm:w-64"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  icon={<Search size={16} />}
                  glass
                />
                <Button 
                  variant="primary" 
                  size="sm"
                  className="w-full sm:w-auto bg-emerald-600 hover:bg-emerald-500 shadow-lg shadow-emerald-600/20"
                  onClick={() => setSectionModal({ open: true })}
                >
                  <Plus size={16} className="mr-2" />
                  New Section
                </Button>
              </div>
            </CardHeader>

            <CardContent className="p-6 bg-black/20">
              {sections.length === 0 ? (
                <div className="py-20 text-center">
                  <Layers className="mx-auto text-gray-700 mb-6 opacity-20" size={64} />
                  <p className="text-gray-500 mb-6 font-medium">No active sections mapped to this department.</p>
                  <Button variant="glass" onClick={() => setSectionModal({ open: true })}>
                    Initialize First Section
                  </Button>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                  {filteredSections.map((section) => (
                    <div 
                      key={section.id}
                      className="bg-zinc-900/40 border border-white/5 hover:border-emerald-500/30 rounded-3xl p-6 transition-all group relative overflow-hidden"
                    >
                      <div className="absolute top-4 right-4 p-2 flex space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button 
                          onClick={() => setSectionModal({ open: true, data: section })}
                          className="p-2 bg-black/40 text-gray-400 hover:text-white rounded-xl border border-white/10"
                        >
                          <Edit size={14} />
                        </button>
                        <button 
                          onClick={() => handleDeleteSection(section.id)}
                          className="p-2 bg-black/40 text-gray-400 hover:text-red-400 rounded-xl border border-white/10"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>

                      <div className="flex items-start mb-6">
                        <div className={`p-2 rounded-xl mr-4 ${section.isActive ? 'bg-emerald-500/10 text-emerald-500' : 'bg-red-500/10 text-red-500'}`}>
                          {section.isActive ? <CheckCircle2 size={20} /> : <XCircle size={20} />}
                        </div>
                        <div className="space-y-0.5">
                          <h4 className="font-bold text-white text-lg">{section.name}</h4>
                          <p className="text-[10px] text-gray-500 font-bold uppercase tracking-widest">{section.program}</p>
                        </div>
                      </div>

                      <div className="grid grid-cols-3 gap-3 mb-6">
                        <div className="bg-white/5 rounded-xl py-2 px-3 border border-white/5">
                          <span className="text-[8px] text-gray-500 uppercase block mb-1">Batch</span>
                          <span className="text-xs font-bold text-white">{section.batchYear}</span>
                        </div>
                        <div className="bg-white/5 rounded-xl py-2 px-3 border border-white/5">
                          <span className="text-[8px] text-gray-500 uppercase block mb-1">Semester</span>
                          <span className="text-xs font-bold text-blue-400">S{section.currentSemester}</span>
                        </div>
                        <div className="bg-white/5 rounded-xl py-2 px-3 border border-white/5">
                          <span className="text-[8px] text-gray-500 uppercase block mb-1">Years</span>
                          <span className="text-xs font-bold text-purple-400">{section.totalAcademicYears}Y</span>
                        </div>
                      </div>

                      <div className="flex items-center justify-between pt-4 border-t border-white/5">
                        <div 
                          onClick={() => handleViewStudents(section)}
                          className="flex items-center space-x-2 cursor-pointer hover:bg-emerald-500/10 p-1 rounded-lg transition-colors group/stats"
                        >
                          <Users size={16} className="text-gray-500 group-hover/stats:text-emerald-400" />
                          <span className="text-sm font-bold text-white group-hover/stats:text-emerald-400">{section.studentCount || 0} / {section.capacity}</span>
                          <span className="text-[10px] text-gray-600">Students</span>
                        </div>
                        <div className="text-[10px] text-gray-700 font-mono">
                          #{section.id.substring(0, 6)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Modals */}
      <DeptModal 
        isOpen={deptModal.open} 
        onClose={() => { setDeptModal({ open: false }); setModalError(null); }}
        onSubmit={deptModal.data ? handleUpdateDept : handleCreateDept}
        initialData={deptModal.data}
        loading={modalLoading}
        error={modalError}
      />

      <SectionModal
        isOpen={sectionModal.open}
        onClose={() => { setSectionModal({ open: false }); setModalError(null); }}
        onSubmit={sectionModal.data ? handleUpdateSection : handleCreateSection}
        initialData={sectionModal.data}
        loading={modalLoading}
        error={modalError}
      />

      <FacultyListModal
        isOpen={facultyModal.open}
        onClose={() => setFacultyModal(prev => ({ ...prev, open: false }))}
        deptName={facultyModal.dept?.name || ''}
        faculty={facultyModal.list}
        loading={modalLoading}
      />

      <StudentListModal
        isOpen={studentModal.open}
        onClose={() => setStudentModal(prev => ({ ...prev, open: false }))}
        sectionName={studentModal.section?.name || ''}
        students={studentModal.list}
        loading={modalLoading}
      />
    </div>
  );
};