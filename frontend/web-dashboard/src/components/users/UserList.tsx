'use client';

import React, { useState, useEffect } from 'react';
import { Search, Filter, Download, UserPlus, Edit, Trash2, Eye, MoreVertical, Users, Activity } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuth } from '@/stores/authContext';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { UserListResponse, UserListItem } from '@/types/user-management';

interface UserListProps {
  role?: 'STUDENT' | 'FACULTY' | 'ADMIN' | 'SUPER_ADMIN';
  department?: string;
  onEditUser?: (user: UserListItem) => void;
  onViewUser?: (user: UserListItem) => void;
}

export const UserList: React.FC<UserListProps> = ({ role, department, onEditUser, onViewUser }) => {
  const { user: currentUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<UserListItem[]>([]);
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 0
  });
  const [filters, setFilters] = useState({
    search: '',
    status: '',
    department: department || ''
  });
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, [pagination.page, filters, role, department]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await userManagementService.getAllUsers(
        pagination.page,
        pagination.limit
      );
      
      // @ts-expect-error omega clearance
      setUsers(response.users);
      setPagination(prev => ({
        ...prev,
        // @ts-expect-error omega clearance
        total: response.total,
        // @ts-expect-error omega clearance
        totalPages: Math.ceil(response.total / prev.limit)
      }));
    } catch (error) {
      console.error('Failed to fetch users:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (query: string) => {
    setFilters(prev => ({ ...prev, search: query }));
    setPagination(prev => ({ ...prev, page: 1 }));
  };

  const handleFilter = (key: string, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPagination(prev => ({ ...prev, page: 1 }));
  };

  const handleExport = async () => {
    try {
      // Since exportUsers was removed, create a simple client-side export
      const csvContent = [
        ['Name', 'Email', 'Role', 'Department', 'Status', 'Registration Number', 'Employee ID'],
        ...users.map(user => [
          user.name,
          user.email,
          user.role,
          user.department,
          user.status,
          user.registrationNumber || '',
          user.employeeId || ''
        ])
      ].map(row => row.join(',')).join('\n');
      
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `users-${role || 'all'}-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Export failed:', error);
    }
  };

  const getStatusBadge = (status: string) => {
    const statusStyles = {
      ACTIVE: 'bg-green-500/20 text-green-400 border-green-500/30',
      INACTIVE: 'bg-red-500/20 text-red-400 border-red-500/30',
      PENDING: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30'
    };

    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${statusStyles[status as keyof typeof statusStyles] || statusStyles.PENDING}`}>
        {status}
      </span>
    );
  };

  const getRoleBadge = (userRole: string) => {
    const roleStyles = {
      STUDENT: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
      FACULTY: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
      ADMIN: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
      SUPER_ADMIN: 'bg-red-500/20 text-red-400 border-red-500/30'
    };

    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${roleStyles[userRole as keyof typeof roleStyles] || roleStyles.STUDENT}`}>
        {userRole.replace('_', ' ')}
      </span>
    );
  };

  const handlePageChange = (page: number) => {
    setPagination(prev => ({ ...prev, page }));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card glass>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h2 className="text-xl font-bold text-white flex items-center">
                <Users className="h-5 w-5 mr-2" />
                {role ? `${role.replace('_', ' ')}s` : 'All Users'}
              </h2>
              <p className="text-gray-400 text-sm">
                {pagination.total} total users
              </p>
            </div>
            
            <div className="flex items-center space-x-3">
              <Button
                variant="glass"
                size="sm"
                onClick={() => setShowFilters(!showFilters)}
              >
                <Filter className="h-4 w-4" />
                Filters
              </Button>
              
              {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
                <Button
                  variant="primary"
                  size="sm"
                  className="bg-emerald-600 hover:bg-emerald-500 border-none shadow-lg shadow-emerald-500/20 active:scale-95 transition-transform"
                  onClick={handleExport}
                >
                  <Download className="h-4 w-4" />
                  Download Excel
                </Button>
              )}
              
              {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
                <Button
                  variant="primary"
                  size="sm"
                >
                  <UserPlus className="h-4 w-4" />
                  Add User
                </Button>
              )}
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          {/* Search and Filters */}
          <div className="space-y-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search users by name, email, or ID..."
                value={filters.search}
                onChange={(e) => handleSearch(e.target.value)}
                className="pl-10"
                glass
              />
            </div>

            {showFilters && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Status
                  </label>
                  <select
                    value={filters.status}
                    onChange={(e) => handleFilter('status', e.target.value)}
                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                  >
                    <option value="">All Status</option>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="PENDING">Pending</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Department
                  </label>
                  <select
                    value={filters.department}
                    onChange={(e) => handleFilter('department', e.target.value)}
                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                  >
                    <option value="">All Departments</option>
                    <option value="cs">Computer Science</option>
                    <option value="ee">Electrical Engineering</option>
                    <option value="me">Mechanical Engineering</option>
                  </select>
                </div>

                <div className="flex items-end">
                  <Button
                    variant="glass"
                    onClick={() => setFilters({ search: '', status: '', department: '' })}
                    className="w-full"
                  >
                    Clear Filters
                  </Button>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Users Table */}
      <Card glass>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-8">
              <Loading size="lg" text="Loading users..." />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-700">
                    <th className="px-6 py-4 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                      User
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                      Role
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                      Department
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-4 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                      Last Active
                    </th>
                    <th className="px-6 py-4 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700">
                  {users.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-800/30 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="h-10 w-10 flex-shrink-0">
                            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center">
                              <span className="text-white font-medium">
                                {user.name.charAt(0).toUpperCase()}
                              </span>
                            </div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-white">
                              {user.name}
                            </div>
                            <div className="text-sm text-gray-400">
                              {user.email}
                            </div>
                            {user.registrationNumber && (
                              <div className="text-xs text-gray-500">
                                ID: {user.registrationNumber}
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getRoleBadge(user.role)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                        {user.department || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(user.status)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                        N/A
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end space-x-2">
                          <Button
                            variant="glass"
                            size="sm"
                            onClick={() => onViewUser?.(user)}
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
                            <Button
                              variant="glass"
                              size="sm"
                              onClick={() => onEditUser?.(user)}
                            >
                              <Edit className="h-4 w-4" />
                            </Button>
                          )}
                          {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
                            <Button
                              variant="glass"
                              size="sm"
                            >
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {users.length === 0 && !loading && (
                <div className="text-center py-12">
                  <Users className="h-12 w-12 text-gray-500 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-white mb-2">No users found</h3>
                  <p className="text-gray-400">
                    {filters.search || filters.status || filters.department
                      ? 'Try adjusting your filters'
                      : 'Get started by adding your first user'}
                  </p>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-400">
            Showing {((pagination.page - 1) * pagination.limit) + 1} to{' '}
            {Math.min(pagination.page * pagination.limit, pagination.total)} of{' '}
            {pagination.total} results
          </div>
          
          <div className="flex items-center space-x-2">
            <Button
              variant="glass"
              size="sm"
              disabled={pagination.page === 1}
              onClick={() => handlePageChange(pagination.page - 1)}
            >
              Previous
            </Button>
            
            <div className="flex items-center space-x-1">
              {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                const page = i + 1;
                return (
                  <Button
                    key={page}
                    variant={page === pagination.page ? 'primary' : 'glass'}
                    size="sm"
                    onClick={() => handlePageChange(page)}
                  >
                    {page}
                  </Button>
                );
              })}
            </div>
            
            <Button
              variant="glass"
              size="sm"
              disabled={pagination.page === pagination.totalPages}
              onClick={() => handlePageChange(pagination.page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserList;
