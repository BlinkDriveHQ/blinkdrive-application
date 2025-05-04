package edu.co.upb.blinkdrive.storage.model;

public class FileStorageLocation {
    private String fileId;
    private int nodeId;
    private String filePath;
    private boolean isPrimary;

    public FileStorageLocation() {
    }

    public FileStorageLocation(String fileId, int nodeId, String filePath, boolean isPrimary) {
        this.fileId = fileId;
        this.nodeId = nodeId;
        this.filePath = filePath;
        this.isPrimary = isPrimary;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}