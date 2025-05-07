package edu.co.upb.blinkdrive.web.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.upb.api.db.CreateFileMetadataRequest;
import co.edu.upb.api.db.CreateFileMetadataResponse;
import co.edu.upb.api.db.DeleteFileRequest;
import co.edu.upb.api.db.DeleteFileResponse;
import co.edu.upb.api.db.FileType;
import co.edu.upb.api.db.GetFileMetadataRequest;
import co.edu.upb.api.db.GetFileMetadataResponse;
import co.edu.upb.api.db.GetFilesByDirectoryRequest;
import co.edu.upb.api.db.GetFilesByDirectoryResponse;
import edu.co.upb.blinkdrive.db.service.DatabaseService;
import edu.co.upb.blinkdrive.storage.model.FileStorageLocation;
import edu.co.upb.blinkdrive.storage.service.StorageService;
import edu.co.upb.blinkdrive.web.model.File;

@Service
public class WebFilesService {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private StorageService storageService;

    /**
     * Retrieves all files in a directory
     *
     * @param directoryId The directory ID
     * @return List of files in the directory
     */
    public List<File> getFilesByDirectory(int directoryId) {
        GetFilesByDirectoryRequest request = new GetFilesByDirectoryRequest();
        request.setDirectoryId(directoryId);

        GetFilesByDirectoryResponse response = databaseService.getFilesByDirectory(request);

        List<File> files = new ArrayList<>();
        for (FileType fileType : response.getFiles()) {
            files.add(convertToFile(fileType));
        }

        return files;
    }

    public boolean uploadFile(String fileName, byte[] content, int directoryId, String contentType, String username, String token) {
        try {
            // 1. Generate a unique fileId for storage
            String storageFileId = UUID.randomUUID().toString();
            System.out.println("Generated storage file ID: " + storageFileId);

            // 2. Calculate checksum for file integrity verification
            String checksum = calculateChecksum(content);
            System.out.println("Generated checksum: " + checksum);

            // 3. Upload file to storage nodes
            List<FileStorageLocation> locations = storageService.uploadFile(storageFileId, fileName, content);

            if (locations.isEmpty()) {
                System.err.println("No storage locations returned after upload");
                return false;
            }

            // 4. Log storage locations for debugging
            System.out.println("File uploaded to " + locations.size() + " locations:");
            for (FileStorageLocation loc : locations) {
                System.out.println("Node: " + loc.getNodeId() + ", Path: " + loc.getFilePath() + ", FileID: " + loc.getFileId());
            }

            // 5. Store the storageFileId in the checksum field as a workaround
            // This ensures we can find the file in storage when we need it
            CreateFileMetadataRequest metadataRequest = new CreateFileMetadataRequest();
            metadataRequest.setFileName(fileName);
            metadataRequest.setDirectoryId(directoryId);
            metadataRequest.setFileSize((long) content.length);
            metadataRequest.setFileType(contentType);
            metadataRequest.setOwnerId(getUserId(username));
            metadataRequest.setChecksum(storageFileId); // Store the storage file ID here

            CreateFileMetadataResponse metadataResponse = databaseService.createFileMetadata(metadataRequest);

            return metadataResponse.getFile() != null;
        } catch (Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
            return false;
        }
    }

    public FileDownloadResult downloadFile(int fileId, String token) {
        try {
            // 1. Get file metadata
            GetFileMetadataRequest metadataRequest = new GetFileMetadataRequest();
            metadataRequest.setFileId(fileId);

            GetFileMetadataResponse metadataResponse = databaseService.getFileMetadata(metadataRequest);

            if (metadataResponse.getFile() == null) {
                System.err.println("File metadata not found for ID: " + fileId);
                return null;
            }

            // 2. Get the storage file ID
            String storageFileId = getStorageFileId(fileId);
            System.out.println("Using storage file ID for download: " + storageFileId);

            // 3. Get file locations from storage
            List<FileStorageLocation> locations = storageService.checkFileExistence(storageFileId, null);

            if (locations.isEmpty()) {
                System.err.println("No storage locations found for file: " + storageFileId);
                return null;
            }

            // 4. Download file content
            byte[] fileContent = storageService.downloadFile(storageFileId, locations);

            if (fileContent == null || fileContent.length == 0) {
                System.err.println("Downloaded file content is empty");
                return null;
            }

            System.out.println("File downloaded successfully, size: " + fileContent.length + " bytes");

            return new FileDownloadResult(
                    metadataResponse.getFile().getFileName(),
                    metadataResponse.getFile().getFileType(),
                    fileContent
            );
        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteFile(int fileId, String token) {
        try {
            // 1. Get the storage file ID
            String storageFileId = getStorageFileId(fileId);
            System.out.println("Using storage file ID for deletion: " + storageFileId);

            // 2. Get file locations
            List<FileStorageLocation> locations = storageService.checkFileExistence(storageFileId, null);
            System.out.println("Found " + locations.size() + " storage locations for deletion");

            boolean storageSuccess = true;
            if (!locations.isEmpty()) {
                // 3. Delete from storage nodes
                storageSuccess = storageService.deleteFile(storageFileId, locations);
                System.out.println("Storage deletion result: " + storageSuccess);
            }

            // 4. Delete metadata
            DeleteFileRequest metadataRequest = new DeleteFileRequest();
            metadataRequest.setFileId(fileId);
            metadataRequest.setUserId(1); // Default user ID

            DeleteFileResponse metadataResponse = databaseService.deleteFile(metadataRequest);
            boolean metadataSuccess = metadataResponse.isSuccess();

            System.out.println("Metadata deletion result: " + metadataSuccess);

            return storageSuccess && metadataSuccess;
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    private String getStorageFileId(int databaseFileId) {
        try {
            // Get the file metadata
            GetFileMetadataRequest request = new GetFileMetadataRequest();
            request.setFileId(databaseFileId);

            GetFileMetadataResponse response = databaseService.getFileMetadata(request);

            if (response.getFile() != null) {
                // The storage file ID should be in the checksum field
                String storageFileId = response.getFile().getChecksum();

                if (storageFileId != null && !storageFileId.isEmpty()) {
                    System.out.println("Found storage file ID in checksum: " + storageFileId);
                    return storageFileId;
                }
            }

            // Fallback: use the database ID as string
            System.out.println("No storage file ID found, using database ID: " + databaseFileId);
            return String.valueOf(databaseFileId);
        } catch (Exception e) {
            System.err.println("Error getting storage file ID: " + e.getMessage());
            return String.valueOf(databaseFileId);
        }
    }

    // Helper method to calculate checksum
    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] checksumBytes = md.digest(content);
            return bytesToHex(checksumBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error calculating checksum: " + e.getMessage());
            return null;
        }
    }

// Helper method to convert bytes to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts a FileType from the database service to a File model for the UI
     *
     * @param fileType The FileType object
     * @return File model for UI display
     */
    private File convertToFile(FileType fileType) {
        File file = new File();
        file.setId(fileType.getFileId());
        file.setName(fileType.getFileName());
        file.setDirectoryId(fileType.getDirectoryId());
        file.setSize(fileType.getFileSize());
        file.setType(fileType.getFileType());
        file.setOwnerId(fileType.getOwnerId());
        file.setCreationDate(fileType.getCreationDate());
        file.setLastModifiedDate(fileType.getLastModifiedDate());

        return file;
    }

    /**
     * Helper class for file downloads
     */
    public static class FileDownloadResult {

        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public FileDownloadResult(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    /**
     * Placeholder method - you'll need to implement user ID retrieval
     *
     * @param username The username
     * @return User ID associated with the username
     */
    private int getUserId(@SuppressWarnings("unused") String username) {
        // In a real implementation, you would look up the user ID from the username
        // This might involve a database query or a service call
        return 1; // Default user ID for simplicity
    }
}
