'use client';

import React, { useState, useEffect } from 'react';
import { X, Save, User, Mail, Shield, Building2, Layers, AlertCircle, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import userManagementService from '@/services/userManagement.service';
import { DropdownDTO } from '@/types/user-management';

interface EditUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: any;
  onSuccess: () => void;
}

export const EditUserModal: React.FC<EditUserModalProps> = ({ isOpen, onClose, user, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    role: user?.role || '',
    department: user?.department || '',
    sectionId: user?.sectionId || '',
    status: user?.status || 'ACTIVE',
    studentMobile: user?.studentMobile || '',
    parentMobile: user?.parentMobile || '',
    semester: user?.currentSemester || 1,
    totalAcademicYears: user?.totalAcademicYears || ''
  });

  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  useEffect(() => {
    if (isOpen && user) {
      const loadInitialData = async () => {
        const currentDepts = await fetchDepartments();
        
        // Use the returned data directly to avoid closure stale state
        if (user.department && currentDepts.length > 0) {
          const dept = currentDepts.find((d: DropdownDTO) => 
            d.label === user.department || d.id === user.department
          );
          if (dept) {
            await fetchSections(dept.id);
          }
        }
      };
      loadInitialData();
    }
  }, [isOpen, user]);

  const fetchDepartments = async () => {
    try {
      const data = await userManagementService.getDepartments();
      setDepartments(data);
      return data; // Return data for immediate use in effects
    } catch (err) {
      console.error('Failed to fetch departments');
      return [];
    }
  };

  const fetchSections = async (deptId: string) => {
    try {
      const data = await userManagementService.getSections(deptId);
      setSections(data);
    } catch (err) {
      console.error('Failed to fetch sections');
    }
  };

  const handleDepartmentChange = (deptName: string) => {
    const dept = departments.find(d => d.label === deptName);
    setFormData(prev => ({ ...prev, department: deptName, sectionId: '' }));
    if (dept) {
      fetchSections(dept.id);
    } else {
      setSections([]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await userManagementService.updateUser(user.id, formData);
      setSaveSuccess(true);
      setTimeout(() => {
        onSuccess();
        onClose();
        setSaveSuccess(false);
      }, 1500);
    } catch (err: any) {
      setError(err.message || 'Failed to update user');
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
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-2xl shadow-sky-900/10"
        >
          {/* Header */}
          <div className="px-8 py-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
            <div>
              <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-3">
                <User className="text-primary" /> Edit Identity
              </h2>
              <p className="text-slate-500 text-sm mt-1">Modify account details for {user.name}</p>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-slate-100 rounded-xl transition-colors text-slate-400 hover:text-slate-600"
            >
              <X size={24} />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Basic Info */}
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Full Name</label>
                  <Input 
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    placeholder="Enter full name"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Email Address</label>
                  <Input 
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                    placeholder="email@example.com"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">User Role</label>
                  <select 
                    value={formData.role}
                    onChange={(e) => setFormData({...formData, role: e.target.value})}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all font-medium"
                  >
                    <option value="STUDENT">Student</option>
                    <option value="FACULTY">Faculty</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
              </div>

              {/* Status & Department */}
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Account Status</label>
                  <select 
                    value={formData.status}
                    onChange={(e) => setFormData({...formData, status: e.target.value as any})}
                    className={`w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all font-bold ${
                      formData.status === 'ACTIVE' ? 'text-emerald-600' : 'text-red-500'
                    }`}
                  >
                    <option value="ACTIVE" className="text-emerald-600">Active</option>
                    <option value="INACTIVE" className="text-slate-600">Inactive</option>
                    <option value="SUSPENDED" className="text-red-600">Suspended</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Department</label>
                  <select 
                    value={formData.department}
                    onChange={(e) => handleDepartmentChange(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all font-medium"
                  >
                    <option value="">Select Department</option>
                    {departments.map(dept => (
                      <option key={dept.id} value={dept.label} className="text-slate-900">{dept.label}</option>
                    ))}
                  </select>
                </div>

                {(formData.role === 'STUDENT' || formData.role === 'CR' || formData.role === 'LR') && (
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Section</label>
                    <select 
                      value={formData.sectionId}
                      onChange={(e) => setFormData({...formData, sectionId: e.target.value})}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-slate-900 focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all font-medium"
                    >
                      <option value="">Select Section</option>
                      {sections.map(section => (
                        <option key={section.id} value={section.id} className="text-slate-900">{section.label}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>

            {error && (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-6 p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-600 text-sm flex items-center gap-3"
              >
                <AlertCircle size={18} /> {error}
              </motion.div>
            )}

            {saveSuccess && (
              <motion.div 
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="mt-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 text-sm flex items-center gap-3"
              >
                <CheckCircle2 size={18} /> Identity updated successfully!
              </motion.div>
            )}

            <div className="mt-8 flex justify-end gap-3">
              <Button type="button" variant="secondary" onClick={onClose} disabled={loading} className="px-6">
                Cancel
              </Button>
              <Button type="submit" variant="primary" className="px-8 shadow-lg shadow-primary/20 font-bold" disabled={loading}>
                {loading ? 'Saving...' : (
                  <>
                    <Save size={18} className="mr-2" /> Save Changes
                  </>
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
