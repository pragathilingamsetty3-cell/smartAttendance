"use client";

import { LogOut, Bell, Search } from "lucide-react";
import { useAuthStore } from "../../stores/authStore";

export function Header() {
  const logout = useAuthStore((s) => s.logout);
  const user = useAuthStore((s) => s.user);

  return (
    <header className="h-20 px-8 flex items-center justify-between glass-panel rounded-none border-t-0 border-l-0 border-r-0 sticky top-0 z-10">
      <div className="flex items-center bg-obsidian-800/50 rounded-xl px-4 py-2 border border-white/5 w-96">
        <Search size={18} className="text-slate-400 mr-2" />
        <input 
          type="text" 
          placeholder="Search endpoints, users, or metrics..." 
          className="bg-transparent border-none outline-none text-sm text-white w-full placeholder:text-slate-500"
        />
      </div>

      <div className="flex items-center gap-6">
        <button className="relative p-2 text-slate-400 hover:text-white transition-colors">
          <Bell size={20} />
          <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-accent animate-pulse" />
        </button>

        <div className="h-8 w-px bg-white/10" />

        <div className="flex items-center gap-4">
          <div className="text-right hidden md:block">
            <p className="text-sm font-medium text-white">{user?.name || "Admin User"}</p>
            <p className="text-xs text-slate-400">{user?.role || "SUPER_ADMIN"}</p>
          </div>
          <div className="h-10 w-10 rounded-full bg-gradient-to-tr from-primary to-secondary flex items-center justify-center text-white font-bold neon-border">
            {user?.name?.charAt(0) || "A"}
          </div>

          <button 
            onClick={() => logout()}
            className="p-2 ml-2 text-slate-400 hover:text-accent hover:bg-accent/10 rounded-lg transition-colors group"
            title="Secure Logout"
          >
            <LogOut size={20} className="group-hover:-translate-x-0.5 transition-transform" />
          </button>
        </div>
      </div>
    </header>
  );
}
