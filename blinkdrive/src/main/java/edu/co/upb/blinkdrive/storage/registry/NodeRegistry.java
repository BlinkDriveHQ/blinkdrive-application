package edu.co.upb.blinkdrive.storage.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.co.upb.blinkdrive.storage.grpc.StorageNodeClient;
import edu.co.upb.blinkdrive.storage.model.StorageNode;

@Component
public class NodeRegistry {

    private final Map<Integer, StorageNode> nodes = new ConcurrentHashMap<>();
    private final Map<Integer, StorageNodeClient> clients = new ConcurrentHashMap<>();

    public void registerNode(StorageNode node) {
        nodes.put(node.getNodeId(), node);
    }

    public void unregisterNode(int nodeId) {
        nodes.remove(nodeId);
        StorageNodeClient client = clients.remove(nodeId);
        if (client != null) {
            client.shutdown();
        }
    }

    public StorageNode getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    public List<StorageNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<StorageNode> getActiveNodes() {
        return nodes.values()
                .stream()
                .filter(node -> "ACTIVE".equals(node.getStatus()))
                .collect(Collectors.toList());
    }

    public StorageNodeClient getClient(int nodeId) {
        return clients.computeIfAbsent(nodeId, id -> {
            StorageNode node = nodes.get(id);
            if (node == null) {
                throw new IllegalArgumentException("Node with ID " + id + " not found");
            }
            return new StorageNodeClient(node.getIpAddress(), node.getPort());
        });
    }

    public StorageNodeClient getClientForNode(StorageNode node) {
        return getClient(node.getNodeId());
    }

    public List<StorageNode> selectNodesForReplication(int count) {
        List<StorageNode> activeNodes = getActiveNodes();
        if (activeNodes.size() <= count) {
            return activeNodes;
        }

        // Sort nodes by available capacity (descending)
        return activeNodes.stream()
                .sorted(Comparator.comparingLong(StorageNode::getAvailableCapacityBytes).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public StorageNode selectPrimaryNode() {
        List<StorageNode> candidates = selectNodesForReplication(1);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public List<StorageNode> selectReplicaNodes(int primaryNodeId, int replicaCount) {
        List<StorageNode> activeNodes = getActiveNodes().stream()
                .filter(node -> node.getNodeId() != primaryNodeId)
                .collect(Collectors.toList());

        if (activeNodes.size() <= replicaCount) {
            return activeNodes;
        }

        return activeNodes.stream()
                .sorted(Comparator.comparingLong(StorageNode::getAvailableCapacityBytes).reversed())
                .limit(replicaCount)
                .collect(Collectors.toList());
    }

    public void updateNodeStatus(int nodeId, String status, long availableCapacityBytes) {
        StorageNode node = nodes.get(nodeId);
        if (node != null) {
            node.setStatus(status);
            node.setAvailableCapacityBytes(availableCapacityBytes);
        }
    }
}