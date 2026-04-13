'use client';

import React, { useState, useEffect } from 'react';
import { X, Save, Trash2, Building, Users, MapPin, AlertTriangle, CheckCircle, Info } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/Button';
import { safeParseInt } from '@/utils/numberUtils';
import { Input } from '@/components/ui/Input';
import { roomManagementService } from '@/services/roomManagement.service';
import { RoomListItem, Room } from '@/types/room-management';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
}

// --- VIEW MODAL ---
interface ViewModalProps extends ModalProps {
  room: RoomListItem | null;
}

export const RoomViewModal: React.FC<ViewModalProps> = ({ isOpen, onClose, room }) => {
  const [details, setDetails] = useState<Room | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen && room) {
      fetchDetails();
    }
  }, [isOpen, room]);

  const fetchDetails = async () => {
    if (!room) return;
    setLoading(true);
    try {
      const data = await roomManagementService.getRoomById(room.roomId);
      setDetails(data);
    } catch (err) {
      console.error('Failed to fetch room details');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !room) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          onClick={onClose} className="absolute inset-0 bg-black/60 backdrop-blur-sm" 
        />
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-lg bg-[#0F0F16] border border-white/10 rounded-3xl overflow-hidden shadow-2xl"
        >
          <div className="px-8 py-6 border-b border-white/5 flex items-center justify-between bg-blue-500/5">
            <h2 className="text-xl font-bold text-white flex items-center gap-3">
              <Info className="text-blue-400" /> Room Intelligence
            </h2>
            <button onClick={onClose} className="p-2 hover:bg-white/5 rounded-xl text-slate-400"><X size={20} /></button>
          </div>

          <div className="p-8 space-y-6">
            <div className="flex items-center gap-4 p-4 bg-white/5 rounded-2xl border border-white/5">
              <div className="p-3 bg-blue-500/20 rounded-xl">
                <Building className="text-blue-400" size={24} />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">{room.name}</h3>
                <p className="text-slate-400 text-sm">{room.building}, Floor {room.floor}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-white/[0.02] border border-white/5 rounded-2xl">
                <p className="text-xs font-black uppercase tracking-widest text-slate-500 mb-1">Capacity</p>
                <div className="flex items-center gap-2 text-white font-bold text-lg">
                  <Users size={18} className="text-purple-400" /> {room.capacity} students
                </div>
              </div>
              <div className="p-4 bg-white/[0.02] border border-white/5 rounded-2xl">
                <p className="text-xs font-black uppercase tracking-widest text-slate-500 mb-1">Boundary</p>
                <div className="flex items-center gap-2 text-white font-bold">
                  <MapPin size={18} className={room.hasBoundary ? 'text-green-400' : 'text-slate-500'} />
                  <span className={room.hasBoundary ? 'text-green-400' : 'text-slate-500'}>
                    {room.hasBoundary ? room.boundaryType : 'Not Set'}
                  </span>
                </div>
              </div>
            </div>

            {details?.description && (
              <div className="space-y-2">
                <p className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Asset Description</p>
                <p className="text-slate-300 text-sm leading-relaxed p-4 bg-white/[0.03] rounded-2xl border border-white/5 italic">
                  "{details.description}"
                </p>
              </div>
            )}

            <div className="pt-4 flex justify-end">
              <Button variant="secondary" onClick={onClose} className="px-8">Close</Button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

// --- EDIT MODAL ---
interface EditModalProps extends ModalProps {
  room: RoomListItem | null;
  onSuccess: () => void;
}

export const RoomEditModal: React.FC<EditModalProps> = ({ isOpen, onClose, room, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    capacity: 0,
    building: '',
    floor: 0,
    description: '',
    sensorUrl: '',
    boundaryPoints: []
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && room) {
      setFormData({
        name: room.name,
        capacity: room.capacity,
        building: room.building,
        floor: room.floor,
        description: room.description || '',
        sensorUrl: '', // Will be fetched if needed
        boundaryPoints: []
      });
      fetchFullDetails();
    }
  }, [isOpen, room]);

  const fetchFullDetails = async () => {
    if (!room) return;
    try {
      const data = await roomManagementService.getRoomById(room.roomId);
      setFormData(prev => ({
        ...prev,
        description: data.description || '',
        sensorUrl: data.sensorUrl || ''
      }));
    } catch (err) {
      console.error('Failed to load full room details');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!room) return;
    setLoading(true);
    setError(null);
    try {
      await roomManagementService.updateRoom(room.roomId, formData);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to update room');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !room) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          onClick={onClose} className="absolute inset-0 bg-black/60 backdrop-blur-sm" 
        />
        <motion.div
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 20 }}
          className="relative w-full max-w-xl bg-[#0F0F16] border border-white/10 rounded-3xl overflow-hidden shadow-2xl"
        >
          <div className="px-8 py-6 border-b border-white/5 flex items-center justify-between">
            <h2 className="text-xl font-bold text-white flex items-center gap-3">
               Edit Room Configuration
            </h2>
            <button onClick={onClose} className="p-2 hover:bg-white/5 rounded-xl text-slate-400"><X size={20} /></button>
          </div>

          <form onSubmit={handleSubmit} className="p-8 space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2 col-span-2">
                <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Room Name</label>
                <Input value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} glass required />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Building</label>
                <Input value={formData.building} onChange={e => setFormData({...formData, building: e.target.value})} glass required />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Floor</label>
                <Input 
                  type="number" 
                  value={formData.floor} 
                  onChange={(e) => setFormData({ ...formData, floor: safeParseInt(e.target.value) })}
                  glass 
                  required 
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Capacity</label>
                <Input 
                  type="number" 
                  value={formData.capacity} 
                  onChange={(e) => setFormData({ ...formData, capacity: safeParseInt(e.target.value) })}
                  glass 
                  required 
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">IoT Sensor URL</label>
                <Input value={formData.sensorUrl} onChange={e => setFormData({...formData, sensorUrl: e.target.value})} placeholder="Optional" glass />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Description</label>
              <textarea 
                value={formData.description} 
                onChange={e => setFormData({...formData, description: e.target.value})}
                className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all min-h-[100px]"
                placeholder="Details about equipment or room usage..."
              />
            </div>

            {error && (
              <div className="p-4 rounded-xl bg-accent/10 border border-accent/20 text-accent text-sm flex items-center gap-3">
                <AlertTriangle size={18} /> {error}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>Cancel</Button>
              <Button type="submit" variant="primary" disabled={loading} className="px-8 flex items-center gap-2">
                {loading ? 'Saving...' : <><Save size={18} /> Update Asset</>}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

// --- DELETE MODAL ---
interface DeleteModalProps extends ModalProps {
  room: RoomListItem | null;
  onSuccess: () => void;
}

export const RoomDeleteModal: React.FC<DeleteModalProps> = ({ isOpen, onClose, room, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!room) return;
    setLoading(true);
    setError(null);
    try {
      await roomManagementService.deleteRoom(room.roomId);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to delete room');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !room) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          onClick={onClose} className="absolute inset-0 bg-black/60 backdrop-blur-sm" 
        />
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-md bg-[#0F0F16] border border-accent/20 rounded-3xl overflow-hidden shadow-2xl"
        >
          <div className="p-8 text-center space-y-6">
            <div className="w-20 h-20 bg-accent/10 rounded-full flex items-center justify-center mx-auto border border-accent/20">
              <Trash2 className="text-accent" size={40} />
            </div>
            
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-white">Critical Action</h2>
              <p className="text-slate-400">
                Are you sure you want to delete <span className="text-white font-bold">"{room.name}"</span>? 
                This action is irreversible and will remove all boundary data.
              </p>
            </div>

            {error && (
              <div className="p-4 rounded-xl bg-accent/10 border border-accent/20 text-accent text-sm flex items-center gap-3">
                <AlertTriangle size={18} /> {error}
              </div>
            )}

            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" onClick={onClose} disabled={loading}>
                Dismiss
              </Button>
              <Button variant="primary" className="flex-1 bg-accent hover:bg-accent/80 border-transparent text-white" 
                onClick={handleDelete} disabled={loading}>
                {loading ? 'Deleting...' : 'Confirm Delete'}
              </Button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
