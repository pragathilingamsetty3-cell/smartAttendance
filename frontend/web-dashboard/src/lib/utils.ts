import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Generate an initial fake device footprint stored heavily in browser 
export const getDeviceFingerprint = (): string => {
  if (typeof window === "undefined") return "server-initialization";
  const stored = localStorage.getItem("sa_fingerprint");
  if (stored) return stored;
  
  const hash = crypto.randomUUID();
  localStorage.setItem("sa_fingerprint", hash);
  return hash;
};
