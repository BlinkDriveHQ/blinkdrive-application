package edu.co.upb.blinkdrive.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.interceptor.PayloadLoggingInterceptor;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadRootSmartSoapEndpointInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    @Autowired
    private JwtValidationInterceptor jwtValidationInterceptor;
    
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }
    
    // Authentication WSDL definition
    @Bean(name = "authentication")
    public DefaultWsdl11Definition authenticationWsdl(XsdSchema authenticationSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("AuthenticationPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://upb.edu.co/api/auth");
        wsdl11Definition.setSchema(authenticationSchema);
        return wsdl11Definition;
    }
    
    // Database WSDL definition
    @Bean(name = "database")
    public DefaultWsdl11Definition databaseWsdl(XsdSchema databaseSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("DatabasePort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://upb.edu.co/api/db");
        wsdl11Definition.setSchema(databaseSchema);
        return wsdl11Definition;
    }
    
    // Storage WSDL definition
    @Bean(name = "storage")
    public DefaultWsdl11Definition storageWsdl(XsdSchema storageSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("StoragePort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://upb.edu.co/api/storage");
        wsdl11Definition.setSchema(storageSchema);
        return wsdl11Definition;
    }
    
    // BlinkDrive WSDL definition
    @Bean(name = "blinkdrive")
    public DefaultWsdl11Definition blinkdriveWsdl(XsdSchema blinkdriveSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("BlinkDrivePort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://upb.edu.co/api/blinkdrive");
        wsdl11Definition.setSchema(blinkdriveSchema);
        return wsdl11Definition;
    }
    
    // XML Schemas
    @Bean
    public XsdSchema authenticationSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/authentication.xsd"));
    }
    
    @Bean
    public XsdSchema databaseSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/database.xsd"));
    }
    
    @Bean
    public XsdSchema storageSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/storage.xsd"));
    }
    
    @Bean
    public XsdSchema blinkdriveSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/blinkdrive.xsd"));
    }
    
    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        // Add logging interceptor for all endpoints
        interceptors.add(new PayloadLoggingInterceptor());
        
        // Add JWT validation for database, storage, and blinkdrive endpoints
        
        // Database namespace
        interceptors.add(new PayloadRootSmartSoapEndpointInterceptor(
                jwtValidationInterceptor, 
                "http://upb.edu.co/api/db", 
                null));  // null means any local part in this namespace
        
        // Storage namespace
        interceptors.add(new PayloadRootSmartSoapEndpointInterceptor(
                jwtValidationInterceptor, 
                "http://upb.edu.co/api/storage", 
                null));  // null means any local part in this namespace
        
        // BlinkDrive namespace - add JWT validation for file operations
        interceptors.add(new PayloadRootSmartSoapEndpointInterceptor(
                jwtValidationInterceptor, 
                "http://upb.edu.co/api/blinkdrive", 
                null));  // null means any local part in this namespace
        
        // Notice we don't apply the interceptor to auth namespace operations
    }
}