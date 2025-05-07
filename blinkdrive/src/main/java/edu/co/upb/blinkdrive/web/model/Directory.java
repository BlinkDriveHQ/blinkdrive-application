package edu.co.upb.blinkdrive.web.model;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

public class Directory {
    private int id;
    private String name;
    private Integer parentId;
    private int ownerId;
    private Date creationDate;
    private Date lastModifiedDate;
    private Long sizeBytes;
    
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
    
    public Integer getParentId() {
        return parentId;
    }
    
    public void setParentId(Integer parentId) {
        this.parentId = parentId;
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
    
    public Long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    // Utility method to format size for display
    public String getFormattedSize() {
        if (sizeBytes == null || sizeBytes == 0) {
            return "0 B";
        }
        
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(sizeBytes) / Math.log10(1024));
        
        return String.format("%.1f %s", sizeBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}