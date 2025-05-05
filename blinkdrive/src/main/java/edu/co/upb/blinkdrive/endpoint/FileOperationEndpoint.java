package edu.co.upb.blinkdrive.endpoint;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;
import org.w3c.dom.DOMException;

import co.edu.upb.api.blinkdrive.DeleteFileOperationRequest;
import co.edu.upb.api.blinkdrive.DeleteFileOperationResponse;
import co.edu.upb.api.blinkdrive.DownloadFileOperationRequest;
import co.edu.upb.api.blinkdrive.DownloadFileOperationResponse;
import co.edu.upb.api.blinkdrive.FileMetadataType;
import co.edu.upb.api.blinkdrive.FileStorageLocationType;
import co.edu.upb.api.blinkdrive.UploadFileOperationRequest;
import co.edu.upb.api.blinkdrive.UploadFileOperationResponse;
import edu.co.upb.blinkdrive.service.FileOperationService;
import edu.co.upb.blinkdrive.service.FileOperationService.FileDownloadResult;
import edu.co.upb.blinkdrive.service.FileOperationService.FileUploadResult;
import edu.co.upb.blinkdrive.storage.exception.StorageException;
import edu.co.upb.blinkdrive.storage.model.FileStorageLocation;

@Endpoint
public class FileOperationEndpoint {

    private static final String NAMESPACE_URI = "http://upb.edu.co/api/blinkdrive";
    private static final String AUTH_NAMESPACE = "http://upb.edu.co/api/auth";
    private static final Logger logger = Logger.getLogger(FileOperationEndpoint.class.getName());

    @Autowired
    private FileOperationService fileOperationService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "UploadFileOperationRequest")
    @ResponsePayload
    public UploadFileOperationResponse uploadFile(
            @RequestPayload UploadFileOperationRequest request,
            @SoapHeader("{" + AUTH_NAMESPACE + "}AuthHeader") SoapHeaderElement authHeader) {

        UploadFileOperationResponse response = new UploadFileOperationResponse();

        try {
            // Extract token from header - improved extraction logic
            String token = extractToken(authHeader);
            if (token == null) {
                throw new SecurityException("Authentication token not found in header");
            }

            logger.log(Level.INFO, "Extracted token: {0}", token.substring(0, Math.min(10, token.length())) + "...");

            // Process upload
            FileUploadResult result = fileOperationService.uploadFile(
                    token,
                    request.getFileName(),
                    request.getFileContent(),
                    request.getDirectoryId(),
                    request.getFileType(),
                    request.getUserId());

            // Build response
            response.setSuccess(true);
            response.setFileId(result.getFileMetadataId());

            // Map metadata
            FileMetadataType metadata = new FileMetadataType();
            metadata.setFileId(result.getMetadata().getFileId());
            metadata.setFileName(result.getMetadata().getFileName());
            metadata.setFileSize(result.getMetadata().getFileSize());
            metadata.setFileType(result.getMetadata().getFileType());
            metadata.setDirectoryId(result.getMetadata().getDirectoryId());
            response.setMetadata(metadata);

            // Map storage locations
            for (FileStorageLocation location : result.getLocations()) {
                FileStorageLocationType locationType = new FileStorageLocationType();
                locationType.setFileId(location.getFileId());
                locationType.setNodeId(location.getNodeId());
                locationType.setFilePath(location.getFilePath());
                locationType.setIsPrimary(location.isPrimary());
                response.getStorageLocations().add(locationType);
            }

        } catch (SecurityException e) {
            response.setSuccess(false);
            response.setMessage("Authentication failure: " + e.getMessage());
        } catch (StorageException e) {
            response.setSuccess(false);
            response.setMessage("Storage error: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error during file upload: " + e.getMessage());
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DownloadFileOperationRequest")
    @ResponsePayload
    public DownloadFileOperationResponse downloadFile(
            @RequestPayload DownloadFileOperationRequest request,
            @SoapHeader("{" + AUTH_NAMESPACE + "}AuthHeader") SoapHeaderElement authHeader) {

        DownloadFileOperationResponse response = new DownloadFileOperationResponse();

        try {
            // Extract token from header - improved extraction logic
            String token = extractToken(authHeader);
            if (token == null) {
                throw new SecurityException("Authentication token not found in header");
            }

            // Process download
            FileDownloadResult result = fileOperationService.downloadFile(
                    token,
                    request.getFileId(),
                    request.getUserId());

            // Build response
            response.setSuccess(true);
            response.setFileId(result.getFileId());
            response.setFileName(result.getFileName());
            response.setFileContent(result.getFileContent());
            response.setFileSize(result.getFileSize());
            response.setFileType(result.getFileType());

        } catch (SecurityException e) {
            response.setSuccess(false);
            response.setMessage("Authentication failure: " + e.getMessage());
        } catch (StorageException e) {
            response.setSuccess(false);
            response.setMessage("Storage error: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error during file download: " + e.getMessage());
        }

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DeleteFileOperationRequest")
    @ResponsePayload
    public DeleteFileOperationResponse deleteFile(
            @RequestPayload DeleteFileOperationRequest request,
            @SoapHeader("{" + AUTH_NAMESPACE + "}AuthHeader") SoapHeaderElement authHeader) {

        DeleteFileOperationResponse response = new DeleteFileOperationResponse();

        try {
            // Extract token from header - improved extraction logic
            String token = extractToken(authHeader);
            if (token == null) {
                throw new SecurityException("Authentication token not found in header");
            }

            // Process deletion
            boolean success = fileOperationService.deleteFile(
                    token,
                    request.getFileId(),
                    request.getUserId());

            // Build response
            response.setSuccess(success);
            if (!success) {
                response.setMessage("File deletion partially failed");
            }

        } catch (SecurityException e) {
            response.setSuccess(false);
            response.setMessage("Authentication failure: " + e.getMessage());
        } catch (StorageException e) {
            response.setSuccess(false);
            response.setMessage("Storage error: " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error during file deletion: " + e.getMessage());
        }

        return response;
    }

    /**
     * Enhanced method to extract token from SOAP header Tries multiple
     * approaches to get the token
     */
    private String extractToken(SoapHeaderElement authHeader) {
        if (authHeader == null) {
            return null;
        }

        // Method 1: Try to get direct text content first
        String directText = authHeader.getText();
        if (directText != null && !directText.trim().isEmpty()) {
            return directText.trim();
        }

        // Method 2: Look for Token attribute
        QName tokenQName = new QName(AUTH_NAMESPACE, "Token");
        String attributeToken = authHeader.getAttributeValue(tokenQName);
        if (attributeToken != null && !attributeToken.isEmpty()) {
            return attributeToken;
        }

        // Method 3: Try to access through DOM directly if the header supports it
        try {
            // Most SOAP implementations will use a DOMSource internally
            javax.xml.transform.Source source = authHeader.getSource();
            if (source instanceof javax.xml.transform.dom.DOMSource domSource) {
                org.w3c.dom.Node node = domSource.getNode();

                // If we have a DOM Element, we can navigate its children
                if (node instanceof org.w3c.dom.Element element) {
                    org.w3c.dom.NodeList tokenElements = element.getElementsByTagNameNS(AUTH_NAMESPACE, "Token");

                    if (tokenElements.getLength() > 0) {
                        return tokenElements.item(0).getTextContent();
                    }
                }
            }
        } catch (DOMException e) {
            logger.log(Level.WARNING, "Error accessing DOM structure: {0}", e.getMessage());
        }

        // No token found with any method
        return null;
    }
}
