package edu.co.upb.blinkdrive.config;

import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapEnvelopeException;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapHeaderException;
import org.springframework.ws.soap.SoapMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.co.upb.blinkdrive.auth.service.AuthenticationService;

@Component
public class JwtValidationInterceptor implements EndpointInterceptor {

    private static final String AUTH_NAMESPACE = "http://upb.edu.co/api/auth";
    private static final QName AUTH_HEADER = new QName(AUTH_NAMESPACE, "AuthHeader");
    
    private static final String[] SKIP_VALIDATION_OPERATIONS = {
        "AuthenticateRequest",
        "ValidateTokenRequest",
        "RegisterUserRequest",
        "RevokeTokenRequest"
    };
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        try {
            SoapMessage soapMessage = (SoapMessage) messageContext.getRequest();
            
            String soapAction = getSoapAction(messageContext);
            String payloadString = getPayloadAsString(soapMessage);
            
            for (String skipOp : SKIP_VALIDATION_OPERATIONS) {
                if ((soapAction != null && soapAction.contains(skipOp)) || 
                    (payloadString != null && payloadString.contains(skipOp))) {
                    return true; // Skip validation for this operation
                }
            }
            
            if (soapMessage.getEnvelope().getHeader() == null) {
                throw new AuthenticationException("Missing SOAP header");
            }
            
            Iterator<SoapHeaderElement> authHeaders = soapMessage.getEnvelope().getHeader()
                                             .examineHeaderElements(AUTH_HEADER);
            
            if (!authHeaders.hasNext()) {
                throw new AuthenticationException("Missing authentication header");
            }
            
            SoapHeaderElement authHeader = authHeaders.next();
            
            String token = null;
            
            // Method 1: Direct getText() with cleaning
            String rawText = authHeader.getText();
            if (rawText != null && !rawText.isEmpty()) {
                token = rawText.replaceAll("\\s+", "");
            }
            
            // Method 2: DOM transformation
            if (token == null || token.isEmpty()) {
                token = extractTokenFromDOM(authHeader);
                if (token != null) {
                    token = token.replaceAll("\\s+", "");
                }
            }
            
            // Method 3: String parsing
            if (token == null || token.isEmpty()) {
                token = extractTokenFromString(authHeader);
                if (token != null) {
                    token = token.replaceAll("\\s+", "");
                }
            }
            
            if (token == null || token.isEmpty()) {
                throw new AuthenticationException("Missing authentication token");
            }
            
            boolean isValid = authenticationService.validateToken(token);
            
            if (!isValid) {
                throw new AuthenticationException("Invalid or expired token");
            }
            
            return true;
        } catch (AuthenticationException e) {
            throw e;
        } catch (SoapEnvelopeException | SoapHeaderException e) {
            throw new AuthenticationException("Authentication processing error: " + e.getMessage());
        }
    }
    
    private String getPayloadAsString(SoapMessage soapMessage) {
        try {
            Source source = soapMessage.getEnvelope().getSource();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException | SoapEnvelopeException e) {
            return null;
        }
    }
    
    private String extractTokenFromDOM(SoapHeaderElement authHeader) {
        try {
            Source source = authHeader.getSource();
            DOMResult domResult = new DOMResult();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, domResult);
            
            Node node = domResult.getNode();
            if (node instanceof Document document) {
                Element root = document.getDocumentElement();
                
                NodeList tokenNodes = root.getElementsByTagName("Token");
                if (tokenNodes.getLength() == 0) {
                    tokenNodes = root.getElementsByTagNameNS(AUTH_NAMESPACE, "Token");
                }
                
                if (tokenNodes.getLength() > 0) {
                    Node tokenNode = tokenNodes.item(0);
                    if (tokenNode.getTextContent() != null) {
                        return tokenNode.getTextContent();
                    }
                }
                
                for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                        if ("Token".equals(nodeName)) {
                            return child.getTextContent();
                        }
                    }
                }
            }
        } catch (TransformerException | DOMException e) {
            // Silently fail and try next method
        }
        return null;
    }
    
    private String extractTokenFromString(SoapHeaderElement authHeader) {
        try {
            Source source = authHeader.getSource();
            StringWriter writer = new StringWriter();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, new StreamResult(writer));
            String xml = writer.toString();
            
            String nsTokenStart = "<auth:Token>";
            String nsTokenEnd = "</auth:Token>";
            
            int start = xml.indexOf(nsTokenStart);
            int end = xml.indexOf(nsTokenEnd);
            
            if (start >= 0 && end > start) {
                return xml.substring(start + nsTokenStart.length(), end);
            }
            
            String tokenStart = "<Token>";
            String tokenEnd = "</Token>";
            
            start = xml.indexOf(tokenStart);
            end = xml.indexOf(tokenEnd);
            
            if (start >= 0 && end > start) {
                return xml.substring(start + tokenStart.length(), end);
            }
            
            String[] parts = xml.replaceAll(">\\s+<", "><").split("[<>]");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("Token") || parts[i].equals("auth:Token")) {
                    if (i+1 < parts.length) {
                        String token = parts[i+1];
                        if (!token.isEmpty() && !token.startsWith("<") && !token.startsWith("/")) {
                            return token;
                        }
                    }
                }
            }
        } catch (TransformerException e) {
            // Silently fail and try next method
        }
        return null;
    }
    
    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        return true;
    }
    
    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        return true;
    }
    
    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
        // No implementation needed
    }
    
    private String getSoapAction(MessageContext messageContext) {
        Object soapAction = messageContext.getProperty("SOAPAction");
        return soapAction != null ? soapAction.toString() : null;
    }
    
    public static class AuthenticationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public AuthenticationException(String message) {
            super(message);
        }
    }
}