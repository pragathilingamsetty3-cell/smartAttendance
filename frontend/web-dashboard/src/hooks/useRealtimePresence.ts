/**
 * useRealtimePresence Hook
 * Subscribes to live student presence data from Firestore 'presence_pings' collection.
 * Replaces the previous WebSocket-based heartbeat approach.
 */
'use client';

import { useEffect, useState, useCallback } from 'react';
import {
  collection,
  query,
  where,
  onSnapshot,
  Timestamp,
  type DocumentData,
} from 'firebase/firestore';
import { db } from '@/lib/firebase';

export interface StudentPresence {
  studentId: string;
  sessionId: string;
  lat: number;
  lng: number;
  accuracy: number;
  deviceId: string;
  timestamp: Date;
  status: 'PRESENT' | 'DRIFTED' | 'ABSENT' | 'WALKOUT';
}

interface UseRealtimePresenceOptions {
  sessionId: string;
  enabled?: boolean;
}

interface UseRealtimePresenceResult {
  presenceMap: Map<string, StudentPresence>;
  presentCount: number;
  driftedStudents: StudentPresence[];
  walkoutStudents: StudentPresence[];
  isConnected: boolean;
  error: string | null;
}

export function useRealtimePresence({
  sessionId,
  enabled = true,
}: UseRealtimePresenceOptions): UseRealtimePresenceResult {
  const [presenceMap, setPresenceMap] = useState<Map<string, StudentPresence>>(
    new Map()
  );
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const parsePresence = useCallback(
    (id: string, data: DocumentData): StudentPresence => ({
      studentId: id,
      sessionId: data.sessionId ?? sessionId,
      lat: data.lat ?? 0,
      lng: data.lng ?? 0,
      accuracy: data.accuracy ?? 0,
      deviceId: data.deviceId ?? '',
      timestamp:
        data.timestamp instanceof Timestamp
          ? data.timestamp.toDate()
          : new Date(),
      status: data.status ?? 'ABSENT',
    }),
    [sessionId]
  );

  useEffect(() => {
    if (!enabled || !sessionId) return;

    // Subscribe to 5-minute active window to reduce read costs
    const fiveMinutesAgo = Timestamp.fromDate(
      new Date(Date.now() - 5 * 60 * 1000)
    );

    const q = query(
      collection(db, 'presence_pings'),
      where('sessionId', '==', sessionId),
      where('timestamp', '>=', fiveMinutesAgo)
    );

    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        setIsConnected(true);
        setError(null);

        setPresenceMap((prev) => {
          const next = new Map(prev);
          snapshot.docChanges().forEach((change) => {
            const presence = parsePresence(change.doc.id, change.doc.data());
            if (change.type === 'removed') {
              next.delete(presence.studentId);
            } else {
              next.set(presence.studentId, presence);
            }
          });
          return next;
        });
      },
      (err) => {
        console.error('[useRealtimePresence] Firestore error:', err);
        setIsConnected(false);
        setError(err.message);
      }
    );

    return () => {
      unsubscribe();
      setIsConnected(false);
    };
  }, [sessionId, enabled, parsePresence]);

  const presenceList = Array.from(presenceMap.values());

  return {
    presenceMap,
    presentCount: presenceList.filter((p) => p.status === 'PRESENT').length,
    driftedStudents: presenceList.filter((p) => p.status === 'DRIFTED'),
    walkoutStudents: presenceList.filter((p) => p.status === 'WALKOUT'),
    isConnected,
    error,
  };
}
