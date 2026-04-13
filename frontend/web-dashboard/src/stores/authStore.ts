import { create } from "zustand";
import { persist } from "zustand/middleware";
import { EnhancedUserDTO } from "../types";
import apiClient from "../lib/apiClient";

interface AuthState {
  user: EnhancedUserDTO | null;
  accessToken: string | null;
  refreshToken: string | null;
  _hasHydrated: boolean;
  setHasHydrated: (state: boolean) => void;
  setUser: (user: EnhancedUserDTO) => void;
  setToken: (token: string, refresh?: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      _hasHydrated: false,
      
      setHasHydrated: (state) => set({ _hasHydrated: state }),
      
      setUser: (user) => set({ user }),
      
      setToken: (token, refresh) => {
        if (refresh) set({ accessToken: token, refreshToken: refresh });
        else set({ accessToken: token });
      },
      
      logout: async () => {
        try {
          // Attempt graceful backend logout if possible
          await apiClient.post("/api/v1/auth/logout");
        } catch (e) {
          // Ignore server errors on logout
        } finally {
          set({ user: null, accessToken: null, refreshToken: null });
          window.location.href = "/login"; // Force full reload transition
        }
      },
    }),
    {
      name: "smart-attendance-auth",
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
