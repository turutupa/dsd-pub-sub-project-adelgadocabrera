package zookeeper;

import models.Node;
import protos.ZK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author Alberto Delgado on 4/11/22
 * @project dsd-pub-sub
 * <p>
 * Data structure to handle the actions on the membership table; to have
 * some sort of concurrently control over it. Therefore, nodes, producers
 * and consumer (the caching maps) are kept as private.
 */
class MembershipTable {
    private final Map<Integer, ZKNode> nodes = new HashMap<>();
    private final Map<Integer, Node> producers = new HashMap<>();
    private final Map<Integer, Node> consumers = new HashMap<>();

    // zookeeper nodes lock // producers lock // consumers lock
    private final ReentrantReadWriteLock zLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock pLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock cLock = new ReentrantReadWriteLock();
    private int leaderId = Constants.UNASSIGNED_LEADER_ID;

    MembershipTable() {
    }

    // better reading - mainly for testing
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("LEADER: ").append(leaderId).append("\n");
        for (ZKNode node : nodes.values())
            out.append(node.toString()).append("\n");

        return out.toString();
    }

    // Adds new remote node
    void addNode(ZKNode node) {
        zLock.writeLock().lock();
        try {
            nodes.put(node.ID, node);
        } finally {
            zLock.writeLock().unlock();
        }
    }

    // Removes remote node
    void removeNode(int nodeId) {
        zLock.writeLock().lock();
        try {
            ZKNode node = nodes.get(nodeId);
            node.setHasCrashed();
            node.close();
        } finally {
            zLock.writeLock().unlock();
        }
    }

    // Sets a new leader and sets to "false"
    // the status of the old leader.
    void setLeader(int id) {
        zLock.writeLock().lock();
        try {
            if (nodes.containsKey(leaderId))
                nodes.get(leaderId).setLeader(false);

            leaderId = id;

            if (nodes.containsKey(id)) {
                ZKNode node = nodes.get(id);
                node.setLeader();
            }
        } finally {
            zLock.writeLock().unlock();
        }
    }

    // returns current leader
    int getLeaderId() {
        return leaderId;
    }

    // returns current leader node
    ZKNode getLeader() {
        zLock.readLock().lock();
        try {
            if (getLeaderId() == Constants.UNASSIGNED_LEADER_ID) return null;
            return nodes.get(leaderId);
        } finally {
            zLock.readLock().unlock();
        }
    }

    // returns a node requested by id
    public ZKNode getNode(int id) {
        zLock.readLock().lock();
        try {
            return nodes.get(id);
        } finally {
            zLock.readLock().unlock();
        }
    }

    // returns all nodes - mainly for
    // testing purposes
    List<ZKNode> getNodes() {
        zLock.readLock().lock();
        try {
            return new ArrayList<>(nodes.values());
        } finally {
            zLock.readLock().unlock();
        }
    }

    // Returns a node by id
    ZKNode getById(int hostId) {
        zLock.readLock().lock();
        try {
            return nodes.get(hostId);
        } finally {
            zLock.readLock().unlock();
        }
    }

    // checks if host is already registered
    // in membership table
    boolean has(int hostId) {
        zLock.readLock().lock();
        try {
            return nodes.containsKey(hostId);
        } finally {
            zLock.readLock().unlock();
        }
    }

    // Returns all producers - mainly for testing
    // purposes
    public Map<Integer, Node> getProducers() {
        pLock.writeLock().lock();
        try {
            return producers;
        } finally {
            pLock.writeLock().unlock();
        }
    }

    // Adds a new producer
    public void addProducer(Node producer) {
        pLock.writeLock().lock();
        try {
            if (!producers.containsKey(producer.ID))
                producers.put(producer.ID, producer);
        } finally {
            pLock.writeLock().unlock();
        }
    }

    // Removes a producer - to be tested
    public void removeProducer(Node producer) {
        pLock.writeLock().lock();
        try {
            producers.remove(producer.ID);
        } finally {
            pLock.writeLock().unlock();
        }
    }

    // returns all consumer - mainly for testing
    // purposes
    public Map<Integer, Node> getConsumers() {
        cLock.writeLock().lock();
        try {
            return consumers;
        } finally {
            cLock.writeLock().unlock();
        }
    }

    // adds a new remote consumer
    public void addConsumer(Node consumer) {
        cLock.writeLock().lock();
        try {
            consumers.put(consumer.ID, consumer);
        } finally {
            cLock.writeLock().unlock();
        }
    }

    // removes a consumer - to be tested
    public void removeConsumer(Node consumer) {
        cLock.writeLock().lock();
        try {
            consumers.remove(consumer.ID);
        } finally {
            cLock.writeLock().unlock();
        }
    }

    /**
     * Will I deadlock? Looks bad
     *
     * @return
     */
    ZK.MembershipTable toProtobuf() {
        zLock.readLock().lock();
        List<ZK.Node> allNodes = protobufNodes(nodes.values().stream().toList());
        zLock.readLock().unlock();

        pLock.readLock().lock();
        List<ZK.Node> allProducers = protobufClients(producers.values().stream().toList());
        pLock.readLock().unlock();

        cLock.readLock().lock();
        List<ZK.Node> allConsumers = protobufClients(consumers.values().stream().toList());
        cLock.readLock().unlock();

        return ZK.MembershipTable.newBuilder()
                .addAllNodes(allNodes)
                .addAllProducers(allProducers)
                .addAllConsumers(allConsumers)
                .build();
    }

    /**
     * I tried making protobufNodes and protobufClients the same method
     * by creating an interface called Serializeable that implements
     * toProtobuf; and the method would take List<Serializeable> but
     * for some reason it wouldn't work.
     *
     * @param nodes
     * @return
     */
    private List<ZK.Node> protobufNodes(List<ZKNode> nodes) {
        return nodes.stream()
                .map(ZKNode::toProtobuf)
                .collect(Collectors.toList());
    }

    private List<ZK.Node> protobufClients(List<Node> nodes) {
        return nodes.stream()
                .map(Node::toProtobuf)
                .collect(Collectors.toList());
    }

}
