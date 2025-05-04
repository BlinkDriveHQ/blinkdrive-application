package edu.co.upb.blinkdrive.db.repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Repository;

import co.edu.upb.api.db.ShareDirectoryRequest;
import co.edu.upb.api.db.ShareFileRequest;
import edu.co.upb.blinkdrive.db.client.DatabaseRestClient;
import edu.co.upb.blinkdrive.db.dto.sharing.DirectoryUserPermissionDto;
import edu.co.upb.blinkdrive.db.dto.sharing.FileUserPermissionDto;
import edu.co.upb.blinkdrive.db.dto.sharing.ShareDirectoryRequestDto;
import edu.co.upb.blinkdrive.db.dto.sharing.ShareFileRequestDto;

@Repository
public class SharingRepository {
    
    private final DatabaseRestClient restClient;
    
    public SharingRepository(DatabaseRestClient restClient) {
        this.restClient = restClient;
    }
    
    public boolean shareFile(ShareFileRequest request) {
        List<FileUserPermissionDto> users = request.getUsers().stream()
            .map(user -> new FileUserPermissionDto(
                user.getUserId(),
                user.getPermissionLevel().value(),
                convertToDate(user.getExpiryDate())))
            .collect(Collectors.toList());
            
        ShareFileRequestDto requestDto = new ShareFileRequestDto(users);
        
        // Add headers to the request to include the sharedBy user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getSharedByUserId()));
        
        try {
            restClient.post("/sharing/file/" + request.getFileId(), requestDto, headers, Object.class);
            return true;
        } catch (Exception ex) {
            System.err.println("Error sharing file: " + ex.getMessage());
            return false;
        }
    }
    
    public boolean shareDirectory(ShareDirectoryRequest request) {
        List<DirectoryUserPermissionDto> users = request.getUsers().stream()
            .map(user -> new DirectoryUserPermissionDto(
                user.getUserId(),
                user.getPermissionLevel().value(),
                convertToDate(user.getExpiryDate()),
                user.isIncludeSubdirectories()))
            .collect(Collectors.toList());
            
        ShareDirectoryRequestDto requestDto = new ShareDirectoryRequestDto(users);
        
        // Add headers to the request to include the sharedBy user ID
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-id", String.valueOf(request.getSharedByUserId()));
        
        try {
            restClient.post("/sharing/directory/" + request.getDirectoryId(), requestDto, headers, Object.class);
            return true;
        } catch (Exception ex) {
            System.err.println("Error sharing directory: " + ex.getMessage());
            return false;
        }
    }
    
    // Helper method to convert XMLGregorianCalendar to java.util.Date
    private Date convertToDate(javax.xml.datatype.XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().getTime();
    }
}