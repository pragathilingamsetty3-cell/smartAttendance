import * as React from "react";
import { motion, HTMLMotionProps } from "framer-motion";
import { cn } from "../../lib/utils";

export interface ButtonProps extends HTMLMotionProps<"button"> {
  variant?: "primary" | "secondary" | "danger" | "ghost" | "glass" | "success" | "error";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", size = "md", loading, children, ...props }, ref) => {
    
    const variants = {
      primary: "bg-violet-600 text-white shadow-[0_0_20px_rgba(124,58,237,0.25)] border border-violet-500/50 hover:bg-violet-500 active:scale-[0.98]",
      secondary: "bg-[#0F0F16] text-gray-200 hover:bg-[#13131F] border border-gray-800 shadow-lg shadow-black/20",
      danger: "bg-red-600 text-white hover:bg-red-500 shadow-lg shadow-red-600/20",
      ghost: "bg-transparent hover:bg-white/5 text-gray-400 hover:text-white transition-colors border border-transparent",
      glass: "bg-[#0F0F16]/50 backdrop-blur-md hover:bg-[#13131F] text-gray-200 border border-gray-800/50 shadow-sm",
      success: "bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 border border-emerald-500/30",
      error: "bg-red-500/15 text-red-400 hover:bg-red-500/25 border border-red-500/30",
    };

    const sizes = {
      sm: "h-9 px-4 text-xs rounded-lg",
      md: "h-11 px-6 text-sm rounded-xl",
      lg: "h-14 px-8 text-base rounded-2xl",
    };

    return (
      <motion.button
        ref={ref}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        className={cn(
          "inline-flex items-center justify-center font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-violet-500/50 disabled:opacity-50 disabled:pointer-events-none relative overflow-hidden",
          variants[variant],
          sizes[size],
          className
        )}
        {...props}
      >
        {loading ? (
          <div className="flex items-center space-x-2">
            <svg className="animate-spin h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span className="opacity-80">{children as React.ReactNode}</span>
          </div>
        ) : (
          (children as React.ReactNode)
        )}
      </motion.button>
    );
  }
);

Button.displayName = "Button";
