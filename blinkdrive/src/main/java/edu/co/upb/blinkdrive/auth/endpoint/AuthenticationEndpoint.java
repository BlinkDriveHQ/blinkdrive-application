package edu.co.upb.blinkdrive.auth.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import co.edu.upb.api.auth.AuthenticateRequest;
import co.edu.upb.api.auth.AuthenticateResponse;
import co.edu.upb.api.auth.RegisterUserRequest;
import co.edu.upb.api.auth.RegisterUserResponse;
import co.edu.upb.api.auth.RevokeTokenRequest;
import co.edu.upb.api.auth.RevokeTokenResponse;
import co.edu.upb.api.auth.ValidateTokenRequest;
import co.edu.upb.api.auth.ValidateTokenResponse;
import edu.co.upb.blinkdrive.auth.service.AuthenticationService;

@Endpoint
public class AuthenticationEndpoint {

    private static final String NAMESPACE_URI = "http://upb.edu.co/api/auth";

    private final AuthenticationService authenticationService;

    public AuthenticationEndpoint(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "AuthenticateRequest")
    @ResponsePayload
    public AuthenticateResponse authenticate(@RequestPayload AuthenticateRequest request) {
        AuthenticateResponse response = new AuthenticateResponse();
        
        try {
            String token = authenticationService.authenticate(request.getUsername(), request.getPassword());
            response.setSuccess(token != null);
            response.setToken(token != null ? token : "");
            
            if (token == null) {
                response.setMessage("Authentication failed");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error during authentication: " + e.getMessage());
        }
        
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ValidateTokenRequest")
    @ResponsePayload
    public ValidateTokenResponse validateToken(@RequestPayload ValidateTokenRequest request) {
        ValidateTokenResponse response = new ValidateTokenResponse();
        
        try {
            boolean isValid = authenticationService.validateToken(request.getToken());
            response.setValid(isValid);
            
            if (!isValid) {
                response.setMessage("Token is invalid or expired");
            }
        } catch (Exception e) {
            response.setValid(false);
            response.setMessage("Error validating token: " + e.getMessage());
        }
        
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "RegisterUserRequest")
    @ResponsePayload
    public RegisterUserResponse registerUser(@RequestPayload RegisterUserRequest request) {
        RegisterUserResponse response = new RegisterUserResponse();
        
        try {
            boolean success = authenticationService.registerUser(request.getUsername(), request.getPassword());
            response.setSuccess(success);
            
            if (!success) {
                response.setMessage("User registration failed. Username may already exist.");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error during user registration: " + e.getMessage());
        }
        
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "RevokeTokenRequest")
    @ResponsePayload
    public RevokeTokenResponse revokeToken(@RequestPayload RevokeTokenRequest request) {
        RevokeTokenResponse response = new RevokeTokenResponse();
        
        try {
            boolean success = authenticationService.revokeToken(request.getToken());
            response.setSuccess(success);
            
            if (!success) {
                response.setMessage("Token revocation failed. Token may not exist or may already be revoked.");
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error revoking token: " + e.getMessage());
        }
        
        return response;
    }
}