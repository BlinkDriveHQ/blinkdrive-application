package edu.co.upb.blinkdrive.storage.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.co.upb.blinkdrive.storage.service.StorageService;

@Component
public class NodeHealthCheckTask {
    private static final Logger logger = LoggerFactory.getLogger(NodeHealthCheckTask.class);
    
    @Autowired
    private StorageService storageService;
    
    @Scheduled(fixedRateString = "${blinkdrive.health-check.interval:300000}")
    public void performHealthCheck() {
        logger.info("Performing scheduled health check...");
        
        try {
            StorageService.HealthStatus status = storageService.performHealthCheck();
            logger.info("Health check completed. Status: {}, Nodes up: {}, Nodes down: {}",
                    status.getStatus(), status.getNodesUp(), status.getNodesDown());
        } catch (Exception e) {
            logger.error("Scheduled health check failed", e);
        }
    }
}