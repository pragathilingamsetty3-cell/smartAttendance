package com.example.smartAttendence.config;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.domain.UserStatus;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Lazy(false)
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminInitializer.class);
    
    private final UserV1Repository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminInitializer(UserV1Repository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("🔍 [DIAGNOSTIC] SuperAdminInitializer starting check...");
        String adminEmail = "super.admin@smartattendence.com";
        
        try {
            if (userRepository.findByEmailIgnoreCase(adminEmail).isEmpty()) {
                logger.info("Initializing SuperAdmin account...");
            
            User superAdmin = new User();
            superAdmin.setName("Super Admin");
            superAdmin.setEmail(adminEmail);
            superAdmin.setRegistrationNumber("ADMIN-001");
            superAdmin.setPassword(passwordEncoder.encode("Pragathi@2105"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setStatus(UserStatus.ACTIVE);
            superAdmin.setFirstLogin(false);
            superAdmin.setIsTemporaryPassword(false);
            
            userRepository.save(superAdmin);
            logger.info("✅ SuperAdmin initialized successfully with email: {}", adminEmail);
            } else {
                logger.info("SuperAdmin account already exists.");
            }
        } catch (Exception e) {
            logger.error("❌ Critical Error during SuperAdmin initialization: {}", e.getMessage());
        }
    }
}
