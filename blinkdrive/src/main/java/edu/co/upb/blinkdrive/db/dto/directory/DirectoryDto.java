package edu.co.upb.blinkdrive.db.dto.directory;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryDto(
    Integer directory_id,
    String directory_name,
    Integer parent_directory_id,
    Integer owner_user_id,
    Date creation_date,
    Date last_modified_date,
    Long size_bytes
) {}