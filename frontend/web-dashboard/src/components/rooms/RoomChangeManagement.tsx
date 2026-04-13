'use client';

import React, { useState, useEffect } from 'react';
import { ArrowRightLeft, QrCode, Calendar, Clock, Users, MapPin, AlertTriangle, CheckCircle, Eye, Settings } from 'lucide-react';
import { roomManagementService } from '@/services/roomManagement.service';
import { attendanceService } from '@/services/attendance.service';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { RoomChangeRequest, QRRoomChangeRequest, RoomChangeTransition, WeeklyRoomSwapConfig, RoomListItem } from '@/types/room-management';

interface RoomChangeManagementProps {
  facultyId: string;
  departmentId?: string;
}

interface RoomChangeRequestForm {
  roomId: string;
  sectionId: string;
  scheduledTime: string;
  reason: string;
  notifyStudents: boolean;
}

interface QRScanResult {
  qrData: string;
  roomInfo: {
    roomId: string;
    roomName: string;
    building: string;
    floor: string;
  };
  timestamp: string;
}

export const RoomChangeManagement: React.FC<RoomChangeManagementProps> = ({
  facultyId,
  departmentId
}) => {
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'scheduled' | 'qr' | 'weekly' | 'history'>('scheduled');
  const [availableRooms, setAvailableRooms] = useState<RoomListItem[]>([]);
  const [roomChangeHistory, setRoomChangeHistory] = useState<RoomChangeTransition[]>([]);
  const [weeklySwaps, setWeeklySwaps] = useState<WeeklyRoomSwapConfig[]>([]);
  const [showQRScanner, setShowQRScanner] = useState(false);
  const [showWeeklyForm, setShowWeeklyForm] = useState(false);
  
  const [scheduledForm, setScheduledForm] = useState<RoomChangeRequestForm>({
    roomId: '',
    sectionId: '',
    scheduledTime: '',
    reason: '',
    notifyStudents: true
  });

  const [qrForm, setQrForm] = useState<QRRoomChangeRequest>({
    roomId: '',
    facultyId: facultyId,
    sectionId: '',
    reason: '',
    isEmergency: false,
    isPermanent: false,
    requiresGracePeriod: false,
    notifyStudents: true
  });

  const [weeklyForm, setWeeklyForm] = useState<WeeklyRoomSwapConfig>({
    originalRoomId: '',
    newRoomId: '',
    swapDate: '',
    reason: '',
    notifyStudents: true,
    isActive: true
  });

  useEffect(() => {
    fetchRoomChangeHistory();
    fetchAvailableRooms();
    fetchWeeklySwaps();
  }, [facultyId]);

  const fetchRoomChangeHistory = async () => {
    try {
      const history = await roomManagementService.getRoomChangeHistory();
      // @ts-expect-error omega clearance
      setRoomChangeHistory(history);
    } catch (error) {
      console.error('Failed to fetch room change history:', error);
    }
  };

  const fetchAvailableRooms = async () => {
    try {
      const rooms = await roomManagementService.getAvailableRooms();
      setAvailableRooms(rooms as unknown as RoomListItem[]);
    } catch (error) {
      console.error('Failed to fetch available rooms:', error);
    }
  };

  const fetchWeeklySwaps = async () => {
    try {
      const swaps = await roomManagementService.getWeeklySwaps();
      // @ts-expect-error omega clearance
      setWeeklySwaps(swaps);
    } catch (error) {
      console.error('Failed to fetch weekly swaps:', error);
    }
  };

  const handleScheduledRoomChange = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const request: RoomChangeRequest = {
        roomId: scheduledForm.roomId,
        sectionId: scheduledForm.sectionId,
        scheduledTime: scheduledForm.scheduledTime,
        reason: scheduledForm.reason,
        notifyStudents: scheduledForm.notifyStudents
      };

      const transition = await roomManagementService.requestPrePlannedRoomChange(request);
      
      // Reset form
      setScheduledForm({
        roomId: '',
        sectionId: '',
        scheduledTime: '',
        reason: '',
        notifyStudents: true
      });
      
      await fetchRoomChangeHistory();
    } catch (error) {
      console.error('Scheduled room change failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleQRRoomChange = async (qrData: string) => {
    try {
      // Parse QR data (would contain room information)
      const roomInfo = parseQRData(qrData);
      
      const request: QRRoomChangeRequest = {
        roomId: roomInfo.roomId,
        facultyId: facultyId,
        sectionId: '',
        reason: 'QR-based room change',
        isEmergency: false,
        isPermanent: false,
        requiresGracePeriod: false,
        notifyStudents: true
      };

      const transition = await roomManagementService.requestQRRoomChange(request);
      
      setQrForm({
        roomId: '',
        facultyId: facultyId,
        sectionId: '',
        reason: '',
        isEmergency: false,
        isPermanent: false,
        requiresGracePeriod: false,
        notifyStudents: true
      });
      
      setShowQRScanner(false);
      await fetchRoomChangeHistory();
    } catch (error) {
      console.error('QR room change failed:', error);
    }
  };

  const parseQRData = (qrData: string) => {
    // Parse QR code data to extract room information
    // Format: room:ROOM_ID:ROOM_NAME:BUILDING:FLOOR
    const parts = qrData.split(':');
    if (parts[0] === 'room' && parts.length >= 5) {
      return {
        roomId: parts[1],
        roomName: parts[2],
        building: parts[3],
        floor: parts[4]
      };
    }
    throw new Error('Invalid QR code format');
  };

  const simulateQRScan = () => {
    // Simulate QR scan for demo purposes
    const mockQRData = `room:ROOM_101:Computer Lab 101:Main Building:1st Floor`;
    handleQRRoomChange(mockQRData);
  };

  const handleWeeklySwap = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const swap = await roomManagementService.createWeeklySwapConfig(weeklyForm);
      
      setWeeklyForm({
        originalRoomId: '',
        newRoomId: '',
        swapDate: '',
        reason: '',
        notifyStudents: true,
        isActive: true
      });
      
      setShowWeeklyForm(false);
      await fetchWeeklySwaps();
    } catch (error) {
      console.error('Weekly swap creation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const approveRoomChange = async (transitionId: string) => {
    try {
      await roomManagementService.approveRoomChange(transitionId);
      await fetchRoomChangeHistory();
    } catch (error) {
      console.error('Room change approval failed:', error);
    }
  };

  const denyRoomChange = async (transitionId: string, reason: string) => {
    try {
      await roomManagementService.denyRoomChange(transitionId);
      await fetchRoomChangeHistory();
    } catch (error) {
      console.error('Room change denial failed:', error);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'ACTIVE':
        return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      case 'PENDING':
        return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      default:
        return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle className="h-4 w-4" />;
      case 'ACTIVE':
        return <Clock className="h-4 w-4" />;
      case 'PENDING':
        return <AlertTriangle className="h-4 w-4" />;
      default:
        return <Clock className="h-4 w-4" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card glass>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-blue-500/20 rounded-lg">
                <ArrowRightLeft className="h-5 w-5 text-blue-400" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Room Change Management</h3>
                <p className="text-gray-400 text-sm">
                  Schedule, QR-based, and weekly room switching
                </p>
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Tab Navigation */}
      <div className="flex space-x-1 p-1 bg-gray-800/30 rounded-lg border border-gray-700">
        <Button
          variant={activeTab === 'scheduled' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setActiveTab('scheduled')}
          className="flex-1"
        >
          <Calendar className="h-4 w-4 mr-2" />
          Scheduled
        </Button>
        
        <Button
          variant={activeTab === 'qr' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setActiveTab('qr')}
          className="flex-1"
        >
          <QrCode className="h-4 w-4 mr-2" />
          QR-Based
        </Button>
        
        <Button
          variant={activeTab === 'weekly' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setActiveTab('weekly')}
          className="flex-1"
        >
          <Clock className="h-4 w-4 mr-2" />
          Weekly Swaps
        </Button>
        
        <Button
          variant={activeTab === 'history' ? 'primary' : 'glass'}
          size="sm"
          onClick={() => setActiveTab('history')}
          className="flex-1"
        >
          <Eye className="h-4 w-4 mr-2" />
          History
        </Button>
      </div>

      {/* Tab Content */}
      {activeTab === 'scheduled' && (
        <div className="space-y-6">
          <Card glass>
            <CardHeader>
              <h4 className="text-white font-medium">Schedule Room Change</h4>
              <p className="text-gray-400 text-sm">Plan future room changes with notifications</p>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleScheduledRoomChange} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Target Room
                    </label>
                    <select
                      value={scheduledForm.roomId}
                      onChange={(e) => setScheduledForm(prev => ({ ...prev, roomId: e.target.value }))}
                      className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                      required
                    >
                      <option value="">Select a room</option>
                      {availableRooms.map(room => (
                        <option key={room.roomId} value={room.roomId}>
                          {room.name} - {room.building}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Section
                    </label>
                    <select
                      value={scheduledForm.sectionId}
                      onChange={(e) => setScheduledForm(prev => ({ ...prev, sectionId: e.target.value }))}
                      className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                      required
                    >
                      <option value="">Select a section</option>
                      <option value="cs-a">CS-A</option>
                      <option value="cs-b">CS-B</option>
                      <option value="ee-a">EE-A</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Scheduled Time
                    </label>
                    <Input
                      type="datetime-local"
                      value={scheduledForm.scheduledTime}
                      onChange={(e) => setScheduledForm(prev => ({ ...prev, scheduledTime: e.target.value }))}
                      glass
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Reason
                    </label>
                    <textarea
                      value={scheduledForm.reason}
                      onChange={(e) => setScheduledForm(prev => ({ ...prev, reason: e.target.value }))}
                      className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                      rows={3}
                      placeholder="Reason for room change..."
                      required
                    />
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="notifyStudents"
                    checked={scheduledForm.notifyStudents}
                    onChange={(e) => setScheduledForm(prev => ({ ...prev, notifyStudents: e.target.checked }))}
                    className="rounded border-gray-600 bg-gray-800 text-blue-500 focus:ring-blue-500"
                  />
                  <label htmlFor="notifyStudents" className="text-sm text-gray-300">
                    Notify students via email/app notification
                  </label>
                </div>

                <div className="flex justify-end space-x-3">
                  <Button
                    type="button"
                    variant="glass"
                    onClick={() => setScheduledForm({
                      roomId: '',
                      sectionId: '',
                      scheduledTime: '',
                      reason: '',
                      notifyStudents: true
                    })}
                    disabled={loading}
                  >
                    Clear
                  </Button>
                  
                  <Button
                    type="submit"
                    loading={loading}
                    disabled={loading}
                  >
                    {loading ? 'Scheduling...' : 'Schedule Room Change'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'qr' && (
        <div className="space-y-6">
          <Card glass>
            <CardHeader>
              <h4 className="text-white font-medium">QR-Based Room Change</h4>
              <p className="text-gray-400 text-sm">Scan QR codes for instant room switching</p>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="text-center">
                  <div className="p-8 bg-gray-800/30 rounded-lg border-2 border-dashed border-gray-600">
                    <QrCode className="h-16 w-16 text-gray-500 mx-auto mb-4" />
                    <h4 className="text-xl font-semibold text-white mb-2">QR Room Scanner</h4>
                    <p className="text-gray-400 mb-4">
                      Scan a room QR code to instantly change locations
                    </p>
                    
                    <Button
                      variant="primary"
                      onClick={simulateQRScan}
                      className="mx-auto"
                    >
                      <QrCode className="h-4 w-4 mr-2" />
                      Simulate QR Scan
                    </Button>
                  </div>
                </div>

                <div className="p-4 bg-blue-500/10 rounded-lg border border-blue-500/20">
                  <h5 className="text-blue-400 font-medium mb-2">How it works:</h5>
                  <ul className="text-gray-300 text-sm space-y-1">
                    <li>• Scan QR code displayed in target room</li>
                    <li>• System validates room availability</li>
                    <li>• Automatic room change with notification</li>
                    <li>• Attendance tracking updates automatically</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'weekly' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <h4 className="text-lg font-semibold text-white">Weekly Room Swaps</h4>
            <Button
              variant="primary"
              onClick={() => setShowWeeklyForm(true)}
            >
              <Clock className="h-4 w-4 mr-2" />
              Add Weekly Swap
            </Button>
          </div>

          {showWeeklyForm && (
            <Card glass>
              <CardHeader>
                <h4 className="text-white font-medium">Create Weekly Swap</h4>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleWeeklySwap} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        Source Room
                      </label>
                      <select
                        value={weeklyForm.originalRoomId}
                        onChange={(e) => setWeeklyForm(prev => ({ ...prev, originalRoomId: e.target.value }))}
                        className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                        required
                      >
                        <option value="">Select source room</option>
                        {availableRooms.map(room => (
                          <option key={room.roomId} value={room.roomId}>
                            {room.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        Target Room
                      </label>
                      <select
                        value={weeklyForm.newRoomId}
                        onChange={(e) => setWeeklyForm(prev => ({ ...prev, newRoomId: e.target.value }))}
                        className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                        required
                      >
                        <option value="">Select target room</option>
                        {availableRooms.map(room => (
                          <option key={room.roomId} value={room.roomId}>
                            {room.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        Day of Week
                      </label>
                      <select
                        value={weeklyForm.swapDate}
                        onChange={(e) => setWeeklyForm(prev => ({ ...prev, swapDate: e.target.value }))}
                        className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                        required
                      >
                        <option value="MONDAY">Monday</option>
                        <option value="TUESDAY">Tuesday</option>
                        <option value="WEDNESDAY">Wednesday</option>
                        <option value="THURSDAY">Thursday</option>
                        <option value="FRIDAY">Friday</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        Time Range
                      </label>
                      <div className="grid grid-cols-2 gap-2">
                        <Input
                          type="time"
                          value={weeklyForm.swapDate}
                          onChange={(e) => setWeeklyForm(prev => ({ ...prev, swapDate: e.target.value }))}
                          placeholder="Start time"
                          glass
                          required
                        />
                        <Input
                          type="time"
                          value={weeklyForm.swapDate}
                          onChange={(e) => setWeeklyForm(prev => ({ ...prev, swapDate: e.target.value }))}
                          placeholder="End time"
                          glass
                          required
                        />
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      Reason
                    </label>
                    <textarea
                      value={weeklyForm.reason}
                      onChange={(e) => setWeeklyForm(prev => ({ ...prev, reason: e.target.value }))}
                      className="w-full px-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 glass-input"
                      rows={3}
                      placeholder="Reason for weekly swap..."
                      required
                    />
                  </div>

                  <div className="flex justify-end space-x-3">
                    <Button
                      type="button"
                      variant="glass"
                      onClick={() => setShowWeeklyForm(false)}
                      disabled={loading}
                    >
                      Cancel
                    </Button>
                    
                    <Button
                      type="submit"
                      loading={loading}
                      disabled={loading}
                    >
                      {loading ? 'Creating...' : 'Create Weekly Swap'}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          <div className="space-y-4">
            {weeklySwaps.map((swap, index) => (
              <Card key={index} glass>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h5 className="text-white font-medium">
                        {swap.swapDate}: {swap.originalRoomId} → {swap.newRoomId}
                      </h5>
                      <p className="text-gray-400 text-sm">
                        {swap.swapDate} - {swap.swapDate}
                      </p>
                      <p className="text-gray-500 text-xs mt-1">
                        {swap.reason}
                      </p>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${
                        swap.isActive ? 'bg-green-500/20 text-green-400 border-green-500/30' : 'bg-gray-500/20 text-gray-400 border-gray-500/30'
                      }`}>
                        {swap.isActive ? 'Active' : 'Inactive'}
                      </span>
                      
                      <Button variant="glass" size="sm">
                        <Settings className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'history' && (
        <div className="space-y-4">
          {roomChangeHistory.map((transition) => (
            <Card key={transition.id} glass>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      {getStatusIcon(transition.status)}
                      <span className="text-white font-medium">
                        Room Change: {transition.newRoomId}
                      </span>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium border ${getStatusColor(transition.status)}`}>
                        {transition.status}
                      </span>
                    </div>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-gray-400">Scheduled:</span>
                        <p className="text-white">
                          {new Date(transition.transitionStartTime).toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <span className="text-gray-400">Duration:</span>
                        <p className="text-white">
                          {new Date(transition.transitionEndTime).toLocaleTimeString()}
                        </p>
                      </div>
                      <div>
                        <span className="text-gray-400">Section:</span>
                        <p className="text-white">{transition.sessionId}</p>
                      </div>
                      <div>
                        <span className="text-gray-400">Reason:</span>
                        <p className="text-white">Room transition</p>
                      </div>
                    </div>
                  </div>
                  
                  {transition.status === 'PENDING' && (
                    <div className="flex space-x-2 ml-4">
                      <Button
                        variant="success"
                        size="sm"
                        onClick={() => approveRoomChange(transition.id)}
                      >
                        <CheckCircle className="h-3 w-3" />
                      </Button>
                      
                      <Button
                        variant="error"
                        size="sm"
                        onClick={() => denyRoomChange(transition.id, 'Room not available')}
                      >
                        <AlertTriangle className="h-3 w-3" />
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export default RoomChangeManagement;
