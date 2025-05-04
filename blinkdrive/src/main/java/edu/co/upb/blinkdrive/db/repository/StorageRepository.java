package edu.co.upb.blinkdrive.db.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Repository;

import co.edu.upb.api.db.DirectoryReportType;
import co.edu.upb.api.db.FileReportType;
import co.edu.upb.api.db.StorageNodeType;
import edu.co.upb.blinkdrive.db.client.DatabaseRestClient;
import edu.co.upb.blinkdrive.db.dto.storage.DirectoryReportDto;
import edu.co.upb.blinkdrive.db.dto.storage.StorageNodeDto;
import edu.co.upb.blinkdrive.db.dto.storage.StorageReportDto;

@Repository
public class StorageRepository {
    
    private final DatabaseRestClient restClient;
    
    public StorageRepository(DatabaseRestClient restClient) {
        this.restClient = restClient;
    }
    
    public List<StorageNodeType> getStorageNodes() {
        try {
            // Use array class directly for the response type
            StorageNodeDto[] nodesArray = restClient.get("/storage/nodes", StorageNodeDto[].class);
            
            // Convert array to list and map to StorageNodeType objects
            return Arrays.stream(nodesArray)
                .map(this::mapToStorageNodeType)
                .collect(Collectors.toList());
        } catch (Exception ex) {
            System.err.println("Error getting storage nodes: " + ex.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }
    
    public StorageReportDto getStorageReport() {
        return restClient.get("/storage/report", StorageReportDto.class);
    }
    
    public DirectoryReportType getDirectoryStorageReport(int directoryId) {
        DirectoryReportDto response = restClient.get("/storage/report/directory/" + directoryId, DirectoryReportDto.class);
        return mapToDirectoryReportType(response);
    }
    
    private StorageNodeType mapToStorageNodeType(StorageNodeDto dto) {
        StorageNodeType nodeType = new StorageNodeType();
        nodeType.setNodeId(dto.node_id());
        nodeType.setNodeName(dto.node_name());
        nodeType.setIpAddress(dto.ip_address());
        nodeType.setPort(dto.port());
        nodeType.setTotalCapacityBytes(dto.total_capacity_bytes());
        nodeType.setAvailableCapacityBytes(dto.available_capacity_bytes());
        nodeType.setStatus(dto.status());
        
        if (dto.last_heartbeat() != null) {
            nodeType.setLastHeartbeat(convertToXMLGregorianCalendar(dto.last_heartbeat()));
        }
        
        return nodeType;
    }
    
    private DirectoryReportType mapToDirectoryReportType(DirectoryReportDto dto) {
        DirectoryReportType reportType = new DirectoryReportType();
        reportType.setDirectoryId(dto.directory_id());
        reportType.setDirectoryName(dto.directory_name());
        reportType.setSizeBytes(dto.size_bytes());
        reportType.setFileCount(dto.file_count());
        reportType.setSubdirectoryCount(dto.subdirectory_count());
        
        if (dto.subdirectories() != null) {
            dto.subdirectories().forEach(subdir -> 
                reportType.getSubdirectories().add(mapToDirectoryReportType(subdir)));
        }
        
        if (dto.files() != null) {
            dto.files().forEach(file -> {
                FileReportType fileReport = new FileReportType();
                fileReport.setFileId(file.file_id());
                fileReport.setFileName(file.file_name());
                fileReport.setFileSize(file.file_size_bytes());
                fileReport.setFileType(file.file_type());
                reportType.getFiles().add(fileReport);
            });
        }
        
        return reportType;
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