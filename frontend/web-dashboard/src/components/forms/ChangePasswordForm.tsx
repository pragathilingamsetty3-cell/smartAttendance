'use client';

import React, { useState } from 'react';
import { Shield, Lock, Eye, EyeOff, CheckCircle2, AlertCircle } from 'lucide-react';
import { authService } from '@/services/auth.service';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { motion, AnimatePresence } from 'framer-motion';

export const ChangePasswordForm: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPasswords, setShowPasswords] = useState(false);
  
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    // Validation
    if (formData.newPassword !== formData.confirmPassword) {
      setError("New passwords do not match.");
      return;
    }

    if (formData.newPassword.length < 8) {
      setError("New password must be at least 8 characters long.");
      return;
    }

    setLoading(true);
    try {
      await authService.changePassword({
        currentPassword: formData.currentPassword,
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword
      });
      
      setSuccess(true);
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      });
    } catch (err: any) {
      setError(err.message || "Failed to change password. Please verify your current password.");
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  if (success) {
    return (
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="p-8 text-center flex flex-col items-center justify-center bg-emerald-500/5 border border-emerald-500/10 rounded-2xl"
      >
        <div className="h-16 w-16 rounded-full bg-emerald-500/10 flex items-center justify-center text-emerald-400 mb-4 ">
           <CheckCircle2 size={32} />
        </div>
        <h3 className="text-xl font-bold text-white mb-2">Password Updated</h3>
        <p className="text-slate-400 text-sm mb-6">Your security credentials have been successfully updated across the system.</p>
        <Button variant="secondary" onClick={() => setSuccess(false)}>
           Done
        </Button>
      </motion.div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-sm font-bold text-white uppercase tracking-widest flex items-center gap-2">
           <Shield size={16} className="text-primary" /> Security Credentials
        </h3>
        <button 
          type="button"
          onClick={() => setShowPasswords(!showPasswords)}
          className="text-[10px] font-bold text-slate-500 hover:text-white transition-colors uppercase tracking-tight flex items-center gap-1.5"
        >
          {showPasswords ? <EyeOff size={12} /> : <Eye size={12} />} {showPasswords ? "Hide Passwords" : "Show Passwords"}
        </button>
      </div>

      <AnimatePresence>
        {error && (
          <motion.div 
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="p-3 rounded-xl bg-accent/10 border border-accent/20 text-accent text-xs flex items-center gap-3 overflow-hidden"
          >
             <AlertCircle size={14} className="shrink-0" />
             {error}
          </motion.div>
        )}
      </AnimatePresence>

      <div className="space-y-5">
        <Input
          name="currentPassword"
          type={showPasswords ? "text" : "password"}
          label="Current Password"
          placeholder="••••••••"
          value={formData.currentPassword}
          onChange={handleChange}
          icon={<Lock className="h-4 w-4" />}
          glass
          required
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <Input
            name="newPassword"
            type={showPasswords ? "text" : "password"}
            label="New Password"
            placeholder="Min. 8 characters"
            value={formData.newPassword}
            onChange={handleChange}
            icon={<Shield className="h-4 w-4" />}
            glass
            required
          />

          <Input
            name="confirmPassword"
            type={showPasswords ? "text" : "password"}
            label="Confirm New Password"
            placeholder="Repeat new password"
            value={formData.confirmPassword}
            onChange={handleChange}
            icon={<Shield className="h-4 w-4" />}
            glass
            required
          />
        </div>
      </div>

      <div className="pt-4">
        <Button 
          type="submit" 
          variant="primary" 
          className="w-full shadow-lg shadow-primary/20 h-12 text-sm"
          loading={loading}
        >
          Update Security Key
        </Button>
      </div>

      <div className="p-4 rounded-xl bg-white/[0.02] border border-white/5">
         <p className="text-[10px] text-slate-500 leading-relaxed font-medium">
            <span className="text-slate-400 font-bold block mb-1">Security Note:</span>
            Updating your password will invalidate all current session signatures across your linked devices for security enforcement.
         </p>
      </div>
    </form>
  );
};
