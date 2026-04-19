'use client';

import { useState } from 'react';
import { Shield, Mail, Lock, AlertCircle, CheckCircle2, Loader2, Fingerprint } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/authStore'; 
import apiClient from '@/lib/apiClient';
import { getDeviceFingerprint } from '@/lib/utils';
import { motion } from 'framer-motion';

export default function LoginPage() {
  const { setToken, setUser } = useAuthStore();
  const router = useRouter();

  const [formData, setFormData] = useState({ email: '', password: '' });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [focusedField, setFocusedField] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setIsLoading(true);

    try {
      const res = await apiClient.post('/api/v1/auth/login', {
        email: formData.email,
        password: formData.password,
        deviceFingerprint: getDeviceFingerprint()
      });

      setToken(res.data.accessToken, res.data.refreshToken);
      setUser(res.data.user);

      if (res.data.requiresFirstLoginSetup) {
        setSuccess('Setup required. Redirecting to setup...');
        setTimeout(() => {
          router.push('/setup');
        }, 800);
      } else {
        setSuccess('Login successful! Redirecting to dashboard...');
        setTimeout(() => {
          router.push('/dashboard');
        }, 800);
      }
    } catch (err: unknown) {
      // @ts-expect-error omega clearance
      const errorMessage = err.response?.data?.error || err.message || 'Login failed. Please try again.';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
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
    backgroundColor: 'rgba(155,81,224,0.08)',
    border: '1px solid rgba(155,81,224,0.4)',
    boxShadow: '0 0 0 3px rgba(155,81,224,0.15)',
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
        {/* Card */}
        <div style={{
          background: 'linear-gradient(180deg, rgba(26,16,60,0.95) 0%, rgba(15,9,32,0.98) 100%)',
          backdropFilter: 'blur(40px)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: '24px',
          boxShadow: '0 25px 50px rgba(0,0,0,0.5), 0 0 80px rgba(155,81,224,0.05)',
          padding: '48px 40px',
        }}>
          
          {/* Logo */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '40px' }}>
            <motion.div 
              initial={{ scale: 0, rotate: -10 }}
              animate={{ scale: 1, rotate: 3 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
              style={{ position: 'relative', marginBottom: '24px' }}
            >
              <div style={{
                width: '72px', height: '72px',
                background: 'linear-gradient(135deg, #9b51e0 0%, #7c3aed 50%, #2d9cdb 100%)',
                borderRadius: '18px',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                boxShadow: '0 8px 32px rgba(155,81,224,0.35)',
                transform: 'rotate(3deg)',
              }}>
                <Shield style={{ width: '36px', height: '36px', color: '#fff', filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.3))' }} />
              </div>
              <div style={{
                position: 'absolute', bottom: '-4px', right: '-4px',
                width: '22px', height: '22px',
                backgroundColor: '#34d399',
                borderRadius: '50%',
                border: '3px solid #0f0920',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Fingerprint style={{ width: '12px', height: '12px', color: '#0f0920' }} />
              </div>
            </motion.div>
            <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#fff', letterSpacing: '-0.02em', margin: 0 }}>
              Smart Attendance
            </h1>
            <p style={{ fontSize: '10px', fontWeight: 600, color: '#64748b', letterSpacing: '0.2em', textTransform: 'uppercase', marginTop: '6px' }}>
              Secure Identity Portal
            </p>
          </div>

          {/* Status messages */}
          {success && (
            <motion.div 
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              style={{
                marginBottom: '24px',
                background: 'rgba(16,185,129,0.08)',
                border: '1px solid rgba(16,185,129,0.2)',
                borderRadius: '12px',
                padding: '14px',
                display: 'flex', alignItems: 'center', gap: '12px',
              }}
            >
              <CheckCircle2 style={{ width: '16px', height: '16px', color: '#34d399', flexShrink: 0 }} />
              <span style={{ color: '#6ee7b7', fontSize: '13px' }}>{success}</span>
            </motion.div>
          )}

          {error && (
            <motion.div 
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              style={{
                marginBottom: '24px',
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

          {/* Login form */}
          <form onSubmit={handleLogin} autoComplete="off">
            {/* Email */}
            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px', marginLeft: '2px' }}>
                Email Address
              </label>
              <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', top: '50%', left: '14px', transform: 'translateY(-50%)', pointerEvents: 'none' }}>
                  <Mail style={{ width: '16px', height: '16px', color: focusedField === 'email' ? '#9b51e0' : '#475569', transition: 'color 0.3s' }} />
                </div>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  onFocus={() => setFocusedField('email')}
                  onBlur={() => setFocusedField(null)}
                  autoComplete="off"
                  style={focusedField === 'email' ? inputFocusStyle : inputStyle}
                  placeholder="you@example.com"
                  required
                  disabled={isLoading}
                />
              </div>
            </div>

            {/* Password */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', color: '#94a3b8', fontSize: '12px', fontWeight: 500, marginBottom: '8px', marginLeft: '2px' }}>
                Password
              </label>
              <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', top: '50%', left: '14px', transform: 'translateY(-50%)', pointerEvents: 'none' }}>
                  <Lock style={{ width: '16px', height: '16px', color: focusedField === 'password' ? '#9b51e0' : '#475569', transition: 'color 0.3s' }} />
                </div>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  onFocus={() => setFocusedField('password')}
                  onBlur={() => setFocusedField(null)}
                  autoComplete="new-password"
                  style={focusedField === 'password' ? inputFocusStyle : inputStyle}
                  placeholder="••••••••••"
                  required
                  disabled={isLoading}
                />
              </div>
            </div>

            {/* Forgot password */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '24px' }}>
              <button
                type="button"
                onClick={() => router.push('/forgot-password')}
                style={{
                  background: 'none', border: 'none', cursor: 'pointer',
                  color: '#64748b', fontSize: '12px', fontWeight: 500,
                  padding: 0,
                  transition: 'color 0.2s',
                }}
                onMouseEnter={e => (e.currentTarget.style.color = '#9b51e0')}
                onMouseLeave={e => (e.currentTarget.style.color = '#64748b')}
              >
                Forgot password?
              </button>
            </div>

            {/* Submit */}
            <motion.button
              type="submit"
              disabled={isLoading}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              style={{
                width: '100%',
                padding: '14px',
                background: 'linear-gradient(135deg, #9b51e0 0%, #7c3aed 50%, #6d28d9 100%)',
                color: '#fff',
                fontWeight: 600,
                fontSize: '14px',
                border: 'none',
                borderRadius: '14px',
                cursor: isLoading ? 'not-allowed' : 'pointer',
                boxShadow: '0 8px 24px rgba(155,81,224,0.3)',
                transition: 'all 0.3s ease',
                opacity: isLoading ? 0.6 : 1,
              }}
            >
              {isLoading ? (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                  <Loader2 style={{ width: '16px', height: '16px', animation: 'spin 1s linear infinite' }} />
                  Authenticating...
                </div>
              ) : (
                'Sign In'
              )}
            </motion.button>
          </form>

          {/* Footer */}
          <div style={{
            marginTop: '32px',
            paddingTop: '24px',
            borderTop: '1px solid rgba(255,255,255,0.05)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
          }}>
            <div style={{
              width: '6px', height: '6px',
              borderRadius: '50%',
              backgroundColor: '#34d399',
              boxShadow: '0 0 8px rgba(52,211,153,0.5)',
              animation: 'pulse 2s ease-in-out infinite',
            }} />
            <p style={{ color: '#475569', fontSize: '10px', fontWeight: 600, letterSpacing: '0.15em', textTransform: 'uppercase', margin: 0 }}>
              Zero-Trust Verification Active
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