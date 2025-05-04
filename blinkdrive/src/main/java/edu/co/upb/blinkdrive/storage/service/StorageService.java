package edu.co.upb.blinkdrive.storage.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.co.upb.blinkdrive.storage.exception.StorageException;
import edu.co.upb.blinkdrive.storage.grpc.StatusResponse;
import edu.co.upb.blinkdrive.storage.grpc.StorageNodeClient;
import edu.co.upb.blinkdrive.storage.model.FileStorageLocation;
import edu.co.upb.blinkdrive.storage.model.StorageNode;
import edu.co.upb.blinkdrive.storage.registry.NodeRegistry;

@Service
public class StorageService {
    private static final Logger logger = Logger.getLogger(StorageService.class.getName());
    
    @Autowired
    private NodeRegistry nodeRegistry;
    
    @Value("${blinkdrive.storage.replication.count:1}")
    private int replicationCount;
    
    private final Object replicationLock = new Object();
    
    /**
     * Upload a file to storage nodes with replication.
     */
    public List<FileStorageLocation> uploadFile(String fileId, String fileName, byte[] fileContent) {
        if (fileId == null || fileId.isEmpty()) {
            fileId = UUID.randomUUID().toString();
        }
        
        List<FileStorageLocation> locations = new ArrayList<>();
        
        // Select nodes for storage
        StorageNode primaryNode = nodeRegistry.selectPrimaryNode();
        if (primaryNode == null) {
            throw new StorageException("No active storage nodes available");
        }
        
        try {
            // Upload to primary node
            String finalFileId = fileId;
            StorageNodeClient primaryClient = nodeRegistry.getClientForNode(primaryNode);
            String resultFileId = primaryClient.uploadFile(finalFileId, fileContent);
            
            // Record primary location
            FileStorageLocation primaryLocation = new FileStorageLocation(
                    resultFileId,
                    primaryNode.getNodeId(),
                    fileName, // Using filename as path for simplicity
                    true);
            locations.add(primaryLocation);
            
            // Handle replication to other nodes
            if (replicationCount > 0) {
                List<StorageNode> replicaNodes = nodeRegistry.selectReplicaNodes(
                        primaryNode.getNodeId(), replicationCount);
                
                List<CompletableFuture<FileStorageLocation>> futures = new ArrayList<>();
                
                for (StorageNode replicaNode : replicaNodes) {
                    CompletableFuture<FileStorageLocation> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            StorageNodeClient replicaClient = nodeRegistry.getClientForNode(replicaNode);
                            String replicaFileId = replicaClient.uploadFile(resultFileId, fileContent);
                            
                            return new FileStorageLocation(
                                    replicaFileId,
                                    replicaNode.getNodeId(),
                                    fileName,
                                    false);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Replication failed for node {0}: {1}",
                                    new Object[]{replicaNode.getNodeId(), e.getMessage()});
                            throw new RuntimeException("Replication failed", e);
                        }
                    });
                    
                    futures.add(future);
                }
                
                // Wait for all replications to complete
                CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
                
                try {
                    allOf.get(); // Wait for all replications
                    
                    // Add successful replications to locations
                    for (CompletableFuture<FileStorageLocation> future : futures) {
                        try {
                            FileStorageLocation location = future.get();
                            if (location != null) {
                                locations.add(location);
                            }
                        } catch (ExecutionException ee) {
                            logger.log(Level.WARNING, "Replication task failed", ee);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.WARNING, "Replication interrupted", e);
                } catch (ExecutionException e) {
                    logger.log(Level.WARNING, "Replication failed", e.getCause());
                }
            }
            
            return locations;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Upload failed", e);
            throw new StorageException("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download a file from any available node containing it.
     */
    public byte[] downloadFile(String fileId, List<FileStorageLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            throw new StorageException("No storage locations available for file: " + fileId);
        }
        
        Exception lastException = null;
        
        // Try primary node first
        FileStorageLocation primaryLocation = locations.stream()
                .filter(FileStorageLocation::isPrimary)
                .findFirst()
                .orElse(locations.get(0));
                
        try {
            StorageNode node = nodeRegistry.getNode(primaryLocation.getNodeId());
            if (node != null && "ACTIVE".equals(node.getStatus())) {
                StorageNodeClient client = nodeRegistry.getClientForNode(node);
                return client.downloadFile(fileId);
            }
        } catch (Exception e) {
            lastException = e;
            logger.log(Level.WARNING, "Download from primary node failed", e);
        }
        
        // Try replica nodes
        for (FileStorageLocation location : locations) {
            if (location.getNodeId() == primaryLocation.getNodeId()) {
                continue; // Skip primary, already tried
            }
            
            try {
                StorageNode node = nodeRegistry.getNode(location.getNodeId());
                if (node != null && "ACTIVE".equals(node.getStatus())) {
                    StorageNodeClient client = nodeRegistry.getClientForNode(node);
                    return client.downloadFile(fileId);
                }
            } catch (Exception e) {
                lastException = e;
                logger.log(Level.WARNING, "Download from replica node {0} failed", location.getNodeId());
            }
        }
        
        throw new StorageException("Failed to download file from any available node", lastException);
    }
    
    /**
     * Delete a file from all nodes.
     */
    public boolean deleteFile(String fileId, List<FileStorageLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            logger.warning("No storage locations provided for deletion");
            return false;
        }
        
        boolean allSuccessful = true;
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (FileStorageLocation location : locations) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    StorageNode node = nodeRegistry.getNode(location.getNodeId());
                    if (node != null && "ACTIVE".equals(node.getStatus())) {
                        StorageNodeClient client = nodeRegistry.getClientForNode(node);
                        return client.deleteFile(fileId);
                    }
                    return false;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Deletion failed on node {0}: {1}",
                            new Object[]{location.getNodeId(), e.getMessage()});
                    return false;
                }
            });
            
            futures.add(future);
        }
        
        // Wait for all deletions to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        
        try {
            allOf.get(); // Wait for all deletions
            
            // Check results
            for (CompletableFuture<Boolean> future : futures) {
                try {
                    Boolean result = future.get();
                    allSuccessful &= result != null && result;
                } catch (ExecutionException ee) {
                    allSuccessful = false;
                    logger.log(Level.WARNING, "Deletion task failed", ee);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Deletion interrupted", e);
            return false;
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "Deletion failed", e.getCause());
            return false;
        }
        
        return allSuccessful;
    }
    
    /**
     * Check if a file exists in any node.
     */
    public List<FileStorageLocation> checkFileExistence(String fileId, List<FileStorageLocation> knownLocations) {
        List<FileStorageLocation> validLocations = new ArrayList<>();
        
        if (knownLocations != null && !knownLocations.isEmpty()) {
            // Verify known locations
            for (FileStorageLocation location : knownLocations) {
                try {
                    StorageNode node = nodeRegistry.getNode(location.getNodeId());
                    if (node != null && "ACTIVE".equals(node.getStatus())) {
                        StorageNodeClient client = nodeRegistry.getClientForNode(node);
                        // Try to download a small chunk to see if file exists
                        client.downloadFile(fileId);
                        validLocations.add(location);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "File check failed on node {0}: {1}",
                            new Object[]{location.getNodeId(), e.getMessage()});
                }
            }
        } else {
            // Try to find the file on all active nodes
            List<StorageNode> activeNodes = nodeRegistry.getActiveNodes();
            for (StorageNode node : activeNodes) {
                try {
                    StorageNodeClient client = nodeRegistry.getClientForNode(node);
                    // Try to download a small chunk to see if file exists
                    client.downloadFile(fileId);
                    
                    // If no exception, file exists
                    FileStorageLocation location = new FileStorageLocation(
                            fileId,
                            node.getNodeId(),
                            fileId, // Using fileId as path for simplicity
                            false); // We don't know which is primary
                            
                    validLocations.add(location);
                } catch (Exception e) {
                    // File doesn't exist on this node, skip
                }
            }
            
            // Mark first found location as primary (if any)
            if (!validLocations.isEmpty()) {
                validLocations.get(0).setPrimary(true);
            }
        }
        
        return validLocations;
    }
    
    /**
     * Perform a health check on all nodes.
     */
    public HealthStatus performHealthCheck() {
        List<StorageNode> allNodes = nodeRegistry.getAllNodes();
        int nodesUp = 0;
        int nodesDown = 0;
        
        for (StorageNode node : allNodes) {
            try {
                StorageNodeClient client = nodeRegistry.getClientForNode(node);
                StatusResponse status = client.getStatus();
                
                if (status.getHealthy()) {
                    nodesUp++;
                    nodeRegistry.updateNodeStatus(node.getNodeId(), "ACTIVE", (long) status.getDiskSpaceAvailable());
                } else {
                    nodesDown++;
                    nodeRegistry.updateNodeStatus(node.getNodeId(), "INACTIVE", 0);
                }
            } catch (Exception e) {
                nodesDown++;
                nodeRegistry.updateNodeStatus(node.getNodeId(), "INACTIVE", 0);
                logger.log(Level.WARNING, "Health check failed for node {0}: {1}",
                        new Object[]{node.getNodeId(), e.getMessage()});
            }
        }
        
        String status = nodesUp > 0 ? "UP" : "DOWN";
        return new HealthStatus(status, nodesUp, nodesDown);
    }
    
    /**
     * Get status of all nodes.
     */
    public List<StorageNode> getNodesStatus(Integer specificNodeId) {
        List<StorageNode> nodes;
        
        if (specificNodeId != null) {
            StorageNode node = nodeRegistry.getNode(specificNodeId);
            nodes = node != null ? List.of(node) : List.of();
        } else {
            nodes = nodeRegistry.getAllNodes();
        }
        
        // Update all node statuses
        nodes.forEach(node -> {
            try {
                StorageNodeClient client = nodeRegistry.getClientForNode(node);
                StatusResponse status = client.getStatus();
                nodeRegistry.updateNodeStatus(
                        node.getNodeId(),
                        status.getHealthy() ? "ACTIVE" : "INACTIVE",
                        (long) status.getDiskSpaceAvailable());
            } catch (Exception e) {
                nodeRegistry.updateNodeStatus(node.getNodeId(), "INACTIVE", 0);
                logger.log(Level.WARNING, "Failed to get status for node {0}: {1}",
                        new Object[]{node.getNodeId(), e.getMessage()});
            }
        });
        
        return nodes;
    }
    
    /**
     * Internal class to represent health check result.
     */
    public static class HealthStatus {
        private final String status;
        private final int nodesUp;
        private final int nodesDown;
        
        public HealthStatus(String status, int nodesUp, int nodesDown) {
            this.status = status;
            this.nodesUp = nodesUp;
            this.nodesDown = nodesDown;
        }
        
        public String getStatus() {
            return status;
        }
        
        public int getNodesUp() {
            return nodesUp;
        }
        
        public int getNodesDown() {
            return nodesDown;
        }
    }
    
    /**
     * Replicate a file from one node to another.
     */
    public FileStorageLocation replicateFile(String fileId, int sourceNodeId, int targetNodeId) {
        // Get source node
        StorageNode sourceNode = nodeRegistry.getNode(sourceNodeId);
        if (sourceNode == null || !"ACTIVE".equals(sourceNode.getStatus())) {
            throw new StorageException("Source node not available: " + sourceNodeId);
        }
        
        // Get target node
        StorageNode targetNode = nodeRegistry.getNode(targetNodeId);
        if (targetNode == null || !"ACTIVE".equals(targetNode.getStatus())) {
            throw new StorageException("Target node not available: " + targetNodeId);
        }
        
        synchronized(replicationLock) {
            try {
                // Download from source
                StorageNodeClient sourceClient = nodeRegistry.getClientForNode(sourceNode);
                byte[] fileContent = sourceClient.downloadFile(fileId);
                
                // Upload to target
                StorageNodeClient targetClient = nodeRegistry.getClientForNode(targetNode);
                String resultFileId = targetClient.uploadFile(fileId, fileContent);
                
                // Return new location
                return new FileStorageLocation(
                        resultFileId,
                        targetNodeId,
                        fileId, // Using fileId as path for simplicity
                        false);
                        
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Replication failed", e);
                throw new StorageException("Replication failed: " + e.getMessage(), e);
            }
        }
    }
}