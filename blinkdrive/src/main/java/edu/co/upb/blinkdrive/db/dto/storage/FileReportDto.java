package edu.co.upb.blinkdrive.db.dto.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileReportDto(
    Integer file_id,
    String file_name,
    Long file_size_bytes,
    String file_type
) {}