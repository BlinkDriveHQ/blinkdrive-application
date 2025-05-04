package edu.co.upb.blinkdrive.db.dto.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageReportDto(
    Long total_storage_bytes,
    Long used_storage_bytes,
    Double usage_percentage,
    DirectoryReportDto directoryReport
) {}