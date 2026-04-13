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
    if (isOpen) {
      const loadInitialData = async () => {
        await fetchDepartments();
        // Since the user object might use department names, find the department ID to load sections
        if (user?.department) {
          const dept = departments.find(d => d.label === user.department);
          if (dept) {
            await fetchSections(dept.id);
          }
        }
      };
      loadInitialData();
    }
  }, [isOpen]);

  const fetchDepartments = async () => {
    try {
      const data = await userManagementService.getDepartments();
      setDepartments(data);
    } catch (err) {
      console.error('Failed to fetch departments');
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
          className="relative w-full max-w-2xl bg-[#0F0F16] border border-white/10 rounded-3xl overflow-hidden shadow-2xl shadow-primary/10"
        >
          {/* Header */}
          <div className="px-8 py-6 border-b border-white/5 flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white flex items-center gap-3">
                <User className="text-primary" /> Edit Identity
              </h2>
              <p className="text-slate-500 text-sm mt-1">Modify account details for {user.name}</p>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-white/5 rounded-xl transition-colors text-slate-400 hover:text-white"
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
                    glass
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
                    glass
                    required
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">User Role</label>
                  <select 
                    value={formData.role}
                    onChange={(e) => setFormData({...formData, role: e.target.value})}
                    className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
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
                    className={`w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all ${
                      formData.status === 'ACTIVE' ? 'text-emerald-400' : 'text-accent'
                    }`}
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="SUSPENDED">Suspended</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Department</label>
                  <select 
                    value={formData.department}
                    onChange={(e) => handleDepartmentChange(e.target.value)}
                    className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
                  >
                    <option value="">Select Department</option>
                    {departments.map(dept => (
                      <option key={dept.id} value={dept.label}>{dept.label}</option>
                    ))}
                  </select>
                </div>

                {formData.role === 'STUDENT' && (
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Section</label>
                    <select 
                      value={formData.sectionId}
                      onChange={(e) => setFormData({...formData, sectionId: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
                    >
                      <option value="">Select Section</option>
                      {sections.map(section => (
                        <option key={section.id} value={section.id}>{section.label}</option>
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
                className="mt-6 p-4 rounded-2xl bg-accent/10 border border-accent/20 text-accent text-sm flex items-center gap-3"
              >
                <AlertCircle size={18} /> {error}
              </motion.div>
            )}

            {saveSuccess && (
              <motion.div 
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="mt-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center gap-3"
              >
                <CheckCircle2 size={18} /> Identity updated successfully!
              </motion.div>
            )}

            <div className="mt-8 flex justify-end gap-3">
              <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" className="px-8 shadow-lg shadow-primary/20" disabled={loading}>
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
