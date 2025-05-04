package edu.co.upb.blinkdrive.db.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import co.edu.upb.api.db.AddFileStorageLocationRequest;
import co.edu.upb.api.db.AddFileStorageLocationResponse;
import co.edu.upb.api.db.CreateDirectoryRequest;
import co.edu.upb.api.db.CreateDirectoryResponse;
import co.edu.upb.api.db.CreateFileMetadataRequest;
import co.edu.upb.api.db.CreateFileMetadataResponse;
import co.edu.upb.api.db.DeleteDirectoryRequest;
import co.edu.upb.api.db.DeleteDirectoryResponse;
import co.edu.upb.api.db.DeleteFileRequest;
import co.edu.upb.api.db.DeleteFileResponse;
import co.edu.upb.api.db.GetDirectoriesByParentRequest;
import co.edu.upb.api.db.GetDirectoriesByParentResponse;
import co.edu.upb.api.db.GetDirectoryRequest;
import co.edu.upb.api.db.GetDirectoryResponse;
import co.edu.upb.api.db.GetFileMetadataRequest;
import co.edu.upb.api.db.GetFileMetadataResponse;
import co.edu.upb.api.db.GetFilesByDirectoryRequest;
import co.edu.upb.api.db.GetFilesByDirectoryResponse;
import co.edu.upb.api.db.GetStorageNodesRequest;
import co.edu.upb.api.db.GetStorageNodesResponse;
import co.edu.upb.api.db.GetStorageReportRequest;
import co.edu.upb.api.db.GetStorageReportResponse;
import co.edu.upb.api.db.MoveDirectoryRequest;
import co.edu.upb.api.db.MoveDirectoryResponse;
import co.edu.upb.api.db.MoveFileRequest;
import co.edu.upb.api.db.MoveFileResponse;
import co.edu.upb.api.db.RenameDirectoryRequest;
import co.edu.upb.api.db.RenameDirectoryResponse;
import co.edu.upb.api.db.RenameFileRequest;
import co.edu.upb.api.db.RenameFileResponse;
import co.edu.upb.api.db.ShareDirectoryRequest;
import co.edu.upb.api.db.ShareDirectoryResponse;
import co.edu.upb.api.db.ShareFileRequest;
import co.edu.upb.api.db.ShareFileResponse;
import edu.co.upb.blinkdrive.db.service.DatabaseService;

@Endpoint
public class DatabaseEndpoint {
    
    private static final String NAMESPACE_URI = "http://upb.edu.co/api/db";
    
    private final DatabaseService databaseService;
    
    public DatabaseEndpoint(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    // Directory operations
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateDirectoryRequest")
    @ResponsePayload
    public CreateDirectoryResponse createDirectory(@RequestPayload CreateDirectoryRequest request) {
        return databaseService.createDirectory(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetDirectoryRequest")
    @ResponsePayload
    public GetDirectoryResponse getDirectory(@RequestPayload GetDirectoryRequest request) {
        return databaseService.getDirectory(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetDirectoriesByParentRequest")
    @ResponsePayload
    public GetDirectoriesByParentResponse getDirectoriesByParent(@RequestPayload GetDirectoriesByParentRequest request) {
        return databaseService.getDirectoriesByParent(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "RenameDirectoryRequest")
    @ResponsePayload
    public RenameDirectoryResponse renameDirectory(@RequestPayload RenameDirectoryRequest request) {
        return databaseService.renameDirectory(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "MoveDirectoryRequest")
    @ResponsePayload
    public MoveDirectoryResponse moveDirectory(@RequestPayload MoveDirectoryRequest request) {
        return databaseService.moveDirectory(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DeleteDirectoryRequest")
    @ResponsePayload
    public DeleteDirectoryResponse deleteDirectory(@RequestPayload DeleteDirectoryRequest request) {
        return databaseService.deleteDirectory(request);
    }
    
    // File operations
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateFileMetadataRequest")
    @ResponsePayload
    public CreateFileMetadataResponse createFileMetadata(@RequestPayload CreateFileMetadataRequest request) {
        return databaseService.createFileMetadata(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetFileMetadataRequest")
    @ResponsePayload
    public GetFileMetadataResponse getFileMetadata(@RequestPayload GetFileMetadataRequest request) {
        return databaseService.getFileMetadata(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetFilesByDirectoryRequest")
    @ResponsePayload
    public GetFilesByDirectoryResponse getFilesByDirectory(@RequestPayload GetFilesByDirectoryRequest request) {
        return databaseService.getFilesByDirectory(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "RenameFileRequest")
    @ResponsePayload
    public RenameFileResponse renameFile(@RequestPayload RenameFileRequest request) {
        return databaseService.renameFile(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "MoveFileRequest")
    @ResponsePayload
    public MoveFileResponse moveFile(@RequestPayload MoveFileRequest request) {
        return databaseService.moveFile(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DeleteFileRequest")
    @ResponsePayload
    public DeleteFileResponse deleteFile(@RequestPayload DeleteFileRequest request) {
        return databaseService.deleteFile(request);
    }
    
    // Storage location operations
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "AddFileStorageLocationRequest")
    @ResponsePayload
    public AddFileStorageLocationResponse addFileStorageLocation(@RequestPayload AddFileStorageLocationRequest request) {
        return databaseService.addFileStorageLocation(request);
    }
    
    // Sharing operations
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ShareFileRequest")
    @ResponsePayload
    public ShareFileResponse shareFile(@RequestPayload ShareFileRequest request) {
        return databaseService.shareFile(request);
    }
    
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ShareDirectoryRequest")
    @ResponsePayload
    public ShareDirectoryResponse shareDirectory(@RequestPayload ShareDirectoryRequest request) {
        return databaseService.shareDirectory(request);
    }
    
    // Storage node operations
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetStorageNodesRequest")
    @ResponsePayload
    public GetStorageNodesResponse getStorageNodes(@RequestPayload GetStorageNodesRequest request) {
        return databaseService.getStorageNodes(request);
    }
    
    // Storage report
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetStorageReportRequest")
    @ResponsePayload
    public GetStorageReportResponse getStorageReport(@RequestPayload GetStorageReportRequest request) {
        return databaseService.getStorageReport(request);
    }
}