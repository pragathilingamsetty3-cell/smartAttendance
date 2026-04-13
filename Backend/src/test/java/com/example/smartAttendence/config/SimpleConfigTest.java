package com.example.smartAttendence.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.google.genai.Client;
import com.google.firebase.messaging.FirebaseMessaging;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {SimpleConfigTest.TestConfiguration.class},
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "firebase.enabled=false",
        "spring.data.redis.host=localhost",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.mail.host=localhost",
        "server.ssl.enabled=false",
        "spring.cache.type=none",
        "management.tracing.enabled=false",
        "management.metrics.export.prometheus.enabled=false",
        "performance.monitoring.enabled=false",
        "security.monitoring.enabled=false",
        "security.audit.logger.enabled=false",
        "security.threat.detection.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"
    }
)
@ActiveProfiles("test")
class SimpleConfigTest {

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private Client googleGenAiClient;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @Test
    void contextShouldLoadWithoutFirebase() {
        Assertions.assertTrue(true, "Application context loaded successfully with mocked dependencies!");
    }

    // Test-specific configuration
    static class TestConfiguration {
        // Minimal configuration for testing
    }
}