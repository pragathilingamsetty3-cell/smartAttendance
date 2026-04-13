package com.example.smartAttendence.service.v1;

import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.repository.SectionRepository;
import com.example.smartAttendence.repository.TimetableRepository;
import com.example.smartAttendence.repository.v1.UserV1Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class MetadataV1Service {

    private final SectionRepository sectionRepository;
    private final TimetableRepository timetableRepository;
    private final UserV1Repository userV1Repository;

    public MetadataV1Service(SectionRepository sectionRepository,
                            TimetableRepository timetableRepository,
                            UserV1Repository userV1Repository) {
        this.sectionRepository = sectionRepository;
        this.timetableRepository = timetableRepository;
        this.userV1Repository = userV1Repository;
    }

    /**
     * Get distinct semesters actually present in the database.
     */
    public List<String> getDistinctSemesters() {
        // Fetch from Sections and User entities
        List<String> sectionSemesters = sectionRepository.findAll().stream()
                .map(s -> s.getCurrentSemester() != null ? s.getCurrentSemester().toString() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        List<String> userSemesters = userV1Repository.findAll().stream()
                .map(u -> u.getSemester() != null ? u.getSemester().toString() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return Stream.concat(sectionSemesters.stream(), userSemesters.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get distinct academic years actually present in the database.
     */
    public List<String> getDistinctAcademicYears() {
        int currentYear = java.time.LocalDate.now().getYear();
        List<String> generatedYears = new java.util.ArrayList<>();
        
        // Generate years starting from last year up to 5 years in the future
        for (int startYear = currentYear - 1; startYear <= currentYear + 5; startYear++) {
            // Common academic durations (1 to 5 years)
            for (int duration = 1; duration <= 5; duration++) {
                generatedYears.add(startYear + "-" + (startYear + duration));
            }
        }

        List<String> databaseYears = Stream.concat(
                sectionRepository.findAll().stream().map(s -> s.getTotalAcademicYears()),
                userV1Repository.findAll().stream().map(u -> u.getTotalAcademicYears())
        ).filter(Objects::nonNull).distinct().toList();

        return Stream.concat(generatedYears.stream(), databaseYears.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get available roles (filtered for onboarding focus).
     */
    public List<String> getAvailableRoles() {
        return Arrays.stream(Role.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
