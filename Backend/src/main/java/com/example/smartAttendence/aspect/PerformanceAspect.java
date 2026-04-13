package com.example.smartAttendence.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect for performance monitoring and timing of critical operations
 */
@Aspect
@Component
public class PerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Monitor attendance service methods
     */
    @Around("execution(* com.example.smartAttendence.service.v1.AttendanceV1Service.*(..))")
    public Object monitorAttendanceOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorOperation(joinPoint, "attendance.operations");
    }

    /**
     * Monitor user service methods
     */
    @Around("execution(* com.example.smartAttendence.service.v1.*Service.*(..))")
    public Object monitorUserServiceOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorOperation(joinPoint, "service.operations");
    }

    /**
     * Monitor repository operations
     */
    @Around("execution(* com.example.smartAttendence.repository.*.*(..))")
    public Object monitorRepositoryOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorOperation(joinPoint, "repository.operations");
    }

    /**
     * Monitor controller operations
     */
    @Around("execution(* com.example.smartAttendence.controller.*.*(..))")
    public Object monitorControllerOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorOperation(joinPoint, "controller.operations");
    }

    /**
     * Generic method to monitor operation performance
     */
    private Object monitorOperation(ProceedingJoinPoint joinPoint, String timerName) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Timer timer = Timer.builder(timerName)
                .description("Time taken for " + timerName)
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry);

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Log slow operations (> 1000ms)
            if (duration > 1000) {
                logger.warn("SLOW OPERATION: {}.{} took {}ms", className, methodName, duration);
            }
            
            sample.stop(timer);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("FAILED OPERATION: {}.{} failed after {}ms - {}", className, methodName, duration, e.getMessage());
            
            // Record failure metrics
            meterRegistry.counter(timerName + ".failures", 
                    "class", className, 
                    "method", methodName, 
                    "exception", e.getClass().getSimpleName())
                    .increment();
            
            sample.stop(timer);
            throw e;
        }
    }
}
