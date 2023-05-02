package zookeeper;

import com.google.protobuf.InvalidProtocolBufferException;
import common.*;
import models.Node;
import protos.ZK;
import utils.Demo;

import java.io.IOException;
import java.util.List;

/**
 * @author Alberto Delgado on 4/11/22
 * @project dsd-pub-sub
 * <p>
 * Handles each connection objects to the server
 */
class ConnectionHandler extends ServerConnectionHandler {
    final ZooKeeper zk;
    final String TAG;
    Context context;

    ConnectionHandler(ZooKeeper zk) {
        this.zk = zk;
        this.TAG = "[ZK SERVER " + zk.ID + "] ";
    }

    @Override
    public void setContext(Context ctxt) {
        this.context = ctxt;
    }

    // Handles subscription of remote zookeeper node
    private void handleSubscription(ZK.Record record) {
        String hostname = record.getHostname();
        int brokerPort = record.getBrokerPort();
        int zPort = record.getZkPort();
        int hostId = record.getHostId();

        // Subscribe back!
        zk.subscribe(new Node(hostId, hostname, zPort, brokerPort));
    }

    // handles heartbeat from remote zookeeper node
    private void handleHeartbeat(ZK.Record record) {
        // update received time of Heartbeat
        int fromNode = record.getHostId();
        String hostname = record.getHostname();
        int port = record.getZkPort();

        // log for testing purposes
        Demo.printHeartbeat("[HEARTBEAT] Node " + fromNode + " -> " + hostname + ":" + port);
        Demo.printMembershipTable(zk.membersToString());

        zk.failureDetector.heartbeatReceived(fromNode);

        // Subscribe to each node from received membership table
        // if not already subscribed
        ZK.MembershipTable remoteMembershipTable = record.getMembershipTable();
        List<ZK.Node> nodeList = remoteMembershipTable.getNodesList();
        List<ZK.Node> producerList = remoteMembershipTable.getProducersList();
        List<ZK.Node> consumerList = remoteMembershipTable.getConsumersList();

        for (ZK.Node producer : producerList)
            zk.addProducer(producer.getId(),
                    producer.getHostname(),
                    producer.getPort());

        for (ZK.Node consumer : consumerList)
            zk.addConsumer(consumer.getId(),
                    consumer.getHostname(),
                    consumer.getPort());

        for (ZK.Node n : nodeList) {
            if (n.getHasCrashed()) continue;
            zk.subscribe(new Node(
                    n.getId(),
                    n.getHostname(),
                    n.getZkPort(),
                    n.getBrokerPort()
            ));
        }
    }

    // handles election request from remote node
    private void handleElection(Connection conn, ZK.Record record) {
        // Start election if in running state
        if (context.getState() == State.RUNNING)
            context.setState(State.ELECTION);

        int fromId = record.getHostId();

        // With Bully Algo we should not be receiving
        // candidate requests from nodes with higher priority
        // number. Nonetheless, we make sure so we don't
        // send Alive messages to higher priority nodes.
        if (fromId < zk.ID) return;

        ZK.Record alive = ZK.Record.newBuilder()
                .setType(RequestType.ZOOKEEPER_ALIVE.name())
                .setHostId(zk.ID)
                .build();

        try {
            for (ZKNode node : zk.getNodes()) {
                if (node.ID == fromId)
                    conn.send(alive.toByteArray());
            }
        } catch (IOException e) {
            System.out.println(TAG + "Failed sending ALIVE message");
            // Other side will time out if no response from this server
            // No need to re-send
        }
    }

    // Helper to receive data from remote node
    private ZK.Record receive(Connection conn) {
        byte[] data = conn.receive();
        if (data == null) return null; // connection closed!

        try {
            return ZK.Record.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    // Main logic.
    // It is expecting a series of possible requests, otherwise we close connection
    // to avoid memory leaks
    @Override
    public void handle(Connection conn) {
        while (!conn.isClosed()) {
            ZK.Record record = receive(conn);
            if (record == null) continue;

            String requestType = record.getType();
            if (requestType.equals(RequestType.ZOOKEEPER_MEMBERSHIP.name())) {
                handleSubscription(record);
            } else if (requestType.equals(RequestType.ZOOKEEPER_HEARTBEAT.name())) {
                handleHeartbeat(record);
            } else if (requestType.equals(RequestType.ZOOKEEPER_LEADER_VICTORY.name())) {
                System.out.println(TAG + " New leader is Broker " + record.getHostId());
                zk.setLeader(record.getHostId());
            } else if (requestType.equals(RequestType.ZOOKEEPER_LEADER_CANDIDATE.name())) {
                handleElection(conn, record);
            } else {
                conn.close(); // avoid memory leaks
                return;
            }
        }

    }

    private void print(String msg) {
        System.out.println(TAG + msg);
    }

    private void print(Integer msg) {
        System.out.println(TAG + msg);
    }
}
