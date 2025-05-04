package edu.co.upb.blinkdrive.db.dto.storage;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryReportDto(
    Integer directory_id,
    String directory_name,
    Long size_bytes,
    Integer file_count,
    Integer subdirectory_count,
    List<DirectoryReportDto> subdirectories,
    List<FileReportDto> files
) {}