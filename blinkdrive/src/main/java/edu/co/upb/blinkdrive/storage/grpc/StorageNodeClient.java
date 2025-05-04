package edu.co.upb.blinkdrive.storage.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import java.util.logging.Logger;
import java.util.logging.Level;

import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;

public class StorageNodeClient {
    private static final Logger logger = Logger.getLogger(StorageNodeClient.class.getName());
    
    private final ManagedChannel channel;
    private final StorageNodeGrpc.StorageNodeBlockingStub blockingStub;
    private final StorageNodeGrpc.StorageNodeStub asyncStub;
    
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    
    public StorageNodeClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }
    
    StorageNodeClient(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = StorageNodeGrpc.newBlockingStub(channel);
        this.asyncStub = StorageNodeGrpc.newStub(channel);
    }
    
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Error shutting down channel", e);
            Thread.currentThread().interrupt();
        }
    }
    
    public String uploadFile(String fileId, byte[] fileContent) {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final List<String> responseFileId = new ArrayList<>(1);
        final List<Throwable> errors = new ArrayList<>(1);
        
        StreamObserver<UploadResponse> responseObserver = new StreamObserver<UploadResponse>() {
            @Override
            public void onNext(UploadResponse response) {
                if (response.getSuccess()) {
                    responseFileId.add(response.getFileId());
                } else {
                    errors.add(new RuntimeException(response.getMessage()));
                }
            }
            
            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "Upload failed: {0}", t.getMessage());
                errors.add(t);
                finishLatch.countDown();
            }
            
            @Override
            public void onCompleted() {
                logger.info("Upload completed");
                finishLatch.countDown();
            }
        };
        
        StreamObserver<FileChunk> requestObserver = asyncStub.uploadFile(responseObserver);
        
        try {
            // If fileId is null or empty, generate a new one
            if (fileId == null || fileId.isEmpty()) {
                fileId = UUID.randomUUID().toString();
            }
            
            // Break file into chunks
            for (int i = 0; i < fileContent.length; i += CHUNK_SIZE) {
                int chunkSize = Math.min(CHUNK_SIZE, fileContent.length - i);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(fileContent, i, chunk, 0, chunkSize);
                
                FileChunk fileChunk = FileChunk.newBuilder()
                        .setFileId(fileId)
                        .setContent(ByteString.copyFrom(chunk))
                        .setChunkNumber(i / CHUNK_SIZE)
                        .build();
                        
                requestObserver.onNext(fileChunk);
                
                // Check if any errors occurred
                if (!errors.isEmpty()) {
                    break;
                }
            }
            
            // Mark end of requests
            requestObserver.onCompleted();
            
            // Wait for server to process all chunks
            if (!finishLatch.await(5, TimeUnit.MINUTES)) {
                logger.warning("Upload timed out");
                throw new RuntimeException("Upload timed out");
            }
            
            if (!errors.isEmpty()) {
                throw new RuntimeException("Upload failed: " + errors.get(0).getMessage(), errors.get(0));
            }
            
            return responseFileId.isEmpty() ? fileId : responseFileId.get(0);
            
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Upload interrupted", e);
            requestObserver.onError(new RuntimeException("Upload interrupted", e));
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Upload failed", e);
            requestObserver.onError(e);
            throw e;
        }
    }
    
    public byte[] downloadFile(String fileId) {
        List<byte[]> chunks = new ArrayList<>();
        long totalSize = 0;
        
        try {
            FileRequest request = FileRequest.newBuilder()
                    .setFileId(fileId)
                    .build();
                    
            Iterator<FileChunk> fileChunks = blockingStub.downloadFile(request);
            
            while (fileChunks.hasNext()) {
                FileChunk chunk = fileChunks.next();
                byte[] data = chunk.getContent().toByteArray();
                chunks.add(data);
                totalSize += data.length;
            }
            
            // Combine all chunks
            byte[] result = new byte[(int) totalSize];
            int offset = 0;
            
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }
            
            return result;
            
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "Download failed: {0}", e.getStatus());
            
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new RuntimeException("File not found: " + fileId, e);
            }
            
            throw new RuntimeException("Download failed: " + e.getMessage(), e);
        }
    }
    
    public boolean deleteFile(String fileId) {
        try {
            FileRequest request = FileRequest.newBuilder()
                    .setFileId(fileId)
                    .build();
                    
            DeleteResponse response = blockingStub.deleteFile(request);
            return response.getSuccess();
            
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "Delete failed: {0}", e.getStatus());
            return false;
        }
    }
    
    public StatusResponse getStatus() {
        try {
            StatusRequest request = StatusRequest.newBuilder().build();
            return blockingStub.getNodeStatus(request);
            
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "Status check failed: {0}", e.getStatus());
            throw new RuntimeException("Status check failed: " + e.getMessage(), e);
        }
    }
}