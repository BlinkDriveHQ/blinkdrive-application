package edu.co.upb.blinkdrive.db.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Repository
public class DatabaseRestClient {
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public DatabaseRestClient(@Value("${database.server.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }
    
    public <T> T get(String path, Class<T> responseType) {
        try {
            return restTemplate.getForObject(baseUrl + path, responseType);
        } catch (RestClientException ex) {
            System.err.println("Error calling database server: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to get resource: " + path, ex);
        }
    }
    
    public <T, R> R post(String path, T requestBody, Class<R> responseType) {
        return post(path, requestBody, new HttpHeaders(), responseType);
    }
    
    public <T, R> R post(String path, T requestBody, HttpHeaders headers, Class<R> responseType) {
        try {
            if (headers.getContentType() == null) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            
            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<R> response = restTemplate.exchange(
                baseUrl + path, 
                HttpMethod.POST, 
                entity, 
                responseType
            );
            
            return response.getBody();
        } catch (RestClientException ex) {
            System.err.println("Error calling database server: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to create resource: " + path, ex);
        }
    }
    
    public <T, R> R put(String path, T requestBody, Class<R> responseType) {
        return put(path, requestBody, new HttpHeaders(), responseType);
    }
    
    public <T, R> R put(String path, T requestBody, HttpHeaders headers, Class<R> responseType) {
        try {
            if (headers.getContentType() == null) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            
            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<R> response = restTemplate.exchange(
                baseUrl + path, 
                HttpMethod.PUT, 
                entity, 
                responseType
            );
            
            return response.getBody();
        } catch (RestClientException ex) {
            System.err.println("Error calling database server: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to update resource: " + path, ex);
        }
    }
    
    public void delete(String path) {
        delete(path, new HttpHeaders());
    }
    
    public void delete(String path, HttpHeaders headers) {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(headers);
            restTemplate.exchange(
                baseUrl + path, 
                HttpMethod.DELETE, 
                entity, 
                Void.class
            );
        } catch (RestClientException ex) {
            System.err.println("Error calling database server: " + ex.getMessage());
            throw new DatabaseServiceException("Failed to delete resource: " + path, ex);
        }
    }
}