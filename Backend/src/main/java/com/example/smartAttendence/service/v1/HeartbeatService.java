package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.dto.v1.EnhancedHeartbeatPing;
import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatService {

    private static final String HEARTBEAT_KEY_PATTERN = "heartbeat:%s:%s";
    private static final Duration HEARTBEAT_TTL = Duration.ofHours(2);
    private static final int MAX_PINGS = 5;

    private final StringRedisTemplate redisTemplate;

    public HeartbeatService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordPing(EnhancedHeartbeatPing ping) {
        Objects.requireNonNull(ping, "ping is required");
        String key = heartbeatKey(
                Objects.requireNonNull(ping.sessionId(), "sessionId is required"),
                Objects.requireNonNull(ping.studentId(), "studentId is required")
        );

        String value = ping.latitude() + "," + ping.longitude();

        var ops = redisTemplate.opsForList();
        ops.leftPush(key, value);
        ops.trim(key, 0, MAX_PINGS - 1L);

        redisTemplate.expire(key, HEARTBEAT_TTL);
    }

    public String driftKey(String sessionId, String studentId) {
        return "drift:" + sessionId + ":" + studentId;
    }

    private String heartbeatKey(Object sessionId, Object studentId) {
        return HEARTBEAT_KEY_PATTERN.formatted(sessionId, studentId);
    }
}

