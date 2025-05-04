package edu.co.upb.blinkdrive.db.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
import co.edu.upb.api.db.DirectoryReportType;
import co.edu.upb.api.db.DirectoryType;
import co.edu.upb.api.db.FileStorageLocationType;
import co.edu.upb.api.db.FileType;
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
import co.edu.upb.api.db.StorageNodeType;
import edu.co.upb.blinkdrive.db.dto.storage.StorageReportDto;
import edu.co.upb.blinkdrive.db.repository.DirectoryRepository;
import edu.co.upb.blinkdrive.db.repository.FileRepository;
import edu.co.upb.blinkdrive.db.repository.SharingRepository;
import edu.co.upb.blinkdrive.db.repository.StorageRepository;

@Service
public class DatabaseService {
    
    private final DirectoryRepository directoryRepository;
    private final FileRepository fileRepository;
    private final StorageRepository storageRepository;
    private final SharingRepository sharingRepository;
    
    public DatabaseService(
            DirectoryRepository directoryRepository,
            FileRepository fileRepository,
            StorageRepository storageRepository,
            SharingRepository sharingRepository) {
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
        this.storageRepository = storageRepository;
        this.sharingRepository = sharingRepository;
    }
    
    // Directory operations
    public CreateDirectoryResponse createDirectory(CreateDirectoryRequest request) {
        DirectoryType directory = directoryRepository.createDirectory(request);
        CreateDirectoryResponse response = new CreateDirectoryResponse();
        response.setDirectory(directory);
        return response;
    }
    
    public GetDirectoryResponse getDirectory(GetDirectoryRequest request) {
        DirectoryType directory = directoryRepository.getDirectory(request.getDirectoryId());
        List<FileType> files = fileRepository.getFilesByDirectory(request.getDirectoryId());
        List<DirectoryType> subdirectories = directoryRepository.getDirectoriesByParent(request.getDirectoryId());
        
        GetDirectoryResponse response = new GetDirectoryResponse();
        response.setDirectory(directory);
        response.getFiles().addAll(files);
        response.getSubdirectories().addAll(subdirectories);
        return response;
    }
    
    public GetDirectoriesByParentResponse getDirectoriesByParent(GetDirectoriesByParentRequest request) {
        List<DirectoryType> directories = directoryRepository.getDirectoriesByParent(request.getParentDirectoryId());
        GetDirectoriesByParentResponse response = new GetDirectoriesByParentResponse();
        response.getDirectories().addAll(directories);
        return response;
    }
    
    public RenameDirectoryResponse renameDirectory(RenameDirectoryRequest request) {
        DirectoryType directory = directoryRepository.renameDirectory(request);
        RenameDirectoryResponse response = new RenameDirectoryResponse();
        response.setDirectory(directory);
        return response;
    }
    
    public MoveDirectoryResponse moveDirectory(MoveDirectoryRequest request) {
        DirectoryType directory = directoryRepository.moveDirectory(request);
        MoveDirectoryResponse response = new MoveDirectoryResponse();
        response.setDirectory(directory);
        return response;
    }
    
    public DeleteDirectoryResponse deleteDirectory(DeleteDirectoryRequest request) {
        boolean success = directoryRepository.deleteDirectory(request.getDirectoryId(), request.getUserId());
        DeleteDirectoryResponse response = new DeleteDirectoryResponse();
        response.setSuccess(success);
        return response;
    }
    
    // File operations
    public CreateFileMetadataResponse createFileMetadata(CreateFileMetadataRequest request) {
        FileType file = fileRepository.createFileMetadata(request);
        CreateFileMetadataResponse response = new CreateFileMetadataResponse();
        response.setFile(file);
        return response;
    }
    
    public GetFileMetadataResponse getFileMetadata(GetFileMetadataRequest request) {
        FileType file = fileRepository.getFileMetadata(request.getFileId());
        List<FileStorageLocationType> locations = fileRepository.getFileStorageLocations(request.getFileId());
        
        GetFileMetadataResponse response = new GetFileMetadataResponse();
        response.setFile(file);
        response.getStorageLocations().addAll(locations);
        return response;
    }
    
    public GetFilesByDirectoryResponse getFilesByDirectory(GetFilesByDirectoryRequest request) {
        List<FileType> files = fileRepository.getFilesByDirectory(request.getDirectoryId());
        GetFilesByDirectoryResponse response = new GetFilesByDirectoryResponse();
        response.getFiles().addAll(files);
        return response;
    }
    
    public RenameFileResponse renameFile(RenameFileRequest request) {
        FileType file = fileRepository.renameFile(request);
        RenameFileResponse response = new RenameFileResponse();
        response.setFile(file);
        return response;
    }
    
    public MoveFileResponse moveFile(MoveFileRequest request) {
        FileType file = fileRepository.moveFile(request);
        MoveFileResponse response = new MoveFileResponse();
        response.setFile(file);
        return response;
    }
    
    public DeleteFileResponse deleteFile(DeleteFileRequest request) {
        boolean success = fileRepository.deleteFile(request.getFileId(), request.getUserId());
        DeleteFileResponse response = new DeleteFileResponse();
        response.setSuccess(success);
        return response;
    }
    
    // Storage location operations
    public AddFileStorageLocationResponse addFileStorageLocation(AddFileStorageLocationRequest request) {
        FileStorageLocationType location = fileRepository.addFileStorageLocation(
                request.getFileId(),
                request.getNodeId(),
                request.getFilePath(),
                request.isIsPrimary());
                
        AddFileStorageLocationResponse response = new AddFileStorageLocationResponse();
        response.setStorageLocation(location);
        return response;
    }
    
    // Storage node operations
    public GetStorageNodesResponse getStorageNodes(GetStorageNodesRequest request) {
        List<StorageNodeType> nodes = storageRepository.getStorageNodes();
        GetStorageNodesResponse response = new GetStorageNodesResponse();
        response.getNodes().addAll(nodes);
        return response;
    }
    
    // Storage report
    public GetStorageReportResponse getStorageReport(GetStorageReportRequest request) {
        GetStorageReportResponse response = new GetStorageReportResponse();
        
        if (request.getDirectoryId() != null) {
            DirectoryReportType directoryReport = storageRepository.getDirectoryStorageReport(request.getDirectoryId());
            StorageReportDto overallReport = storageRepository.getStorageReport();
            
            response.setTotalStorageBytes(overallReport.total_storage_bytes());
            response.setUsedStorageBytes(overallReport.used_storage_bytes());
            response.setUsagePercentage(overallReport.usage_percentage());
            response.setDirectoryReport(directoryReport);
        } else {
            StorageReportDto report = storageRepository.getStorageReport();
            response.setTotalStorageBytes(report.total_storage_bytes());
            response.setUsedStorageBytes(report.used_storage_bytes());
            response.setUsagePercentage(report.usage_percentage());
        }
        
        return response;
    }
    
    // Sharing operations
    public ShareFileResponse shareFile(ShareFileRequest request) {
        boolean success = sharingRepository.shareFile(request);
        ShareFileResponse response = new ShareFileResponse();
        response.setSuccess(success);
        return response;
    }
    
    public ShareDirectoryResponse shareDirectory(ShareDirectoryRequest request) {
        boolean success = sharingRepository.shareDirectory(request);
        ShareDirectoryResponse response = new ShareDirectoryResponse();
        response.setSuccess(success);
        return response;
    }
}