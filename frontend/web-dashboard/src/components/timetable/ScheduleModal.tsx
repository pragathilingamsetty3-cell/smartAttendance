'use client';

import React, { useState, useEffect } from 'react';
import { X, Clock, Coffee, Utensils, Save, GraduationCap, Calendar } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '../ui/Button';
import { Card, CardContent } from '../ui/Card';

interface ScheduleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: any) => void;
  editingEntry?: any;
  rooms: any[];
  faculties: any[];
  sections: any[];
}

export function ScheduleModal({ isOpen, onClose, onSave, editingEntry, rooms, faculties, sections }: ScheduleModalProps) {
  const getCurrentAcademicYear = () => {
    const today = new Date();
    const currentYear = today.getFullYear();
    // If today is July or later, year is currentYear - currentYear+1
    // Otherwise it's currentYear-1 - currentYear
    if (today.getMonth() >= 6) { // July is 6 (0-indexed)
      return `${currentYear}-${currentYear + 1}`;
    }
    return `${currentYear - 1}-${currentYear}`;
  };

  const [showHolidaySuccess, setShowHolidaySuccess] = useState(false);
  const [formData, setFormData] = useState({
    subject: '',
    roomId: '',
    facultyId: '',
    dayOfWeek: 'MONDAY',
    startTime: '09:00:00',
    endTime: '10:30:00',
    academicYear: getCurrentAcademicYear(),
    semester: 'Active Semester',
    sectionId: '',
    hasLunchBreak: false,
    lunchBreakStart: '13:00:00',
    lunchBreakEnd: '14:00:00',
    hasShortBreak: false,
    shortBreakStart: '11:00:00',
    shortBreakEnd: '11:15:00',
    breakToleranceMinutes: 10,
    isExamDay: false,
    isHoliday: false,
    holidayDate: new Date().toISOString().split('T')[0] // Default to today
  });

  useEffect(() => {
    if (editingEntry) {
      setFormData({
        ...formData,
        ...editingEntry,
        roomId: editingEntry.room?.id || '',
        facultyId: editingEntry.faculty?.id || '',
        sectionId: editingEntry.section?.id || ''
      });
    } else {
      setFormData({
        subject: '',
        roomId: '',
        facultyId: '',
        dayOfWeek: 'MONDAY',
        startTime: '09:00:00',
        endTime: '10:30:00',
        academicYear: getCurrentAcademicYear(),
        semester: 'Active Semester',
        sectionId: '',
        hasLunchBreak: false,
        lunchBreakStart: '13:00:00',
        lunchBreakEnd: '14:00:00',
        hasShortBreak: false,
        shortBreakStart: '11:00:00',
        shortBreakEnd: '11:15:00',
        breakToleranceMinutes: 10,
        isExamDay: false,
        isHoliday: false,
        holidayDate: new Date().toISOString().split('T')[0]
      });
    }
  }, [editingEntry, isOpen]);

  // Handle holiday toggle behavior
  const toggleHoliday = (checked: boolean) => {
    const todayStr = new Date().toISOString().split('T')[0];
    const todayDay = new Date().toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();

    setFormData(prev => ({
      ...prev,
      isHoliday: checked,
      // Default holiday date to today if enabled
      holidayDate: checked ? todayStr : prev.holidayDate,
      dayOfWeek: checked ? todayDay : prev.dayOfWeek,
      // If turning on holiday, turn off everything else
      isExamDay: checked ? false : prev.isExamDay,
      hasLunchBreak: checked ? false : prev.hasLunchBreak,
      hasShortBreak: checked ? false : prev.hasShortBreak,
      // Clear specific IDs to prevent ghost dependencies
      roomId: checked ? '' : prev.roomId,
      facultyId: checked ? '' : prev.facultyId,
      subject: checked ? (prev.subject || 'INSTITUTIONAL HOLIDAY') : prev.subject
    }));
  };

  const handleHolidayDateChange = (date: string) => {
    const selectedDate = new Date(date);
    const dayOfWeek = selectedDate.toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase();
    
    setFormData(prev => ({
      ...prev,
      holidayDate: date,
      dayOfWeek: dayOfWeek
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.isHoliday && !editingEntry) {
      setShowHolidaySuccess(true);
      setTimeout(() => {
        onSave(formData);
        setShowHolidaySuccess(false);
      }, 2500);
    } else {
      onSave(formData);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <motion.div 
          initial={{ opacity: 0 }} 
          animate={{ opacity: 1 }} 
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm" 
        />
        
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 20 }}
          className="relative w-full max-w-4xl bg-[#0F0F16] border border-white/10 rounded-3xl overflow-hidden shadow-2xl shadow-primary/10"
        >
          {/* SUCCESS OVERLAY FOR HOLIDAYS */}
          <AnimatePresence>
            {showHolidaySuccess && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 z-[100] flex flex-col items-center justify-center bg-[#0F0F16]/95 backdrop-blur-xl"
              >
                <motion.div
                  initial={{ scale: 0 }}
                  animate={{ scale: [0, 1.2, 1] }}
                  transition={{ duration: 0.5 }}
                  className="w-24 h-24 rounded-full bg-orange-500/20 border border-orange-500/40 flex items-center justify-center mb-6 shadow-[0_0_50px_rgba(249,115,22,0.3)]"
                >
                  <Calendar className="text-orange-400 w-12 h-12" />
                </motion.div>
                <motion.h2 
                  initial={{ y: 20, opacity: 0 }}
                  animate={{ y: 0, opacity: 1 }}
                  transition={{ delay: 0.3 }}
                  className="text-3xl font-black text-white tracking-tight text-center"
                >
                  RESTING MODE ACTIVATED
                </motion.h2>
                <motion.p
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.6 }}
                  className="text-orange-400/60 font-mono text-sm mt-2 uppercase tracking-widest flex items-center gap-2"
                >
                  <span className="w-2 h-2 rounded-full bg-orange-500 animate-pulse" />
                  AI Scanning: Suspended
                </motion.p>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Header */}
          <div className="px-8 py-6 border-b border-white/5 flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white flex items-center gap-3">
                <Clock className="text-primary" /> {editingEntry ? 'Edit Class Schedule' : 'Schedule New Class'}
              </h2>
              <p className="text-slate-500 text-sm mt-1">Configure class timing and smart breaks</p>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-white/5 rounded-xl transition-colors text-slate-400 hover:text-white"
            >
              <X size={24} />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-8 space-y-6 overflow-y-auto max-h-[80vh]">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              {/* Left Column: Basic Info */}
              <div className="space-y-6">
                <div className="space-y-1">
                  <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Subject Name</label>
                  <input 
                    type="text" 
                    value={formData.subject}
                    onChange={e => setFormData({...formData, subject: e.target.value})}
                    className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all font-medium"
                    placeholder="e.g. Advanced AI Systems" 
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className={`space-y-1 transition-opacity ${formData.isHoliday ? 'opacity-50 pointer-events-none' : ''}`}>
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Faculty</label>
                    <select 
                      value={formData.facultyId}
                      onChange={e => setFormData({...formData, facultyId: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      required={!formData.isHoliday}
                      disabled={formData.isHoliday}
                    >
                      <option value="" className="bg-[#0F0F16]">Select Faculty</option>
                      {faculties.map(f => (
                        <option key={f.id} value={f.id} className="bg-[#0F0F16]">{f.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className={`space-y-1 transition-opacity ${formData.isHoliday ? 'opacity-50 pointer-events-none' : ''}`}>
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Room</label>
                    <select 
                      value={formData.roomId}
                      onChange={e => setFormData({...formData, roomId: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      required={!formData.isHoliday}
                      disabled={formData.isHoliday}
                    >
                      <option value="" className="bg-[#0F0F16]">Select Room</option>
                      {rooms.map(r => (
                        <option key={r.id} value={r.id} className="bg-[#0F0F16]">{r.name}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Academic Year</label>
                    <input 
                      type="text" 
                      value={formData.academicYear}
                      onChange={e => setFormData({...formData, academicYear: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      placeholder="e.g. 2026-2027"
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Semester</label>
                    <input 
                      type="text" 
                      value={formData.semester}
                      onChange={e => setFormData({...formData, semester: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      placeholder="e.g. Fall 2026"
                      required
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                   <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Section</label>
                    <select 
                      value={formData.sectionId}
                      onChange={e => setFormData({...formData, sectionId: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                    >
                      <option value="" className="bg-[#0F0F16]">All Sections</option>
                      {sections.map(s => (
                        <option key={s.id} value={s.id} className="bg-[#0F0F16]">{s.label || s.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">
                      {formData.isHoliday ? 'Specific Holiday Date' : 'Day of Week'}
                    </label>
                    {formData.isHoliday ? (
                      <input 
                        type="date"
                        value={formData.holidayDate}
                        onChange={e => handleHolidayDateChange(e.target.value)}
                        className="w-full bg-orange-500/10 border border-orange-500/20 rounded-xl px-4 py-2.5 text-white focus:outline-none ring-1 ring-orange-500/20"
                        required
                      />
                    ) : (
                      <select 
                        value={formData.dayOfWeek}
                        onChange={e => setFormData({...formData, dayOfWeek: e.target.value})}
                        className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                        required
                      >
                        <option value="MONDAY" className="bg-[#0F0F16]">Monday</option>
                        <option value="TUESDAY" className="bg-[#0F0F16]">Tuesday</option>
                        <option value="WEDNESDAY" className="bg-[#0F0F16]">Wednesday</option>
                        <option value="THURSDAY" className="bg-[#0F0F16]">Thursday</option>
                        <option value="FRIDAY" className="bg-[#0F0F16]">Friday</option>
                        <option value="SATURDAY" className="bg-[#0F0F16]">Saturday</option>
                        <option value="SUNDAY" className="bg-[#0F0F16]">Sunday</option>
                      </select>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">Start Time</label>
                    <input 
                      type="time" 
                      step="1"
                      value={formData.startTime}
                      onChange={e => setFormData({...formData, startTime: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-black uppercase tracking-widest text-slate-500 ml-1">End Time</label>
                    <input 
                      type="time" 
                      step="1"
                      value={formData.endTime}
                      onChange={e => setFormData({...formData, endTime: e.target.value})}
                      className="w-full bg-white/[0.03] border border-white/10 rounded-xl px-4 py-2.5 text-white focus:outline-none"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Right Column: Smart Breaks */}
              <div className="space-y-6">
                <h3 className="text-xs font-black uppercase tracking-widest text-primary ml-1 flex items-center gap-2">
                  <GraduationCap size={16} /> AI Monitoring Adjustments
                </h3>
                
                <Card glass className="bg-white/[0.02] border-white/5">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center text-sm font-bold text-white">
                        <Utensils className="w-4 h-4 mr-2 text-orange-400" />
                        Lunch Break Window
                      </div>
                      <input 
                        type="checkbox" 
                        checked={formData.hasLunchBreak}
                        onChange={e => setFormData({...formData, hasLunchBreak: e.target.checked})}
                        className="accent-primary h-4 w-4 disabled:opacity-30"
                        disabled={formData.isHoliday}
                      />
                    </div>
                    {formData.hasLunchBreak && (
                      <div className="grid grid-cols-2 gap-4">
                         <div className="space-y-1">
                          <label className="text-[10px] uppercase text-gray-500 font-bold">Start</label>
                          <input 
                            type="time" 
                            step="1"
                            value={formData.lunchBreakStart}
                            onChange={e => setFormData({...formData, lunchBreakStart: e.target.value})}
                            className="w-full bg-black/40 border border-white/10 rounded-lg p-2 text-xs text-white focus:outline-none" 
                          />
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] uppercase text-gray-500 font-bold">End</label>
                          <input 
                            type="time" 
                            step="1"
                            value={formData.lunchBreakEnd}
                            onChange={e => setFormData({...formData, lunchBreakEnd: e.target.value})}
                            className="w-full bg-black/40 border border-white/10 rounded-lg p-2 text-xs text-white focus:outline-none" 
                          />
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>

                <Card glass className="bg-white/[0.02] border-white/5">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center text-sm font-bold text-white">
                        <Coffee className="w-4 h-4 mr-2 text-blue-400" />
                        Short Break Window
                      </div>
                      <input 
                        type="checkbox" 
                        checked={formData.hasShortBreak}
                        onChange={e => setFormData({...formData, hasShortBreak: e.target.checked})}
                        className="accent-primary h-4 w-4 disabled:opacity-30"
                        disabled={formData.isHoliday}
                      />
                    </div>
                    {formData.hasShortBreak && (
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <label className="text-[10px] uppercase text-gray-500 font-bold">Start</label>
                          <input 
                            type="time" 
                            step="1"
                            value={formData.shortBreakStart}
                            onChange={e => setFormData({...formData, shortBreakStart: e.target.value})}
                            className="w-full bg-black/40 border border-white/10 rounded-lg p-2 text-xs text-white focus:outline-none" 
                          />
                        </div>
                        <div className="space-y-1">
                          <label className="text-[10px] uppercase text-gray-500 font-bold">End</label>
                          <input 
                            type="time" 
                            step="1"
                            value={formData.shortBreakEnd}
                            onChange={e => setFormData({...formData, shortBreakEnd: e.target.value})}
                            className="w-full bg-black/40 border border-white/10 rounded-lg p-2 text-xs text-white focus:outline-none" 
                          />
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
                
                <Card glass className="bg-primary/5 border border-primary/20">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="text-sm font-bold text-white flex items-center gap-2">
                          <GraduationCap className="w-4 h-4 text-red-400" />
                          Mark as Exam Session
                        </h4>
                        <p className="text-[10px] text-gray-400 mt-1">
                          Skips AI autonomous absence marking.
                        </p>
                      </div>
                      <input 
                        type="checkbox" 
                        checked={formData.isExamDay}
                        onChange={e => setFormData({...formData, isExamDay: e.target.checked})}
                        className="accent-red-500 h-5 w-5 cursor-pointer"
                      />
                    </div>
                  </CardContent>
                </Card>

                <Card glass className="bg-orange-500/5 border border-orange-500/20">
                  <CardContent className="p-5">
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="text-sm font-bold text-white flex items-center gap-2">
                          <Calendar className="w-4 h-4 text-orange-400" />
                          Mark as Holiday Session
                        </h4>
                        <p className="text-[10px] text-gray-400 mt-1">
                          AI enters Resting Mode for this session.
                        </p>
                      </div>
                      <input 
                        type="checkbox" 
                        checked={formData.isHoliday}
                        onChange={e => toggleHoliday(e.target.checked)}
                        className="accent-orange-500 h-5 w-5 cursor-pointer"
                      />
                    </div>
                  </CardContent>
                </Card>

                <div className="p-5 rounded-2xl bg-primary/5 border border-primary/20 space-y-2">
                    <p className="text-xs text-gray-300 font-semibold uppercase tracking-wider">Smart Monitoring Tip</p>
                    <p className="text-[11px] text-gray-400 leading-relaxed">
                        Defining lunch and short breaks helps the AI Attendance engine differentiate between legitimate recesses and unauthorized walk-outs.
                    </p>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-8 border-t border-white/5">
              <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
              <Button type="submit" variant="primary" className="px-10 shadow-lg shadow-primary/20">
                <Save size={18} className="mr-2" /> {editingEntry ? 'Update Schedule' : 'Create Entry'}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
