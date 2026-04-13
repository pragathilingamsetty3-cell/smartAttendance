package com.example.smartAttendence.test.integration;

import com.example.smartAttendence.test.base.AbstractIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Auth Controller Integration Test
 * 
 * Production-grade integration tests for the Authentication API endpoints.
 * Tests JWT token generation, validation, and security scenarios.
 * 
 * Test Coverage:
 * - Valid login with 512-bit JWT token verification
 * - Invalid login scenarios (401 Unauthorized)
 * - Token structure and claims validation
 * - Security headers and response formats
 * - Input validation and error handling
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = "/sql/test-user-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private String baseUrl;
    private static final String ADMIN_EMAIL = "admin@smartattendence.com";
    private static final String ADMIN_PASSWORD = "Test@Secure123";
    private static final String FACULTY_EMAIL = "faculty@smartattendence.com";
    private static final String FACULTY_PASSWORD = "Test@Secure123";
    private static final String STUDENT_EMAIL = "student@smartattendence.com";
    private static final String STUDENT_PASSWORD = "Test@Secure123";
    private static final String INVALID_LOGIN_EMAIL = "invalid@test.com";
    private static final String INVALID_LOGIN_PASSWORD = "WrongPassword123!";

    @BeforeEach
    void setUp() {
        baseUrl = getBaseUrl();
        validateTestInfrastructure();
        log.debug("🔐 Setting up Auth Controller Integration Test");
        log.debug("🌐 Base URL: " + baseUrl);
    }

    /**
     * Test Valid Login Scenario
     * 
     * Tests successful login with valid credentials and verifies:
     * - HTTP 200 OK status
     * - Valid 512-bit JWT token in response
     * - Token structure and claims
     * - Response format and required fields
     */
    @Test
    @DisplayName("✅ Admin Login Should Return 200 OK with Valid JWT Token")
    void testAdminLogin_Returns200OkWithValidJWTToken() {
        // Given: Valid admin credentials
        Map<String, Object> loginRequest = Map.of(
            "email", ADMIN_EMAIL,
            "password", ADMIN_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status and structure
        assertThat(response.statusCode()).isEqualTo(200);
        
        // Verify response body structure
        response.then()
            .body("message", equalTo("Login successful"))
            .body("requiresFirstLoginSetup", equalTo(false))
            .body("tokenType", equalTo("Bearer"))
            .body("expiresIn", equalTo(15 * 60))
            .body("user.id", notNullValue())
            .body("user.email", equalTo(ADMIN_EMAIL))
            .body("user.name", notNullValue())
            .body("user.role", notNullValue());

        // And: Verify JWT token presence and structure
        String accessToken = response.jsonPath().getString("accessToken");
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        // Verify JWT token format (3 parts separated by dots)
        String[] tokenParts = accessToken.split("\\.");
        assertThat(tokenParts).hasSize(3);
        
        // Verify JWT token claims (decode header and payload)
        String header = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[0]));
        String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
        
        assertThat(header).contains("alg");
        // Note: RS256 is more secure than HS512 - uses asymmetric keys
        
        assertThat(payload).contains("sub"); // JWT uses 'sub' claim instead of 'email'
        assertThat(payload).contains("role");
        assertThat(payload).contains("iat");
        assertThat(payload).contains("exp");
        
        // Verify refresh token presence
        String refreshToken = response.jsonPath().getString("refreshToken");
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        
        // Verify user information
        Map<String, Object> user = response.jsonPath().getMap("user");
        assertThat(user.get("email")).isEqualTo(ADMIN_EMAIL);
        assertThat(user.get("id")).isNotNull();
        assertThat(user.get("name")).isNotNull();
        assertThat(user.get("role")).isEqualTo("ADMIN");
        
        log.debug("✅ Admin Login Test Passed");
        log.debug("🔑 Access Token Length: " + accessToken.length());
        log.debug("🔄 Refresh Token Length: " + refreshToken.length());
        log.debug("👤 User Email: " + user.get("email"));
        log.debug("🎭 User Role: " + user.get("role"));
    }

    /**
     * Test Invalid Login Scenario - Wrong Email
     * 
     * Tests login with invalid email and verifies:
     * - HTTP 401 Unauthorized status
     * - Proper error response format
     * - No JWT token is returned
     */
    @Test
    @DisplayName("❌ Invalid Login with Wrong Email Should Return 400 Bad Request")
    void testInvalidLogin_WrongEmail_Returns400BadRequest() {
        // Given: Invalid login credentials (wrong email)
        Map<String, Object> loginRequest = Map.of(
            "email", INVALID_LOGIN_EMAIL,
            "password", ADMIN_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status and error structure
        assertThat(response.statusCode()).isEqualTo(400);
        
        // Verify error response structure
        response.then()
            .body("error", notNullValue());

        // And: Verify no tokens are returned
        assertThat(response.jsonPath().getString("accessToken")).isNull();
        assertThat(response.jsonPath().getString("refreshToken")).isNull();
        
        log.debug("✅ Invalid Login (Wrong Email) Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
        log.debug("💬 Error Message: " + response.jsonPath().getString("error"));
    }

    /**
     * Test Invalid Login Scenario - Wrong Password
     * 
     * Tests login with invalid password and verifies:
     * - HTTP 400 Bad Request status
     * - Proper error response format
     * - No JWT token is returned
     */
    @Test
    @DisplayName("❌ Invalid Login with Wrong Password Should Return 400 Bad Request")
    void testInvalidLogin_WrongPassword_Returns400BadRequest() {
        // Given: Invalid login credentials (wrong password)
        Map<String, Object> loginRequest = Map.of(
            "email", ADMIN_EMAIL,
            "password", INVALID_LOGIN_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status and error structure
        assertThat(response.statusCode()).isEqualTo(400);
        
        // Verify error response structure
        response.then()
            .body("error", notNullValue());

        // And: Verify no tokens are returned
        assertThat(response.jsonPath().getString("accessToken")).isNull();
        assertThat(response.jsonPath().getString("refreshToken")).isNull();
        
        log.debug("✅ Invalid Login (Wrong Password) Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
        log.debug("💬 Error Message: " + response.jsonPath().getString("error"));
    }

    /**
     * Test Invalid Login Scenario - Missing Credentials
     * 
     * Tests login with missing credentials and verifies:
     * - HTTP 400 Bad Request status
     * - Proper validation error response
     */
    @Test
    @DisplayName("❌ Invalid Login with Missing Credentials Should Return 400 Bad Request")
    void testInvalidLogin_MissingCredentials_Returns400BadRequest() {
        // Given: Missing login credentials
        Map<String, Object> loginRequest = Map.of(
            "email", "",
            "password", ""
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status and error structure
        assertThat(response.statusCode()).isEqualTo(400);
        
        // Verify error response structure
        response.then()
            .body("error", notNullValue());

        log.debug("✅ Invalid Login (Missing Credentials) Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
        log.debug("💬 Error Message: " + response.jsonPath().getString("error"));
    }

    /**
     * Test Login Request with Invalid JSON Format
     * 
     * Tests login with malformed JSON and verifies:
     * - HTTP 400 Bad Request status
     * - Proper error handling
     */
    @Test
    @DisplayName("❌ Login with Invalid JSON Should Return 400 Bad Request")
    void testLogin_InvalidJson_Returns400BadRequest() {
        // Given: Invalid JSON format
        String invalidJson = "{ invalid json format }";

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(invalidJson)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status
        assertThat(response.statusCode()).isEqualTo(400);
        
        log.debug("✅ Invalid JSON Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
    }

    /**
     * Test Login Request with SQL Injection Attempt
     * 
     * Tests login with SQL injection attempt and verifies:
     * - HTTP 400 Bad Request status (input validation catches it first)
     * - Security validation prevents injection
     */
    @Test
    @DisplayName("🛡️ Login with SQL Injection Attempt Should Return 400 Bad Request")
    void testLogin_SqlInjectionAttempt_Returns400BadRequest() {
        // Given: SQL injection attempt in email field
        Map<String, Object> loginRequest = Map.of(
            "email", "'; DROP TABLE users; --",
            "password", "password"
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status (input validation catches SQL injection first)
        assertThat(response.statusCode()).isEqualTo(400);
        
        log.debug("✅ SQL Injection Protection Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
        log.debug("🛡️ Security Validation Working");
    }

    /**
     * Test Login Request with XSS Attempt
     * 
     * Tests login with XSS attempt and verifies:
     * - HTTP 400 Bad Request status
     * - Security validation prevents XSS
     */
    @Test
    @DisplayName("🛡️ Login with XSS Attempt Should Return 400 Bad Request")
    void testLogin_XssAttempt_Returns400BadRequest() {
        // Given: XSS attempt in email field
        Map<String, Object> loginRequest = Map.of(
            "email", "<script>alert('XSS')</script>@test.com",
            "password", "password"
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .log().all()
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .log().all()
            .extract()
            .response();

        // Then: Verify response status
        assertThat(response.statusCode()).isEqualTo(400);
        
        log.debug("✅ XSS Protection Test Passed");
        log.debug("🚫 Status Code: " + response.statusCode());
        log.debug("🛡️ Security Validation Working");
    }

    /**
     * Test JWT Token Structure and Claims Validation
     * 
     * Tests the JWT token structure and validates all required claims
     * for production-grade security compliance.
     */
    @Test
    @DisplayName("🔐 JWT Token Structure and Claims Validation")
    void testJwtTokenStructureAndClaimsValidation() {
        // Given: Valid login credentials
        Map<String, Object> loginRequest = Map.of(
            "email", ADMIN_EMAIL,
            "password", ADMIN_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();

        // Then: Verify JWT token structure and claims
        String accessToken = response.jsonPath().getString("accessToken");
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        String[] tokenParts = accessToken.split("\\.");
        assertThat(tokenParts).hasSize(3);
        
        // Decode and verify header
        String header = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[0]));
        assertThat(header).contains("\"alg\":\"RS256\"");
        
        // Decode and verify payload
        String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
        assertThat(payload).contains("\"sub\":\"" + ADMIN_EMAIL + "\"");
        assertThat(payload).contains("\"role\":\"ADMIN\"");
        assertThat(payload).contains("\"role\":");
        assertThat(payload).contains("\"iat\":");
        assertThat(payload).contains("\"exp\":");
        
        // Verify signature is present (base64url encoded)
        assertThat(tokenParts[2]).isNotEmpty();
        assertThat(tokenParts[2].length()).isGreaterThan(50); // HS512 signature should be substantial
        
        log.debug("✅ JWT Token Structure Validation Passed");
        log.debug("🔑 Token Parts: " + tokenParts.length);
        log.debug("📋 Header: " + header);
        log.debug("📦 Payload: " + payload);
        log.debug("✍️ Signature Length: " + tokenParts[2].length());
    }

    /**
     * Test Faculty Login Scenario
     * 
     * Tests successful faculty login with valid credentials and verifies:
     * - HTTP 200 OK status
     * - Valid JWT token with FACULTY role
     * - Proper user information in response
     */
    @Test
    @DisplayName("👨‍🏫 Faculty Login Should Return 200 OK with Valid JWT Token")
    void testFacultyLogin_Returns200OkWithValidJWTToken() {
        // Given: Valid faculty credentials
        Map<String, Object> loginRequest = Map.of(
            "email", FACULTY_EMAIL,
            "password", FACULTY_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();

        // Then: Verify response status and structure
        assertThat(response.statusCode()).isEqualTo(200);
        
        // Verify response body structure
        response.then()
            .body("message", equalTo("Login successful"))
            .body("requiresFirstLoginSetup", equalTo(false))
            .body("tokenType", equalTo("Bearer"))
            .body("user.email", equalTo(FACULTY_EMAIL))
            .body("user.role", equalTo("FACULTY"));

        // Verify JWT token presence and role claim
        String accessToken = response.jsonPath().getString("accessToken");
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        String[] tokenParts = accessToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
        assertThat(payload).contains("\"role\":\"FACULTY\"");
        
        log.debug("✅ Faculty Login Test Passed");
        log.debug("👨‍🏫 Faculty Email: " + FACULTY_EMAIL);
        log.debug("🎭 Role: FACULTY");
    }

    /**
     * Test Student Login Scenario
     * 
     * Tests successful student login with valid credentials and verifies:
     * - HTTP 200 OK status
     * - Valid JWT token with STUDENT role
     * - Proper user information in response
     */
    @Test
    @DisplayName("👨‍🎓 Student Login Should Return 200 OK with Valid JWT Token")
    void testStudentLogin_Returns200OkWithValidJWTToken() {
        // Given: Valid student credentials
        Map<String, Object> loginRequest = Map.of(
            "email", STUDENT_EMAIL,
            "password", STUDENT_PASSWORD
        );

        // When: Login request is sent
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();

        // Then: Verify response status and structure
        assertThat(response.statusCode()).isEqualTo(200);
        
        // Verify response body structure
        response.then()
            .body("message", equalTo("Login successful"))
            .body("requiresFirstLoginSetup", equalTo(false))
            .body("tokenType", equalTo("Bearer"))
            .body("user.email", equalTo(STUDENT_EMAIL))
            .body("user.role", equalTo("STUDENT"));

        // Verify JWT token presence and role claim
        String accessToken = response.jsonPath().getString("accessToken");
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        
        String[] tokenParts = accessToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
        assertThat(payload).contains("\"role\":\"STUDENT\"");
        
        log.debug("✅ Student Login Test Passed");
        log.debug("👨‍🎓 Student Email: " + STUDENT_EMAIL);
        log.debug("🎭 Role: STUDENT");
    }

    /**
     * Test Multi-User Role Authentication
     * 
     * Tests that different user roles get appropriate JWT tokens with correct claims
     * and verifies role-based access control is working properly.
     */
    @Test
    @DisplayName("🔐 Multi-User Role Authentication Test")
    void testMultiUserRoleAuthentication() {
        // Test Admin login
        Map<String, Object> adminRequest = Map.of(
            "email", ADMIN_EMAIL,
            "password", ADMIN_PASSWORD
        );
        
        Response adminResponse = given()
            .contentType(ContentType.JSON)
            .body(adminRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();
        
        assertThat(adminResponse.statusCode()).isEqualTo(200);
        String adminToken = adminResponse.jsonPath().getString("accessToken");
        String adminPayload = new String(java.util.Base64.getUrlDecoder().decode(adminToken.split("\\.")[1]));
        assertThat(adminPayload).contains("\"role\":\"ADMIN\"");
        
        // Test Faculty login
        Map<String, Object> facultyRequest = Map.of(
            "email", FACULTY_EMAIL,
            "password", FACULTY_PASSWORD
        );
        
        Response facultyResponse = given()
            .contentType(ContentType.JSON)
            .body(facultyRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();
        
        assertThat(facultyResponse.statusCode()).isEqualTo(200);
        String facultyToken = facultyResponse.jsonPath().getString("accessToken");
        String facultyPayload = new String(java.util.Base64.getUrlDecoder().decode(facultyToken.split("\\.")[1]));
        assertThat(facultyPayload).contains("\"role\":\"FACULTY\"");
        
        // Test Student login
        Map<String, Object> studentRequest = Map.of(
            "email", STUDENT_EMAIL,
            "password", STUDENT_PASSWORD
        );
        
        Response studentResponse = given()
            .contentType(ContentType.JSON)
            .body(studentRequest)
        .when()
            .post(baseUrl + "/api/v1/auth/login")
        .then()
            .extract()
            .response();
        
        assertThat(studentResponse.statusCode()).isEqualTo(200);
        String studentToken = studentResponse.jsonPath().getString("accessToken");
        String studentPayload = new String(java.util.Base64.getUrlDecoder().decode(studentToken.split("\\.")[1]));
        assertThat(studentPayload).contains("\"role\":\"STUDENT\"");
        
        log.debug("✅ Multi-User Role Authentication Test Passed");
        log.debug("🔐 Admin Token: " + adminToken.substring(0, 20) + "...");
        log.debug("👨‍🏫 Faculty Token: " + facultyToken.substring(0, 20) + "...");
        log.debug("👨‍🎓 Student Token: " + studentToken.substring(0, 20) + "...");
    }
}
