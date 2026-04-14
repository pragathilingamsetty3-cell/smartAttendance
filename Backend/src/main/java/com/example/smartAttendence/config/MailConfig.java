package com.example.smartAttendence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.annotation.PostConstruct;
import org.springframework.mail.SimpleMailMessage;

@Configuration
public class MailConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private String starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    private String sslTrust;

    @PostConstruct
    public void debugMailConfiguration() {
        logger.debug("🔧 MAIL CONFIGURATION DEBUG:");
        logger.debug("==========================================");
        logger.debug("📧 EMAIL_USERNAME: [{}]", mailUsername);
        logger.debug("🔐 EMAIL_PASSWORD: [{}]", mailPassword);
        logger.debug("🌐 MAIL_HOST: [{}]", mailHost);
        logger.debug("🚪 MAIL_PORT: [{}]", mailPort);
        logger.debug("🔐 SMTP_AUTH: [{}]", smtpAuth);
        logger.debug("🔒 STARTTLS_ENABLE: [{}]", starttlsEnable);
        logger.debug("🔒 STARTTLS_REQUIRED: [{}]", starttlsRequired);
        logger.debug("🔒 SSL_TRUST: [{}]", sslTrust);
        logger.debug("==========================================");
        
        logger.info("🔧 MAIL CONFIGURATION DEBUG:");
        logger.info("📧 EMAIL_USERNAME: [{}]", mailUsername);
        logger.info("🔐 EMAIL_PASSWORD: [{}]", mailPassword);
        logger.info("🌐 MAIL_HOST: [{}]", mailHost);
        logger.info("🚪 MAIL_PORT: [{}]", mailPort);
        logger.info("🔐 SMTP_AUTH: [{}]", smtpAuth);
        logger.info("🔒 STARTTLS_ENABLE: [{}]", starttlsEnable);
        logger.info("🔒 STARTTLS_REQUIRED: [{}]", starttlsRequired);
        logger.info("🔒 SSL_TRUST: [{}]", sslTrust);

        // 🔍 VERIFY GMAIL ADDRESS FORMAT
        if (mailUsername != null && !mailUsername.endsWith("@gmail.com")) {
            logger.warn("⚠️ WARNING: Email username does not appear to be a Gmail address: {}", mailUsername);
        } else if (mailUsername != null && mailUsername.endsWith("@gmail.com")) {
            logger.info("✅ Gmail address format confirmed: {}", mailUsername);
        }

        // 🔍 CHECK FOR EMPTY OR NULL VALUES
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            logger.error("❌ ERROR: Email password is null or empty!");
        } else {
            logger.info("✅ Email password length: {} characters", mailPassword.length());
        }

        // 🔧 TEST MAIL CONNECTIVITY
        testMailConnectivity(); // Enabled to verify Gmail password
    }

    /**
     * Test mail connectivity by attempting to send a test message
     */
    public void testMailConnectivity() {
        try {
            logger.info("🧪 TESTING MAIL CONNECTIVITY...");
            
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setTo(mailUsername); // Send test to self
            testMessage.setSubject("🧪 Smart Attendance - Mail Configuration Test");
            testMessage.setText("This is a test message to verify mail configuration is working correctly.\n\n" +
                           "If you receive this email, your mail configuration is working!\n\n" +
                           "Sent at: " + new java.util.Date() + "\n" +
                           "From: Smart Attendance System");
            
            mailSender.send(testMessage);
            
            logger.info("✅ MAIL CONNECTIVITY TEST PASSED - Test email sent successfully!");
            
        } catch (Exception e) {
            logger.error("❌ MAIL CONNECTIVITY TEST FAILED: {}", e.getMessage(), e);
            
            // Print detailed error information
            logger.error("🔍 DETAILED ERROR ANALYSIS:");
            logger.error("  Error Type: {}", e.getClass().getSimpleName());
            logger.error("  Error Message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("  Root Cause: {}", e.getCause().getMessage());
            }
        }
    }
}
