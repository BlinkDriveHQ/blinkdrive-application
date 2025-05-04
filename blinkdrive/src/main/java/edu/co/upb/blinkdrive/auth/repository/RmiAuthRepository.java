package edu.co.upb.blinkdrive.auth.repository;

import java.rmi.RemoteException;

import org.springframework.stereotype.Repository;

import edu.co.upb.blinkdrive.auth.client.AuthClient;

@Repository
public class RmiAuthRepository implements AuthRepository {

    private final AuthClient authClient;

    public RmiAuthRepository(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public String authenticate(String username, String password) {
        try {
            return authClient.authenticate(username, password);
        } catch (RemoteException e) {
            throw new AuthRepositoryException("Error authenticating user", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return authClient.validateToken(token);
        } catch (RemoteException e) {
            throw new AuthRepositoryException("Error validating token", e);
        }
    }

    @Override
    public boolean registerUser(String username, String password) {
        try {
            return authClient.registerUser(username, password);
        } catch (RemoteException e) {
            throw new AuthRepositoryException("Error registering user", e);
        }
    }

    @Override
    public boolean revokeToken(String token) {
        try {
            return authClient.revokeToken(token);
        } catch (RemoteException e) {
            throw new AuthRepositoryException("Error revoking token", e);
        }
    }
}