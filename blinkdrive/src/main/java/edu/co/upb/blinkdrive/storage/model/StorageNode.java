package edu.co.upb.blinkdrive.storage.model;

public class StorageNode {
    private int nodeId;
    private String nodeName;
    private String ipAddress;
    private int port;
    private String status;
    private long availableCapacityBytes;

    public StorageNode() {
    }

    public StorageNode(int nodeId, String nodeName, String ipAddress, int port, String status, long availableCapacityBytes) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = status;
        this.availableCapacityBytes = availableCapacityBytes;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAvailableCapacityBytes() {
        return availableCapacityBytes;
    }

    public void setAvailableCapacityBytes(long availableCapacityBytes) {
        this.availableCapacityBytes = availableCapacityBytes;
    }
}