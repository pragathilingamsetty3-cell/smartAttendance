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
const HOURS = Array.from({ length: 24 }, (_, i) => i); // 12 AM to 11 PM (00:00 to 23:00)

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
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
      {entries.length === 0 ? (
        <div className="p-20 text-center flex flex-col items-center justify-center space-y-4">
          <div className="p-4 rounded-full bg-slate-50 border border-slate-100">
            <Calendar className="w-8 h-8 text-primary/50" />
          </div>
          <div>
            <h3 className="text-lg font-medium text-slate-900">No classes scheduled</h3>
            <p className="text-sm text-slate-500 max-w-xs mx-auto mt-1">
              {isFaculty 
                ? "You don't have any classes assigned to you for the current academic session."
                : "No timetable records found for the selected filter criteria."}
            </p>
          </div>
        </div>
      ) : (
        <table className="w-full border-collapse min-w-[1000px]">
        <thead>
          <tr className="border-b border-slate-100 bg-slate-50">
            <th className="p-4 px-6 text-left text-sm font-semibold text-slate-500 w-24">Time</th>
            {DAYS.map(day => (
              <th key={day} className="p-4 text-center text-sm font-semibold text-slate-700">
                {day.charAt(0) + day.slice(1).toLowerCase()}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {HOURS.map(hour => (
            <tr key={hour} className="border-b border-slate-100 group">
              <td className="p-4 px-6 text-xs text-slate-400 font-mono align-top">
                {hour}:00
              </td>
              {DAYS.map(day => {
                const dayEntries = groupedEntries[day.toUpperCase()]?.[hour] || [];

                return (
                  <td key={day} className="p-2 border-r border-slate-100 min-h-[100px] align-top relative">
                    {dayEntries.map(entry => (
                      <div 
                        key={entry.id}
                        className={cn(
                          "mb-2 p-3 rounded-lg shadow-sm transition-all group/item relative overflow-hidden",
                          entry.isHoliday 
                            ? "bg-orange-50 border border-orange-200 hover:bg-orange-100" 
                            : "bg-primary/5 border border-primary/10 hover:bg-primary/10"
                        )}
                      >
                        <div className={cn("absolute top-0 left-0 w-1 h-full", entry.isHoliday ? "bg-orange-500" : "bg-primary")} />
                        
                        <div className="flex justify-between items-start mb-1 gap-2">
                          <h4 className="text-xs font-bold text-slate-900 leading-tight">
                            {entry.isHoliday ? `⛱️ ${entry.subject}` : entry.subject}
                            {entry.section && !entry.isHoliday && (
                              <span className="block text-[10px] text-primary mt-0.5 opacity-80 uppercase tracking-wider">
                                {entry.section.name}
                              </span>
                            )}
                          </h4>
                          
                          <div className="flex flex-col gap-1">
                            {entry.isExamDay && (
                              <span className="px-2 py-0.5 rounded-full bg-red-50 text-red-600 text-[10px] font-black uppercase tracking-tighter border border-red-100">
                                Exam
                              </span>
                            )}
                            {entry.isHoliday && (
                              <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-600 text-[10px] font-black uppercase tracking-tighter border border-orange-200">
                                Holiday
                              </span>
                            )}
                          </div>
                          
                          {!isFaculty && (
                            <div className="opacity-0 group-hover/item:opacity-100 flex gap-1 transition-opacity absolute top-2 right-2">
                              <button onClick={() => onEdit(entry)} className="p-1 hover:bg-slate-100 rounded text-slate-400 hover:text-slate-900">
                                <Edit2 size={12} />
                              </button>
                              <button onClick={() => onDelete(entry.id)} className="p-1 hover:bg-red-50 rounded text-slate-400 hover:text-red-500">
                                <Trash2 size={12} />
                              </button>
                            </div>
                          )}
                        </div>

                        <div className="space-y-1 mt-1">
                          <div className="flex items-center text-[10px] text-slate-500">
                            <Clock className="w-3 h-3 mr-1 text-primary/50" />
                            {entry.startTime?.substring(0, 5)} - {entry.endTime?.substring(0, 5)}
                          </div>
                          {!entry.isHoliday && (
                            <>
                              <div className="flex items-center text-[10px] text-slate-500">
                                <MapPin className="w-3 h-3 mr-1 text-primary/50" />
                                {entry.room?.name || 'N/A'}
                              </div>
                              
                              {!isFaculty && (
                                <div className="flex items-center text-[10px] text-slate-500">
                                  <User className="w-3 h-3 mr-1 text-primary/50" />
                                  {entry.faculty?.name || 'Unassigned'}
                                </div>
                              )}
                            </>
                          )}
                          {entry.isHoliday && (
                             <div className="flex items-center text-[10px] text-orange-600/80 font-medium">
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
