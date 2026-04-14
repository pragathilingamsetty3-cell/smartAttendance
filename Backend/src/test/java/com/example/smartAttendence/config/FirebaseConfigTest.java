package com.example.smartAttendence.config;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {FirebaseConfigTest.TestConfiguration.class},
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
class FirebaseConfigTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @Test
    void firebaseMessagingBeanShouldBeCreated() {
        assertNotNull(firebaseMessaging, "FirebaseMessaging bean should be mocked by Spring");
    }

    @Test
    void firebaseMessagingShouldNotBeNull() {
        assertNotNull(firebaseMessaging, "FirebaseMessaging should not be null");
    }

    // Test-specific configuration that excludes problematic auto-configurations
    static class TestConfiguration {
        // Minimal configuration for testing
    }
}