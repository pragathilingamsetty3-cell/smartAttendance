import React from "react";
import { motion, Variants } from "framer-motion";
import { Users, Activity, CheckCircle, ShieldAlert } from "lucide-react";
import { AreaChart, Area, ResponsiveContainer, XAxis, Tooltip, CartesianGrid } from "recharts";
import { Skeleton } from "@/components/ui/Skeleton";
import { CalendarSettings } from "./CalendarSettings";
import { Calendar, Power, AlertTriangle } from "lucide-react";
import apiClient from "@/lib/apiClient";

// MOCK DATA for Chart
const MOCK_GRAPH_DATA = [
  { time: "08:00", value: 120 },
  { time: "09:00", value: 340 },
  { time: "10:00", value: 310 },
  { time: "11:00", value: 450 },
  { time: "12:00", value: 410 },
  { time: "13:00", value: 500 },
  { time: "14:00", value: 480 },
];

interface AdminDashboardViewProps {
  stats: any;
  loading: boolean;
  AnimatedCounter: React.FC<{ value: number; isPercentage?: boolean }>;
}

export const AdminDashboardView: React.FC<AdminDashboardViewProps> = ({ stats, loading, AnimatedCounter }) => {
  const item: Variants = {
    hidden: { opacity: 0, y: 20 },
    show: { 
      opacity: 1, 
      y: 0, 
      transition: { 
        duration: 0.5,
        ease: [0.22, 1, 0.36, 1] 
      } 
    }
  };

  const [activeTab, setActiveTab] = React.useState<"overview" | "calendar">("overview");
  const [isHolidayToday, setIsHolidayToday] = React.useState(false);
  const [loadingHoliday, setLoadingHoliday] = React.useState(false);

  const declareSuddenHoliday = async () => {
    if (!confirm("⚠️ Declare Today as a Holiday? This will put the AI Monitor into 'Resting Mode' for all future sessions today. (Past attendance will be preserved)")) return;
    
    setLoadingHoliday(true);
    try {
      await apiClient.post("/api/v1/admin/calendar/day", {
        date: new Date().toISOString().split("T")[0],
        type: "HOLIDAY",
        description: "Sudden Holiday Declared from Dashboard"
      });
      setIsHolidayToday(true);
      alert("✅ Holiday Declared for Today. AI Monitor is now in Resting Mode.");
    } catch (err) {
      console.error("Failed to declare sudden holiday", err);
      alert("❌ Failed to declare sudden holiday.");
    } finally {
      setLoadingHoliday(false);
    }
  };

  return (
    <div className="space-y-8">
        {isHolidayToday && (
          <motion.div 
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            className="bg-primary/10 border border-primary/20 rounded-2xl p-4 flex items-center justify-between overflow-hidden"
          >
            <div className="flex items-center gap-3">
              <div className="bg-primary/20 p-2 rounded-full text-primary animate-pulse">
                <Power size={20} />
              </div>
              <div>
                <h4 className="text-sm font-bold text-slate-900 uppercase tracking-wider">AI Monitor: Resting Mode Active</h4>
                <p className="text-xs text-slate-500">Institutional holiday detected. All autonomous scanning is suspended for the day.</p>
              </div>
            </div>
            <div className="px-3 py-1 rounded-full bg-primary/20 text-primary text-[10px] font-black uppercase tracking-tighter">
              Sudden Holiday
            </div>
          </motion.div>
        )}

        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <motion.h1 variants={item} className="text-3xl font-bold text-slate-900 mb-2 tracking-tight">
                {activeTab === "overview" ? "System Overview" : "Academic Calendar"}
              </motion.h1>
              <motion.p variants={item} className="text-slate-500">
                {activeTab === "overview" 
                  ? "Live monitoring of attendance sessions and system performance." 
                  : "Manage institutional holidays and exam schedules."}
              </motion.p>
            </div>

          <div className="flex flex-col md:flex-row gap-2">
            {!isHolidayToday && activeTab === "overview" && (
              <button 
                onClick={declareSuddenHoliday}
                disabled={loadingHoliday}
                className="px-4 py-2 rounded-xl text-xs font-bold transition-all bg-red-500/10 text-red-500 border border-red-500/20 hover:bg-red-500 hover:text-white flex items-center gap-2 group disabled:opacity-50"
              >
                <AlertTriangle size={14} className="group-hover:animate-bounce" /> 
                {loadingHoliday ? "Processing..." : "Declare Today Holiday"}
              </button>
            )}

            <motion.div variants={item} className="flex bg-white p-1 rounded-xl border border-slate-200 h-fit shadow-sm">
              <button 
                onClick={() => setActiveTab("overview")}
                className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${
                  activeTab === "overview" ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-slate-400 hover:text-slate-900 hover:bg-slate-50"
                }`}
              >
                <Activity size={16} /> Overview
              </button>
              <button 
                onClick={() => setActiveTab("calendar")}
                className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${
                  activeTab === "calendar" ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-slate-400 hover:text-slate-900 hover:bg-slate-50"
                }`}
              >
                <Calendar size={16} /> Calendar
              </button>
            </motion.div>
          </div>
        </div>

        {activeTab === "overview" ? (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
            {/* Card 1: Users/Students */}
            <motion.div variants={item} className="bg-white border border-slate-200 p-6 rounded-2xl relative overflow-hidden group shadow-sm hover:shadow-xl transition-all">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <Users size={64} className="text-primary" />
              </div>
              <p className="text-slate-500 text-[10px] font-black uppercase tracking-widest mb-1">Total Active Entities</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mb-4" />
              ) : (
                <p className="text-4xl font-bold text-slate-900 mb-4">
                  <AnimatedCounter value={stats?.totalUsers ?? stats?.totalStudents ?? 0} />
                </p>
              )}
              <div className="flex items-center text-xs font-bold text-emerald-600">
                <span className="bg-emerald-500/10 px-2 py-0.5 rounded-full mr-2">Verified Range</span>
                System Active
              </div>
            </motion.div>

            {/* Card 2: Live/Active Today */}
            <motion.div variants={item} className="bg-white border border-slate-200 p-6 rounded-2xl relative overflow-hidden group shadow-sm hover:shadow-xl transition-all">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <Activity size={64} className="text-sky-500" />
              </div>
              <p className="text-slate-500 text-[10px] font-black uppercase tracking-widest mb-1">Active Today</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mb-4" />
              ) : (
                <p className="text-4xl font-bold text-slate-900 mb-4">
                  <AnimatedCounter value={stats?.activeToday ?? stats?.activeSessions ?? 0} />
                </p>
              )}
              <div className="flex items-center text-xs font-bold text-sky-600">
                <span className="bg-sky-500/10 px-2 py-0.5 rounded-full mr-2">Live Now</span>
                {stats?.totalScheduledToday ?? 0} Total Sessions
              </div>
            </motion.div>

            {/* Card 3: Attendance Rate / Verified Count */}
            <motion.div variants={item} className="bg-white border border-slate-200 p-6 rounded-2xl relative overflow-hidden group shadow-sm hover:shadow-xl transition-all">
              <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
                <CheckCircle size={64} className="text-emerald-500" />
              </div>
              <p className="text-slate-500 text-[10px] font-black uppercase tracking-widest mb-1">Avg Reliability / Rate</p>
              {loading ? (
                <Skeleton className="h-10 w-24 mb-4" />
              ) : (
                <p className="text-4xl font-bold text-slate-900 mb-4">
                  <AnimatedCounter value={stats?.attendanceRate ?? 95.5} isPercentage />
                </p>
              )}
              <div className="flex items-center text-xs font-bold text-slate-500">
                <span className="bg-slate-100 px-2 py-0.5 rounded-full mr-2 uppercase tracking-widest">Optimal Range</span>
              </div>
            </motion.div>


        </div>

        {/* Live Velocity Chart */}
        <motion.div variants={item} className="bg-white border border-slate-200 p-8 rounded-3xl shadow-sm overflow-hidden">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-xl font-bold text-slate-900">Live Heartbeat Velocity</h2>
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 text-emerald-600 text-[10px] font-black border border-emerald-500/20 uppercase tracking-widest">
              <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
              Monitoring Active
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={MOCK_GRAPH_DATA} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis 
                  dataKey="time" 
                  stroke="#94a3b8" 
                  fontSize={10} 
                  tickLine={false} 
                  axisLine={false} 
                  tick={{ fill: "#64748b", fontWeight: 600 }}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '16px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}
                  itemStyle={{ color: '#0f172a', fontSize: '12px', fontWeight: '700' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="value" 
                  stroke="#0ea5e9" 
                  strokeWidth={4} 
                  fillOpacity={1} 
                  fill="url(#colorValue)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </motion.div>
      </>
    ) : (
      <motion.div variants={item}>
        <CalendarSettings />
      </motion.div>
    )}
    </div>
  );
};
