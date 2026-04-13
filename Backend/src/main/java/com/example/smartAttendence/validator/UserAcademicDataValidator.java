package com.example.smartAttendence.validator;

import com.example.smartAttendence.enums.Role;
import com.example.smartAttendence.domain.User;
import com.example.smartAttendence.domain.AcademicStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator to ensure only student roles have academic data
 */
@Component
public class UserAcademicDataValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;
        
        // Check if non-student role has academic data
        if (!isStudentRole(user.getRole())) {
            if (user.getTotalAcademicYears() != null) {
                errors.rejectValue("totalAcademicYears", "invalid.academic.data", 
                    "Non-student users cannot have total academic years");
            }
            
            if (user.getSemester() != null) {
                errors.rejectValue("semester", "invalid.academic.data", 
                    "Non-student users cannot have semester data");
            }
        }
    }

    /**
     * Check if role is a student role
     */
    public static boolean isStudentRole(Role role) {
        return role == Role.STUDENT || role == Role.CR || role == Role.LR;
    }

    /**
     * Validate that user has appropriate academic data for their role
     */
    public static void validateUserAcademicData(User user) {
        boolean isStudent = isStudentRole(user.getRole());
        
        if (isStudent) {
            // Students must have academic data
            if (user.getTotalAcademicYears() == null || user.getTotalAcademicYears().trim().isEmpty()) {
                throw new IllegalArgumentException("Students must have total academic years");
            }
            if (user.getSemester() == null || user.getSemester() < 1) {
                throw new IllegalArgumentException("Students must have a valid semester (>= 1)");
            }
        } else {
            // Non-students must not have academic data
            if (user.getTotalAcademicYears() != null && !user.getTotalAcademicYears().trim().isEmpty()) {
                throw new IllegalArgumentException("Non-student users cannot have total academic years");
            }
            if (user.getSemester() != null) {
                throw new IllegalArgumentException("Non-student users cannot have semester data");
            }
        }
    }
}
