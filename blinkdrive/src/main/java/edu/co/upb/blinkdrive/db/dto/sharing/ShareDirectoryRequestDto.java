package edu.co.upb.blinkdrive.db.dto.sharing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShareDirectoryRequestDto(List<DirectoryUserPermissionDto> users) {}