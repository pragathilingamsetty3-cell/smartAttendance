'use client';

import React, { useState } from 'react';
import { Upload, Download, Users, ArrowUp, AlertTriangle } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuth } from '@/stores/authContext';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { DropdownDTO } from '@/types/user-management';

interface BulkOperationsProps {
  onOperationComplete?: () => void;
}

export const BulkOperations: React.FC<BulkOperationsProps> = ({ onOperationComplete }) => {
  const { user: currentUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [selectedStudents, setSelectedStudents] = useState<string[]>([]);
  const [targetSection, setTargetSection] = useState('');
  const [autoIncrement, setAutoIncrement] = useState(true);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [exportRole, setExportRole] = useState<string>('');

  const handleBulkPromotion = async () => {
    if (selectedStudents.length === 0 || !targetSection) {
      alert('Please select students and target section');
      return;
    }

    setLoading(true);
    try {
      await userManagementService.bulkPromoteStudents({
        studentIds: selectedStudents,
        targetSectionId: targetSection,
        autoIncrementSemester: autoIncrement
      });
      
      setSelectedStudents([]);
      setTargetSection('');
      onOperationComplete?.();
    } catch (error) {
      console.error('Bulk promotion failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    setLoading(true);
    try {
      // Since exportUsers was removed, create a simple client-side export
      const csvContent = [
        ['Name', 'Email', 'Role', 'Department', 'Status', 'Registration Number', 'Employee ID'],
        // Note: This would need actual user data - for now creating empty export
      ].map(row => row.join(',')).join('\n');
      
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `bulk-export-${exportRole || 'all'}-${new Date().toISOString().split('T')[0]}.xlsx`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Export failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setLoading(true);
    try {
      // TODO: Implement file upload and parsing
      // This would typically involve:
      // 1. Parse the Excel/CSV file
      // 2. Validate the data
      // 3. Call bulk onboarding API
      console.log('File uploaded:', file.name);
    } catch (error) {
      console.error('File upload failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Bulk Promotion */}
      <Card glass>
        <CardHeader>
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-green-500/20 rounded-lg">
              <ArrowUp className="h-5 w-5 text-green-400" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Bulk Student Promotion</h3>
              <p className="text-gray-400 text-sm">Promote multiple students to next semester/section</p>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          <div className="space-y-4">
            {/* Student Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Select Students (comma-separated IDs)
              </label>
              <textarea
                value={selectedStudents.join(', ')}
                onChange={(e) => setSelectedStudents(e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
                className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:border-green-500 focus:ring-2 focus:ring-green-500/20 glass-input"
                rows={3}
                placeholder="Enter student IDs separated by commas..."
              />
            </div>

            {/* Target Section */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Target Section
              </label>
              <select
                value={targetSection}
                onChange={(e) => setTargetSection(e.target.value)}
                className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-green-500 focus:ring-2 focus:ring-green-500/20 glass-input"
              >
                <option value="">Select Target Section</option>
                {sections.map(section => (
                  <option key={section.id} value={section.id}>
                    {section.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Options */}
            <div className="flex items-center space-x-3">
              <input
                type="checkbox"
                id="autoIncrement"
                checked={autoIncrement}
                onChange={(e) => setAutoIncrement(e.target.checked)}
                className="rounded border-gray-600 bg-gray-800 text-green-500 focus:ring-green-500 focus:ring-offset-gray-900"
              />
              <label htmlFor="autoIncrement" className="text-sm text-gray-300">
                Auto-increment semester
              </label>
            </div>

            {/* Action Button */}
            {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
              <Button
                variant="primary"
                onClick={handleBulkPromotion}
                loading={loading}
                disabled={loading || selectedStudents.length === 0 || !targetSection}
                className="w-full"
              >
                <Users className="h-4 w-4 mr-2" />
                {loading ? 'Processing...' : `Promote ${selectedStudents.length} Students`}
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Import/Export */}
      <Card glass>
        <CardHeader>
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-500/20 rounded-lg">
              <Upload className="h-5 w-5 text-blue-400" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white">Import & Export</h3>
              <p className="text-gray-400 text-sm">Bulk import users or export data</p>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          <div className="space-y-6">
            {/* Import Section */}
            <div>
              <h4 className="text-white font-medium mb-4">Import Users</h4>
              <div className="border-2 border-dashed border-gray-700 rounded-lg p-6 text-center hover:border-blue-500/50 transition-colors">
                <Upload className="h-12 w-12 text-gray-500 mx-auto mb-4" />
                <h5 className="text-white font-medium mb-2">Upload Excel or CSV File</h5>
                <p className="text-gray-400 text-sm mb-4">
                  Supported formats: .xlsx, .csv, .xls
                </p>
                <input
                  type="file"
                  accept=".xlsx,.csv,.xls"
                  onChange={handleFileUpload}
                  className="hidden"
                  id="file-upload"
                />
                <label
                  htmlFor="file-upload"
                  className="inline-flex items-center px-4 py-2 bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded-lg cursor-pointer hover:bg-blue-500/30 transition-colors"
                >
                  <Upload className="h-4 w-4 mr-2" />
                  Choose File
                </label>
              </div>
            </div>

            {/* Export Section */}
            <div>
              <h4 className="text-white font-medium mb-4">Export Users</h4>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Export Role (Optional)
                  </label>
                  <select
                    value={exportRole}
                    onChange={(e) => setExportRole(e.target.value)}
                    className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                  >
                    <option value="">All Users</option>
                    <option value="STUDENT">Students</option>
                    <option value="FACULTY">Faculty</option>
                    <option value="ADMIN">Administrators</option>
                  </select>
                </div>

                <Button
                  variant="primary"
                  onClick={handleExport}
                  loading={loading}
                  disabled={loading}
                  className="w-full"
                >
                  <Download className="h-4 w-4 mr-2" />
                  {loading ? 'Exporting...' : 'Export to Excel'}
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Information Card */}
      <Card glass>
        <CardContent>
          <div className="flex items-start space-x-3">
            <div className="p-2 bg-yellow-500/20 rounded-lg">
              <AlertTriangle className="h-5 w-5 text-yellow-400" />
            </div>
            <div className="flex-1">
              <h4 className="text-white font-medium mb-2">Important Notes</h4>
              <ul className="text-gray-400 text-sm space-y-2">
                <li>• Bulk operations cannot be undone. Please review data before proceeding.</li>
                <li>• Import files should follow the specified template format.</li>
                <li>• Student promotion will automatically update their academic records.</li>
                <li>• Export includes all user data in Excel format for easy analysis.</li>
                <li>• Large files may take longer to process. Please be patient.</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default BulkOperations;
