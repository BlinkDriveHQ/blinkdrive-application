package edu.co.upb.blinkdrive.auth.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthClient {

    private final AuthService authService;

    public AuthClient(
            @Value("${auth.rmi.host:localhost}") String host,
            @Value("${auth.rmi.port:1099}") int port,
            @Value("${auth.rmi.service-name:AuthService}") String serviceName) throws RemoteException, NotBoundException {
        
        Registry registry = LocateRegistry.getRegistry(host, port);
        authService = (AuthService) registry.lookup(serviceName);
    }

    public String authenticate(String username, String password) throws RemoteException {
        return authService.authenticate(username, password);
    }

    public boolean validateToken(String token) throws RemoteException {
        return authService.validateToken(token);
    }

    public boolean registerUser(String username, String password) throws RemoteException {
        return authService.registerUser(username, password);
    }

    public boolean revokeToken(String token) throws RemoteException {
        return authService.revokeToken(token);
    }
}