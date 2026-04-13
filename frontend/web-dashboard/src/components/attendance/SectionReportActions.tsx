'use client';

import React, { useState } from 'react';
import { Download, Filter, FileText, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { attendanceService } from '@/services/attendance.service';

interface SectionReportActionsProps {
  sectionId: string;
}

export const SectionReportActions: React.FC<SectionReportActionsProps> = ({ sectionId }) => {
  const [threshold, setThreshold] = useState<number>(75);
  const [downloadingReport, setDownloadingReport] = useState(false);
  const [downloadingList, setDownloadingList] = useState(false);

  const handleDownloadFullReport = async () => {
    if (!sectionId) return;
    setDownloadingReport(true);
    try {
      // Pass null/undefined for threshold to get everyone
      await attendanceService.downloadSectionReport(sectionId, undefined);
      console.log(`Full attendance report downloaded!`);
    } catch (error) {
      console.error('Failed to download attendance report');
    } finally {
      setDownloadingReport(false);
    }
  };

  const handleDownloadDefaulters = async () => {
    if (!sectionId) return;
    setDownloadingReport(true);
    try {
      await attendanceService.downloadSectionReport(sectionId, threshold);
      console.log(`Defaulter report (Threshold: ${threshold}%) downloaded!`);
    } catch (error) {
      console.error('Failed to download attendance report');
    } finally {
      setDownloadingReport(false);
    }
  };

  const handleDownloadList = async () => {
// ... existing handleDownloadList ...
    if (!sectionId) return;
    setDownloadingList(true);
    try {
      await attendanceService.downloadStudentList(sectionId);
      console.log('Student registry downloaded!');
    } catch (error) {
      console.error('Failed to download student list');
    } finally {
      setDownloadingList(false);
    }
  };

  return (
    <div className="glass-panel p-6 mb-6 border border-white/5">
      <div className="flex flex-col gap-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-col gap-1">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <FileText className="h-5 w-5 text-secondary" />
              Registry & Reports Center
            </h3>
            <p className="text-slate-400 text-sm">Download section-wide attendance data and student lists.</p>
          </div>
          
          <Button 
            variant="secondary" 
            size="sm" 
            onClick={handleDownloadList}
            loading={downloadingList}
            className="flex items-center gap-2 bg-secondary/20 hover:bg-secondary/30 text-secondary border-none"
          >
            <Download size={16} />
            Download Student Registry
          </Button>
        </div>

        <div className="h-px bg-white/5 w-full" />

        <div className="flex flex-wrap items-center gap-6">
          {/* Full Report Action */}
          <div className="flex flex-col gap-2">
             <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Complete Data</span>
             <Button 
              variant="ghost" 
              size="sm" 
              onClick={handleDownloadFullReport}
              loading={downloadingReport}
              className="flex items-center gap-2 border-white/10 hover:bg-white/5 text-white"
            >
              <Download size={16} className="text-blue-400" />
              Full Class Attendance
            </Button>
          </div>

          <div className="w-px h-10 bg-white/5 hidden md:block" />

          {/* Defaulter Report Action */}
          <div className="flex flex-col gap-2">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Defaulter List (Filter)</span>
            <div className="flex items-center gap-3">
              <div className="flex items-center space-x-2 bg-obsidian-800/80 border border-white/5 rounded-lg px-3 py-1.5 h-9">
                <Filter size={16} className="text-slate-500" />
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={threshold}
                  onChange={(e) => setThreshold(Number(e.target.value))}
                  className="bg-transparent text-white text-sm w-12 focus:outline-none font-bold"
                />
                <span className="text-sm text-slate-500 font-bold">%</span>
              </div>

              <Button 
                variant="secondary" 
                size="sm" 
                onClick={handleDownloadDefaulters}
                loading={downloadingReport}
                className="flex items-center gap-2 bg-amber-500/20 hover:bg-amber-500/30 text-amber-500 border-none"
              >
                <Download size={16} />
                Download Defaulter Report
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
