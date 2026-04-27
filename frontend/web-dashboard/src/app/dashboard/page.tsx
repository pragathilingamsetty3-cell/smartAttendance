"use client";

import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence, animate, Variants } from "framer-motion";
import { WifiOff } from "lucide-react";
import apiClient from "@/lib/apiClient";
import { DashboardStatsDTO, Role } from "@/types/index";
import { useAuthStore } from "@/stores/authStore";
import { AdminDashboardView } from "@/components/dashboard/AdminDashboardView";
import { FacultyDashboardView } from "@/components/dashboard/FacultyDashboardView";
import { StudentDashboardView } from "@/components/dashboard/StudentDashboardView";

const container: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

// Framer Motion Animated Counter
const AnimatedCounter = ({ value, isPercentage = false }: { value: number; isPercentage?: boolean }) => {
  const nodeRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const node = nodeRef.current;
    if (!node) return;
    
    const controls = animate(0, value, {
      duration: 1.2,
      ease: "easeOut",
      onUpdate(cur) {
        node.textContent = isPercentage 
          ? cur.toFixed(1) + "%" 
          : Math.round(cur).toString();
      }
    });

    return () => controls.stop();
  }, [value, isPercentage]);

  return <span ref={nodeRef}>{isPercentage ? "0.0%" : "0"}</span>;
};

export default function Dashboard() {
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorSyncing, setErrorSyncing] = useState<boolean>(false);
  const user = useAuthStore(state => state.user);

  useEffect(() => {
    const fetchStats = async () => {
      // 🛡️ [RESILIENCY] Small stagger delay to prevent Thundering Herd on free tier
      await new Promise(resolve => setTimeout(resolve, 800));
      
      try {
        const isFaculty = user?.role === Role.FACULTY;
        const isStudent = user?.role === Role.STUDENT || user?.role === Role.CR || user?.role === Role.LR;
        
        const endpoint = isFaculty 
          ? '/api/v1/faculty/dashboard/stats' 
          : isStudent
          ? '/api/v1/student/dashboard/stats'
          : '/api/v1/admin/dashboard/stats';
          
        const response = await apiClient.get<any>(endpoint);
        setStats(response.data);
        setErrorSyncing(false);
      } catch (err) {
        setErrorSyncing(true);
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      fetchStats();
      const intervalId = setInterval(fetchStats, 60000);
      return () => clearInterval(intervalId);
    }
  }, [user]);

  const isFaculty = user?.role === Role.FACULTY;

  return (
    <div className="relative p-8">
      <motion.div 
        variants={container}
        initial="hidden"
        animate="show"
        className="space-y-8 pb-10"
      >
        {user?.role === Role.FACULTY ? (
          <FacultyDashboardView stats={stats} loading={loading} sectionId={user?.sectionId} />
        ) : (user?.role === Role.STUDENT || user?.role === Role.CR || user?.role === Role.LR) ? (
          <StudentDashboardView stats={stats} loading={loading} user={user} />
        ) : (
          <AdminDashboardView stats={stats} loading={loading} AnimatedCounter={AnimatedCounter} />
        )}
      </motion.div>

      {/* Subtle Offline / Sync Error Toast */}
      <AnimatePresence>
        {errorSyncing && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            className="fixed bottom-8 right-8 z-50 glass-card px-4 py-3 flex items-center shadow-accent/20"
          >
            <div className="bg-accent/20 p-2 rounded-full mr-3 text-accent animate-pulse">
              <WifiOff size={18} />
            </div>
            <div>
              <p className="text-white text-sm font-bold tracking-wide">Syncing with Secure Node...</p>
              <p className="text-slate-400 text-xs">Awaiting backend handshake.</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
