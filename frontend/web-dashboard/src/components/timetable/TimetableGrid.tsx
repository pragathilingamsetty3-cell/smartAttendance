'use client';

import React from 'react';
import { Card, CardContent } from '../ui/Card';
import { cn } from '@/lib/utils';
import { Clock, MapPin, User, Trash2, Edit2, Calendar } from 'lucide-react';
import { Button } from '../ui/Button';

interface TimetableEntry {
  id: string;
  subject: string;
  startTime: string;
  endTime: string;
  dayOfWeek: string;
  room: { name: string };
  faculty: { name: string };
  section?: { name: string };
  isExamDay?: boolean;
  isHoliday?: boolean;
}

interface TimetableGridProps {
  entries: TimetableEntry[];
  onEdit: (entry: TimetableEntry) => void;
  onDelete: (id: string) => void;
  userRole?: string;
}

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const HOURS = Array.from({ length: 12 }, (_, i) => i + 8); // 8 AM to 8 PM

export function TimetableGrid({ entries, onEdit, onDelete, userRole }: TimetableGridProps) {
  const isFaculty = String(userRole || '').includes('FACULTY');
  
  // ⚡ PERFORMANCE OPTIMIZATION: Pre-calculate a grouped Map of entries by Day and Hour
  // This avoids running .filter() 84 times (12 hours * 7 days) on every render.
  const groupedEntries = React.useMemo(() => {
    const map: Record<string, Record<number, TimetableEntry[]>> = {};
    const safeEntries = Array.isArray(entries) ? entries : [];
    
    // Initialize empty maps for each day
    DAYS.forEach(day => {
      map[day.toUpperCase()] = {};
    });

    safeEntries.forEach(entry => {
      if (!entry.startTime || !entry.dayOfWeek) return;
      
      const day = entry.dayOfWeek.toUpperCase();
      const timePart = entry.startTime.split(' ')[0];
      const entryHour = parseInt(timePart.split(':')[0], 10);
      
      if (map[day]) {
        if (!map[day][entryHour]) map[day][entryHour] = [];
        map[day][entryHour].push(entry);
      }
    });
    
    return map;
  }, [entries]);

  return (
    <div className="overflow-x-auto rounded-xl border border-white/5 bg-[#0F0F16]/50 backdrop-blur-md">
      {entries.length === 0 ? (
        <div className="p-20 text-center flex flex-col items-center justify-center space-y-4">
          <div className="p-4 rounded-full bg-white/5 border border-white/10">
            <Calendar className="w-8 h-8 text-[#7C3AED]/50" />
          </div>
          <div>
            <h3 className="text-lg font-medium text-white">No classes scheduled</h3>
            <p className="text-sm text-gray-400 max-w-xs mx-auto mt-1">
              {isFaculty 
                ? "You don't have any classes assigned to you for the current academic session."
                : "No timetable records found for the selected filter criteria."}
            </p>
          </div>
        </div>
      ) : (
        <table className="w-full border-collapse min-w-[1000px]">
        <thead>
          <tr className="border-b border-white/5 bg-white/5">
            <th className="p-4 px-6 text-left text-sm font-semibold text-gray-400 w-24">Time</th>
            {DAYS.map(day => (
              <th key={day} className="p-4 text-center text-sm font-semibold text-gray-300">
                {day.charAt(0) + day.slice(1).toLowerCase()}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {HOURS.map(hour => (
            <tr key={hour} className="border-b border-white/5 group">
              <td className="p-4 px-6 text-xs text-gray-500 font-mono align-top">
                {hour}:00
              </td>
              {DAYS.map(day => {
                const dayEntries = groupedEntries[day.toUpperCase()]?.[hour] || [];

                return (
                  <td key={day} className="p-2 border-r border-white/5 min-h-[100px] align-top relative">
                    {dayEntries.map(entry => (
                      <div 
                        key={entry.id}
                        className={cn(
                          "mb-2 p-3 rounded-lg shadow-sm transition-all group/item relative overflow-hidden",
                          entry.isHoliday 
                            ? "bg-orange-500/10 border border-orange-500/30 hover:bg-orange-500/20" 
                            : "bg-[#7C3AED]/10 border border-[#7C3AED]/20 hover:bg-[#7C3AED]/20"
                        )}
                      >
                        <div className={cn("absolute top-0 left-0 w-1 h-full", entry.isHoliday ? "bg-orange-500" : "bg-[#7C3AED]")} />
                        
                        <div className="flex justify-between items-start mb-1 gap-2">
                          <h4 className="text-xs font-bold text-white leading-tight">
                            {entry.isHoliday ? `⛱️ ${entry.subject}` : entry.subject}
                            {entry.section && !entry.isHoliday && (
                              <span className="block text-[10px] text-[#7C3AED] mt-0.5 opacity-80 uppercase tracking-wider">
                                {entry.section.name}
                              </span>
                            )}
                          </h4>
                          
                          <div className="flex flex-col gap-1">
                            {entry.isExamDay && (
                              <span className="px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 text-[10px] font-black uppercase tracking-tighter border border-red-500/30">
                                Exam
                              </span>
                            )}
                            {entry.isHoliday && (
                              <span className="px-2 py-0.5 rounded-full bg-orange-500/20 text-orange-400 text-[10px] font-black uppercase tracking-tighter border border-orange-500/30">
                                Holiday
                              </span>
                            )}
                          </div>
                          
                          {!isFaculty && (
                            <div className="opacity-0 group-hover/item:opacity-100 flex gap-1 transition-opacity absolute top-2 right-2">
                              <button onClick={() => onEdit(entry)} className="p-1 hover:bg-white/10 rounded text-gray-400 hover:text-white">
                                <Edit2 size={12} />
                              </button>
                              <button onClick={() => onDelete(entry.id)} className="p-1 hover:bg-red-500/20 rounded text-gray-400 hover:text-red-400">
                                <Trash2 size={12} />
                              </button>
                            </div>
                          )}
                        </div>

                        <div className="space-y-1 mt-1">
                          <div className="flex items-center text-[10px] text-gray-400">
                            <Clock className="w-3 h-3 mr-1 text-[#7C3AED]/50" />
                            {entry.startTime?.substring(0, 5)} - {entry.endTime?.substring(0, 5)}
                          </div>
                          {!entry.isHoliday && (
                            <>
                              <div className="flex items-center text-[10px] text-gray-400">
                                <MapPin className="w-3 h-3 mr-1 text-[#7C3AED]/50" />
                                {entry.room?.name || 'N/A'}
                              </div>
                              
                              {!isFaculty && (
                                <div className="flex items-center text-[10px] text-gray-400">
                                  <User className="w-3 h-3 mr-1 text-[#7C3AED]/50" />
                                  {entry.faculty?.name || 'Unassigned'}
                                </div>
                              )}
                            </>
                          )}
                          {entry.isHoliday && (
                             <div className="flex items-center text-[10px] text-orange-400/80 font-medium">
                               <Calendar className="w-3 h-3 mr-1" />
                               AI Resting Mode Active
                             </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      )}
    </div>
  );
}
