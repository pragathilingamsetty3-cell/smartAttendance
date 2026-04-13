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

      const payload: EnhancedHeartbeatPing = {
        studentId,
        sessionId,
        deviceFingerprint,
        timestamp: new Date().toISOString(),
        latitude: 0, // In production, grab real geolocation
        longitude: 0,
        stepCount: 0, // In production, grab device pedometer bridged data
        accelerationX: 0,
        accelerationY: 0,
        accelerationZ: 0,
        isDeviceMoving: false,
        batteryLevel: battery.level,
        isCharging: battery.isCharging,
        isScreenOn: true,
        deviceState: 'STATIONARY',
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
