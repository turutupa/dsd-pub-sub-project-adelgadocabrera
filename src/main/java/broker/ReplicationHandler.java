package broker;

import common.Connection;
import protos.Kafka;
import utils.Demo;
import zookeeper.MembershipTableListener;
import zookeeper.ZKNode;
import zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alberto Delgado on 4/18/22
 * @project dsd-pub-sub
 * <p>
 * Handles the replication logic. It is subscribed to the membership table
 * (as a listener) and each time a node is unsubscribed or subscribed it is
 * reflected here to have up-to-date nodes.
 * <p>
 * Will only replicate if it is leader.
 */
public class ReplicationHandler extends BrokerService implements MembershipTableListener {
    private final Map<Integer, ZKNode> replicas = new HashMap<>();
    private final Map<Integer, Connection> connections = new HashMap<>();

    ReplicationHandler(Broker broker, ZooKeeper zooKeeper) {
        super(broker, zooKeeper);
    }

    //  send data to replicas
    public synchronized void send(Kafka.Record record) {
        handleConnections();

        Kafka.Record replica = Kafka.Record.newBuilder(record)
                .setRole(Kafka.Record.Role.BROKER)
                .build();

        for (Connection conn : connections.values()) {
            try {
                Demo.printReplication("[REPLICATION HANDLER] Sending data to replica");
                conn.send(replica.toByteArray());
            } catch (IOException e) {
                // Not handling this here
                // If connection broker, it is up to ZooKeeper
                // to remove that connection
            }
        }
    }

    // Subscribes a node
    @Override
    public void subscribedNode(ZKNode node) {
        addReplica(node);
    }

    // Unsubscribes a node
    @Override
    public void unsubscribedNode(ZKNode node) {
        removeReplica(node);
    }

    // Adds a replica to replica map (new node)
    private void addReplica(ZKNode node) {
        synchronized (replicas) {
            if (replicas.containsKey(node.ID) || node.ID == zooKeeper.ID) return;
            replicas.put(node.ID, node);
        }
    }

    // Removes replica from map (crashed? node)
    private void removeReplica(ZKNode node) {
        synchronized (replicas) {
            replicas.remove(node.ID);
        }
    }

    // Checks if it is leader. In case it is not it removes all
    // connections. Trying to avoid memory leaks.
    private void handleConnections() {
        // There is definitely a more efficient way of doing this.
        // The intention here is to avoid memory leaks by preventing
        // having too many connections open
        if (amILeader())
            establishConnection();
        else
            closeConnections();
    }

    // Establishes connections to nodes if
    // not already established
    private void establishConnection() {
        for (ZKNode node : replicas.values()) {
            if (!node.getIsSynced()) continue;
            if (!connections.containsKey(node.ID)) {
                System.out.println("[REPLICATION " + zooKeeper.ID + "] Establishing connection to node " + node.ID + ".");
                Connection replica = new Connection(node.HOSTNAME, node.BROKER_PORT);
                if (replica.hasConnected)
                    connections.put(node.ID, replica);
            }
        }
    }

    // Closes all connections
    private void closeConnections() {
        for (Map.Entry<Integer, Connection> entry : connections.entrySet()) {
            int id = entry.getKey();
            Connection conn = entry.getValue();
            conn.close();
            connections.remove(id);
        }
    }

    // Checks if currently leader
    private boolean amILeader() {
        return zooKeeper.getLeaderId() == zooKeeper.ID;
    }

    // Set node as sync. It is important, otherwise it won't replicate
    // to avoid corruption of data.
    public void setNodeAsSynced(int nodeId) {
        if (replicas.containsKey(nodeId)) {
            replicas.get(nodeId).setIsSynced(true);
        }
    }
}