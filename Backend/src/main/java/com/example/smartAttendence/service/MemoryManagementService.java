package com.example.smartAttendence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-Grade Memory Management Service
 * Provides comprehensive memory leak detection and cleanup for 50K+ concurrent users
 */
@Service
public class MemoryManagementService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryManagementService.class);
    
    private final MemoryMXBean memoryMXBean;
    private final ConcurrentHashMap<String, AtomicLong> resourceCounters;
    private final AtomicLong totalAllocations;
    private final AtomicLong totalDeallocations;
    
    // Memory thresholds for production scale
    private static final double MEMORY_WARNING_THRESHOLD = 0.80; // 80%
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.90; // 90%
    private static final long MAX_MEMORY_USAGE_MB = 4096; // 4GB max for production
    
    public MemoryManagementService() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.resourceCounters = new ConcurrentHashMap<>();
        this.totalAllocations = new AtomicLong(0);
        this.totalDeallocations = new AtomicLong(0);
        
        logger.info("Memory Management Service initialized for production scale");
    }
    
    /**
     * Track resource allocation for memory leak detection
     */
    public void trackResourceAllocation(String resourceType) {
        resourceCounters.computeIfAbsent(resourceType, k -> new AtomicLong(0)).incrementAndGet();
        totalAllocations.incrementAndGet();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Resource allocated: {}, total: {}", resourceType, 
                        resourceCounters.get(resourceType).get());
        }
    }
    
    /**
     * Track resource deallocation for memory leak detection
     */
    public void trackResourceDeallocation(String resourceType) {
        AtomicLong counter = resourceCounters.get(resourceType);
        if (counter != null) {
            counter.decrementAndGet();
            totalDeallocations.incrementAndGet();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Resource deallocated: {}, total: {}", resourceType, counter.get());
            }
        }
    }
    
    /**
     * Get current memory usage statistics
     */
    public MemoryUsage getHeapMemoryUsage() {
        return memoryMXBean.getHeapMemoryUsage();
    }
    
    /**
     * Get current non-heap memory usage statistics
     */
    public MemoryUsage getNonHeapMemoryUsage() {
        return memoryMXBean.getNonHeapMemoryUsage();
    }
    
    /**
     * Check if memory usage is within acceptable thresholds
     */
    public boolean isMemoryUsageHealthy() {
        MemoryUsage heapUsage = getHeapMemoryUsage();
        double usageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        return usageRatio < MEMORY_WARNING_THRESHOLD;
    }
    
    /**
     * Get memory usage ratio
     */
    public double getMemoryUsageRatio() {
        MemoryUsage heapUsage = getHeapMemoryUsage();
        return (double) heapUsage.getUsed() / heapUsage.getMax();
    }
    
    /**
     * Scheduled memory monitoring and cleanup
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performMemoryMonitoring() {
        try {
            MemoryUsage heapUsage = getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = getNonHeapMemoryUsage();
            
            double heapUsageRatio = (heapUsage.getMax() > 0) ? (double) heapUsage.getUsed() / heapUsage.getMax() : 0;
            double nonHeapUsageRatio = (nonHeapUsage.getMax() > 0) ? (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() : 0;
            
            long usedHeapMB = heapUsage.getUsed() / 1024 / 1024;
            long maxHeapMB = heapUsage.getMax() / 1024 / 1024;
            
            logger.info("Memory Status - Heap: {}MB/{}MB ({}%), Non-Heap: {}MB/{}MB ({})", 
                       usedHeapMB, maxHeapMB, String.format("%.1f", heapUsageRatio * 100),
                       nonHeapUsage.getUsed() / 1024 / 1024, 
                       (nonHeapUsage.getMax() > 0 ? nonHeapUsage.getMax() / 1024 / 1024 : 0),
                       (nonHeapUsage.getMax() > 0 ? String.format("%.1f%%", nonHeapUsageRatio * 100) : "N/A"));
            
            // Check for memory warnings
            if (heapUsageRatio > MEMORY_CRITICAL_THRESHOLD) {
                logger.error("CRITICAL: Memory usage at {}% - Immediate cleanup required", 
                           String.format("%.1f", heapUsageRatio * 100));
                performEmergencyCleanup();
            } else if (heapUsageRatio > MEMORY_WARNING_THRESHOLD) {
                logger.warn("WARNING: Memory usage at {}% - Cleanup recommended", 
                           String.format("%.1f", heapUsageRatio * 100));
                performPreventiveCleanup();
            }
            
            // Check for potential memory leaks
            checkForMemoryLeaks();
            
        } catch (Exception e) {
            logger.error("Error during memory monitoring", e);
        }
    }
    
    /**
     * Perform preventive memory cleanup
     */
    public void performPreventiveCleanup() {
        logger.info("Performing preventive memory cleanup");
        
        try {
            // Suggest garbage collection
            System.gc();
            
            // Wait for GC to complete
            Thread.sleep(1000);
            
            // Log cleanup results
            MemoryUsage afterCleanup = getHeapMemoryUsage();
            long usedAfterMB = afterCleanup.getUsed() / 1024 / 1024;
            
            logger.info("Preventive cleanup completed - Current heap usage: {}MB", usedAfterMB);
            
        } catch (Exception e) {
            logger.error("Error during preventive cleanup", e);
        }
    }
    
    /**
     * Perform emergency memory cleanup
     */
    public void performEmergencyCleanup() {
        logger.warn("Performing emergency memory cleanup");
        
        try {
            // Force garbage collection multiple times
            for (int i = 0; i < 3; i++) {
                System.gc();
                // System.runFinalization(); // Deprecated and no longer needed
                Thread.sleep(500);
            }
            
            // Clear resource counters if they're growing abnormally
            clearAbnormalResourceCounters();
            
            // Log emergency cleanup results
            MemoryUsage afterCleanup = getHeapMemoryUsage();
            long usedAfterMB = afterCleanup.getUsed() / 1024 / 1024;
            
            logger.warn("Emergency cleanup completed - Current heap usage: {}MB", usedAfterMB);
            
        } catch (Exception e) {
            logger.error("Error during emergency cleanup", e);
        }
    }
    
    /**
     * Check for potential memory leaks
     */
    private void checkForMemoryLeaks() {
        try {
            long totalAllocated = totalAllocations.get();
            long totalDeallocated = totalDeallocations.get();
            
            if (totalAllocated > 0) {
                double leakRatio = (double) (totalAllocated - totalDeallocated) / totalAllocated;
                
                if (leakRatio > 0.10) { // 10% leak threshold
                    logger.warn("Potential memory leak detected - Allocation/Deallocation imbalance: {}%", 
                               String.format("%.2f", leakRatio * 100));
                    
                    // Log resource counters for debugging
                    resourceCounters.forEach((type, count) -> {
                        if (count.get() > 1000) { // Arbitrary threshold for abnormal accumulation
                            logger.warn("High resource count for {}: {}", type, count.get());
                        }
                    });
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during memory leak detection", e);
        }
    }
    
    /**
     * Clear abnormal resource counters
     */
    private void clearAbnormalResourceCounters() {
        resourceCounters.entrySet().removeIf(entry -> {
            long count = entry.getValue().get();
            if (count > 5000) { // Arbitrary threshold for abnormal accumulation
                logger.warn("Clearing abnormal resource counter for {}: {}", entry.getKey(), count);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get comprehensive memory statistics
     */
    public String getMemoryStatistics() {
        MemoryUsage heapUsage = getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = getNonHeapMemoryUsage();
        
        return String.format(
            "Heap: %dMB/%dMB (%.1f%%), Non-Heap: %dMB/%dMB (%.1f%%), " +
            "Allocations: %d, Deallocations: %d, Active Resources: %d",
            heapUsage.getUsed() / 1024 / 1024,
            (heapUsage.getMax() > 0 ? heapUsage.getMax() / 1024 / 1024 : 0),
            (heapUsage.getMax() > 0 ? (double) heapUsage.getUsed() / heapUsage.getMax() * 100 : 0),
            nonHeapUsage.getUsed() / 1024 / 1024,
            (nonHeapUsage.getMax() > 0 ? nonHeapUsage.getMax() / 1024 / 1024 : 0),
            (nonHeapUsage.getMax() > 0 ? (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() * 100 : 0),
            totalAllocations.get(),
            totalDeallocations.get(),
            resourceCounters.values().stream().mapToLong(AtomicLong::get).sum()
        );
    }
    
    /**
     * Cleanup on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Memory Management Service shutting down");
        
        // Perform final cleanup
        performEmergencyCleanup();
        
        // Log final statistics
        logger.info("Final memory statistics: {}", getMemoryStatistics());
        
        // Clear all counters
        resourceCounters.clear();
    }
}
