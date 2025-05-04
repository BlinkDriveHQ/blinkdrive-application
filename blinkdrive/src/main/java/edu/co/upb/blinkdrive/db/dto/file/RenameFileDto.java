package edu.co.upb.blinkdrive.db.dto.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RenameFileDto(String file_name) {}