package broker;

import broker.connectionHandler.ConnectionHandler;
import common.Context;
import common.Server;
import common.State;
import models.Node;
import zookeeper.HeartbeatRecord;
import zookeeper.ZKNode;
import zookeeper.ZKProperties;
import zookeeper.ZooKeeper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alberto Delgado on 2/28/22
 * @project dsd-pub-sub
 * <p>
 * Broker. Its task is to store locally data sent from the
 * publishers, and serve that data to consumers that may
 * request so.
 * <p>
 * Consumers may poll data or may also subscribe to get the
 * data from the broker as soon as it is received from the publisher.
 * <p>
 * Additionally, with ZooKeeper it acts as a distributed Broker.
 */
public class Broker implements Runnable {
    final int ID;
    String HOSTNAME;
    final int BROKER_PORT;
    int ZK_PORT;
    final String TAG;
    final Server server;
    final ConnectionHandler connectionHandler;
    ExecutorService serverThread;
    ZooKeeper zooKeeper;
    ExecutorService zooKeeperThread;
    Context context;
    final BrokerDataStore dataStore;
    final PushBasedConsumerHandler pushBasedConsumerHandler;
    final SegmentHandler segmentHandler;
    SyncHandler syncHandler;
    ReplicationHandler replicationHandler;
    ElectionHandler electionHandler;

    public Broker(int id, int port) {
        this(id, port, 0);
    }

    public Broker(int id, int brokerPort, int zooKeeperPort) {
        ID = id;
        ZK_PORT = zooKeeperPort;
        BROKER_PORT = brokerPort;
        TAG = "[BROKER " + ID + "] ";
        segmentHandler = new SegmentHandler("node-" + ID + "/");
        dataStore = new BrokerDataStore(segmentHandler);
        pushBasedConsumerHandler = new PushBasedConsumerHandler();
        connectionHandler = new ConnectionHandler(id, dataStore, pushBasedConsumerHandler);
        server = new Server(id, brokerPort, connectionHandler);

        try {
            HOSTNAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            HOSTNAME = null;
        }

        if (zooKeeperPort != 0)
            createZooKeeper();
        else
            zooKeeper = null;
    }

    // Creates ZooKeeper with the required dependencies.
    // And it is initialized.
    private void createZooKeeper() {
        System.out.println(TAG + "Starting ZooKeeper " + ID + ".");
        ZKProperties zkProps = new ZKProperties(ID, HOSTNAME, ZK_PORT, BROKER_PORT);
        zooKeeper = new ZooKeeper(zkProps);
        syncHandler = new SyncHandler(this, zooKeeper);
        replicationHandler = new ReplicationHandler(this, zooKeeper);
        electionHandler = new ElectionHandler(this, zooKeeper);
        connectionHandler.setZooKeeper(zooKeeper);
        connectionHandler.setReplicationHandler(replicationHandler);
        connectionHandler.setSyncHandler(syncHandler);
        zooKeeper.addMembershipTableListener(replicationHandler);

        // add context to both broker server and zookeeper
        context = initContext();
        server.setContext(context);
        zooKeeper.setContext(context);

        // start zookeeper
        zooKeeperThread = Executors.newSingleThreadExecutor();
        zooKeeperThread.submit(zooKeeper);
    }

    // Creates a context to be shared between Broker and ZooKeeper
    // This is the medium in which they will "communicate" and
    // handle the lifecycle
    private Context initContext() {
        Context ctxt = new Context(this, zooKeeper);
        ctxt.addStrategy(State.BOOTING, new BootingStrategy());
        ctxt.addStrategy(State.SYNC, new SyncStrategy());
        ctxt.addStrategy(State.RUNNING, new RunningStrategy());
        ctxt.addStrategy(State.ELECTION, new ElectionStrategy());
        ctxt.addStrategy(State.SHUTDOWN, new ShutdownStrategy());
        return ctxt;
    }

    // Adds zooKeeper for a distributed broker
    public void addZooKeeper(int zkPort) {
        if (zooKeeper != null) return;
        ZK_PORT = zkPort;
        createZooKeeper();
    }

    // Adds a remote node to ZooKeeper
    public void addNode(Node node) {
        if (zooKeeper == null) return;
        new Thread(() -> zooKeeper.subscribe(node)).start();
    }

    // Returns current nodes. Mainly for testing
    // purposes.
    public List<ZKNode> getNodes() {
        if (zooKeeper == null) return null;
        return zooKeeper.getNodes();
    }

    // Returns the current leader ID
    public int getLeaderId() {
        return zooKeeper.getLeaderId();
    }

    // Prints current membership table nodes
    // Mainly for testing purposes.
    public void printNodes() {
        if (zooKeeper == null) return;
        zooKeeper.printMembers();
    }

    // Sets the broker to shut down
    public void close() {
        context.setState(State.SHUTDOWN);
    }

    // Gets the list of current heartbeats
    // For testing purposes.
    List<HeartbeatRecord> getHeartbeats() {
        return zooKeeper.getHeartbeats();
    }

    // Logic for first boot of the broker
    // Will boot, then sync IF necessary and
    // then will set to running
    @Override
    public void run() {
        context.setState(State.BOOTING);
        context.setState(State.SYNC);
        context.setState(State.RUNNING);
    }
}
