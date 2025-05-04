package edu.co.upb.blinkdrive.db.dto.file;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileDto(
    Integer file_id,
    String file_name,
    Integer directory_id,
    Long file_size_bytes,
    String file_type,
    Integer owner_user_id,
    String checksum,
    Date creation_date,
    Date last_modified_date
) {}