package edu.co.upb.blinkdrive.db.dto.storage;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageNodeDto(
    Integer node_id,
    String node_name,
    String ip_address,
    Integer port,
    Long total_capacity_bytes,
    Long available_capacity_bytes,
    String status,
    Date last_heartbeat
) {}