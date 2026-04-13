import { HallPassStatusDTO } from './attendance';

export interface TimetableResponseDTO {
  id: string;
  subject: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  isExamDay: boolean;
  isAdhoc: boolean;
  academicYear: string;
  semester: string;
  description: string;
  room?: {
    id: string;
    name: string;
    building: string;
    floor: number;
  };
  faculty?: {
    id: string;
    name: string;
    email: string;
  };
  section?: {
    id: string;
    name: string;
  };
}

export interface StudentDashboardStatsDTO {
  overallAttendance: number;
  attendedClasses: number;
  totalClasses: number;
  attendanceTrend: Record<string, number>;
  todayClasses: TimetableResponseDTO[];
  activeSession?: TimetableResponseDTO;
  recentHallPass?: HallPassStatusDTO;
  departmentName: string;
  sectionName: string;
  semester: number;
  registrationNumber: string;
  aiVerificationConfidence: number;
}
