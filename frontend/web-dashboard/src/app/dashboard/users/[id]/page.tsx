"use client";

import { useEffect, useState, use } from "react";
import { motion } from "framer-motion";
import { 
  ArrowLeft, 
  User, 
  Mail, 
  Hash, 
  Shield, 
  ShieldCheck, 
  Building2, 
  Calendar, 
  Smartphone, 
  Fingerprint, 
  History,
  Activity,
  Layers,
  CheckCircle2,
  AlertCircle
} from "lucide-react";
import { useRouter } from "next/navigation";
import { Button } from "../../../../components/ui/Button";
import { Skeleton } from "../../../../components/ui/Skeleton";
import userManagementService from "../../../../services/userManagement.service";
import { EditUserModal } from "../../../../components/users/EditUserModal";
import { ActivityLogModal } from "../../../../components/users/ActivityLogModal";

export default function UserDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [userDetails, setUserDetails] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isActivityLogOpen, setIsActivityLogOpen] = useState(false);
  const [resetDeviceLoading, setResetDeviceLoading] = useState(false);
  const [resetDeviceMessage, setResetDeviceMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  const fetchUserDetails = async () => {
    try {
      setLoading(true);
      const data = await userManagementService.getUserDetails(id);
      setUserDetails(data);
    } catch (err: any) {
      setError(err.message || "Failed to load identity profile");
    } finally {
      setTimeout(() => setLoading(false), 600);
    }
  };

  const handleResetDevice = async () => {
    if (!window.confirm(`Reset device lock for ${userDetails.registrationNumber}? This will unbind current device security.`)) {
      return;
    }
    
    try {
      setResetDeviceLoading(true);
      setResetDeviceMessage(null);
      
      await userManagementService.resetUserDevice(userDetails.registrationNumber);
      
      setResetDeviceMessage({ 
        type: 'success', 
        text: '✅ Device lock reset successfully. Student can re-register device.' 
      });
      
      setTimeout(() => fetchUserDetails(), 1000);
    } catch (err: any) {
      setResetDeviceMessage({ 
        type: 'error', 
        text: `❌ ${err.message || 'Failed to reset device lock'}` 
      });
    } finally {
      setResetDeviceLoading(false);
    }
  };

  useEffect(() => {
    fetchUserDetails();
  }, [id]);

  if (loading) {
    return (
      <div className="space-y-8 animate-pulse">
        <div className="flex items-center gap-4">
           <Skeleton className="h-10 w-10 rounded-xl" />
           <Skeleton className="h-8 w-64" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
           <Skeleton className="h-[400px] lg:col-span-1 rounded-2xl" />
           <Skeleton className="h-[400px] lg:col-span-2 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (error || !userDetails) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <div className="h-20 w-20 rounded-full bg-accent/10 flex items-center justify-center text-accent mb-6">
           <AlertCircle size={40} />
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">Identity Not Found</h2>
        <p className="text-slate-400 max-w-md mb-8">{error || "The requested user profile does not exist or access was denied."}</p>
        <Button variant="secondary" onClick={() => router.push('/dashboard/users')}>
           Back to User Management
        </Button>
      </div>
    );
  }

  const isStudent = userDetails.role === "STUDENT";

  return (
    <div className="space-y-8 pb-10">
      {/* Header & Back Navigation */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => router.push('/dashboard/users')}
            className="p-2.5 hover:bg-white/5 rounded-xl border border-white/5 transition-all text-slate-400 hover:text-white"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-3xl font-bold text-white tracking-tight">Identity Profile</h1>
            <div className="flex items-center gap-2 text-slate-500 text-sm mt-1">
               <span>User Management</span>
               <span>/</span>
               <span className="text-primary/80">{userDetails.name}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
           <Button 
             variant="secondary" 
             className="gap-2 border-white/5 active:scale-95 transition-transform"
             onClick={() => setIsActivityLogOpen(true)}
           >
              <History size={16} /> Activity Log
           </Button>
           <Button 
             variant="primary" 
             className="shadow-lg shadow-primary/20 active:scale-95 transition-transform"
             onClick={() => setIsEditModalOpen(true)}
           >
              Edit Identity
           </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Profile Card */}
        <div className="lg:col-span-4 space-y-6">
          <motion.div 
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="glass-panel overflow-hidden"
          >
            <div className="h-24 bg-gradient-to-r from-primary/40 to-violet-600/40" />
            <div className="px-6 pb-8 -mt-12 text-center">
              <div className="inline-block relative">
                <div className="h-24 w-24 rounded-2xl bg-[#13131F] border-4 border-[#0F0F16] flex items-center justify-center text-primary text-3xl font-bold mx-auto shadow-xl">
                  {userDetails.name?.charAt(0)}
                </div>
                <div className="absolute -bottom-1 -right-1 h-6 w-6 rounded-full bg-emerald-500 border-4 border-[#0F0F16]" />
              </div>
              
              <h2 className="text-xl font-bold text-white mt-4">{userDetails.name}</h2>
              <p className="text-slate-400 text-sm mb-6">{userDetails.email}</p>
              
              <div className="flex flex-wrap justify-center gap-2 mb-8">
                <span className="px-3 py-1 rounded-lg bg-primary/10 text-primary text-xs font-semibold border border-primary/20">
                   {userDetails.role}
                </span>
                <span className={`px-3 py-1 rounded-lg text-xs font-semibold border ${
                  userDetails.status === "ACTIVE" 
                    ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" 
                    : "bg-accent/10 text-accent border-accent/20"
                }`}>
                  {userDetails.status}
                </span>
              </div>

              <div className="space-y-4 text-left">
                <div className="flex items-center gap-4 text-sm p-3 rounded-xl bg-white/[0.03] border border-white/5">
                   <div className="h-8 w-8 rounded-lg bg-violet-500/10 flex items-center justify-center text-violet-400">
                      <Hash size={16} />
                   </div>
                   <div>
                      <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Registration ID</p>
                      <p className="text-slate-200 font-mono">{userDetails.registrationNumber}</p>
                   </div>
                </div>

                <div className="flex items-center gap-4 text-sm p-3 rounded-xl bg-white/[0.03] border border-white/5">
                   <div className="h-8 w-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400">
                      <Calendar size={16} />
                   </div>
                   <div>
                      <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Member Since</p>
                      <p className="text-slate-200">{userDetails.createdAt ? new Date(userDetails.createdAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) : "N/A"}</p>
                   </div>
                </div>
              </div>
            </div>
          </motion.div>

          {/* Quick Stats Card */}
          <motion.div 
             initial={{ opacity: 0, x: -20 }}
             animate={{ opacity: 1, x: 0 }}
             transition={{ delay: 0.1 }}
             className="glass-panel p-6"
          >
             <h3 className="text-sm font-bold text-white mb-4 flex items-center gap-2">
                <Activity size={16} className="text-primary" /> Metrics
             </h3>
             <div className="grid grid-cols-2 gap-4 text-center">
                <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5">
                   <p className="text-2xl font-bold text-white">94%</p>
                   <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-1">Attendance</p>
                </div>
                <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5">
                   <p className="text-2xl font-bold text-white">0</p>
                   <p className="text-[10px] text-slate-500 uppercase tracking-widest font-bold mt-1">Alerts</p>
                </div>
             </div>
          </motion.div>
        </div>

        {/* Right Column: Detailed Info */}
        <div className="lg:col-span-8 flex flex-col gap-8">
           {/* Section & Department Information */}
           <motion.div 
             initial={{ opacity: 0, y: 10 }}
             animate={{ opacity: 1, y: 0 }}
             transition={{ delay: 0.1 }}
             className="glass-panel p-8"
           >
              <h3 className="text-xl font-bold text-white mb-6 flex items-center gap-3">
                 <Building2 className="text-primary" /> Academic & Departmental Scope
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                 <div className="space-y-6">
                    <div>
                       <label className="text-[10px] text-slate-500 uppercase tracking-widest font-black block mb-2">Department Architecture</label>
                       <div className="flex items-center gap-3">
                          <div className="p-2.5 rounded-xl bg-primary/10 text-primary border border-primary/20">
                             <Layers size={18} />
                          </div>
                          <span className="text-lg font-medium text-slate-200">{userDetails.department || "No Department Assigned"}</span>
                       </div>
                    </div>

                    {isStudent && (
                      <div>
                         <label className="text-[10px] text-slate-500 uppercase tracking-widest font-black block mb-2">Current Batch / Section</label>
                         <div className="flex items-center gap-3">
                            <div className="p-2.5 rounded-xl bg-violet-500/10 text-violet-400 border border-violet-500/20">
                               <Shield size={18} />
                            </div>
                            <span className="text-lg font-medium text-slate-200">{userDetails.section?.name || "Unassigned"}</span>
                         </div>
                      </div>
                    )}
                 </div>

                 <div className="space-y-6">
                    <div>
                       <label className="text-[10px] text-slate-500 uppercase tracking-widest font-black block mb-2">Academic Progression</label>
                       <div className="flex gap-4">
                          <div className="flex-1 p-4 rounded-2xl bg-white/[0.03] border border-white/5">
                             <p className="text-[10px] text-slate-500 font-bold mb-1">YEAR</p>
                             <p className="text-xl font-bold text-white">{userDetails.totalAcademicYears || "N/A"}</p>
                          </div>
                          <div className="flex-1 p-4 rounded-2xl bg-white/[0.03] border border-white/5">
                             <p className="text-[10px] text-slate-500 font-bold mb-1">SEMESTER</p>
                             <p className="text-xl font-bold text-white">{userDetails.currentSemester || "1"}</p>
                          </div>
                       </div>
                    </div>
                 </div>
              </div>
           </motion.div>

           {/* Security & Device Fingerprinting */}
           <motion.div 
             initial={{ opacity: 0, y: 10 }}
             animate={{ opacity: 1, y: 0 }}
             transition={{ delay: 0.2 }}
             className="glass-panel p-8 relative overflow-hidden"
           >
              <div className="absolute top-0 right-0 p-8 opacity-[0.03] pointer-events-none">
                 <ShieldCheck size={120} />
              </div>

              <h3 className="text-xl font-bold text-white mb-6 flex items-center gap-3">
                 <ShieldCheck className="text-emerald-400" /> Advanced Security & Auth Core
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
                 <div className="p-6 rounded-2xl bg-emerald-500/5 border border-emerald-500/10">
                    <div className="flex items-start justify-between mb-4">
                       <div className="p-2.5 rounded-xl bg-emerald-500/20 text-emerald-400">
                          <Fingerprint size={22} />
                       </div>
                       <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-[10px] font-black text-emerald-400 tracking-tighter">SECURE</span>
                    </div>
                    <h4 className="font-bold text-white mb-1">Biometric Hash</h4>
                    <p className="text-xs text-slate-500 mb-4 font-mono break-all">{userDetails.biometricSignature || "FINGERPRINT_STUB_PENDING"}</p>
                    <div className="flex items-center gap-2 text-emerald-400 text-[11px] font-bold uppercase tracking-widest">
                       <CheckCircle2 size={14} /> Identity Verified
                    </div>
                 </div>

                 <div className="p-6 rounded-2xl bg-primary/5 border border-primary/10">
                    <div className="flex items-start justify-between mb-4">
                       <div className="p-2.5 rounded-xl bg-primary/20 text-primary">
                          <Smartphone size={22} />
                       </div>
                    </div>
                    <h4 className="font-bold text-white mb-1">Hardware Bound ID</h4>
                    <p className="text-xs text-slate-500 mb-4 font-mono">{userDetails.deviceId || "UNBOUND_DEVICE"}</p>
                    <button 
                      className={`text-[11px] font-bold transition-colors flex items-center gap-2 uppercase tracking-widest ${
                        resetDeviceLoading ? 'opacity-50 cursor-not-allowed text-slate-400' : 'text-primary hover:text-white'
                      }`}
                      onClick={handleResetDevice}
                      disabled={resetDeviceLoading}
                    >
                       {resetDeviceLoading ? '⏳ Resetting...' : 'Reset Hardware Binding'}
                    </button>
                    {resetDeviceMessage && (
                      <p className={`text-xs mt-3 font-medium ${
                        resetDeviceMessage.type === 'success' ? 'text-emerald-400' : 'text-red-400'
                      }`}>
                        {resetDeviceMessage.text}
                      </p>
                    )}
                 </div>
              </div>
           </motion.div>
        </div>
      </div>

      {/* Modals */}
      <EditUserModal 
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        user={userDetails}
        onSuccess={fetchUserDetails}
      />

      <ActivityLogModal 
        isOpen={isActivityLogOpen}
        onClose={() => setIsActivityLogOpen(false)}
        userId={id}
        userName={userDetails.name}
      />
    </div>
  );
}
