package edu.co.upb.blinkdrive.db.dto.directory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RenameDirectoryDto(String directory_name) {}