package zookeeper;

import common.Context;
import common.Server;
import common.State;
import models.Node;
import utils.Demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alberto Delgado on 4/4/22
 * @project dsd-pub-sub
 * <p>
 * Main class that handles the distributed part of the Broker. Handles heartbeats,
 * failure detections, membership and status of every subscribed node.
 */
public class ZooKeeper implements Runnable {
    public int ID;
    public String HOSTNAME;
    public int ZK_PORT;
    public int BROKER_PORT;
    private Context context;
    private final Server server;
    private final ConnectionHandler connectionHandler;
    private final MembershipTable membershipTable;
    private final MembershipTableManager membershipTableManager = new MembershipTableManager();
    private final HeartbeatReceivedTimes heartbeatReceivedTimes;
    private final Membership membership;
    private final HeartbeatManager heartbeatManager;
    final FailureDetector failureDetector;

    public ZooKeeper(Node node) {
        this(node.ID, node.HOSTNAME, node.ZK_PORT, node.BROKER_PORT);
    }

    private ZooKeeper(int ID, String HOSTNAME, int ZK_PORT, int BROKER_PORT) {
        this(new ZKProperties(ID, HOSTNAME, ZK_PORT, BROKER_PORT));
    }

    public ZooKeeper(ZKProperties props) {
        ID = props.ID;
        HOSTNAME = props.HOSTNAME;
        ZK_PORT = props.ZK_PORT;
        BROKER_PORT = props.BROKER_PORT;
        membershipTable = new MembershipTable();
        heartbeatReceivedTimes = new HeartbeatReceivedTimes();
        membership = new Membership(props, membershipTable);
        heartbeatManager = new HeartbeatManager(this, membershipTable);
        failureDetector = new FailureDetector(this, heartbeatReceivedTimes);
        connectionHandler = new ConnectionHandler(this);
        server = new Server(ID, ZK_PORT, connectionHandler);
    }

    // Sets the context to be shared with the broker
    public void setContext(Context ctxt) {
        if (context != null) return;
        context = ctxt;
        connectionHandler.setContext(ctxt);
    }

    // Returns current heartbeats - for testing purposes
    public List<HeartbeatRecord> getHeartbeats() {
        return new ArrayList<>(heartbeatReceivedTimes.getRecords());
    }

    // Returns current nodes - for testing purposes
    public List<ZKNode> getNodes() {
        return membershipTable.getNodes();
    }

    // Returns a node given ID
    private ZKNode getNode(int nodeId) {
        return membershipTable.getNode(nodeId);
    }

    // Subscribes a list of nodes
    public void subscribe(List<Node> nodes) {
        for (Node node : nodes)
            subscribe(node);
    }

    // Subscribes nodes - if it doesn't yet a heartbeat
    // it starts one. Notifies listeners of new node
    public void subscribe(Node node) {
        ZKNode newNode = membership.subscribe(node);
        if (!heartbeatManager.hasHeartbeat(newNode.ID) && newNode.ID != ID) {
            Demo.printHeartbeat("[HEARTBEAT] Initiating heartbeats with node " + newNode.ID);
            Demo.printHeartbeat("[HEARTBEAT] Node: " + newNode.HOSTNAME + ":" + newNode.ZK_PORT);
            heartbeatManager.init(newNode.ID, newNode.heartbeat);
        }

        membershipTableManager.notifySubscribed(newNode);
    }

    // Unsubscribes a node by id - removes from membership table
    // and heartbeat
    private void unsubscribe(int id) {
        membershipTable.removeNode(id);
        heartbeatManager.cancel(id);
        heartbeatReceivedTimes.remove(id);
        membershipTableManager.notifyUnsubscribed(getNode(id));
    }

    // Adds listener to membership table
    public void addMembershipTableListener(MembershipTableListener listener) {
        membershipTableManager.subscribe(listener);
    }

    // Marks down a node - if it passes the max amount permissible
    // it will be set as crashed and new election will start
    void markNodeDown(HeartbeatRecord record) {
        // Only care about the misses in running state
        if (context.getState() != State.RUNNING) return;

        System.out.println("[ZOOKEEPER " + ID + "] Missing heartbeat from " + record.id);
        synchronized (record) {
            record.misses++;
            Demo.printHeartbeat("Node " + record.id + " has " + record.misses + "right now");

            if (record.misses == Constants.MAX_NODE_FAILURE_DETECTIONS) {
                unsubscribe(record.id);
                if (record.id == getLeaderId())
                    context.setState(State.ELECTION);
            }
        }
    }

    // sets to healthy current heartbeat
    public void markNodeUp(HeartbeatRecord record) {
        synchronized (record) {
            record.misses = 0;
        }
    }

    // Same as setLeader, but in this case it is the node
    // itself requesting leadership. This is only used in
    // case there are no other leaders.
    public void acquireLeadershipOnBoot(int id) {
        if (id == Constants.UNASSIGNED_LEADER_ID) return;
        membershipTable.setLeader(id);
    }

    // Sets leader by given node id
    public void setLeader(int leaderId) {
        membershipTable.setLeader(leaderId);
    }

    // Returns current leader ID
    public int getLeaderId() {
        return membershipTable.getLeaderId();
    }

    // Returns leader node
    public ZKNode getLeader() {
        return membershipTable.getLeader();
    }

    // Adds a producer to the Membership table
    public void addProducer(int id, String hostname, int port) {
        membershipTable.addProducer(new Node(id, hostname, port));
    }

    // Returns all producers - mainly testing purposes
    public Map<Integer, Node> getProducers() {
        return membershipTable.getProducers();
    }

    // Adds a consumer to membership table
    public void addConsumer(int id, String hostname, int port) {
        membershipTable.addConsumer(new Node(id, hostname, port));
    }

    // returns all consumers - mainly for testing purposes
    public Map<Integer, Node> getConsumers() {
        return membershipTable.getConsumers();
    }

    // print members - for testing purposes
    public void printMembers() {
        System.out.println(membersToString());
    }

    // stringifies membership table
    public String membersToString() {
        String out = "=============== Members ====================" + "\n";
        out += membership.toString() + "\n";
        out += "============================================" + "\n";
        return out;
    }

    // SHUTDOWN! Closes server and all services.
    public void close() {
        server.close();
        failureDetector.close();
        heartbeatManager.close();
    }

    // Runs ZooKeeper.
    @Override
    public void run() {
        server.run();
        failureDetector.run();
    }
}