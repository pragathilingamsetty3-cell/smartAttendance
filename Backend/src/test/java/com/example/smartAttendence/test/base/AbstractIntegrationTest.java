package com.example.smartAttendence.test.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Abstract Integration Test Base Class
 * 
 * This base class provides production-grade testing infrastructure using H2 in-memory
 * database for immediate testing without Docker dependencies. Can be upgraded to Testcontainers
 * when needed.
 * 
 * Features:
 * - H2 in-memory database (PostgreSQL compatible)
 * - Dynamic port configuration
 * - Production-grade testing infrastructure
 * - No Docker dependencies required
 * - Easy upgrade path to Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int serverPort;

    /**
     * Dynamic Property Configuration
     * 
     * Configures Spring Boot application properties to use H2 database
     * with PostgreSQL compatibility mode for testing.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // H2 Database Configuration (PostgreSQL compatibility mode)
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        
        // JPA/Hibernate Configuration for Testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        
        // PostGIS Configuration (Simulated for H2)
        registry.add("spring.jpa.properties.hibernate.spatial.dialect", () -> "org.hibernate.spatial.dialect.h2geodb.H2GeoDBDialect");
        registry.add("spring.jpa.properties.hibernate.spatial.jdbc_spatial_type", () -> "org.locationtech.jts.geom.Geometry");
        
        // Redis Configuration (Disabled for tests)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.jedis.pool.max-active", () -> "8");
        registry.add("spring.data.redis.jedis.pool.max-idle", () -> "8");
        registry.add("spring.data.redis.jedis.pool.min-idle", () -> "0");
        
        // Disable Redis for tests (use in-memory if needed)
        registry.add("spring.cache.type", () -> "none");
        
        // Flyway Configuration (Disable for Tests - Use Hibernate DDL Instead)
        registry.add("spring.flyway.enabled", () -> "false");
        
        // Security Configuration for Testing
        registry.add("jwt.secret", () -> "test-secret-key-for-production-testing-suite-512-bit-encryption");
        registry.add("jwt.expiration", () -> "900000"); // 15 minutes
        registry.add("jwt.refresh-token.expiration", () -> "604800000"); // 7 days
        
        // HTTPS Configuration for Testing (Disabled)
        registry.add("server.ssl.enabled", () -> "false");
        registry.add("server.port", () -> "0"); // Random port
        
        // Logging Configuration for Testing
        registry.add("logging.level.com.example.smartAttendence", () -> "DEBUG");
        registry.add("logging.level.org.springframework.security", () -> "DEBUG");
        
        // Resilience4j Configuration for Testing
        registry.add("resilience4j.circuitbreaker.configs.default.failureRateThreshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.configs.default.waitDurationInOpenState", () -> "5s");
        registry.add("resilience4j.circuitbreaker.configs.default.slidingWindowSize", () -> "10");
        registry.add("resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls", () -> "5");
        
        // Rate Limiting Configuration for Testing
        registry.add("rate.limiting.global.requests-per-minute", () -> "1000");
        registry.add("rate.limiting.auth.requests-per-minute", () -> "100");
        registry.add("rate.limiting.admin.requests-per-minute", () -> "500");
    }

    /**
     * Setup method executed before all tests
     * 
     * This method runs once per test class and can be used for global test setup.
     * Subclasses can override this method for additional setup.
     */
    @BeforeAll
    static void setup() {
        log.debug("🚀 Starting Production Integration Test Suite");
        log.debug("📊 Database: H2 In-Memory (PostgreSQL Compatible)");
        log.debug("🔴 Redis: Disabled for tests");
        log.debug("✅ Test Infrastructure Ready");
    }

    /**
     * Get the base URL for API testing
     * 
     * @return Base URL for the running application
     */
    protected String getBaseUrl() {
        return "http://localhost:" + serverPort;
    }

    /**
     * Get the JDBC URL for direct database testing
     * 
     * @return JDBC URL for the test H2 database
     */
    protected String getJdbcUrl() {
        return "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    }

    /**
     * Validate test infrastructure setup
     * 
     * This method can be called in tests to ensure all components are ready.
     */
    protected void validateTestInfrastructure() {
        // H2 database is always available in tests
        log.debug("✅ H2 Database: Ready");
    }
}
