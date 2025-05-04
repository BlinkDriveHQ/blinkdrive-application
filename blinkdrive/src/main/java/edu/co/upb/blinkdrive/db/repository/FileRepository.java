package edu.co.upb.blinkdrive.db.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Repository;

import co.edu.upb.api.db.CreateFileMetadataRequest;
import co.edu.upb.api.db.FileStorageLocationType;
import co.edu.upb.api.db.FileType;
import co.edu.upb.api.db.MoveFileRequest;
import co.edu.upb.api.db.RenameFileRequest;
import edu.co.upb.blinkdrive.db.client.DatabaseRestClient;
import edu.co.upb.blinkdrive.db.client.DatabaseServiceException;
import edu.co.upb.blinkdrive.db.dto.directory.MoveResourceDto;
import edu.co.upb.blinkdrive.db.dto.file.FileDetailDto;
import edu.co.upb.blinkdrive.db.dto.file.FileDto;
import edu.co.upb.blinkdrive.db.dto.file.FileStorageLocationDto;
import edu.co.upb.blinkdrive.db.dto.file.RenameFileDto;

@Repository
public class FileRepository {
    
    private final DatabaseRestClient restClient;
    
    public FileRepository(DatabaseRestClient restClient) {
        this.restClient = restClient;
    }
    
    public FileType createFileMetadata(CreateFileMetadataRequest request) {
        // Build request body for the REST API
        var requestBody = new FileDto(
            null,
            request.getFileName(),
            request.getDirectoryId(),
            request.getFileSize(),
            request.getFileType(),
            request.getOwnerId(),
            request.getChecksum(),
            null,
            null
        );
        
        FileDto response = restClient.post("/files", requestBody, FileDto.class);
        return mapToFileType(response);
    }
    
    public FileType getFileMetadata(int fileId) {
        try {
            // Get the file details including empty storage locations array
            FileDetailDto fileDetailDto = restClient.get("/files/" + fileId, FileDetailDto.class);
            
            // If we successfully got the file, map it to FileType
            return mapToFileType(fileDetailDto.file());
        } catch (Exception ex) {
            System.err.println("Error getting file metadata: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to get file metadata: " + fileId, ex);
        }
    }
    
    public List<FileStorageLocationType> getFileStorageLocations(int fileId) {
        try {
            // Get the file details
            FileDetailDto fileDetailDto = restClient.get("/files/" + fileId, FileDetailDto.class);
            
            // Handle the empty array case
            if (fileDetailDto.storageLocations() == null || fileDetailDto.storageLocations().isEmpty()) {
                return Collections.emptyList();
            }
            
            return fileDetailDto.storageLocations().stream()
                .map(this::mapToFileStorageLocationType)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            System.err.println("Error getting file storage locations: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    /* 
    // Uncommented code for getting file metadata and storage locations
    // This code is not used in the current implementation but we are going to implement it 
    // once we implement the remaining server that comunicates with the node storage servers.
    public FileType getFileMetadata(int fileId) {
        try {
            // Get the file details
            FileDetailDto fileDetailDto = restClient.get("/files/" + fileId, FileDetailDto.class);
            
            // Check if the response contains a file
            if (fileDetailDto == null || fileDetailDto.file() == null) {
                throw new RuntimeException("File with ID " + fileId + " not found");
            }
            
            return mapToFileType(fileDetailDto.file());
        } catch (RuntimeException ex) {
            System.err.println("Error getting file metadata: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to get file metadata: " + fileId, ex);
        }
    }
    
    public List<FileStorageLocationType> getFileStorageLocations(int fileId) {
        try {
            // Get the file details
            FileDetailDto fileDetailDto = restClient.get("/files/" + fileId, FileDetailDto.class);
            
            // Check if the response contains storage locations
            if (fileDetailDto == null || fileDetailDto.storageLocations() == null) {
                return Collections.emptyList();
            }
            
            return fileDetailDto.storageLocations().stream()
                .map(this::mapToFileStorageLocationType)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            System.err.println("Error getting file storage locations: " + ex.getMessage());
            return Collections.emptyList();
        }
    }
    */
    
    public List<FileType> getFilesByDirectory(int directoryId) {
        try {
            // Use array class directly for the response type
            FileDto[] filesArray = restClient.get("/files/directory/" + directoryId, FileDto[].class);
            
            // Convert array to list and map to FileType objects
            return Arrays.stream(filesArray)
                .map(this::mapToFileType)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            System.err.println("Error getting files by directory: " + ex.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }
    
    public FileType renameFile(RenameFileRequest request) {
        var requestBody = new RenameFileDto(request.getNewFileName());
        
        // Add headers to the request to include the user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getUserId()));
        
        FileDto response = restClient.put(
                "/resources/file/" + request.getFileId() + "/rename", 
                requestBody, 
                headers,
                FileDto.class);
        
        return mapToFileType(response);
    }
    
    public FileType moveFile(MoveFileRequest request) {
        var requestBody = new MoveResourceDto(request.getNewDirectoryId());
        
        // Add headers to the request to include the user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getUserId()));
        
        FileDto response = restClient.put(
                "/resources/file/" + request.getFileId() + "/move", 
                requestBody, 
                headers,
                FileDto.class);
        
        return mapToFileType(response);
    }
    
    public boolean deleteFile(int fileId, int userId) {
        try {
            // Add headers to the request to include the user ID
            HttpHeaders headers = new HttpHeaders();
            headers.set("user-id", String.valueOf(userId));
            
            restClient.delete("/resources/file/" + fileId, headers);
            return true;
        } catch (Exception ex) {
            System.err.println("Error deleting file: " + ex.getMessage());
            return false;
        }
    }
    
    public FileStorageLocationType addFileStorageLocation(int fileId, int nodeId, String filePath, boolean isPrimary) {
        var requestBody = new FileStorageLocationDto(null, fileId, nodeId, filePath, isPrimary, null);
        
        FileStorageLocationDto response = restClient.post(
                "/file-storage-locations", 
                requestBody, 
                FileStorageLocationDto.class);
        
        return mapToFileStorageLocationType(response);
    }
    
    private FileType mapToFileType(FileDto dto) {
        FileType fileType = new FileType();
        fileType.setFileId(dto.file_id());
        fileType.setFileName(dto.file_name());
        fileType.setDirectoryId(dto.directory_id());
        fileType.setFileSize(dto.file_size_bytes());
        fileType.setFileType(dto.file_type());
        fileType.setOwnerId(dto.owner_user_id());
        fileType.setChecksum(dto.checksum());
        
        if (dto.creation_date() != null) {
            fileType.setCreationDate(convertToXMLGregorianCalendar(dto.creation_date()));
        }
        
        if (dto.last_modified_date() != null) {
            fileType.setLastModifiedDate(convertToXMLGregorianCalendar(dto.last_modified_date()));
        }
        
        return fileType;
    }
    
    private FileStorageLocationType mapToFileStorageLocationType(FileStorageLocationDto dto) {
        FileStorageLocationType locationType = new FileStorageLocationType();
        locationType.setLocationId(dto.location_id());
        locationType.setFileId(dto.file_id());
        locationType.setNodeId(dto.node_id());
        locationType.setFilePath(dto.file_path());
        locationType.setIsPrimary(dto.is_primary());
        return locationType;
    }
    
    // Helper method to convert java.util.Date to XMLGregorianCalendar
    private XMLGregorianCalendar convertToXMLGregorianCalendar(java.util.Date date) {
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(date);
            
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Error converting Date to XMLGregorianCalendar", e);
        }
    }
}