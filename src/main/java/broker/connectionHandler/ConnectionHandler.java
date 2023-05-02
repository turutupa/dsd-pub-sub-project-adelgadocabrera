package broker.connectionHandler;

import broker.BrokerDataStore;
import broker.PushBasedConsumerHandler;
import broker.ReplicationHandler;
import broker.SyncHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import common.*;
import protos.Kafka;
import zookeeper.ZooKeeper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Handles the logic for the server's logic. Each connection request
 * will be parsed here according to the state.
 */
public class ConnectionHandler extends ServerConnectionHandler {
    public int ID;
    String TAG;
    private final BrokerDataStore dataStore;
    private final PushBasedConsumerHandler pushBasedConsumers;
    private ReplicationHandler replicationHandler;
    private SyncHandler syncHandler;
    private Context context;
    private final Map<State, ConnectionHandlerState> stateHandlers = new HashMap<>();
    private ZooKeeper zooKeeper = null;
    public AtomicBoolean isSyncing = new AtomicBoolean();

    public ConnectionHandler(int id,
                             BrokerDataStore brokerDataStore,
                             PushBasedConsumerHandler pushBasedConsumerHandler
    ) {
        ID = id;
        TAG = "[BROKER " + id + "] ";
        dataStore = brokerDataStore;
        pushBasedConsumers = pushBasedConsumerHandler;

        // Set the strategy for each state
        stateHandlers.put(State.RUNNING, new RunningState(this));
        stateHandlers.put(State.BOOTING, new BootingState(this));
        stateHandlers.put(State.ELECTION, new ElectionState(this));
        stateHandlers.put(State.SYNC, new SyncState(this));
    }

    // Context setter
    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    // ZooKeeper setter
    public void setZooKeeper(ZooKeeper zk) {
        if (zooKeeper != null) return;
        zooKeeper = zk;
    }

    // Sync Handler setter
    public void setSyncHandler(SyncHandler s) {
        syncHandler = s;
    }

    // Replication Handler setter
    public void setReplicationHandler(ReplicationHandler replicationHandler) {
        this.replicationHandler = replicationHandler;
    }

    // Calls Broker data store to store records
    void storeRecord(Kafka.Record record) {
        dataStore.storeRecord(record);
    }

    // Calls Broker data store to read (and send) segments
    void sendSegment(Connection conn, Kafka.Record record) {
        dataStore.sendSegment(conn, record);
    }

    // Calls Broker data store to sync brokers
    public void syncBroker(Connection conn, Kafka.Record record) {
        dataStore.syncDataStore(conn, record);
    }

    //  Adds consumer to list of push based subscribed consumers
    void subscribeConsumer(Connection conn, String topic) {
        pushBasedConsumers.subscribe(conn, topic);
    }

    // Adds data to queue for Push based subscribers
    void sendToPushBasedConsumers(Kafka.Record record) {
        pushBasedConsumers.add(record);
    }

    // Uses replication handler to replicate data
    void sendToReplicas(Kafka.Record record) {
        replicationHandler.send(record);
    }

    // Sets a node as sync (ready to replicate)
    public void setNodeAsSynced(int nodeId) {
        replicationHandler.setNodeAsSynced(nodeId);
    }

    // Adds a producer to ZooKeeper
    public void addProducer(int id, String hostname, int port) {
        if (zooKeeper == null) return;
        zooKeeper.addProducer(id, hostname, port);
    }

    // Adds a consumer to ZooKeeper
    public void addConsumer(int id, String hostname, int port) {
        if (zooKeeper == null) return;
        zooKeeper.addConsumer(id, hostname, port);
    }

    // Uses sync handler to handle a Sync request
    public void handleSyncRequest(Connection conn, Kafka.Record record) {
        syncHandler.handleSyncRequest(this, conn, record);
    }

    /**
     * Handles a new Connection. The expected connections are of type:
     * - Publisher: is sending data for a specific topic
     * - Consumer: is polling data for a specific topic
     * - Consumer: is subscribing to a specific topic
     * <p>
     * Additionally, depending on the current state of the broker,
     * the connections will be handled by a ConnectionHandlerState
     * or another.
     *
     * @param conn
     */
    @Override
    public void handle(Connection conn) {
        String hostname = conn.getHostname();
        int port = conn.getRemotePort();
        System.out.println(TAG + "New connection from " + hostname + ":" + port);

        while (!conn.isClosed()) {
            byte[] data = conn.receive();
            if (data == null) return; // connection was closed?

            Kafka.Record record;
            try {
                record = Kafka.Record.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                continue;
            }

            String requestType = record.getType();

            // Delegate behavior to ConnectionHandlerStates
            if (requestType.equals(RequestType.PRODUCER_PUBLISH.name())) {
                stateHandlers.get(context.getState()).handleProducerPublish(conn, record);
            } else if (requestType.equals(RequestType.CONSUMER_POLL.name())) {
                stateHandlers.get(context.getState()).handleConsumerPoll(conn, record);
            } else if (requestType.equals(RequestType.CONSUMER_SUBSCRIBE.name())) {
                stateHandlers.get(context.getState()).handleConsumerSubscribe(conn, record);
            } else if (requestType.equals(RequestType.BROKER_SYNC.name())) {
                stateHandlers.get(context.getState()).handleBrokerSync(conn, record);
            } else {
                // We don't identify it.
                // Close it to avoid memory leaks.
                conn.close();
                return;
            }
        }
    }
}
