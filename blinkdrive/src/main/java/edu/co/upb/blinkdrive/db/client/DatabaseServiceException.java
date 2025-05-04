package edu.co.upb.blinkdrive.db.client;

public class DatabaseServiceException extends RuntimeException {
    public DatabaseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseServiceException(String message) {
        super(message);
    }
}