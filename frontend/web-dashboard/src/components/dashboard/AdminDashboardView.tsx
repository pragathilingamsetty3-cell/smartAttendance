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
    if (!confirm("⚠️ Declare a Sudden Holiday? This will put the AI Monitor into 'Resting Mode' for all future sessions today. (Past attendance will be preserved)")) return;
    
    setLoadingHoliday(true);
    try {
      await apiClient.post("/api/v1/admin/calendar/day", {
        date: new Date().toISOString().split("T")[0],
        type: "HOLIDAY",
        description: "Sudden Holiday Declared from Dashboard"
      });
      setIsHolidayToday(true);
      alert("✅ Sudden Holiday Declared. AI Monitor is now in Resting Mode.");
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
                <h4 className="text-sm font-bold text-white uppercase tracking-wider">AI Monitor: Resting Mode Active</h4>
                <p className="text-xs text-gray-400">Institutional holiday detected. All autonomous scanning is suspended for the day.</p>
              </div>
            </div>
            <div className="px-3 py-1 rounded-full bg-primary/20 text-primary text-[10px] font-black uppercase tracking-tighter">
              Sudden Holiday
            </div>
          </motion.div>
        )}

        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <motion.h1 variants={item} className="text-3xl font-bold text-white mb-2 tracking-tight">
              {activeTab === "overview" ? "System Overview" : "Academic Calendar"}
            </motion.h1>
            <motion.p variants={item} className="text-slate-400">
              {activeTab === "overview" 
                ? "Live monitoring of attendance sessions and zero-trust anomalies." 
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
                {loadingHoliday ? "Processing..." : "Declare Sudden Holiday"}
              </button>
            )}

            <motion.div variants={item} className="flex bg-white/5 p-1 rounded-xl border border-white/10 h-fit">
              <button 
                onClick={() => setActiveTab("overview")}
                className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${
                  activeTab === "overview" ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-slate-400 hover:text-white"
                }`}
              >
                <Activity size={16} /> Overview
              </button>
              <button 
                onClick={() => setActiveTab("calendar")}
                className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${
                  activeTab === "calendar" ? "bg-primary text-white shadow-lg shadow-primary/20" : "text-slate-400 hover:text-white"
                }`}
              >
                <Calendar size={16} /> Calendar
              </button>
            </motion.div>
          </div>
        </div>

        {activeTab === "overview" ? (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
          
          {/* Card 1: Users/Students */}
          <motion.div variants={item} className="glass-card p-6 relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
              <Users size={64} className="text-[#9b51e0]" />
            </div>
            <p className="text-slate-400 text-sm font-medium mb-1">Total Active Entities</p>
            {loading ? (
              <Skeleton className="h-10 w-24 mb-4" />
            ) : (
              <p className="text-4xl font-bold text-white mb-4">
                <AnimatedCounter value={stats?.totalUsers ?? stats?.totalStudents ?? 0} />
              </p>
            )}
            <div className="flex items-center text-sm text-emerald-400">
              <span className="bg-emerald-400/10 px-2 py-0.5 rounded-full mr-2">Verified Range</span>
              System Active
            </div>
          </motion.div>

          {/* Card 2: Live/Active Today */}
          <motion.div variants={item} className="glass-card p-6 relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
              <Activity size={64} className="text-[#00d2ff]" />
            </div>
            <p className="text-slate-400 text-sm font-medium mb-1">Active Today</p>
            {loading ? (
              <Skeleton className="h-10 w-24 mb-4" />
            ) : (
              <p className="text-4xl font-bold text-white mb-4">
                <AnimatedCounter value={stats?.activeToday ?? stats?.activeSessions ?? 0} />
              </p>
            )}
            <div className="flex items-center text-sm text-[#00d2ff]">
              <span className="bg-[#00d2ff]/10 px-2 py-0.5 rounded-full mr-2">Live Now</span>
              {stats?.totalScheduledToday ?? 0} Total Sessions
            </div>
          </motion.div>

          {/* Card 3: Attendance Rate / Verified Count */}
          <motion.div variants={item} className="glass-card p-6 relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
              <CheckCircle size={64} className="text-emerald-500" />
            </div>
            <p className="text-slate-400 text-sm font-medium mb-1">Avg Reliability / Rate</p>
            {loading ? (
              <Skeleton className="h-10 w-24 mb-4" />
            ) : (
              <p className="text-4xl font-bold text-white mb-4">
                <AnimatedCounter value={stats?.attendanceRate ?? 95.5} isPercentage />
              </p>
            )}
            <div className="flex items-center text-sm text-slate-400">
              <span className="mr-2">Optimal Range</span>
            </div>
          </motion.div>

          {/* Card 4: Anomalies */}
          <motion.div variants={item} className="glass-card p-6 relative overflow-hidden group border-accent/20">
            <div className="absolute top-0 right-0 p-6 opacity-10 group-hover:opacity-20 transition-opacity">
              <ShieldAlert size={64} className="text-accent" />
            </div>
            <p className="text-slate-400 text-sm font-medium mb-1">Zero-Trust Anomalies</p>
            {loading ? (
              <Skeleton className="h-10 w-24 mb-4" />
            ) : (
              <p className="text-4xl font-bold text-accent mb-4">
                <AnimatedCounter value={stats?.anomalies ?? 0} />
              </p>
            )}
            <div className="flex items-center text-sm text-accent">
              <span className="bg-accent/10 px-2 py-0.5 rounded-full mr-2">Needs Review</span>
              System Violations
            </div>
          </motion.div>

        </div>

        {/* Live Velocity Chart */}
        <motion.div variants={item} className="glass-card p-6 overflow-hidden">
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-lg font-semibold text-white">Live Heartbeat Velocity</h2>
            <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-500 text-[10px] font-bold border border-emerald-500/20 uppercase tracking-widest">
              <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
              Monitoring
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={MOCK_GRAPH_DATA} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#9b51e0" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#9b51e0" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis 
                  dataKey="time" 
                  stroke="#475569" 
                  fontSize={10} 
                  tickLine={false} 
                  axisLine={false} 
                  tick={{ fill: "#64748b" }}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0F0F16', borderColor: '#ffffff10', borderRadius: '12px', boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)' }}
                  itemStyle={{ color: '#e2e8f0', fontSize: '12px' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="value" 
                  stroke="#9b51e0" 
                  strokeWidth={3} 
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
