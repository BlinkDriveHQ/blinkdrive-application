package edu.co.upb.blinkdrive.auth.repository;

public interface AuthRepository {
    String authenticate(String username, String password);
    boolean validateToken(String token);
    boolean registerUser(String username, String password);
    boolean revokeToken(String token);
}