package edu.co.upb.blinkdrive.db.dto.file;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileStorageLocationDto(
    Integer location_id,
    Integer file_id,
    Integer node_id,
    String file_path,
    Boolean is_primary,
    Date storage_date
) {}