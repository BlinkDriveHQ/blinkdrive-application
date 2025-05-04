package edu.co.upb.blinkdrive.db.dto.sharing;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryUserPermissionDto(
    Integer user_id,
    String permission_level,
    Date expiry_date,
    Boolean include_subdirectories
) {}