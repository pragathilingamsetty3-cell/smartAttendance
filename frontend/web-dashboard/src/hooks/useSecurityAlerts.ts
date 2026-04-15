/**
 * useSecurityAlerts Hook
 * Subscribes to live security alerts from Firestore 'security_audit_logs' collection.
 * Replaces polling-based threat monitoring.
 */
'use client';

import { useEffect, useState } from 'react';
import {
  collection,
  query,
  where,
  orderBy,
  limit,
  onSnapshot,
  Timestamp,
  type DocumentData,
} from 'firebase/firestore';
import { db } from '@/lib/firebase';

type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type AlertType =
  | 'GPS_SPOOFING'
  | 'UNAUTHORIZED_WALKOUT'
  | 'BRUTE_FORCE'
  | 'JWT_BLACKLIST'
  | 'DEVICE_MISMATCH'
  | 'RATE_LIMIT_EXCEEDED'
  | string;

export interface SecurityAlert {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  userId?: string;
  ipAddress?: string;
  details: string;
  timestamp: Date;
  resolved: boolean;
}

interface UseSecurityAlertsOptions {
  maxAlerts?: number;
  minSeverity?: AlertSeverity;
  enabled?: boolean;
}

interface UseSecurityAlertsResult {
  alerts: SecurityAlert[];
  criticalCount: number;
  unresolved: SecurityAlert[];
  isConnected: boolean;
}

const SEVERITY_ORDER: Record<AlertSeverity, number> = {
  LOW: 0,
  MEDIUM: 1,
  HIGH: 2,
  CRITICAL: 3,
};

function parseAlert(id: string, data: DocumentData): SecurityAlert {
  return {
    id,
    type: data.type ?? 'UNKNOWN',
    severity: data.severity ?? 'LOW',
    userId: data.userId,
    ipAddress: data.ipAddress,
    details: data.details ?? '',
    timestamp:
      data.timestamp instanceof Timestamp
        ? data.timestamp.toDate()
        : new Date(),
    resolved: data.resolved ?? false,
  };
}

export function useSecurityAlerts({
  maxAlerts = 50,
  minSeverity = 'LOW',
  enabled = true,
}: UseSecurityAlertsOptions = {}): UseSecurityAlertsResult {
  const [alerts, setAlerts] = useState<SecurityAlert[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    if (!enabled) return;

    const q = query(
      collection(db, 'security_audit_logs'),
      where('resolved', '==', false),
      orderBy('timestamp', 'desc'),
      limit(maxAlerts)
    );

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        setIsConnected(true);
        const minOrder = SEVERITY_ORDER[minSeverity];
        const loaded = snapshot.docs
          .map((doc) => parseAlert(doc.id, doc.data()))
          .filter((a) => SEVERITY_ORDER[a.severity] >= minOrder);
        setAlerts(loaded);
      },
      (err) => {
        console.error('[useSecurityAlerts] Firestore error:', err);
        setIsConnected(false);
      }
    );

    return () => unsubscribe();
  }, [enabled, maxAlerts, minSeverity]);

  return {
    alerts,
    criticalCount: alerts.filter((a) => a.severity === 'CRITICAL').length,
    unresolved: alerts.filter((a) => !a.resolved),
    isConnected,
  };
}
