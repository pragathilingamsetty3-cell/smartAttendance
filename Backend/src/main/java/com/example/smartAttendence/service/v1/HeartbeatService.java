package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);
    private static final String COLLECTION_NAME = "heartbeats";
    private static final int MAX_PINGS = 5;

    private final Firestore firestore;

    public HeartbeatService(@Nullable Firestore firestore) {
        this.firestore = firestore;
    }

    public void recordPing(EnhancedHeartbeatPing ping) {
        if (firestore == null) {
            logger.warn("⚠️ Firestore not initialized. Skipping heartbeat record.");
            return;
        }

        Objects.requireNonNull(ping, "ping is required");
        String sessionId = Objects.requireNonNull(ping.sessionId(), "sessionId is required").toString();
        String studentId = Objects.requireNonNull(ping.studentId(), "studentId is required").toString();

        String docId = sessionId + "_" + studentId;
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(docId);

        String pingValue = ping.latitude() + "," + ping.longitude() + "," + System.currentTimeMillis();

        try {
            // 🔥 MODERN ASYNC: Use ApiFutures.addCallback correctly
            ApiFutures.addCallback(docRef.get(), new ApiFutureCallback<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot result) {
                    try {
                        List<String> pings = new ArrayList<>();
                        if (result.exists()) {
                            List<String> existing = (List<String>) result.get("pings");
                            if (existing != null) pings.addAll(existing);
                        }
                        
                        pings.add(0, pingValue); // Add to start (like leftPush)
                        if (pings.size() > MAX_PINGS) {
                            pings = pings.subList(0, MAX_PINGS);
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("pings", pings);
                        data.put("lastUpdated", com.google.cloud.Timestamp.now());
                        data.put("sessionId", sessionId);
                        data.put("studentId", studentId);

                        docRef.set(data, SetOptions.merge());
                    } catch (Exception e) {
                        logger.error("❌ Failed to process Firestore heartbeat: {}", e.getMessage());
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("❌ Firestore read failed: {}", t.getMessage());
                }
            }, com.google.common.util.concurrent.MoreExecutors.directExecutor());

        } catch (Exception e) {
            logger.error("❌ Firestore recordPing failed: {}", e.getMessage());
        }
    }

    public String driftKey(String sessionId, String studentId) {
        return "drift:" + sessionId + ":" + studentId;
    }
}

