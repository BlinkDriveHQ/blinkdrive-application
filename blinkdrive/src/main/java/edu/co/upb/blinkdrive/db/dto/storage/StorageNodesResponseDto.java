package edu.co.upb.blinkdrive.db.dto.storage;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageNodesResponseDto(List<StorageNodeDto> nodes) {}