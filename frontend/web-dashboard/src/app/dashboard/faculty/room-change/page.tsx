'use client';

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Building, 
  MapPin, 
  RefreshCw, 
  AlertTriangle, 
  CheckCircle2, 
  Users,
  Search,
  ArrowRight
} from 'lucide-react';
import { roomManagementService } from '@/services/roomManagement.service';
import { userManagementService } from '@/services/userManagement.service';
import { useAuthStore } from '@/stores/authStore';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { DropdownDTO } from '@/types/user-management';
import { Room, RoomListItem } from '@/types/room-management';

export default function RoomChange() {
  const user = useAuthStore(state => state.user);
  const [departments, setDepartments] = useState<DropdownDTO[]>([]);
  const [sections, setSections] = useState<DropdownDTO[]>([]);
  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [selectedDept, setSelectedDept] = useState<string>('');
  const [selectedSection, setSelectedSection] = useState<string>('');
  const [selectedRoom, setSelectedRoom] = useState<string>('');
  const [reason, setReason] = useState('Sudden room unavailability');
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [result, setResult] = useState<any | null>(null);

  const isFaculty = user?.role?.toUpperCase() === 'FACULTY' || user?.role?.toUpperCase() === 'ROLE_FACULTY';

  useEffect(() => {
    if (!user) return; // 🛑 Critical: Wait for auth store to hydrate

    const init = async () => {
      try {
        setLoading(true);
        
        // 1. Fetch available rooms (Allowed for Faculty in AdminV1Controller)
        const roomPromise = roomManagementService.getAvailableRooms();
        
        // 2. Fetch departments ONLY for Admins, auto-sync for Faculty
        if (!isFaculty) {
          const depts = await userManagementService.getDepartments();
          setDepartments(depts);
        } else if (user?.department) {
          // If faculty, auto-set their department and fetch their sections
          setSelectedDept(user.department);
          // 3. Sections fetch handled by separate useEffect dependent on selectedDept
        }

        const roomList = await roomPromise;
        setRooms(roomList);
      } catch (err) {
        console.error('Failed to load initial data');
      } finally {
        setLoading(false);
      }
    };

    init();
  }, [user, isFaculty]);

  useEffect(() => {
    if (selectedDept) {
      userManagementService.getSections(selectedDept).then(res => {
        setSections(res);
      });
      setSelectedSection('');
    }
  }, [selectedDept]);

  const [showAllRooms, setShowAllRooms] = useState(false);

  const handleRoomChange = async () => {
    if (!selectedSection || !selectedRoom) return;

    try {
      setProcessing(true);
      console.log('[RoomChange] Initiating transition:', { selectedRoom, selectedSection, reason });
      const res = await roomManagementService.quickRoomChange(selectedRoom, selectedSection, reason);
      setResult(res);
      // Reset after success
      setSelectedRoom('');
      setReason('Sudden room unavailability');
    } catch (err: any) {
      console.error('[RoomChange] Transition failure:', err);
      setResult({ error: err.message || 'Room change failed' });
    } finally {
      setProcessing(false);
    }
  };

  const displayedRooms = showAllRooms ? rooms : rooms.slice(0, 8);

  if (loading) return <div className="p-20 text-center"><Loading size="lg" text="Mapping room availability..." /></div>;

  return (
    <div className="p-8 space-y-8 max-w-5xl mx-auto">
      <div>
        <h1 className="text-3xl font-bold text-white tracking-tight flex items-center gap-3">
          <RefreshCw className="text-emerald-500" /> Immediate Room Transition
        </h1>
        <p className="text-slate-400 mt-1 uppercase text-[10px] font-bold tracking-[0.2em]">Geo-Fencing Class Relocation System</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left: Configuration */}
        <Card glass className="p-8 border-white/10 space-y-8">
            <div className="space-y-6">
                <div>
                   <h3 className="text-sm font-bold text-white uppercase tracking-[0.2em] mb-4 flex items-center gap-2">
                     <Users size={14} className="text-violet-500" /> 1. Select Section
                   </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                       {!isFaculty && (
                        <div className="space-y-1.5">
                          <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Department</label>
                          <select 
                            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white outline-none focus:border-violet-500/50 transition-all font-medium"
                            value={selectedDept}
                            onChange={(e) => setSelectedDept(e.target.value)}
                          >
                            <option value="" className="bg-[#0F0F16]">Choose Dept</option>
                            {departments.map(d => <option key={`dept-${d.id}`} value={d.id} className="bg-[#0F0F16]">{d.label || d.name}</option>)}
                          </select>
                        </div>
                       )}
                       <div className={`space-y-1.5 ${isFaculty ? 'md:col-span-2' : ''}`}>
                         <label className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Target Section</label>
                         <select 
                           className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white outline-none focus:border-violet-500/50 transition-all font-medium"
                           value={selectedSection}
                           onChange={(e) => setSelectedSection(e.target.value)}
                           disabled={!selectedDept || sections.length === 0}
                         >
                           <option value="" className="bg-[#0F0F16]">{sections.length === 0 ? 'No sections found' : 'Choose Section'}</option>
                           {sections.map(s => <option key={`sec-${s.id}`} value={s.id} className="bg-[#0F0F16]">{s.label || s.name}</option>)}
                         </select>
                       </div>
                    </div>
                </div>

                <div>
                   <h3 className="text-sm font-bold text-white uppercase tracking-[0.2em] mb-4 flex items-center gap-2">
                     <Building size={14} className="text-emerald-500" /> 2. Select New Room
                   </h3>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                       {displayedRooms.map((room, index) => (
                         <motion.button
                           key={room.roomId || (room as any).id || (room as any).roomId || `room-${index}`}
                           whileHover={{ scale: 1.05, y: -5 }}
                           whileTap={{ scale: 0.95 }}
                           animate={room.isAvailable ? { 
                             y: [0, -8, 0],
                             boxShadow: [
                               "0 0 0px rgba(16, 185, 129, 0)",
                               "0 0 20px rgba(16, 185, 129, 0.2)",
                               "0 0 0px rgba(16, 185, 129, 0)"
                             ]
                           } : {
                             y: [0, -2, 0],
                             boxShadow: [
                               "0 0 0px rgba(239, 68, 68, 0)",
                               "0 0 10px rgba(239, 68, 68, 0.1)",
                               "0 0 0px rgba(239, 68, 68, 0)"
                             ]
                           }}
                           transition={{ 
                             y: { repeat: Infinity, duration: room.isAvailable ? 3 : 5, ease: "easeInOut" },
                             boxShadow: { repeat: Infinity, duration: 2, ease: "easeInOut" }
                           }}
                           onClick={() => setSelectedRoom(room.roomId || (room as any).id)}
                           className={`p-5 rounded-2xl border text-center transition-all relative overflow-hidden ${
                             selectedRoom === (room.roomId || (room as any).id) 
                               ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400 ring-2 ring-emerald-500/20' 
                               : room.isAvailable
                                 ? 'bg-white/5 border-emerald-500/20 text-slate-300 hover:border-emerald-500/40' 
                                 : 'bg-white/[0.02] border-red-500/10 text-slate-500 grayscale-[0.3]'
                           }`}
                         >
                           <div className={`absolute top-0 left-0 w-1 h-full ${room.isAvailable ? 'bg-emerald-500/40' : 'bg-red-500/20'}`} />
                           <Building className={`mx-auto mb-3 ${room.isAvailable ? 'text-emerald-500' : 'text-red-500'} opacity-80`} size={28} />
                           <div className="text-sm font-bold text-white mb-1">{room.name}</div>
                           <div className="text-[10px] text-slate-500 uppercase tracking-widest font-mono">
                             {room.building} • {room.isAvailable ? 'Free' : 'In Use'}
                           </div>
                         </motion.button>
                       ))}
                       {rooms.length > 8 && (
                         <button 
                           onClick={() => setShowAllRooms(!showAllRooms)}
                           className="p-4 rounded-xl border border-dashed border-white/10 text-slate-600 hover:text-slate-400 transition-colors flex flex-col items-center justify-center"
                         >
                            <Search size={16} />
                            <span className="text-[10px] font-bold mt-1">{showAllRooms ? 'Show Less' : 'View All'}</span>
                         </button>
                       )}
                    </div>
                </div>

                <div>
                   <h3 className="text-sm font-bold text-white uppercase tracking-[0.2em] mb-4">3. Relocation Reason</h3>
                   <textarea 
                     className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white outline-none focus:border-emerald-500/50 transition-all h-24 resize-none"
                     placeholder="Brief description for records..."
                     value={reason}
                     onChange={(e) => setReason(e.target.value)}
                   />
                </div>
            </div>

            <Button 
                variant="primary" 
                className={`w-full py-6 text-lg font-bold tracking-wider transition-all h-auto shadow-2xl ${
                  selectedSection && selectedRoom ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/20' : 'opacity-20 translate-y-0 shadow-none'
                }`}
                disabled={processing || !selectedSection || !selectedRoom}
                onClick={handleRoomChange}
            >
              {processing ? <Loading size="sm" /> : (
                <>
                  EXECUTE TRANSITION <ArrowRight size={20} className="ml-2 group-hover:translate-x-1" />
                </>
              )}
            </Button>
        </Card>

        {/* Right: Monitoring & Status */}
        <div className="space-y-8">
            <Card glass className="p-8 border-amber-500/20 bg-amber-500/[0.01]">
               <div className="flex gap-4 items-start mb-6">
                 <div className="bg-amber-500/10 p-3 rounded-2xl text-amber-500">
                    <AlertTriangle size={28} />
                 </div>
                 <div>
                    <h3 className="text-lg font-bold text-white">Transition Protocol</h3>
                    <p className="text-sm text-slate-400 mt-1">Triggering a transition will immediately notify all students and grant a **15-minute grace period** for physical relocation.</p>
                 </div>
               </div>

               <div className="space-y-4 pt-6 border-t border-white/5">
                  <div className="flex items-center gap-3 text-xs text-slate-300">
                    <CheckCircle2 size={16} className="text-emerald-500" />
                    <span>Real-time push notifications sent via Firebase</span>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-slate-300">
                    <CheckCircle2 size={16} className="text-emerald-500" />
                    <span>Geo-fencing polygons shifted in memory repo</span>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-slate-300">
                    <CheckCircle2 size={16} className="text-emerald-500" />
                    <span>Autonomous attendance smoothing engine engaged</span>
                  </div>
               </div>
            </Card>

            <AnimatePresence>
                {result && (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className={`p-8 rounded-3xl border shadow-2xl ${
                          result.error 
                            ? 'bg-red-500/10 border-red-500/20 text-red-500' 
                            : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                        }`}
                    >
                        <div className="flex items-center gap-4 mb-4 font-bold text-xl uppercase tracking-tighter">
                          {result.error ? <RefreshCw className="animate-spin" /> : <CheckCircle2 />}
                          {result.error ? 'TRANSITION_FAILURE' : 'TRANSITION_SUCCESS'}
                        </div>
                        <div className="space-y-2 text-sm opacity-80">
                          {result.error ? (
                            <p>{result.error}</p>
                          ) : (
                            <>
                                <p>Session {result.sessionId?.slice(0,8)}... has been migrated.</p>
                                <div className="flex justify-between items-center bg-black/20 p-3 rounded-xl mt-4 border border-white/5">
                                   <div>
                                      <p className="text-[10px] font-bold opacity-60">NEW_ROOM_HEX</p>
                                      <p className="font-mono text-white font-bold">{result.newRoomId?.slice(0,12)}...</p>
                                   </div>
                                   <div className="text-right">
                                      <p className="text-[10px] font-bold opacity-60">GRACE_PERIOD</p>
                                      <p className="font-mono text-white font-bold">{result.gracePeriodMinutes || 15} MINS</p>
                                   </div>
                                </div>
                            </>
                          )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
