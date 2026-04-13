'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { User, Mail, Phone, Briefcase, BookOpen } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuthStore } from '@/stores/authStore';
import { Role } from '@/types';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { Select } from '@/components/ui/Select';
import { FacultyOnboardingRequest, DropdownDTO } from '@/types/user-management';

interface FacultyOnboardingFormProps {
  onSuccess?: (response: any) => void;
  onCancel?: () => void;
}

export const FacultyOnboardingForm: React.FC<FacultyOnboardingFormProps> = ({
  onSuccess,
  onCancel
}) => {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [formData, setFormData] = useState<FacultyOnboardingRequest>({
    name: '',
    email: '',
    employeeId: '',
    department: '',
    specialization: '',
    mobile: ''
  });

  const { user } = useAuthStore();
  const isAdmin = user?.role === Role.ADMIN;

  // Logic moved to fetchDepartments for better synchronization

  useEffect(() => {
    fetchDepartments();
  }, []);

  const fetchDepartments = async () => {
    try {
      const data = await userManagementService.getDepartments();
      setDepartments(data);
      
      // 🕵️ RELIABLE AUTO-SELECT: If exactly one department is returned for an Admin, select it!
      if (isAdmin && data.length === 1) {
        setFormData(prev => ({ ...prev, department: data[0].id }));
      } else if (isAdmin && user?.department && data.length > 0) {
        // Fallback: Robust name/ID matching if multiple (unlikely for regular admin)
        const matchedDept = data.find(d => 
          d.id === user.department || 
          d.label === user.department || 
          (d as any).name === user.department
        );
        if (matchedDept) {
          setFormData(prev => ({ ...prev, department: matchedDept.id }));
        }
      }
    } catch (error) {
      console.error('Failed to fetch departments:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userManagementService.onboardFaculty(formData);
      onSuccess?.(response);
    } catch (error) {
      console.error('Faculty onboarding failed:', error);
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
              <User className="h-5 w-5 text-violet-400" />
            </div>
            <div>
              <h2 className="text-xl font-semibold text-white tracking-tight">Faculty Onboarding</h2>
              <p className="text-gray-400 text-sm">Add a new faculty member to the system</p>
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Input
                name="name"
                label="Full Name"
                placeholder="Enter faculty member's full name"
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
                placeholder="faculty@example.com"
                value={formData.email}
                onChange={handleChange}
                icon={<Mail className="h-4 w-4" />}
                glass
                required
              />

              <Input
                name="employeeId"
                label="Employee ID"
                placeholder="e.g., EMP001"
                value={formData.employeeId}
                onChange={handleChange}
                icon={<Briefcase className="h-4 w-4" />}
                glass
                required
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
                disabled={isAdmin}
                required
              />

              <Input
                name="specialization"
                label="Specialization"
                placeholder="e.g., Computer Science, Mathematics"
                value={formData.specialization}
                onChange={handleChange}
                icon={<BookOpen className="h-4 w-4" />}
                glass
                required
              />

              <Input
                name="mobile"
                type="tel"
                label="Mobile Number"
                placeholder="+91 9876543210"
                value={formData.mobile}
                onChange={handleChange}
                icon={<Phone className="h-4 w-4" />}
                glass
                required
              />
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
                {loading ? 'Onboarding...' : 'Onboard Faculty'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default FacultyOnboardingForm;
