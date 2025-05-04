package edu.co.upb.blinkdrive.storage.endpoint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import co.edu.upb.api.storage.CheckFileExistenceRequest;
import co.edu.upb.api.storage.CheckFileExistenceResponse;
import co.edu.upb.api.storage.DeleteFileRequest;
import co.edu.upb.api.storage.DeleteFileResponse;
import co.edu.upb.api.storage.DownloadFileRequest;
import co.edu.upb.api.storage.DownloadFileResponse;
import co.edu.upb.api.storage.FileStorageLocationType;
import co.edu.upb.api.storage.FileStorageNodeType;
import co.edu.upb.api.storage.GetNodeStatusRequest;
import co.edu.upb.api.storage.GetNodeStatusResponse;
import co.edu.upb.api.storage.HealthCheckRequest;
import co.edu.upb.api.storage.HealthCheckResponse;
import co.edu.upb.api.storage.ReplicateFileRequest;
import co.edu.upb.api.storage.ReplicateFileResponse;
import co.edu.upb.api.storage.UploadFileRequest;
import co.edu.upb.api.storage.UploadFileResponse;
import edu.co.upb.blinkdrive.storage.model.FileStorageLocation;
import edu.co.upb.blinkdrive.storage.model.StorageNode;
import edu.co.upb.blinkdrive.storage.service.StorageService;

@Endpoint
public class StorageEndpoint {

    private static final String NAMESPACE_URI = "http://upb.edu.co/api/storage";
    private static final Logger logger = Logger.getLogger(StorageEndpoint.class.getName());

    @Autowired
    private StorageService storageService;

    /**
     * Handle upload file requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "UploadFileRequest")
    @ResponsePayload
    public UploadFileResponse uploadFile(@RequestPayload UploadFileRequest request) {
        UploadFileResponse response = new UploadFileResponse();

        try {
            // Extract parameters
            String fileId = request.getFileId();
            String fileName = request.getFileName();
            byte[] fileContent = request.getFileContent();

            logger.log(Level.INFO, "Uploading file: {0}, size: {1} bytes", new Object[]{fileName, fileContent != null ? fileContent.length : 0});

            // Process upload
            List<FileStorageLocation> locations = storageService.uploadFile(fileId, fileName, fileContent);

            // Build response
            response.setSuccess(true);
            response.setFileId(locations.isEmpty() ? "" : locations.get(0).getFileId());

            // Map storage locations
            for (FileStorageLocation location : locations) {
                FileStorageLocationType locationType = mapToFileStorageLocationType(location);
                response.getStorageLocations().add(locationType);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Upload failed", e);
            response.setSuccess(false);
            response.setMessage("Upload failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Handle download file requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DownloadFileRequest")
    @ResponsePayload
    public DownloadFileResponse downloadFile(@RequestPayload DownloadFileRequest request) {
        DownloadFileResponse response = new DownloadFileResponse();

        try {
            String fileId = request.getFileId();
            logger.log(Level.INFO, "Downloading file: {0}", fileId);

            // First check if file exists and where
            List<FileStorageLocation> locations = storageService.checkFileExistence(fileId, null);

            if (locations.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("File not found");
                return response;
            }

            // Download the file
            byte[] fileContent = storageService.downloadFile(fileId, locations);

            response.setSuccess(true);
            response.setFileId(fileId);
            response.setFileContent(fileContent);

            // Try to get filename from first location
            if (!locations.isEmpty()) {
                response.setFileName(locations.get(0).getFilePath());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Download failed", e);
            response.setSuccess(false);
            response.setMessage("Download failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Handle delete file requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DeleteFileRequest")
    @ResponsePayload
    public DeleteFileResponse deleteFile(@RequestPayload DeleteFileRequest request) {
        DeleteFileResponse response = new DeleteFileResponse();

        try {
            String fileId = request.getFileId();
            logger.log(Level.INFO, "Deleting file: {0}", fileId);

            // First check if file exists and where
            List<FileStorageLocation> locations = storageService.checkFileExistence(fileId, null);

            if (locations.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("File not found");
                return response;
            }

            // Delete the file from all locations
            boolean success = storageService.deleteFile(fileId, locations);

            response.setSuccess(success);
            if (!success) {
                response.setMessage("Deletion partially failed on some nodes");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Deletion failed", e);
            response.setSuccess(false);
            response.setMessage("Deletion failed: " + e.getMessage());
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetNodeStatusRequest")
    @ResponsePayload
    public GetNodeStatusResponse getNodeStatus(@RequestPayload GetNodeStatusRequest request) {
        GetNodeStatusResponse response = new GetNodeStatusResponse();

        try {
            // Fix: Check if nodeId exists before comparing to zero
            Integer nodeId = null;
            if (request.getNodeId() != null && request.getNodeId() > 0) {
                nodeId = request.getNodeId();
            }

            List<StorageNode> nodes = storageService.getNodesStatus(nodeId);

            response.setSuccess(true);

            // Map nodes to response type
            for (StorageNode node : nodes) {
                FileStorageNodeType nodeType = new FileStorageNodeType();
                nodeType.setNodeId(node.getNodeId());
                nodeType.setNodeName(node.getNodeName());
                nodeType.setIpAddress(node.getIpAddress());
                nodeType.setPort(node.getPort());
                nodeType.setStatus(node.getStatus());
                nodeType.setAvailableCapacityBytes(node.getAvailableCapacityBytes());

                response.getNodes().add(nodeType);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Get node status failed", e);
            response.setSuccess(false);
            response.setMessage("Get node status failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Handle check file existence requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CheckFileExistenceRequest")
    @ResponsePayload
    public CheckFileExistenceResponse checkFileExistence(@RequestPayload CheckFileExistenceRequest request) {
        CheckFileExistenceResponse response = new CheckFileExistenceResponse();

        try {
            String fileId = request.getFileId();
            List<FileStorageLocation> locations = storageService.checkFileExistence(fileId, null);

            response.setExists(!locations.isEmpty());

            // Map locations to response type
            for (FileStorageLocation location : locations) {
                FileStorageLocationType locationType = mapToFileStorageLocationType(location);
                response.getLocations().add(locationType);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Check file existence failed", e);
            response.setExists(false);
        }

        return response;
    }

    /**
     * Handle replicate file requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ReplicateFileRequest")
    @ResponsePayload
    public ReplicateFileResponse replicateFile(@RequestPayload ReplicateFileRequest request) {
        ReplicateFileResponse response = new ReplicateFileResponse();

        try {
            String fileId = request.getFileId();
            int sourceNodeId = request.getSourceNodeId();
            int targetNodeId = request.getTargetNodeId();

            logger.log(Level.INFO, "Replicating file: {0} from node {1} to node {2}", new Object[]{fileId, sourceNodeId, targetNodeId});

            FileStorageLocation newLocation = storageService.replicateFile(fileId, sourceNodeId, targetNodeId);

            response.setSuccess(true);

            // Map new location to response type
            if (newLocation != null) {
                FileStorageLocationType locationType = mapToFileStorageLocationType(newLocation);
                response.setNewLocation(locationType);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Replication failed", e);
            response.setSuccess(false);
            response.setMessage("Replication failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Handle health check requests.
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "HealthCheckRequest")
    @ResponsePayload
    public HealthCheckResponse healthCheck(@RequestPayload HealthCheckRequest request) {
        HealthCheckResponse response = new HealthCheckResponse();

        try {
            StorageService.HealthStatus status = storageService.performHealthCheck();

            response.setStatus(status.getStatus());
            response.setNodesUp(status.getNodesUp());
            response.setNodesDown(status.getNodesDown());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Health check failed", e);
            response.setStatus("ERROR");
            response.setNodesUp(0);
            response.setNodesDown(0);
        }

        return response;
    }

    private FileStorageLocationType mapToFileStorageLocationType(FileStorageLocation location) {
        FileStorageLocationType locationType = new FileStorageLocationType();
        locationType.setFileId(location.getFileId());
        locationType.setNodeId(location.getNodeId());
        locationType.setFilePath(location.getFilePath());
        locationType.setIsPrimary(location.isPrimary());
        return locationType;
    }
}