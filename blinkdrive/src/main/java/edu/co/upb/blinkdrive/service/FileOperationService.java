package edu.co.upb.blinkdrive.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.upb.api.db.CreateFileMetadataRequest;
import co.edu.upb.api.db.DeleteFileRequest;
import co.edu.upb.api.db.FileType;
import co.edu.upb.api.db.GetFileMetadataRequest;
import edu.co.upb.blinkdrive.auth.service.AuthenticationService;
import edu.co.upb.blinkdrive.db.service.DatabaseService;
import edu.co.upb.blinkdrive.storage.exception.StorageException;
import edu.co.upb.blinkdrive.storage.model.FileStorageLocation;
import edu.co.upb.blinkdrive.storage.service.StorageService;

@Service
public class FileOperationService {

    @Autowired
    private AuthenticationService authService;
    
    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private StorageService storageService;
    
    /**
     * Upload a file to the system.
     * 1. Validate JWT token
     * 2. Upload file to storage nodes
     * 3. Record metadata in database
     */
    public FileUploadResult uploadFile(String token, String fileName, byte[] fileContent, 
                                      int directoryId, String fileType, int userId) {
        // Validate token
        if (!authService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        // Upload to storage nodes
        List<FileStorageLocation> locations;
        String storageFileId;
        try {
            locations = storageService.uploadFile(null, fileName, fileContent);
            if (locations.isEmpty()) {
                throw new StorageException("File upload failed - no storage locations available");
            }
            storageFileId = locations.get(0).getFileId();
        } catch (StorageException e) {
            throw new StorageException("Failed to upload file to storage nodes: " + e.getMessage(), e);
        }
        
        // Record metadata in database
        FileType fileMetadata;
        try {
            CreateFileMetadataRequest metadataRequest = new CreateFileMetadataRequest();
            metadataRequest.setFileName(fileName);
            metadataRequest.setDirectoryId(directoryId);
            metadataRequest.setFileSize((long) fileContent.length);
            metadataRequest.setFileType(fileType);
            metadataRequest.setOwnerId(userId);
            
            // Store the storage node ID in the checksum field
            metadataRequest.setChecksum(storageFileId);
            
            fileMetadata = databaseService.createFileMetadata(metadataRequest).getFile();
        } catch (Exception e) {
            // Rollback - delete file from storage nodes if metadata creation fails
            try {
                storageService.deleteFile(storageFileId, locations);
            } catch (Exception ex) {
                // Log rollback failure
                System.err.println("Failed to rollback file upload: " + ex.getMessage());
            }
            
            throw new RuntimeException("Failed to record file metadata: " + e.getMessage(), e);
        }
        
        // Return success result
        return new FileUploadResult(
                storageFileId, 
                fileMetadata.getFileId(), 
                locations,
                fileMetadata);
    }
    
    /**
     * Download a file from the system.
     * 1. Validate JWT token
     * 2. Check permissions in database
     * 3. Retrieve file from storage nodes
     */
    public FileDownloadResult downloadFile(String token, int fileMetadataId, int userId) {
        // Validate token
        if (!authService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        // Get file metadata from database
        FileType fileMetadata;
        try {
            // Create proper request object
            GetFileMetadataRequest metadataRequest = new GetFileMetadataRequest();
            metadataRequest.setFileId(fileMetadataId);
            
            fileMetadata = databaseService.getFileMetadata(metadataRequest).getFile();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
        }
        
        // Check permissions
        if (fileMetadata.getOwnerId() != userId) {
            // Check if file is shared with user
            // TODO: Implement sharing check
        }
        
        // Extract the storage file ID from the checksum field
        String storageFileId = fileMetadata.getChecksum();
        if (storageFileId == null || storageFileId.isEmpty()) {
            throw new RuntimeException("Storage file ID not found in metadata");
        }
        
        // Get storage locations using the storage file ID
        List<FileStorageLocation> locations;
        try {
            locations = storageService.checkFileExistence(storageFileId, null);
            
            if (locations.isEmpty()) {
                throw new StorageException("File not found in storage nodes");
            }
        } catch (StorageException e) {
            throw new RuntimeException("Failed to locate file in storage: " + e.getMessage(), e);
        }
        
        // Download file from storage
        byte[] fileContent;
        try {
            fileContent = storageService.downloadFile(storageFileId, locations);
        } catch (Exception e) {
            throw new StorageException("Failed to download file from storage: " + e.getMessage(), e);
        }
        
        // Return the file
        return new FileDownloadResult(
                fileMetadata.getFileId(),
                fileMetadata.getFileName(),
                fileContent,
                fileMetadata.getFileSize(),
                fileMetadata.getFileType());
    }
    
    /**
     * Delete a file from the system.
     * 1. Validate JWT token
     * 2. Check permissions in database
     * 3. Delete file from storage nodes
     * 4. Delete metadata from database
     */
    public boolean deleteFile(String token, int fileMetadataId, int userId) {
        // Validate token
        if (!authService.validateToken(token)) {
            throw new SecurityException("Invalid or expired token");
        }
        
        // Get file metadata from database
        FileType fileMetadata;
        try {
            // Create proper request object
            GetFileMetadataRequest metadataRequest = new GetFileMetadataRequest();
            metadataRequest.setFileId(fileMetadataId);
            
            fileMetadata = databaseService.getFileMetadata(metadataRequest).getFile();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
        }
        
        // Check permissions
        if (fileMetadata.getOwnerId() != userId) {
            // Check if user has delete permission
            // TODO: Implement permission check
        }
        
        // Extract storage file ID from checksum field
        String storageFileId = fileMetadata.getChecksum();
        if (storageFileId == null || storageFileId.isEmpty()) {
            throw new RuntimeException("Storage file ID not found in metadata");
        }
        
        // Delete from storage
        AtomicReference<Boolean> storageDeleteSuccess = new AtomicReference<>(false);
        try {
            // Find file locations with storage file ID
            List<FileStorageLocation> locations = storageService.checkFileExistence(storageFileId, null);
            
            if (!locations.isEmpty()) {
                storageDeleteSuccess.set(storageService.deleteFile(storageFileId, locations));
            }
        } catch (Exception e) {
            System.err.println("Failed to delete file from storage: " + e.getMessage());
            // Continue with metadata deletion
        }
        
        // Delete metadata from database
        boolean metadataDeleteSuccess;
        try {
            // Create proper request object
            DeleteFileRequest deleteRequest = new DeleteFileRequest();
            deleteRequest.setFileId(fileMetadataId);
            deleteRequest.setUserId(userId);
            
            metadataDeleteSuccess = databaseService.deleteFile(deleteRequest).isSuccess();
        } catch (Exception e) {
            // If we failed to delete from storage, this is a partial failure
            if (!storageDeleteSuccess.get()) {
                throw new RuntimeException("Failed to delete file from storage and database", e);
            }
            
            throw new RuntimeException("Failed to delete file metadata: " + e.getMessage(), e);
        }
        
        return storageDeleteSuccess.get() && metadataDeleteSuccess;
    }
    
    // // Helper method to calculate checksum
    // private String calculateChecksum(byte[] data) {
    //     try {
    //         java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
    //         byte[] hash = md.digest(data);
    //         StringBuilder hexString = new StringBuilder();
    //         for (byte b : hash) {
    //             String hex = Integer.toHexString(0xff & b);
    //             if (hex.length() == 1) hexString.append('0');
    //             hexString.append(hex);
    //         }
    //         return hexString.toString();
    //     } catch (Exception e) {
    //         throw new RuntimeException("Failed to calculate checksum", e);
    //     }
    // }
    
    // Result classes
    public static class FileUploadResult {
        private final String fileId;
        private final int fileMetadataId;
        private final List<FileStorageLocation> locations;
        private final FileType metadata;
        
        public FileUploadResult(String fileId, int fileMetadataId, List<FileStorageLocation> locations, FileType metadata) {
            this.fileId = fileId;
            this.fileMetadataId = fileMetadataId;
            this.locations = locations;
            this.metadata = metadata;
        }
        
        public String getFileId() {
            return fileId;
        }
        
        public int getFileMetadataId() {
            return fileMetadataId;
        }
        
        public List<FileStorageLocation> getLocations() {
            return locations;
        }
        
        public FileType getMetadata() {
            return metadata;
        }
    }
    
    public static class FileDownloadResult {
        private final int fileId;
        private final String fileName;
        private final byte[] fileContent;
        private final long fileSize;
        private final String fileType;
        
        public FileDownloadResult(int fileId, String fileName, byte[] fileContent, long fileSize, String fileType) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileContent = fileContent;
            this.fileSize = fileSize;
            this.fileType = fileType;
        }
        
        public int getFileId() {
            return fileId;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public byte[] getFileContent() {
            return fileContent;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public String getFileType() {
            return fileType;
        }
    }
}