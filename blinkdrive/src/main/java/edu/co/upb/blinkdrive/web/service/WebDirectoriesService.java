package edu.co.upb.blinkdrive.web.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.upb.api.db.CreateDirectoryRequest;
import co.edu.upb.api.db.CreateDirectoryResponse;
import co.edu.upb.api.db.DirectoryType;
import co.edu.upb.api.db.GetDirectoriesByParentRequest;
import co.edu.upb.api.db.GetDirectoriesByParentResponse;
import co.edu.upb.api.db.GetDirectoryRequest;
import co.edu.upb.api.db.GetDirectoryResponse;
import edu.co.upb.blinkdrive.db.service.DatabaseService;
import edu.co.upb.blinkdrive.web.model.Directory;

@Service
public class WebDirectoriesService {

    @Autowired
    private DatabaseService databaseService;
    
    public Directory getDirectory(int directoryId) {
        GetDirectoryRequest request = new GetDirectoryRequest();
        request.setDirectoryId(directoryId);
        
        GetDirectoryResponse response = databaseService.getDirectory(request);
        
        return convertToDirectory(response.getDirectory());
    }
    
    public List<Directory> getRootDirectories() {
        GetDirectoriesByParentRequest request = new GetDirectoriesByParentRequest();
        request.setParentDirectoryId(0); // Assuming 0 means root
        
        GetDirectoriesByParentResponse response = databaseService.getDirectoriesByParent(request);
        
        List<Directory> directories = new ArrayList<>();
        for (DirectoryType directoryType : response.getDirectories()) {
            directories.add(convertToDirectory(directoryType));
        }
        
        return directories;
    }
    
    public List<Directory> getSubdirectories(int parentDirectoryId) {
        GetDirectoriesByParentRequest request = new GetDirectoriesByParentRequest();
        request.setParentDirectoryId(parentDirectoryId);
        
        GetDirectoriesByParentResponse response = databaseService.getDirectoriesByParent(request);
        
        List<Directory> directories = new ArrayList<>();
        for (DirectoryType directoryType : response.getDirectories()) {
            directories.add(convertToDirectory(directoryType));
        }
        
        return directories;
    }
    
    public boolean createDirectory(String directoryName, Integer parentDirectoryId, String username) {
        try {
            CreateDirectoryRequest request = new CreateDirectoryRequest();
            request.setDirectoryName(directoryName);
            if (parentDirectoryId != null) {
                request.setParentDirectoryId(parentDirectoryId);
            }
            request.setOwnerId(getUserId(username)); // You'll need to implement this method
            
            CreateDirectoryResponse response = databaseService.createDirectory(request);
            
            return response.getDirectory() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Directory convertToDirectory(DirectoryType directoryType) {
        Directory directory = new Directory();
        directory.setId(directoryType.getDirectoryId());
        directory.setName(directoryType.getDirectoryName());
        directory.setParentId(directoryType.getParentDirectoryId());
        directory.setOwnerId(directoryType.getOwnerId());
        directory.setCreationDate(directoryType.getCreationDate());
        directory.setLastModifiedDate(directoryType.getLastModifiedDate());
        directory.setSizeBytes(directoryType.getSizeBytes());
        
        return directory;
    }
    
    // Placeholder method - you'll need to implement user ID retrieval
    private int getUserId(@SuppressWarnings("unused") String username) {
        // In a real implementation, you would look up the user ID from the username
        // This might involve a database query or a service call
        return 1; // Default user ID for simplicity
    }
}