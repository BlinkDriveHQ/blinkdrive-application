package edu.co.upb.blinkdrive.web.model;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

public class File {
    private int id;
    private String name;
    private int directoryId;
    private long size;
    private String type;
    private int ownerId;
    private Date creationDate;
    private Date lastModifiedDate;
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getDirectoryId() {
        return directoryId;
    }
    
    public void setDirectoryId(int directoryId) {
        this.directoryId = directoryId;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
    
    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(XMLGregorianCalendar calendar) {
        if (calendar != null) {
            this.creationDate = calendar.toGregorianCalendar().getTime();
        }
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(XMLGregorianCalendar calendar) {
        if (calendar != null) {
            this.lastModifiedDate = calendar.toGregorianCalendar().getTime();
        }
    }
    
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    // Utility methods
    public String getFormattedSize() {
        if (size == 0) {
            return "0 B";
        }
        
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    public String getFileExtension() {
        if (name == null || !name.contains(".")) {
            return "";
        }
        
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }
    
    public String getIconClass() {
        String extension = getFileExtension();
        
        return switch (extension) {
            case "pdf" -> "fa-file-pdf";
            case "doc", "docx" -> "fa-file-word";
            case "xls", "xlsx" -> "fa-file-excel";
            case "ppt", "pptx" -> "fa-file-powerpoint";
            case "jpg", "jpeg", "png", "gif" -> "fa-file-image";
            case "txt" -> "fa-file-alt";
            case "zip", "rar", "7z" -> "fa-file-archive";
            case "mp3", "wav" -> "fa-file-audio";
            case "mp4", "avi", "mov" -> "fa-file-video";
            case "html", "css", "js" -> "fa-file-code";
            default -> "fa-file";
        };
    }
}