import { useState, useEffect, useRef } from 'react';
import { EnhancedHeartbeatPing } from '../types/attendance';
import { attendanceService } from '../services/attendance.service';

interface DynamicHeartbeatProps {
  studentId: string;
  sessionId: string;
  deviceFingerprint: string;
}

export const useDynamicHeartbeat = ({ studentId, sessionId, deviceFingerprint }: DynamicHeartbeatProps) => {
  const [intervalTime, setIntervalTime] = useState<number>(30000); // default 30s
  const [isTracking, setIsTracking] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // 🚶 LIVE MOTION STATE — updated by DeviceMotion API
  const motionRef = useRef({
    accelerationX: 0,
    accelerationY: 0,
    accelerationZ: 0,
    isDeviceMoving: false,
    deviceState: 'STATIONARY' as 'STATIONARY' | 'MOVING' | 'WALKING',
  });

  // 📍 LIVE GPS STATE — updated by watchPosition
  const locationRef = useRef({ latitude: 0, longitude: 0 });
  const watchIdRef = useRef<number | null>(null);

  // Start listening to DeviceMotion events
  useEffect(() => {
    const handleMotion = (event: DeviceMotionEvent) => {
      const accel = event.accelerationIncludingGravity;
      if (!accel) return;

      const ax = accel.x || 0;
      const ay = accel.y || 0;
      const az = accel.z || 0;
      const magnitude = Math.sqrt(ax * ax + ay * ay + az * az);

      // Subtract gravity (~9.8) to get pure motion magnitude
      const motionMagnitude = Math.abs(magnitude - 9.8);

      motionRef.current = {
        accelerationX: ax,
        accelerationY: ay,
        accelerationZ: az,
        isDeviceMoving: motionMagnitude > 1.5,
        deviceState: motionMagnitude > 6.0 ? 'WALKING' : motionMagnitude > 1.5 ? 'MOVING' : 'STATIONARY',
      };
    };

    window.addEventListener('devicemotion', handleMotion);
    return () => window.removeEventListener('devicemotion', handleMotion);
  }, []);

  // Start GPS watch when tracking
  useEffect(() => {
    if (isTracking && navigator.geolocation) {
      watchIdRef.current = navigator.geolocation.watchPosition(
        (position) => {
          locationRef.current = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };
        },
        (error) => console.warn('GPS watch error:', error.message),
        { enableHighAccuracy: true, maximumAge: 10000, timeout: 15000 }
      );
    }

    return () => {
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
    };
  }, [isTracking]);

  const getBatteryStats = async () => {
    try {
      if ('getBattery' in navigator) {
        const battery: any = await (navigator as any).getBattery();
        return {
          level: battery.level,
          isCharging: battery.charging
        };
      }
    } catch (e) {
      console.warn('Battery API not available');
    }
    return { level: 1, isCharging: false };
  };

  const pingHeartbeat = async () => {
    try {
      const battery = await getBatteryStats();
      const motion = motionRef.current;
      const location = locationRef.current;

      const payload: EnhancedHeartbeatPing = {
        studentId,
        sessionId,
        deviceFingerprint,
        timestamp: new Date().toISOString(),
        latitude: location.latitude,
        longitude: location.longitude,
        stepCount: 0,
        accelerationX: motion.accelerationX,
        accelerationY: motion.accelerationY,
        accelerationZ: motion.accelerationZ,
        isDeviceMoving: motion.isDeviceMoving,
        batteryLevel: battery.level,
        isCharging: battery.isCharging,
        isScreenOn: true,
        deviceState: motion.deviceState,
        nextHeartbeatInterval: intervalTime
      };

      const response = await attendanceService.sendHeartbeatEnhanced(payload);

      // The backend dictates the next interval based on AI analysis
      if (response && response.aiLearning) {
        const recommendedInterval = response.aiLearning.optimalHeartbeatInterval;
        if (recommendedInterval && recommendedInterval !== intervalTime) {
          setIntervalTime(recommendedInterval);
        }
      }
    } catch (error) {
      console.error('Failed to send heartbeat', error);
    }
  };

  useEffect(() => {
    if (isTracking) {
      pingHeartbeat(); // initial ping
      timerRef.current = setInterval(pingHeartbeat, intervalTime);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isTracking, intervalTime, studentId, sessionId, deviceFingerprint]);

  return {
    startTracking: () => setIsTracking(true),
    stopTracking: () => setIsTracking(false),
    currentInterval: intervalTime
  };
};
