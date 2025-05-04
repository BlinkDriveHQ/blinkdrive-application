package edu.co.upb.blinkdrive.auth.service;

import org.springframework.stereotype.Service;

import edu.co.upb.blinkdrive.auth.repository.AuthRepository;

@Service
public class AuthenticationService {

    private final AuthRepository authRepository;

    public AuthenticationService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public String authenticate(String username, String password) {
        return authRepository.authenticate(username, password);
    }

    public boolean validateToken(String token) {
        return authRepository.validateToken(token);
    }

    public boolean registerUser(String username, String password) {
        return authRepository.registerUser(username, password);
    }

    public boolean revokeToken(String token) {
        return authRepository.revokeToken(token);
    }
}