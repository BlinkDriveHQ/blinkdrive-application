package edu.co.upb.blinkdrive.db.dto.directory;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryListDto(List<DirectoryDto> directories) {}