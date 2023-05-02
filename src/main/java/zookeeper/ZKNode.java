package zookeeper;

import common.Connection;
import models.Node;
import protos.ZK;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 4/6/22
 * @project dsd-pub-sub
 * <p>
 * This is the main Remote ZooKeeper node. Has information about its connection,
 * if it is synced, heartbeats and more.
 */
public class ZKNode extends Node {
    public Heartbeat heartbeat;
    Connection conn;
    private boolean isLeader = false;
    private boolean hasConnected = false;
    private boolean isSynced = false;
    private boolean hasCrashed = false;

    ZKNode(Node node) {
        this(node.ID, node.HOSTNAME, node.ZK_PORT, node.BROKER_PORT);
    }

    ZKNode(Node node, boolean initConnection) {
        this(node.ID, node.HOSTNAME, node.ZK_PORT, node.BROKER_PORT, initConnection);
    }

    public ZKNode(int id, String hostname, int zPort, int brokerPort) {
        this(id, hostname, zPort, brokerPort, true);
    }

    // Does several attempts to connect in case it fails. Comes in handy if you start a non-leader
    // broker before the leader broker
    ZKNode(int id, String hostname, int zPort, int brokerPort, boolean initConnection) {
        super(id, hostname, zPort, brokerPort);
        if (initConnection) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            int tries = 0;
            int delayFactor = 2;
            int delayMs = 200;
            int maxConnectionTries = 4;
            while (tries < maxConnectionTries && !hasConnected) {
                ScheduledFuture<Connection> futureConn = scheduler.schedule(
                        () -> new Connection(hostname, zPort),
                        delayMs * delayFactor,
                        TimeUnit.MILLISECONDS
                );

                try {
                    conn = futureConn.get();
                } catch (ExecutionException | InterruptedException e) {
                    tries++;
                    continue; // we'll try again then...
                }

                if (!conn.hasConnected) {
                    tries++;
                } else {
                    this.heartbeat = new Heartbeat(this);
                    hasConnected = true;
                }
            }
        }
    }

    // Sets the node to leader
    synchronized void setLeader() {
        isLeader = true;
    }

    // Sets the node to the specified state
    synchronized void setLeader(boolean bool) {
        isLeader = bool;
    }

    // gets the leader status
    public boolean getIsLeader() {
        return isLeader;
    }

    // gets the remote hostname
    public String getHostname() {
        return HOSTNAME;
    }

    // gets the remote broker port
    public int getBrokerPort() {
        return BROKER_PORT;
    }

    // attempts to reconnect
    void reconnect() {
        if (conn != null) conn.close();
        conn = new Connection(HOSTNAME, ZK_PORT);
    }

    // Sends data through the socket
    public boolean send(byte[] data) throws IOException {
        if (!hasConnected) return false;
        conn.send(data);
        return true;
    }

    // Receives data through the socket
    public byte[] receive() {
        return conn.receive();
    }

    // Retrieves the local hostname
    String getLocalHostname() {
        return conn.getLocalHostname();
    }

    // Sets the node to synced. Has all the expected data
    public void setIsSynced(boolean state) {
        isSynced = state;
    }

    // returns if node is synced (meaning it booted up,
    // and synced with leader before going to RUNNING state)
    public boolean getIsSynced() {
        return isSynced;
    }

    // gets the crash status - is it alive?
    public boolean getHasCrashed() {
        return hasCrashed;
    }

    // Sets the node to crashed - useful for
    // failure detector
    public void setHasCrashed() {
        hasCrashed = true;
    }

    // closes the connection
    public void close() {
        conn.close();
    }

    // Serializes the node to protobuf - to share its info
    // with other nodes
    @Override
    public ZK.Node toProtobuf() {
        return ZK.Node.newBuilder()
                .setId(ID)
                .setHostname(HOSTNAME)
                .setZkPort(ZK_PORT)
                .setBrokerPort(BROKER_PORT)
                .setHasCrashed(hasCrashed)
                .build();
    }

    @Override
    public String toString() {
        String out = "";
        out += "Node {" + "\n";
        out += "   id=" + ID + "\n";
        out += "   hostname=" + HOSTNAME + "\n";
        out += "   zookeeper port=" + ZK_PORT + "\n";
        out += "   broker port=" + BROKER_PORT + "\n";
        out += "   hasCrashed=" + hasCrashed + "\n";
        out += "}";
        return out;
    }
}
