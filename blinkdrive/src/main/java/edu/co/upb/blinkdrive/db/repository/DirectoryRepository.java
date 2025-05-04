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

import co.edu.upb.api.db.CreateDirectoryRequest;
import co.edu.upb.api.db.DirectoryType;
import co.edu.upb.api.db.MoveDirectoryRequest;
import co.edu.upb.api.db.RenameDirectoryRequest;
import edu.co.upb.blinkdrive.db.client.DatabaseRestClient;
import edu.co.upb.blinkdrive.db.dto.directory.DirectoryDto;
import edu.co.upb.blinkdrive.db.dto.directory.MoveResourceDto;
import edu.co.upb.blinkdrive.db.dto.directory.RenameDirectoryDto;

@Repository
public class DirectoryRepository {
    
    private final DatabaseRestClient restClient;
    
    public DirectoryRepository(DatabaseRestClient restClient) {
        this.restClient = restClient;
    }
    
    public DirectoryType createDirectory(CreateDirectoryRequest request) {
        // Build request body for the REST API
        var requestBody = new DirectoryDto(
            null,
            request.getDirectoryName(),
            request.getParentDirectoryId(),
            request.getOwnerId(),
            null,
            null,
            null
        );
        
        DirectoryDto response = restClient.post("/directories", requestBody, DirectoryDto.class);
        return mapToDirectoryType(response);
    }
    
    public DirectoryType getDirectory(int directoryId) {
        DirectoryDto response = restClient.get("/directories/" + directoryId, DirectoryDto.class);
        return mapToDirectoryType(response);
    }
    
    public List<DirectoryType> getDirectoriesByParent(int parentId) {
        try {
            // Use array class directly for the response type
            DirectoryDto[] directoriesArray = restClient.get("/directories/parent/" + parentId, DirectoryDto[].class);
            
            // Convert array to list and map to DirectoryType objects
            return Arrays.stream(directoriesArray)
                .map(this::mapToDirectoryType)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            System.err.println("Error getting directories by parent: " + ex.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }
    
    public DirectoryType renameDirectory(RenameDirectoryRequest request) {
        var requestBody = new RenameDirectoryDto(request.getNewDirectoryName());
        
        // Add headers to the request to include the user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getUserId()));
        
        DirectoryDto response = restClient.put(
                "/resources/directory/" + request.getDirectoryId() + "/rename", 
                requestBody, 
                headers,
                DirectoryDto.class);
        
        return mapToDirectoryType(response);
    }
    
    public DirectoryType moveDirectory(MoveDirectoryRequest request) {
        var requestBody = new MoveResourceDto(request.getNewParentDirectoryId());
        
        // Add headers to the request to include the user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getUserId()));
        
        DirectoryDto response = restClient.put(
                "/resources/directory/" + request.getDirectoryId() + "/move", 
                requestBody, 
                headers,
                DirectoryDto.class);
        
        return mapToDirectoryType(response);
    }
    
    public boolean deleteDirectory(int directoryId, int userId) {
        try {
            // Add headers to the request to include the user ID
            HttpHeaders headers = new HttpHeaders();
            headers.set("user-id", String.valueOf(userId));
            
            restClient.delete("/resources/directory/" + directoryId, headers);
            return true;
        } catch (Exception ex) {
            System.err.println("Error deleting directory: " + ex.getMessage());
            return false;
        }
    }
    
    private DirectoryType mapToDirectoryType(DirectoryDto dto) {
        DirectoryType directoryType = new DirectoryType();
        directoryType.setDirectoryId(dto.directory_id());
        directoryType.setDirectoryName(dto.directory_name());
        directoryType.setParentDirectoryId(dto.parent_directory_id());
        directoryType.setOwnerId(dto.owner_user_id());
        
        if (dto.creation_date() != null) {
            directoryType.setCreationDate(convertToXMLGregorianCalendar(dto.creation_date()));
        }
        
        if (dto.last_modified_date() != null) {
            directoryType.setLastModifiedDate(convertToXMLGregorianCalendar(dto.last_modified_date()));
        }
        
        directoryType.setSizeBytes(dto.size_bytes());
        
        return directoryType;
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