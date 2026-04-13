'use client';

import React, { useState } from 'react';
import { ToggleLeft, ToggleRight, AlertTriangle, CheckCircle, XCircle, Clock } from 'lucide-react';
import { userManagementService } from '@/services/userManagement.service';
import { useAuth } from '@/stores/authContext';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { UpdateUserStatusRequest } from '@/types/user-management';

interface UserStatusManagementProps {
  userId: string;
  currentStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  userName: string;
  onStatusUpdate?: () => void;
}

export const UserStatusManagement: React.FC<UserStatusManagementProps> = ({
  userId,
  currentStatus,
  userName,
  onStatusUpdate
}) => {
  const { user: currentUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [newStatus, setNewStatus] = useState<'ACTIVE' | 'INACTIVE' | 'SUSPENDED'>(currentStatus);
  const [reason, setReason] = useState('');

  const handleStatusUpdate = async () => {
    setLoading(true);
    try {
      await userManagementService.updateUserStatus(
        userId,
        { status: newStatus, reason }
      );
      setShowStatusModal(false);
      onStatusUpdate?.();
    } catch (error) {
      console.error('Failed to update user status:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <CheckCircle className="h-4 w-4 text-green-400" />;
      case 'INACTIVE':
        return <XCircle className="h-4 w-4 text-red-400" />;
      case 'SUSPENDED':
        return <Clock className="h-4 w-4 text-yellow-400" />;
      default:
        return <AlertTriangle className="h-4 w-4 text-gray-400" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'INACTIVE':
        return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'SUSPENDED':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'Active';
      case 'INACTIVE':
        return 'Inactive';
      case 'SUSPENDED':
        return 'Suspended';
      default:
        return 'Unknown';
    }
  };

  const getNextStatus = (current: string) => {
    switch (current) {
      case 'ACTIVE':
        return 'INACTIVE';
      case 'INACTIVE':
        return 'ACTIVE';
      case 'SUSPENDED':
        return 'ACTIVE';
      default:
        return 'ACTIVE';
    }
  };

  return (
    <>
      {/* Status Toggle */}
      <div className="flex items-center space-x-3">
        <span className="text-sm text-gray-400">Status:</span>
        <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(currentStatus)}`}>
          {getStatusIcon(currentStatus)}
          <span>{getStatusText(currentStatus)}</span>
        </div>
        {(currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPER_ADMIN') && (
          <Button
            variant="glass"
            size="sm"
            onClick={() => {
              setNewStatus(getNextStatus(currentStatus) as 'ACTIVE' | 'INACTIVE' | 'SUSPENDED');
              setShowStatusModal(true);
            }}
          >
            {currentStatus === 'ACTIVE' ? (
              <ToggleLeft className="h-4 w-4" />
            ) : (
              <ToggleRight className="h-4 w-4" />
            )}
            Change Status
          </Button>
        )}
      </div>

      {/* Status Update Modal */}
      {showStatusModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <Card glass className="w-full max-w-md">
            <CardHeader>
              <h3 className="text-lg font-semibold text-white">
                Update User Status
              </h3>
              <p className="text-gray-400 text-sm">
                Change status for {userName}
              </p>
            </CardHeader>

            <CardContent>
              <div className="space-y-4">
                {/* Current Status */}
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Current Status
                  </label>
                  <div className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm border ${getStatusColor(currentStatus)}`}>
                    {getStatusIcon(currentStatus)}
                    <span>{getStatusText(currentStatus)}</span>
                  </div>
                </div>

                {/* New Status */}
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    New Status
                  </label>
                  <select
                    value={newStatus}
                    onChange={(e) => setNewStatus(e.target.value as 'ACTIVE' | 'INACTIVE' | 'SUSPENDED')}
                    className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="SUSPENDED">Suspended</option>
                  </select>
                </div>

                {/* Status Description */}
                <div className="p-3 bg-gray-800/30 rounded-lg border border-gray-700">
                  <h4 className="text-white font-medium mb-2">Status Meanings:</h4>
                  <ul className="text-gray-400 text-sm space-y-1">
                    <li>• <span className="text-green-400">Active:</span> User can access all features</li>
                    <li>• <span className="text-red-400">Inactive:</span> User cannot login or access system</li>
                    <li>• <span className="text-yellow-400">Suspended:</span> Temporary restriction with limited access</li>
                  </ul>
                </div>

                {/* Reason */}
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    Reason for Change <span className="text-red-400">*</span>
                  </label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                    rows={3}
                    placeholder="Please provide a reason for this status change..."
                    required
                  />
                </div>

                {/* Warning */}
                {newStatus === 'INACTIVE' && (
                  <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
                    <div className="flex items-start space-x-2">
                      <AlertTriangle className="h-4 w-4 text-red-400 mt-0.5" />
                      <div>
                        <p className="text-red-400 text-sm font-medium">Warning</p>
                        <p className="text-red-300 text-xs">
                          Inactive users will lose all system access immediately. This action can be reversed later.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {/* Actions */}
                <div className="flex justify-end space-x-3 pt-4">
                  <Button
                    variant="glass"
                    onClick={() => {
                      setShowStatusModal(false);
                      setReason('');
                    }}
                    disabled={loading}
                  >
                    Cancel
                  </Button>
                  <Button
                    variant={newStatus === 'INACTIVE' ? 'error' : 'primary'}
                    onClick={handleStatusUpdate}
                    loading={loading}
                    disabled={loading || !reason.trim()}
                  >
                    {loading ? 'Updating...' : 'Update Status'}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
};

export default UserStatusManagement;
