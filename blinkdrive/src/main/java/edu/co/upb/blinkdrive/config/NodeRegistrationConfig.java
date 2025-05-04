package edu.co.upb.blinkdrive.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import edu.co.upb.blinkdrive.storage.model.StorageNode;
import edu.co.upb.blinkdrive.storage.registry.NodeRegistry;

@Configuration
@PropertySource("classpath:nodes.properties")
public class NodeRegistrationConfig {
    private static final Logger logger = LoggerFactory.getLogger(NodeRegistrationConfig.class);
    
    @Autowired
    private NodeRegistry nodeRegistry;
    
    @Value("${nodes.count:5}")
    private int nodeCount;
    
    @Value("${nodes.default.host:localhost}")
    private String defaultHost;
    
    @Value("${nodes.default.port:50051}")
    private int defaultPort;
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void registerStorageNodes() {
        List<StorageNode> nodes = new ArrayList<>();
        
        for (int i = 1; i <= nodeCount; i++) {
            String nodeIdProperty = "nodes.node" + i + ".id";
            String nodeNameProperty = "nodes.node" + i + ".name";
            String nodeHostProperty = "nodes.node" + i + ".host";
            String nodePortProperty = "nodes.node" + i + ".port";
            
            int nodeId = getPropertyAsInt(nodeIdProperty, i);
            String nodeName = getProperty(nodeNameProperty, "Node-" + i);
            String nodeHost = getProperty(nodeHostProperty, defaultHost);
            int nodePort = getPropertyAsInt(nodePortProperty, defaultPort + i - 1);
            
            StorageNode node = new StorageNode(
                    nodeId,
                    nodeName,
                    nodeHost,
                    nodePort,
                    "UNKNOWN", // Initial status
                    0L);        // Initial capacity
                    
            nodes.add(node);
            nodeRegistry.registerNode(node);
            
            logger.info("Registered storage node: ID={}, Name={}, Host={}:{}", 
                    nodeId, nodeName, nodeHost, nodePort);
        }
        
        logger.info("Registered {} storage nodes", nodes.size());
    }
    
    private String getProperty(String propertyName, String defaultValue) {
        try {
            return env.getProperty(propertyName, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private int getPropertyAsInt(String propertyName, int defaultValue) {
        try {
            String value = env.getProperty(propertyName);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}