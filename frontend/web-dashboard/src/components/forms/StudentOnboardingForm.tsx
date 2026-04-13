'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { User, Mail, Phone, Calendar, MapPin, Users, BookOpen } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuthStore } from '@/stores/authStore';
import { Role } from '@/types';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { Select } from '@/components/ui/Select';
import { StudentOnboardingRequest, DropdownDTO } from '@/types/user-management';

interface StudentOnboardingFormProps {
  onSuccess?: (response: any) => void;
  onCancel?: () => void;
}

export const StudentOnboardingForm: React.FC<StudentOnboardingFormProps> = ({
  onSuccess,
  onCancel
}) => {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [availableSemesters, setAvailableSemesters] = useState<string[]>([]);
  const [availableAcademicYears, setAvailableAcademicYears] = useState<string[]>([]);
  const [formData, setFormData] = useState<StudentOnboardingRequest>({
    name: '',
    email: '',
    registrationNumber: '',
    sectionId: '',
    department: '',
    parentEmail: '',
    parentMobile: '',
    studentMobile: '',
    totalAcademicYears: '4',
    semester: 1
  });

  const { user } = useAuthStore();
  const isAdmin = user?.role === Role.ADMIN;

  // Logic moved to fetchDepartments for better synchronization

  useEffect(() => {
    fetchDepartments();
    fetchMetadata();
  }, []);

  useEffect(() => {
    if (formData.department) {
      fetchSections(formData.department);
    }
  }, [formData.department]);

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

  const fetchMetadata = async () => {
    try {
      const [semesters, years] = await Promise.all([
        userManagementService.getSemesters(),
        userManagementService.getAcademicYears()
      ]);
      setAvailableSemesters(semesters);
      setAvailableAcademicYears(years);
      
      // Update form data with first available values if not set
      if (years.length > 0 && !formData.totalAcademicYears) {
        setFormData(prev => ({ ...prev, totalAcademicYears: years[0] }));
      }
    } catch (error) {
      console.error('Failed to fetch metadata:', error);
    }
  };

  const fetchSections = async (departmentId: string) => {
    try {
      const data = await userManagementService.getSections(departmentId);
      setSections(data);
    } catch (error) {
      console.error('Failed to fetch sections:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await userManagementService.onboardStudent(formData);
      onSuccess?.(response);
    } catch (error) {
      console.error('Student onboarding failed:', error);
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
    <div className="max-w-4xl mx-auto">
      <Card glass>
        <CardHeader>
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-violet-500/15 rounded-xl border border-violet-500/20">
              <User className="h-5 w-5 text-violet-400" />
            </div>
            <div>
              <h2 className="text-xl font-semibold text-white tracking-tight">Student Onboarding</h2>
              <p className="text-gray-400 text-sm">Add a new student to the system</p>
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* Basic Information */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-white tracking-tight flex items-center">
                <User className="h-4 w-4 mr-2 text-violet-400" />
                Basic Information
              </h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Input
                  name="name"
                  label="Full Name"
                  placeholder="Enter student's full name"
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
                  placeholder="student@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  icon={<Mail className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="registrationNumber"
                  label="Registration Number"
                  placeholder="e.g., 2024CS001"
                  value={formData.registrationNumber}
                  onChange={handleChange}
                  icon={<BookOpen className="h-4 w-4" />}
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

                <Select
                  name="sectionId"
                  label="Section"
                  value={formData.sectionId}
                  onChange={handleSelectChange}
                  options={sections.map(section => ({ 
                    value: section.id, 
                    label: (section as any).name || section.label 
                  }))}
                  placeholder="Select Section"
                  disabled={!formData.department}
                  required
                />

                {availableAcademicYears.length > 0 ? (
                    <Select
                      name="totalAcademicYears"
                      label="Academic Year"
                      value={formData.totalAcademicYears}
                      onChange={handleSelectChange}
                      options={availableAcademicYears.map(year => ({ value: year, label: year }))}
                      placeholder="Select or type Academic Year"
                      creatable
                      required
                    />
                ) : (
                  <Input
                    name="totalAcademicYears"
                    label="Total Academic Years"
                    placeholder="e.g., 4"
                    value={formData.totalAcademicYears}
                    onChange={handleChange}
                    icon={<Calendar className="h-4 w-4" />}
                    glass
                    required
                  />
                )}

                <Select
                  name="semester"
                  label="Current Semester"
                  value={formData.semester?.toString() || ""}
                  onChange={(val) => handleSelectChange(val, 'semester')}
                  options={availableSemesters.length > 0 
                    ? availableSemesters.map(sem => ({ value: sem, label: `Semester ${sem}` }))
                    : [1, 2, 3, 4, 5, 6, 7, 8].map(sem => ({ value: sem.toString(), label: `Semester ${sem}` }))
                  }
                  placeholder="Select Semester"
                  required
                />
              </div>
            </div>

            {/* Parent/Guardian Information */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-white flex items-center">
                <Users className="h-4 w-4 mr-2 text-violet-400" />
                Parent/Guardian Information
              </h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Input
                  name="parentEmail"
                  type="email"
                  label="Parent Email"
                  placeholder="parent@example.com"
                  value={formData.parentEmail}
                  onChange={handleChange}
                  icon={<Mail className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="parentMobile"
                  type="tel"
                  label="Parent Mobile"
                  placeholder="+91 9876543210"
                  value={formData.parentMobile}
                  onChange={handleChange}
                  icon={<Phone className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="studentMobile"
                  type="tel"
                  label="Student Mobile"
                  placeholder="+91 9876543210"
                  value={formData.studentMobile}
                  onChange={handleChange}
                  icon={<Phone className="h-4 w-4" />}
                  glass
                />
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
                {loading ? 'Onboarding...' : 'Onboard Student'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default StudentOnboardingForm;
