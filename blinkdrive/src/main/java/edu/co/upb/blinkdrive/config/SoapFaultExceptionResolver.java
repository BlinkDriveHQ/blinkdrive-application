package edu.co.upb.blinkdrive.config;

import java.util.Properties;

import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapFaultDetail;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;

@Component
public class SoapFaultExceptionResolver extends SoapFaultMappingExceptionResolver {

    private static final QName ERROR_CODE = new QName("errorCode");
    private static final QName ERROR_MESSAGE = new QName("errorMessage");

    public SoapFaultExceptionResolver() {
        Properties errorMappings = new Properties();
        errorMappings.setProperty(JwtValidationInterceptor.AuthenticationException.class.getName(), "AUTH_FAILURE");
        errorMappings.setProperty("edu.co.upb.blinkdrive.storage.exception.StorageException", "STORAGE_UNAVAILABLE");
        errorMappings.setProperty("edu.co.upb.blinkdrive.db.client.DatabaseServiceException", "DATABASE_ERROR");
        setExceptionMappings(errorMappings);
        
        // Default fault is SERVER error
        SoapFaultDefinition defaultFault = new SoapFaultDefinition();
        defaultFault.setFaultCode(SoapFaultDefinition.SERVER);
        setDefaultFault(defaultFault);
    }

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        SoapFaultDetail detail = fault.addFaultDetail();
        
        // Add error message
        detail.addFaultDetailElement(ERROR_MESSAGE).addText(ex.getMessage());
        
        // Add error code based on exception type
        String errorCode = "SYSTEM_ERROR";
        
        if (ex instanceof JwtValidationInterceptor.AuthenticationException) {
            errorCode = "AUTH_FAILURE";
        } else if (ex.getClass().getName().contains("StorageException")) {
            errorCode = "STORAGE_UNAVAILABLE";
        } else if (ex.getClass().getName().contains("DatabaseServiceException")) {
            errorCode = "DATABASE_ERROR";
        }
        
        detail.addFaultDetailElement(ERROR_CODE).addText(errorCode);
    }
}