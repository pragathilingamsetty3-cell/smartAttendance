"use client";

import axios, { AxiosError } from "axios";
import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Filter, MoreHorizontal, AlertCircle, Eye, X, Check } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "../../../components/ui/Button";
import { Skeleton } from "../../../components/ui/Skeleton";
import apiClient from "../../../lib/apiClient";
import { EnhancedUserDTO, UserStatus, Role } from "../../../types";

export default function UsersPage() {
  const [users, setUsers] = useState<EnhancedUserDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Search & Filter State
  const [searchQuery, setSearchQuery] = useState("");
  const [activeRole, setActiveRole] = useState<Role | "ALL">("ALL");
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const router = useRouter();

  useEffect(() => {
    const getBackendHealthMessage = async () => {
      try {
        const { data } = await apiClient.get<{ status: string; details?: unknown }>('/api/v1/performance/health');
        return `Backend health: ${data.status}${data.details ? ` (${JSON.stringify(data.details)})` : ''}`;
      } catch {
        return "Unable to reach backend health endpoint.";
      }
    };

    const getErrorMessage = async (err: unknown) => {
      if (axios.isAxiosError(err)) {
        const axiosError = err as AxiosError<{ error?: string; message?: string }>;
        if (axiosError.response) {
          const status = axiosError.response.status;
          const statusText = axiosError.response.statusText || "Error";
          const serverMessage = axiosError.response.data?.error || axiosError.response.data?.message;
          const backendHealth = await getBackendHealthMessage();

          return `${status} ${statusText}${serverMessage ? `: ${serverMessage}` : ''}. ${backendHealth}`;
        }

        if (axiosError.request) {
          return `No response from backend. ${await getBackendHealthMessage()}`;
        }
      }

      return typeof err === 'string' ? err : 'Unexpected backend failure. Please check the Render deployment logs.';
    };

    const fetchUsers = async () => {
      try {
        const { data } = await apiClient.get<EnhancedUserDTO[]>("/api/v1/admin/users");
        setUsers(Array.isArray(data) ? data : (data as unknown)?.users || []);
      } catch (err: unknown) {
        const message = await getErrorMessage(err);
        setError(message);
        setUsers([]);
      } finally {
        setTimeout(() => setLoading(false), 800);
      }
    };

    fetchUsers();
  }, []);

  // Compute filtered users
  const filteredUsers = users.filter(user => {
    const matchesSearch = 
      user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.registrationNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (user.department || "").toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesRole = activeRole === "ALL" || user.role === activeRole;
    
    return matchesSearch && matchesRole;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">User Management</h1>
          <p className="text-slate-400 mt-1">Manage and audit system identities.</p>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto relative">
          <Button 
            variant="secondary" 
            className={`gap-2 transition-all ${activeRole !== "ALL" ? "border-primary/50 text-primary bg-primary/5" : ""}`}
            onClick={() => setIsFilterOpen(!isFilterOpen)}
          >
            <Filter size={16} /> Filters
          </Button>

          <AnimatePresence>
            {isFilterOpen && (
              <motion.div 
                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                className="absolute top-full right-0 mt-2 w-56 glass-panel p-2 z-50 shadow-2xl border border-white/10"
              >
                <div className="px-3 py-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">Filter by Role</div>
                {(["ALL", Role.STUDENT, Role.FACULTY, Role.ADMIN, Role.SUPER_ADMIN] as const).map((role) => (
                  <button
                    key={role}
                    onClick={() => {
                      setActiveRole(role);
                      setIsFilterOpen(false);
                    }}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm transition-colors ${
                      activeRole === role ? "bg-primary/20 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
                    }`}
                  >
                    {role === "ALL" ? "All Identities" : role.replace("_", " ")}
                    {activeRole === role && <Check size={14} className="text-primary" />}
                  </button>
                ))}
              </motion.div>
            )}
          </AnimatePresence>

          <Link href="/dashboard/users/onboarding">
            <Button variant="primary">Onboard User</Button>
          </Link>
        </div>
      </div>

      <div className="glass-panel p-6 shadow-2xl overflow-hidden flex flex-col">
        
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div className="flex items-center bg-obsidian-800/80 rounded-xl px-4 py-2.5 border border-white/5 w-full max-w-md shadow-inner focus-within:border-primary/40 transition-colors">
            <Search size={18} className="text-slate-400 mr-2" />
            <input 
              type="text" 
              placeholder="Search by name, email, or Reg No..." 
              className="bg-transparent border-none outline-none text-sm text-white w-full placeholder:text-slate-500"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button onClick={() => setSearchQuery("")} className="text-slate-500 hover:text-white transition-colors">
                <X size={14} />
              </button>
            )}
          </div>

          <AnimatePresence>
            {(activeRole !== "ALL" || searchQuery) && (
              <motion.div 
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="flex items-center gap-2"
              >
                <div className="px-3 py-1.5 rounded-full bg-primary/10 border border-primary/20 flex items-center gap-2 text-[11px] font-bold text-primary">
                  {activeRole !== "ALL" ? `Role: ${activeRole}` : "Active Search"}
                  <button onClick={() => { setActiveRole("ALL"); setSearchQuery(""); }} title="Clear All">
                    <X size={12} className="hover:text-white transition-colors" />
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-accent/10 border border-accent/20 flex items-center text-accent/90 text-sm">
             <AlertCircle size={18} className="mr-3 shrink-0" />
             {error}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead>
              <tr className="border-b border-white/10 text-slate-400 bg-white/[0.02]">
                <th className="px-6 py-4 font-medium rounded-tl-xl">Identify</th>
                <th className="px-6 py-4 font-medium">Role</th>
                <th className="px-6 py-4 font-medium">Department</th>
                <th className="px-6 py-4 font-medium">Device Profile</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium rounded-tr-xl">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4"><Skeleton className="h-10 w-48" /></td>
                    <td className="px-6 py-4"><Skeleton className="h-6 w-24" /></td>
                    <td className="px-6 py-4"><Skeleton className="h-6 w-32" /></td>
                    <td className="px-6 py-4"><Skeleton className="h-6 w-32" /></td>
                    <td className="px-6 py-4"><Skeleton className="h-6 w-20" /></td>
                    <td className="px-6 py-4"><Skeleton className="h-8 w-8 rounded-full" /></td>
                  </tr>
                ))
              ) : (
                filteredUsers.map((user, index) => (
                  <motion.tr 
                    key={user.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className="hover:bg-white/[0.04] transition-all cursor-pointer border-l-2 border-transparent hover:border-primary/40"
                    onClick={() => router.push(`/dashboard/users/${user.id}`)}
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-primary/20 flex flex-col items-center justify-center text-primary font-bold">
                          {user.name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-medium text-slate-200">{user.name}</p>
                          <p className="text-xs text-slate-500">{user.registrationNumber}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-slate-300">{user.role}</td>
                    <td className="px-6 py-4 text-slate-300">{user.department}</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs text-primary bg-primary/10 px-2 py-1 rounded">
                          {user.deviceId?.slice(0,12) || "UNBOUND"}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${
                        user.status === UserStatus.ACTIVE 
                          ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" 
                          : "bg-accent/10 text-accent border-accent/20"
                      }`}>
                        {user.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <button 
                        className="p-2 hover:bg-primary/20 rounded-lg text-slate-400 hover:text-primary transition-all group/action"
                        onClick={(e) => {
                          e.stopPropagation();
                          router.push(`/dashboard/users/${user.id}`);
                        }}
                        title="View Profile"
                      >
                        <Eye size={18} className="group-hover/action:scale-110 transition-transform" />
                      </button>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
          
          {!loading && filteredUsers.length === 0 && (
            <div className="py-20 text-center flex flex-col items-center">
              <div className="h-16 w-16 rounded-full bg-white/[0.03] border border-white/5 flex items-center justify-center text-slate-600 mb-4">
                 <Search size={32} />
              </div>
              <p className="text-slate-500 font-medium">Identity scan complete: No matching records found.</p>
              <button 
                onClick={() => { setActiveRole("ALL"); setSearchQuery(""); }}
                className="mt-4 text-xs font-bold text-primary uppercase tracking-widest hover:text-white transition-colors"
              >
                Reset Search parameters
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
