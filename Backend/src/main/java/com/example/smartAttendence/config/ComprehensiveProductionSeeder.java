package com.example.smartAttendence.config;

import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.domain.UserStatus;
import com.example.smartAttendence.entity.Department;
import com.example.smartAttendence.entity.Room;
import com.example.smartAttendence.entity.Section;
import com.example.smartAttendence.entity.Timetable;
import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.DepartmentRepository;
import com.example.smartAttendence.repository.RoomRepository;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 🚀 UNIFIED INSTITUTIONAL SEEDER
 * 
 * Seeds exactly:
 * - 5 Departments
 * - 4 Sections per department (20 total)
 * - 20 Faculty per department (100 total)
 * - 10 Students per section (200 total)
 * - 3-Period Timetable for today for ALL sections
 */
// @Component
@Order(3)
public class ComprehensiveProductionSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveProductionSeeder.class);
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final UserV1Repository userRepository;
    private final TimetableRepository timetableRepository;
    private final PasswordEncoder passwordEncoder;

    public ComprehensiveProductionSeeder(
            DepartmentRepository departmentRepository,
            SectionRepository sectionRepository,
            RoomRepository roomRepository,
            UserV1Repository userRepository,
            TimetableRepository timetableRepository,
            PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.timetableRepository = timetableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("🎬 STARTING UNIFIED INSTITUTIONAL SEEDING...");

        try {
            String defaultPassword = passwordEncoder.encode("User_Smart@2026");

            // 1. Definition of 5 Departments
            Map<String, String> depts = new LinkedHashMap<>();
            depts.put("Computer Science & Engineering", "CSE");
            depts.put("Electronics & Communication", "ECE");
            depts.put("Information Technology", "IT");
            depts.put("Mechanical Engineering", "ME");
            depts.put("Civil Engineering", "CE");

            DayOfWeek today = LocalDate.now().getDayOfWeek();

            for (Map.Entry<String, String> entry : depts.entrySet()) {
                String deptName = entry.getKey();
                String deptCode = entry.getValue();

                logger.info("🏢 SEEDING DEPARTMENT: {} ({})", deptName, deptCode);

                // Create/Get Department
                Department dept = departmentRepository.save(
                    departmentRepository.findByCode(deptCode).orElseGet(() -> {
                        Department d = new Department();
                        d.setName(deptName);
                        d.setCode(deptCode);
                        d.setIsActive(true);
                        return d;
                    })
                );

                // 2. Create 20 Faculty per department
                List<User> facultyList = new ArrayList<>();
                for (int i = 1; i <= 20; i++) {
                    String email = (deptCode + ".prof." + i + "@smart.local").toLowerCase();
                    User faculty = userRepository.findByEmailIgnoreCase(email).orElse(new User());
                    
                    faculty.setName(deptCode + " Professor " + i);
                    faculty.setEmail(email);
                    faculty.setRegistrationNumber("FAC-" + deptCode + "-" + i);
                    faculty.setPassword(defaultPassword);
                    faculty.setRole(Role.FACULTY);
                    faculty.setDepartment(dept.getId().toString()); // Map to actual UUID
                    faculty.setStatus(UserStatus.ACTIVE);
                    faculty.setFirstLogin(false);
                    faculty.setIsTemporaryPassword(false);
                    
                    facultyList.add(userRepository.save(faculty));
                }

                // 3. Create 4 Sections per department
                char[] sectionSuffixes = {'A', 'B', 'C', 'D'};
                for (int s = 0; s < 4; s++) {
                    final int floorNumber = s + 1;
                    String sectionName = deptCode + "-" + sectionSuffixes[s];
                    logger.info("   📍 Seeding Section: {}", sectionName);

                    // Create Room for section
                    Room room = roomRepository.save(
                        roomRepository.findByName("Room-" + sectionName).orElseGet(() -> {
                            Room r = new Room();
                            r.setName("Room-" + sectionName);
                            r.setCapacity(60);
                            r.setBuilding("Main Block");
                            r.setFloor(floorNumber);
                            r.setBoundaryPolygon(generateRandomGeofence());
                            return r;
                        })
                    );

                    // Create Section
                    Section section = sectionRepository.save(
                        sectionRepository.findByDepartmentId(dept.getId()).stream()
                            .filter(sec -> sec.getName().equalsIgnoreCase(sectionName))
                            .findFirst()
                            .orElseGet(() -> {
                                Section sec = new Section();
                                sec.setName(sectionName);
                                sec.setDepartment(dept);
                                sec.setProgram(deptName);
                                sec.setBatchYear(2024);
                                sec.setAcademicYear("2024-2028");
                                sec.setSemester("4");
                                sec.setCapacity(60);
                                sec.setIsActive(true);
                                return sec;
                            })
                    );

                    // 4. Create 10 Students for section
                    for (int j = 1; j <= 10; j++) {
                        String sEmail = String.format("student.%s.%s.%d@smart.local", 
                            deptCode.toLowerCase(), 
                            String.valueOf(sectionSuffixes[s]).toLowerCase(), 
                            j);
                        User student = userRepository.findByEmailIgnoreCase(sEmail).orElse(new User());
                        
                        student.setName("Student " + sectionName + "-" + j);
                        student.setEmail(sEmail);
                        student.setRegistrationNumber("STU-" + deptCode + "-" + sectionSuffixes[s] + "-" + j);
                        student.setPassword(defaultPassword);
                        student.setRole(Role.STUDENT);
                        student.setDepartment(dept.getId().toString());
                        student.setSectionId(section.getId());
                        student.setStatus(UserStatus.ACTIVE);
                        student.setFirstLogin(false);
                        student.setIsTemporaryPassword(false);
                        
                        userRepository.save(student);
                    }

                    // 5. Seed Strict 3-Period Timetable for Today
                    seedStrictTimetable(section, room, facultyList, today);
                }
            }

            logger.info("✅ UNIFIED SEEDING COMPLETED SUCCESSFULLY!");
        } catch (Exception e) {
            logger.error("❌ Seeder failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void seedStrictTimetable(Section section, Room room, List<User> facultyList, DayOfWeek day) {
        // Strict timings based on user request
        LocalTime[] starts = { 
            LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0), 
            LocalTime.of(11, 0), LocalTime.of(11, 15), LocalTime.of(12, 15), 
            LocalTime.of(12, 45), LocalTime.of(13, 45), LocalTime.of(14, 45) 
        };
        LocalTime[] ends = { 
            LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), 
            LocalTime.of(11, 15), LocalTime.of(12, 15), LocalTime.of(12, 45), 
            LocalTime.of(13, 45), LocalTime.of(14, 45), LocalTime.of(15, 45) 
        };
        String[] subjects = { 
            "Period 1: Advanced AI", "Period 2: Data Engineering", "Period 3: Cyber Security", 
            "Short Break", "Period 4: Cloud Native", "Period 5: Professional Ethics", 
            "Lunch Break", "Period 6: Software Quality", "Period 7: Capstone Project" 
        };
        Random rand = new Random();

        for (int i = 0; i < starts.length; i++) {
            final LocalTime st = starts[i];
            boolean exists = timetableRepository.findSessionForSectionAtTime(section.getId(), day, st).size() > 0;
            
            if (!exists) {
                Timetable t = new Timetable();
                t.setSection(section);
                t.setRoom(room);
                t.setDayOfWeek(day);
                t.setStartTime(starts[i]);
                t.setEndTime(ends[i]);
                t.setSubject(subjects[i]);
                t.setFaculty(facultyList.get(rand.nextInt(facultyList.size())));
                t.setAcademicYear(section.getAcademicYear());
                t.setSemester(section.getSemester());
                t.setIsExamDay(false);
                t.setIsHoliday(false);
                t.setIsAdhoc(true);
                t.setOverridesHoliday(true);
                t.setDescription("Seeded daily period");
                timetableRepository.save(t);
            }
        }
    }

    private Polygon generateRandomGeofence() {
        double baseLong = 77.5946 + (new Random().nextDouble() * 0.05);
        double baseLat = 12.9716 + (new Random().nextDouble() * 0.05);
        double size = 0.0002;
        Coordinate[] coordinates = new Coordinate[] {
            new Coordinate(baseLong, baseLat),
            new Coordinate(baseLong + size, baseLat),
            new Coordinate(baseLong + size, baseLat + size),
            new Coordinate(baseLong, baseLat + size),
            new Coordinate(baseLong, baseLat)
        };
        LinearRing shell = geometryFactory.createLinearRing(coordinates);
        return geometryFactory.createPolygon(shell);
    }
}
