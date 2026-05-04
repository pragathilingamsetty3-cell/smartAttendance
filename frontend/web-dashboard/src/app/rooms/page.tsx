'use client';

import React, { useState, useEffect } from 'react';
import { useAuth } from '@/stores/authContext';
import { RoomCreationForm } from '@/components/rooms/RoomCreationForm';
import { RoomViewModal, RoomEditModal, RoomDeleteModal } from '@/components/rooms/RoomModals';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Building, Plus, Map, Eye, Edit, Trash2, Users, MapPin } from 'lucide-react';
import { roomManagementService } from '@/services/roomManagement.service';
import { Loading } from '@/components/ui/Loading';
import { RoomListItem } from '@/types/room-management';

type ViewMode = 'list' | 'create';

function RoomsContent() {
  const { hasRole, isLoading } = useAuth();
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [loading, setLoading] = useState(false);
  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedRoom, setSelectedRoom] = useState<RoomListItem | null>(null);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  useEffect(() => {
    if (viewMode === 'list' && !isLoading && hasRole(['ADMIN', 'SUPER_ADMIN', 'FACULTY'])) {
      fetchRooms();
    }
  }, [viewMode, isLoading]);

  const fetchRooms = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await roomManagementService.getAllRooms();
      if (data && data.rooms) {
        setRooms(data.rooms);
      } else if (Array.isArray(data)) {
        setRooms(data as unknown as RoomListItem[]);
      } else {
        setRooms([]);
      }
    } catch (err: any) {
      console.error('Failed to fetch rooms:', err);
      setError(err.message || 'Failed to load rooms');
    } finally {
      setLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loading size="lg" text="Authenticating..." />
      </div>
    );
  }

  if (!hasRole(['ADMIN', 'SUPER_ADMIN', 'FACULTY'])) {
    return (
      <div className="text-center py-12 bg-white rounded-3xl border border-slate-200 shadow-xl shadow-sky-900/5 max-w-lg mx-auto">
        <Building className="h-16 w-16 text-slate-200 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-slate-900 mb-2">Access Denied</h2>
        <p className="text-slate-500">You don't have permission to access room management.</p>
      </div>
    );
  }

  const renderContent = () => {
    if (viewMode === 'create') {
      return (
        <RoomCreationForm
          onSuccess={() => setViewMode('list')}
          onCancel={() => setViewMode('list')}
        />
      );
    }

    return (
      <div className="space-y-6">
        <RoomViewModal 
          isOpen={isViewModalOpen} 
          onClose={() => setIsViewModalOpen(false)} 
          room={selectedRoom} 
        />
        <RoomEditModal 
          isOpen={isEditModalOpen} 
          onClose={() => setIsEditModalOpen(false)} 
          room={selectedRoom} 
          onSuccess={fetchRooms} 
        />
        <RoomDeleteModal 
          isOpen={isDeleteModalOpen} 
          onClose={() => setIsDeleteModalOpen(false)} 
          room={selectedRoom} 
          onSuccess={fetchRooms} 
        />

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-900 flex items-center">
              <Building className="h-6 w-6 mr-3 text-primary" /> Room Management
            </h1>
            <p className="text-slate-500">Manage classrooms and their boundaries</p>
          </div>
          {hasRole(['ADMIN', 'SUPER_ADMIN']) && (
            <Button variant="primary" onClick={() => setViewMode('create')}>
              <Plus className="h-4 w-4 mr-2" /> Create Room
            </Button>
          )}
        </div>

        {/* Room Statistics */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <Card glass>
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-blue-500/10 rounded-lg"><Building className="h-5 w-5 text-blue-500" /></div>
                <span className="text-2xl font-bold text-slate-900">{rooms.length}</span>
              </div>
              <p className="text-slate-500 text-sm">Total Rooms</p>
            </CardContent>
          </Card>
          <Card glass>
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-green-500/10 rounded-lg"><Map className="h-5 w-5 text-green-500" /></div>
                <span className="text-2xl font-bold text-slate-900">{rooms.filter(r => r.hasBoundary).length}</span>
              </div>
              <p className="text-slate-500 text-sm">With Boundaries</p>
            </CardContent>
          </Card>
          <Card glass>
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-purple-500/10 rounded-lg"><Users className="h-5 w-5 text-purple-500" /></div>
                <span className="text-2xl font-bold text-slate-900">{rooms.reduce((sum, r) => sum + r.capacity, 0)}</span>
              </div>
              <p className="text-slate-500 text-sm">Total Capacity</p>
            </CardContent>
          </Card>
          <Card glass>
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="p-2 bg-orange-500/10 rounded-lg"><MapPin className="h-5 w-5 text-orange-500" /></div>
                <span className="text-2xl font-bold text-slate-900">{new Set(rooms.map(r => r.building)).size}</span>
              </div>
              <p className="text-slate-500 text-sm">Buildings</p>
            </CardContent>
          </Card>
        </div>

        {/* Room List */}
        <Card glass>
          <CardContent>
            <div className="space-y-4">
              {loading ? (
                <div className="text-center py-12"><Loading size="lg" text="Loading rooms..." /></div>
              ) : rooms.length === 0 ? (
                <div className="text-center py-12">
                  <Building className="h-16 w-16 text-slate-200 mx-auto mb-4" />
                  <h4 className="text-xl font-semibold text-slate-900 mb-2">No rooms yet</h4>
                  <p className="text-slate-500 mb-4">Create your first room to get started</p>
                  {hasRole(['ADMIN', 'SUPER_ADMIN']) && (
                    <Button variant="primary" onClick={() => setViewMode('create')}>
                      <Plus className="h-4 w-4 mr-2" /> Create First Room
                    </Button>
                  )}
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {rooms.map((room) => (
                    <div key={room.roomId} className="p-6 bg-white rounded-3xl border border-slate-200 hover:border-primary/30 transition-all shadow-sm hover:shadow-xl hover:shadow-sky-900/5 group">
                      <div className="flex items-start justify-between mb-4">
                        <div className={`p-2 rounded-xl ${room.hasBoundary ? 'bg-green-500/10' : 'bg-slate-100'}`}>
                          <Building className={`h-5 w-5 ${room.hasBoundary ? 'text-green-500' : 'text-slate-400'}`} />
                        </div>
                        <div className="flex space-x-2">
                          <Button variant="glass" size="sm" onClick={() => { setSelectedRoom(room); setIsViewModalOpen(true); }}>
                            <Eye className="h-3 w-3" />
                          </Button>
                          {hasRole(['ADMIN', 'SUPER_ADMIN']) && (
                            <>
                              <Button variant="glass" size="sm" onClick={() => { setSelectedRoom(room); setIsEditModalOpen(true); }}>
                                <Edit className="h-3 w-3" />
                              </Button>
                              <Button variant="glass" size="sm" onClick={() => { setSelectedRoom(room); setIsDeleteModalOpen(true); }}>
                                <Trash2 className="h-3 w-3" />
                              </Button>
                            </>
                          )}
                        </div>
                      </div>
                      <h4 className="text-slate-900 font-bold text-lg mb-2 group-hover:text-primary transition-colors">{room.name}</h4>
                      <div className="space-y-2 text-sm">
                        <div className="flex items-center justify-between"><span className="text-slate-500">Building:</span><span className="text-slate-700 font-medium">{room.building}</span></div>
                        <div className="flex items-center justify-between"><span className="text-slate-500">Floor:</span><span className="text-slate-700 font-medium">{room.floor}</span></div>
                        <div className="flex items-center justify-between"><span className="text-slate-500">Capacity:</span><span className="text-slate-700 font-medium">{room.capacity}</span></div>
                        <div className="flex items-center justify-between"><span className="text-slate-500">Boundary:</span><span className={room.hasBoundary ? 'text-green-600 font-bold' : 'text-slate-400 italic'}>{room.hasBoundary ? room.boundaryType : 'None'}</span></div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  };

  return <>{renderContent()}</>;
}

export default function RoomsPage() {
  return <RoomsContent />;
}
