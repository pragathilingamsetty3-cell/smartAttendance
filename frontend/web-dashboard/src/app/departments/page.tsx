'use client';

import React from 'react';
import { useAuth } from '@/stores/authContext';
import { DepartmentSectionManagement } from '@/components/users/DepartmentSectionManagement';
import { Loading } from '@/components/ui/Loading';
import { Building } from 'lucide-react';

function DepartmentsContent() {
  const { hasRole, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loading size="lg" text="Authenticating..." />
      </div>
    );
  }

  // Only ADMIN and SUPER_ADMIN can access departments
  if (!hasRole(['ADMIN', 'SUPER_ADMIN'])) {

    return (
      <div className="text-center py-12">
        <Building className="h-16 w-16 text-gray-500 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-white mb-2">Access Denied</h2>
        <p className="text-gray-400">
          You don't have permission to access department management.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold gradient-text flex items-center">
            <Building className="h-6 w-6 mr-3" />
            Department & Section Management
          </h1>
          <p className="text-gray-400">
            Manage academic departments and sections
          </p>
        </div>
      </div>

      {/* Content */}
      <DepartmentSectionManagement />
    </div>
  );
}

export default function DepartmentsPage() {
  return <DepartmentsContent />;
}

