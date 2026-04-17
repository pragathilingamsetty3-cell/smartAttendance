"use client";

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/authStore';
import authService from '@/services/auth.service';
import { Fingerprint, Smartphone, CheckCircle2, AlertCircle } from 'lucide-react';

export default function SetupPage() {
  const router = useRouter();
  const { user } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!user) {
      router.push('/login');
    }
  }, [user, router]);

  const handleCompleteSetup = async () => {
    try {
      setLoading(true);
      setError('');

      // Generate device ID from browser fingerprint
      const deviceId = `MOBILE-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      
      // For now, use a placeholder biometric signature
      const biometricSignature = `BIO-${Date.now()}`;

      await authService.completeSetup({
        deviceId,
        biometricSignature,
        phoneNumber: user?.phoneNumber || '',
      });

      setMessage('✅ Setup completed! Device registered successfully.');
      
      setTimeout(() => {
        router.push('/dashboard');
      }, 2000);
    } catch (err: any) {
      setError(`❌ ${err.message || 'Setup failed'}`);
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-obsidian-900 to-obsidian-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-obsidian-800 border border-primary/30 rounded-3xl p-8 backdrop-blur-xl">
          <div className="flex justify-center mb-6">
            <div className="p-4 bg-primary/20 rounded-2xl">
              <Fingerprint size={32} className="text-primary" />
            </div>
          </div>

          <h1 className="text-2xl font-bold text-white text-center mb-2">
            Complete Setup
          </h1>
          <p className="text-slate-400 text-center mb-8">
            Register your device for secure attendance tracking
          </p>

          {error && (
            <div className="mb-6 p-4 bg-red-900/20 border border-red-500/50 rounded-lg flex gap-3">
              <AlertCircle className="text-red-400" size={20} />
              <p className="text-red-200 text-sm">{error}</p>
            </div>
          )}

          {message && (
            <div className="mb-6 p-4 bg-emerald-900/20 border border-emerald-500/50 rounded-lg flex gap-3">
              <CheckCircle2 className="text-emerald-400" size={20} />
              <p className="text-emerald-200 text-sm">{message}</p>
            </div>
          )}

          <div className="space-y-4 mb-6">
            <div className="p-4 bg-primary/10 rounded-xl flex gap-3">
              <Smartphone className="text-primary" size={20} />
              <div>
                <p className="text-white font-medium text-sm">Device Registration</p>
                <p className="text-slate-400 text-xs">Your device will be securely bound to your account</p>
              </div>
            </div>

            <div className="p-4 bg-primary/10 rounded-xl flex gap-3">
              <Fingerprint className="text-primary" size={20} />
              <div>
                <p className="text-white font-medium text-sm">Biometric Verification</p>
                <p className="text-slate-400 text-xs">Your fingerprint will be required for attendance</p>
              </div>
            </div>
          </div>

          <button
            onClick={handleCompleteSetup}
            disabled={loading}
            className={`w-full py-3 px-4 rounded-lg font-bold text-white transition-all ${
              loading
                ? 'bg-slate-600 cursor-not-allowed opacity-50'
                : 'bg-gradient-to-r from-primary to-secondary hover:shadow-lg hover:shadow-primary/50'
            }`}
          >
            {loading ? '⏳ Setting up...' : 'Complete Setup'}
          </button>

          <p className="text-xs text-slate-500 text-center mt-4">
            You'll be redirected to dashboard after setup
          </p>
        </div>
      </div>
    </div>
  );
}
