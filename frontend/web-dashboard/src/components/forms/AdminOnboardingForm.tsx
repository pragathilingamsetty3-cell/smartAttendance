'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { User, Mail, Phone, Briefcase, Shield } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuthStore } from '@/stores/authStore';
import { Role as UserRole } from '@/types';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { Select } from '@/components/ui/Select';
import { AdminOnboardingRequest, DropdownDTO } from '@/types/user-management';

interface AdminOnboardingFormProps {
  onSuccess?: (response: any) => void;
  onCancel?: () => void;
}

export const AdminOnboardingForm: React.FC<AdminOnboardingFormProps> = ({
  onSuccess,
  onCancel
}) => {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [availableRoles, setAvailableRoles] = useState<string[]>([]);
  const [formData, setFormData] = useState<AdminOnboardingRequest>({
    name: '',
    email: '',
    registrationNumber: '',
    department: '',
    role: 'ADMIN'
  });

  const { user } = useAuthStore();
  const isNormalAdmin = user?.role === UserRole.ADMIN;

  useEffect(() => {
    if (isNormalAdmin && user?.department) {
      setFormData(prev => ({ ...prev, department: user.department }));
    }
  }, [isNormalAdmin, user]);

  useEffect(() => {
    fetchDepartments();
    fetchRoles();
  }, []);

  const fetchDepartments = async () => {
    try {
      const data = await userManagementService.getDepartments();
      setDepartments(data);
    } catch (error) {
      console.error('Failed to fetch departments:', error);
    }
  };

  const fetchRoles = async () => {
    try {
      const roles = await userManagementService.getRoles();
      // Filter for admin onboarding relevant roles
      setAvailableRoles(roles.filter(role => role === 'ADMIN' || role === 'SUPER_ADMIN'));
    } catch (error) {
      console.error('Failed to fetch roles:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userManagementService.onboardAdmin(formData);
      onSuccess?.(response);
    } catch (error) {
      console.error('Admin onboarding failed:', error);
      // Handle error display (could add toast notifications)
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSelectChange = (value: string, name?: string) => {
    if (name) {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <Card glass>
        <CardHeader>
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-violet-500/15 rounded-xl border border-violet-500/20">
              <Shield className="h-5 w-5 text-violet-400" />
            </div>
            <div>
              <h2 className="text-xl font-semibold text-white tracking-tight">Admin Onboarding</h2>
              <p className="text-gray-400 text-sm">Add a new administrator to the system</p>
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Input
                name="name"
                label="Full Name"
                placeholder="Enter administrator's full name"
                value={formData.name}
                onChange={handleChange}
                icon={<User className="h-4 w-4" />}
                glass
                required
              />

              <Input
                name="email"
                type="email"
                label="Email Address"
                placeholder="admin@example.com"
                value={formData.email}
                onChange={handleChange}
                icon={<Mail className="h-4 w-4" />}
                glass
                required
              />

              <Input
                name="registrationNumber"
                label="Employee / Admin ID"
                placeholder="e.g., ADM001"
                value={formData.registrationNumber}
                onChange={handleChange}
                icon={<Briefcase className="h-4 w-4" />}
                glass
              />

              <Select
                name="department"
                label="Department"
                value={formData.department}
                onChange={handleSelectChange}
                options={departments.map(dept => ({ 
                  value: dept.id, 
                  label: (dept as any).name || dept.label 
                }))}
                placeholder="Select Department"
                disabled={isNormalAdmin}
                required
              />

              <Select
                name="role"
                label="Admin Level"
                value={formData.role}
                onChange={handleSelectChange}
                options={isNormalAdmin 
                  ? [{ value: 'ADMIN', label: 'Administrator' }]
                  : availableRoles.length > 0 
                    ? availableRoles.map(role => ({ 
                        value: role, 
                        label: role === 'SUPER_ADMIN' ? 'Super Administrator' : 'Administrator' 
                      }))
                    : [
                        { value: 'ADMIN', label: 'Administrator' },
                        { value: 'SUPER_ADMIN', label: 'Super Administrator' }
                      ]
                }
                placeholder="Select Admin Level"
                disabled={isNormalAdmin}
                required
              />
            </div>

            {/* Admin Level Description */}
            <div className="p-4 bg-[#05050A] rounded-xl border border-gray-800">
              <h4 className="text-sm font-semibold text-gray-200 mb-3">Admin Level Permissions:</h4>
              <div className="space-y-3 text-sm">
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-violet-500 rounded-full mt-1.5"></div>
                  <div>
                    <p className="text-gray-200 font-medium">Administrator:</p>
                    <p className="text-gray-500 text-xs">Can manage users, rooms, attendance, and view analytics</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-red-500 rounded-full mt-1.5"></div>
                  <div>
                    <p className="text-gray-200 font-medium">Super Administrator:</p>
                    <p className="text-gray-500 text-xs">Full system access including AI configuration and system settings</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-gray-800/50">
              <Button
                type="button"
                variant="glass"
                onClick={onCancel}
                disabled={loading}
              >
                Cancel
              </Button>
              
              <Button
                type="submit"
                loading={loading}
                disabled={loading}
              >
                {loading ? 'Onboarding...' : 'Onboard Admin'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default AdminOnboardingForm;
