package com.example.smartAttendence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔐 PRODUCTION-GRADE BASE RESPONSE WRAPPER
 * 
 * Ensures consistent JSON structure across the entire Smart Attendance System
 * Enhanced for Tier-1 international company standards
 * 
 * Features:
 * - Consistent API response format
 * - Comprehensive error handling
 * - Security metadata
 * - Request tracking
 * - Pagination support
 * - Audit trail capabilities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * Response timestamp for audit and debugging
     */
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code representation
     */
    private String status;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * Machine-readable error code (for errors)
     */
    private String errorCode;

    /**
     * Detailed error description (for errors)
     */
    private String errorDescription;

    /**
     * Request tracking ID for distributed tracing
     */
    private String requestId;

    /**
     * Response data payload
     */
    private T data;

    /**
     * Pagination metadata (for paginated responses)
     */
    private PaginationMetadata pagination;

    /**
     * Additional metadata for spatial responses
     */
    private SpatialMetadata spatial;

    /**
     * Security metadata for zero-trust validation
     */
    private SecurityMetadata security;

    // ========== PRODUCTION VALIDATION METHODS ==========
    
    /**
     * 🔐 PRODUCTION VALIDATION - Ensures response integrity
     */
    public boolean isValidProductionResponse() {
        return this.status != null && 
               !this.status.trim().isEmpty() &&
               this.timestamp != null &&
               (this.data != null || this.errorCode != null);
    }
    
    /**
     * 🔐 SECURITY VALIDATION - Checks for data exposure
     */
    public boolean hasSecurityCompliantData() {
        // Check for potential password exposure
        if (this.data != null && this.data.toString().toLowerCase().contains("password")) {
            return false;
        }
        return true;
    }
    
    /**
     * 🔐 AUDIT READY - Ensures response has audit information
     */
    public boolean isAuditReady() {
        return this.timestamp != null && 
               this.status != null &&
               (this.requestId != null || this.errorCode != null);
    }

    // ========== PRODUCTION SUCCESS RESPONSES ==========

    public static <T> BaseResponse<T> success(T data) {
        BaseResponseBuilder<T> builder = new BaseResponseBuilder<>();
        builder.status = "SUCCESS";
        builder.message = "Operation completed successfully";
        builder.data = data;
        return builder.build();
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponseBuilder<T>()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> successWithPagination(T data, PaginationMetadata pagination) {
        return new BaseResponseBuilder<T>()
                .status("SUCCESS")
                .message("Data retrieved successfully")
                .data(data)
                .pagination(pagination)
                .build();
    }

    // ========== ERROR RESPONSES ==========

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponseBuilder<T>()
                .status("ERROR")
                .message(message)
                .build();
    }

    public static <T> BaseResponse<T> error(String message, String errorCode) {
        return new BaseResponseBuilder<T>()
                .status("ERROR")
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    public static <T> BaseResponse<T> error(String message, String errorCode, String errorDescription) {
        return new BaseResponseBuilder<T>()
                .status("ERROR")
                .message(message)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .build();
    }

    public static <T> BaseResponse<T>ValidationError(String message, List<String> validationErrors) {
        BaseResponseBuilder<T> builder = new BaseResponseBuilder<>();
        builder.status = "VALIDATION_ERROR";
        builder.message = message;
        builder.errorCode = "VALIDATION_FAILED";
        builder.errorDescription = String.join("; ", validationErrors);
        return builder.build();
    }

    public static <T> BaseResponse<T> securityError(String message, String requestId) {
        BaseResponseBuilder<T> builder = new BaseResponseBuilder<>();
        builder.status = "SECURITY_ERROR";
        builder.message = message;
        builder.errorCode = "SECURITY_VIOLATION";
        builder.requestId = requestId;
        return builder.build();
    }

    public static <T> BaseResponse<T> spatialError(String message, String spatialContext) {
        BaseResponseBuilder<T> builder = new BaseResponseBuilder<>();
        builder.status = "SPATIAL_ERROR";
        builder.message = message;
        builder.errorCode = "SPATIAL_VALIDATION_FAILED";
        
        SpatialMetadata spatial = new SpatialMetadata();
        spatial.setContext(spatialContext);
        builder.spatial = spatial;
        
        return builder.build();
    }

    // ========== INNER CLASSES ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMetadata {
        private int currentPage;
        private int pageSize;
        private long totalItems;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpatialMetadata {
        private String context;
        private String coordinateSystem;
        private Double accuracy;
        private String geometryType;
        private String boundingBox;
        
        // Explicit setter for builder pattern
        public SpatialMetadata setContext(String context) {
            this.context = context;
            return this;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityMetadata {
        private String trustLevel;
        private String authenticationMethod;
        private String authorizationScope;
        private LocalDateTime validatedAt;
        private boolean biometricVerified;
        private String deviceTrustScore;
    }

    // ===== BUILDER CLASS FOR COMPILATION =====
    
    public static class BaseResponseBuilder<T> {
        private String status;
        private String message;
        private String errorCode;
        private String errorDescription;
        private String requestId;
        private T data;
        private PaginationMetadata pagination;
        private SpatialMetadata spatial;
        private SecurityMetadata security;

        public BaseResponseBuilder<T> status(String status) {
            this.status = status;
            return this;
        }

        public BaseResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public BaseResponseBuilder<T> errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public BaseResponseBuilder<T> errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public BaseResponseBuilder<T> requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public BaseResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public BaseResponseBuilder<T> pagination(PaginationMetadata pagination) {
            this.pagination = pagination;
            return this;
        }

        public BaseResponseBuilder<T> spatial(SpatialMetadata spatial) {
            this.spatial = spatial;
            return this;
        }

        public BaseResponseBuilder<T> security(SecurityMetadata security) {
            this.security = security;
            return this;
        }

        public BaseResponse<T> build() {
            BaseResponse<T> response = new BaseResponse<>();
            response.status = this.status;
            response.message = this.message;
            response.errorCode = this.errorCode;
            response.errorDescription = this.errorDescription;
            response.requestId = this.requestId;
            response.data = this.data;
            response.pagination = this.pagination;
            response.spatial = this.spatial;
            response.security = this.security;
            response.timestamp = LocalDateTime.now();
            return response;
        }
    }
}
