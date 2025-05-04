package edu.co.upb.blinkdrive.db.dto.file;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileDetailDto(
    Integer file_id,
    String file_name,
    Integer directory_id,
    Long file_size_bytes,
    String file_type,
    Date creation_date,
    Date last_modified_date,
    String checksum,
    Integer owner_user_id,
    List<FileStorageLocationDto> storageLocations
) {
    // This acts as the 'file' property
    public FileDto file() {
        return new FileDto(
            file_id,
            file_name,
            directory_id,
            file_size_bytes,
            file_type,
            owner_user_id,
            checksum,
            creation_date,
            last_modified_date
        );
    }
}

/* 
// Uncommented code for getting file metadata and storage locations
// This code is not used in the current implementation but we are going to implement it 
// once we implement the remaining server that comunicates with the node storage servers.
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileDetailDto(FileDto file, List<FileStorageLocationDto> storageLocations) {}
*/