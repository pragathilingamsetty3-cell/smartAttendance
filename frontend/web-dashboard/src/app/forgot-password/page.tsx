'use client';

import { useState } from 'react';
import { Mail, ArrowLeft, Loader2, CheckCircle2, AlertCircle, KeyRound } from 'lucide-react';
import { useRouter } from 'next/navigation';
import apiClient from '@/lib/apiClient';
import { motion } from 'framer-motion';

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [step, setStep] = useState(1); // 1: Request, 2: Reset
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      if (step === 1) {
        await apiClient.post('/api/v1/auth/forgot-password', {
          emailOrPhone: email,
          method: 'EMAIL'
        });
        setStep(2);
        setSuccess('OTP sent! Please check your email.');
      } else {
        if (newPassword !== confirmPassword) {
          throw new Error('Passwords do not match');
        }
        await apiClient.post('/api/v1/auth/reset-password', {
          emailOrPhone: email,
          otp: otp,
          newPassword: newPassword,
          confirmPassword: confirmPassword
        });
        setSuccess('Password reset successfully!');
        setTimeout(() => router.push('/login'), 2000);
      }
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Action failed.';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const inputStyle: React.CSSProperties = {
    width: '100%',
    paddingLeft: '42px',
    paddingRight: '16px',
    paddingTop: '14px',
    paddingBottom: '14px',
    backgroundColor: 'rgba(255,255,255,0.04)',
    border: '1px solid rgba(255,255,255,0.1)',
    borderRadius: '14px',
    color: '#fff',
    fontSize: '14px',
    outline: 'none',
    transition: 'all 0.3s ease',
  };

  const inputFocusStyle: React.CSSProperties = {
    ...inputStyle,
    backgroundColor: 'rgba(45,156,219,0.08)',
    border: '1px solid rgba(45,156,219,0.4)',
    boxShadow: '0 0 0 3px rgba(45,156,219,0.15)',
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0920 0%, #1a103c 50%, #0f0920 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Background glow effects */}
      <div style={{
        position: 'absolute', top: '-200px', right: '-100px',
        width: '600px', height: '600px',
        background: 'radial-gradient(circle, rgba(155,81,224,0.15) 0%, transparent 70%)',
        borderRadius: '50%', pointerEvents: 'none',
      }} />
      <div style={{
        position: 'absolute', bottom: '-200px', left: '-100px',
        width: '600px', height: '600px',
        background: 'radial-gradient(circle, rgba(45,156,219,0.1) 0%, transparent 70%)',
        borderRadius: '50%', pointerEvents: 'none',
      }} />

      <motion.div
        initial={{ opacity: 0, y: 30, scale: 0.95 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        style={{ width: '100%', maxWidth: '400px', margin: '0 auto', position: 'relative', zIndex: 10 }}
      >
        <div style={{
          background: 'linear-gradient(180deg, rgba(26,16,60,0.95) 0%, rgba(15,9,32,0.98) 100%)',
          backdropFilter: 'blur(40px)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: '24px',
          boxShadow: '0 25px 50px rgba(0,0,0,0.5), 0 0 80px rgba(45,156,219,0.05)',
          padding: '48px 40px',
        }}>
          
          {/* Logo */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '40px' }}>
            <motion.div
              initial={{ scale: 0, rotate: 10 }}
              animate={{ scale: 1, rotate: -3 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
              style={{ marginBottom: '24px' }}
            >
              <div style={{
                width: '72px', height: '72px',
                background: 'linear-gradient(135deg, #2d9cdb 0%, #3b82f6 50%, #9b51e0 100%)',
                borderRadius: '18px',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: '0 8px 32px rgba(45,156,219,0.35)',
                transform: 'rotate(-3deg)',
              }}>
                <KeyRound style={{ width: '36px', height: '36px', color: '#fff', filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.3))' }} />
              </div>
            </motion.div>
            <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em', margin: 0 }}>
              {step === 1 ? 'Reset Access' : 'Create New Password'}
            </h1>
            <p style={{ fontSize: '10px', fontWeight: 600, color: '#64748b', letterSpacing: '0.15em', textTransform: 'uppercase', marginTop: '6px', textAlign: 'center' }}>
              {step === 1 ? 'Enter your email to recover your account' : 'Enter the OTP sent to your email'}
            </p>
          </div>

          <form onSubmit={handleSubmit} autoComplete="off">
            {success && (
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                style={{
                  background: 'rgba(16,185,129,0.08)',
                  border: '1px solid rgba(16,185,129,0.2)',
                  borderRadius: '12px',
                  padding: '12px',
                  display: 'flex', alignItems: 'center', gap: '10px',
                  marginBottom: '20px',
                }}
              >
                <CheckCircle2 style={{ width: '16px', height: '16px', color: '#34d399' }} />
                <p style={{ color: '#6ee7b7', fontSize: '13px', margin: 0 }}>{success}</p>
              </motion.div>
            )}

            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                style={{
                  marginBottom: '20px',
                  background: 'rgba(239,68,68,0.08)',
                  border: '1px solid rgba(239,68,68,0.2)',
                  borderRadius: '12px',
                  padding: '14px',
                  display: 'flex', alignItems: 'center', gap: '12px',
                }}
              >
                <AlertCircle style={{ width: '16px', height: '16px', color: '#f87171', flexShrink: 0 }} />
                <span style={{ color: '#fca5a5', fontSize: '13px' }}>{error}</span>
              </motion.div>
            )}

            {step === 1 ? (
              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px', marginLeft: '2px' }}>
                  Email Address
                </label>
                <div style={{ position: 'relative' }}>
                  <div style={{ position: 'absolute', top: '50%', left: '14px', transform: 'translateY(-50%)', pointerEvents: 'none' }}>
                    <Mail style={{ width: '16px', height: '16px', color: isFocused ? '#2d9cdb' : '#475569', transition: 'color 0.3s' }} />
                  </div>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    autoComplete="off"
                    style={isFocused ? inputFocusStyle : inputStyle}
                    placeholder="you@example.com"
                    required
                    disabled={isLoading}
                  />
                </div>
              </div>
            ) : (
              <>
                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px' }}>
                    Enter 6-Digit OTP
                  </label>
                  <input
                    type="text"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    style={inputStyle}
                    placeholder="000000"
                    required
                    maxLength={6}
                  />
                </div>
                <div style={{ marginBottom: '16px' }}>
                  <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px' }}>
                    New Password
                  </label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    style={inputStyle}
                    placeholder="••••••••"
                    required
                  />
                </div>
                <div style={{ marginBottom: '24px' }}>
                  <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px' }}>
                    Confirm Password
                  </label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    style={inputStyle}
                    placeholder="••••••••"
                    required
                  />
                </div>
              </>
            )}

            <motion.button
              type="submit"
              disabled={isLoading}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              style={{
                width: '100%', padding: '14px',
                background: 'linear-gradient(135deg, #2d9cdb 0%, #3b82f6 50%, #9b51e0 100%)',
                color: '#fff',
                fontWeight: 600, fontSize: '14px',
                border: 'none', borderRadius: '14px',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                boxShadow: '0 8px 24px rgba(45,156,219,0.3)',
                transition: 'all 0.3s ease',
                opacity: isLoading ? 0.6 : 1,
                marginBottom: '12px',
              }}
            >
              {isLoading ? (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                  <Loader2 style={{ width: '16px', height: '16px', animation: 'spin 1s linear infinite' }} />
                  Processing...
                </div>
              ) : (
                step === 1 ? 'Send Recovery Email' : 'Reset Password'
              )}
            </motion.button>

            <button
              type="button"
              onClick={() => step === 2 ? setStep(1) : router.push('/login')}
              style={{
                width: '100%', padding: '12px',
                background: 'none', border: 'none',
                color: '#64748b', fontSize: '12px', fontWeight: 500,
                cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                transition: 'color 0.2s',
              }}
            >
              <ArrowLeft style={{ width: '14px', height: '14px' }} />
              {step === 2 ? 'Go Back' : 'Back to Login'}
            </button>
          </form>

          {/* Footer */}
          <div style={{
            marginTop: '32px', paddingTop: '24px',
            borderTop: '1px solid rgba(255,255,255,0.05)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}>
            <div style={{
              width: '6px', height: '6px',
              borderRadius: '50%',
              backgroundColor: '#2d9cdb',
              boxShadow: '0 0 8px rgba(45,156,219,0.5)',
              animation: 'pulse 2s ease-in-out infinite',
            }} />
            <p style={{ color: '#475569', fontSize: '10px', fontWeight: 600, letterSpacing: '0.15em', textTransform: 'uppercase', margin: 0 }}>
              Secured by Smart Attendance
            </p>
          </div>
        </div>
      </motion.div>

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
        input::placeholder {
          color: #334155 !important;
        }
      `}</style>
    </div>
  );
}
