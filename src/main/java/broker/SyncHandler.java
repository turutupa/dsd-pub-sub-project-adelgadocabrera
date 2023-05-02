package broker;

import broker.connectionHandler.ConnectionHandler;
import com.google.protobuf.InvalidProtocolBufferException;
import common.Connection;
import common.RequestType;
import protos.Kafka;
import zookeeper.ZKNode;
import zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Set;

import static broker.ConnectionHelpers.sendRecord;

/**
 * @author Alberto Delgado on 4/17/22
 * @project dsd-pub-sub
 * <p>
 * Handles syncing between brokers
 */
public class SyncHandler extends BrokerService {

    SyncHandler(Broker broker, ZooKeeper zooKeeper) {
        super(broker, zooKeeper);
    }

    // Requests to sync to remote node.
    // Remote node will send all data (from the offsets sent) to be up-to-date
    void requestSync(ZKNode node) {
        if (node == null) return;
        if (broker.ID == node.ID) return;
        Connection conn = new Connection(node.HOSTNAME, node.getBrokerPort());

        boolean sentSyncRequest = sendRequest(conn);
        if (!sentSyncRequest) {
            System.out.println(broker.TAG + "[SYNC] Could not connect to " + node.ID);
            return;
        }

        byte[] data;
        while (true) {
            data = conn.receive();
            if (data == null) {
                System.out.println(broker.TAG + "[SYNC] Something went wrong receiving data.");
                return;
            }

            try {
                Kafka.Record record = Kafka.Record.parseFrom(data);
                String topic = record.getTopic();
                if (topic.equals(Constants.EOT)) {
                    Kafka.Record ack = Kafka.Record.newBuilder()
                            .setTopic(Constants.EOT)
                            .build();
                    try {
                        conn.send(ack.toByteArray());
                    } catch (IOException e) {
                        // close connection
                        conn.close();
                    }
                    return;
                }

                broker.dataStore.storeRecord(record);
            } catch (InvalidProtocolBufferException e) {
                return; // something went wrong. Stop syncing
            }
        }
    }

    // Sends sync request with the last offsets
    private boolean sendRequest(Connection conn) {
        if (conn == null || !conn.hasConnected)
            return false;

        Set<Kafka.Record> offsets = broker.segmentHandler.getOffsets();
        Kafka.Record request = Kafka.Record.newBuilder()
                .setNodeId(broker.ID)
                .setType(RequestType.BROKER_SYNC.name())
                .addAllRecords(offsets)
                .build();

        try {
            conn.send(request.toByteArray());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // This handles when being requested for syncing.
    // Additionally, sets the node as synced
    public void handleSyncRequest(ConnectionHandler connectionHandler,
                                  Connection conn,
                                  Kafka.Record record) {
        System.out.println("[BROKER " + connectionHandler.ID + "] Initiating SYNC request from Broker");

        connectionHandler.isSyncing.set(true);
        connectionHandler.syncBroker(conn, record);

        // Send DONE message!
        Kafka.Record done = Kafka.Record.newBuilder()
                .setTopic(Constants.EOT)
                .build();

        sendRecord(conn, done);
        // we simply want to know everything was received.
        // we don't really care so much about the content of the ack.
        byte[] ack = conn.receive();

        System.out.println("[BROKER " + connectionHandler.ID + "] Finished SYNC request from Broker");
        connectionHandler.setNodeAsSynced(record.getNodeId());
        connectionHandler.isSyncing.set(false);
    }
}
