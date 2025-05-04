package edu.co.upb.blinkdrive.config;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;

import edu.co.upb.blinkdrive.auth.service.AuthenticationService;

@Component
public class JwtValidationInterceptor implements EndpointInterceptor {

    private static final String AUTH_NAMESPACE = "http://upb.edu.co/api/auth";
    private static final QName AUTH_HEADER = new QName(AUTH_NAMESPACE, "AuthHeader");
    private static final QName TOKEN_ELEMENT = new QName(AUTH_NAMESPACE, "Token");
    
    // Skip authentication for these operations
    private static final String[] SKIP_VALIDATION_OPERATIONS = {
        "AuthenticateRequest",
        "ValidateTokenRequest",
        "RegisterUserRequest"
    };
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        SoapMessage soapMessage = (SoapMessage) messageContext.getRequest();
        
        // Check if this operation should skip validation
        String soapAction = getSoapAction(messageContext);
        for (String skipOp : SKIP_VALIDATION_OPERATIONS) {
            if (soapAction != null && soapAction.contains(skipOp)) {
                return true; // Skip validation for this operation
            }
        }
        
        // Extract token from header
        Iterator<SoapHeaderElement> authHeaders = soapMessage.getEnvelope().getHeader().examineHeaderElements(AUTH_HEADER);
        
        if (!authHeaders.hasNext()) {
            throw new AuthenticationException("Missing authentication header");
        }
        
        SoapHeaderElement authHeader = authHeaders.next();
        String token = getTokenFromHeader(authHeader);
        
        if (token == null || token.isEmpty()) {
            throw new AuthenticationException("Missing authentication token");
        }
        
        // Validate token
        boolean isValid = authenticationService.validateToken(token);
        if (!isValid) {
            throw new AuthenticationException("Invalid or expired token");
        }
        
        return true;
    }
    
    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        return true; // Continue processing
    }
    
    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        return true; // Continue processing
    }
    
    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
        // Cleanup if needed
    }
    
    private String getTokenFromHeader(SoapHeaderElement authHeader) {
        Iterator<QName> it = authHeader.getAllAttributes();
        while (it.hasNext()) {
            QName qName = it.next();
            if (TOKEN_ELEMENT.equals(qName)) {
                return authHeader.getAttributeValue(qName);
            }
        }
        
        // If not found in attributes, try to get the text content directly
        // Since getChildElements is not available, try to get the text content directly
        String content = authHeader.getText();
        if (content != null && !content.trim().isEmpty()) {
            return content.trim();
        }
        
        return null;
    }
    
    private String getSoapAction(MessageContext messageContext) {
        Object soapAction = messageContext.getProperty("SOAPAction");
        return soapAction != null ? soapAction.toString() : null;
    }
    
    // Custom authentication exception
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}