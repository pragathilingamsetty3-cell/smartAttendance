package com.example.smartAttendence.config;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.domain.UserStatus;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import com.example.smartAttendence.repository.v1.ClassroomSessionV1Repository;
import com.example.smartAttendence.repository.v1.AttendanceRecordV1Repository;
import com.example.smartAttendence.repository.SectionRepository;
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
    private final ClassroomSessionV1Repository sessionRepository;
    private final AttendanceRecordV1Repository attendanceRepository;
    private final SectionRepository sectionRepository;

    public SuperAdminInitializer(
            UserV1Repository userRepository, 
            PasswordEncoder passwordEncoder,
            ClassroomSessionV1Repository sessionRepository,
            AttendanceRecordV1Repository attendanceRepository,
            SectionRepository sectionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.sectionRepository = sectionRepository;
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
            
            // 🚀 DASHBOARD WARM-UP (Background)
            new Thread(() -> {
                try {
                    logger.info("🔥 [WARM-UP] Starting Dashboard background warming...");
                    long userCount = userRepository.count();
                    long sessionCount = sessionRepository.count();
                    long attendanceCount = attendanceRepository.count();
                    long sectionCount = sectionRepository.count();
                    logger.info("✅ [WARM-UP] Dashboard Ready. Users: {}, Sessions: {}, Records: {}, Sections: {}", 
                        userCount, sessionCount, attendanceCount, sectionCount);
                } catch (Exception e) {
                    logger.warn("⚠️ [WARM-UP] Dashboard warming had a hiccups, but ignoring: {}", e.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            logger.error("❌ Critical Error during SuperAdmin initialization: {}", e.getMessage());
        }
    }
}
