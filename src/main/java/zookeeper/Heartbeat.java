package zookeeper;

import common.Connection;
import common.RequestType;
import protos.ZK;

import java.io.IOException;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Heartbeat takes care of sending the actual message to the broker.
 * It additionally sends the membership table so other brokers can
 * also subscribe to nodes they might be missing. Gossipping.
 */
public class Heartbeat implements Runnable {
    private MembershipTable membershipTable;
    private final ZKNode node;
    private int localId; // localhost ID
    final int to; // remote node ID
    private boolean isRunning = true;
    private int zkPort;
    private Connection conn;

    Heartbeat(ZKNode node) {
        this(node, Constants.UNASSIGNED_LEADER_ID);
        System.out.println("New heartbeat " + node.HOSTNAME + ":" + node.ZK_PORT);
        conn = new Connection(node.HOSTNAME, node.ZK_PORT);
    }

    Heartbeat(ZKNode node, int id) {
        this.node = node;
        this.to = node.ID;
        this.localId = id;
    }

    // membership table setter
    public void setMembershipTable(MembershipTable membershipTable) {
        this.membershipTable = membershipTable;
    }

    // sets the local ID (to inform remote node)
    public void setLocalId(int id) {
        this.localId = id;
    }

    // sets the local ZooKeeper port (to inform remote node)
    public void setZkPort(int port) {
        this.zkPort = port;
    }

    // Sends the heartbeat
    void send() {
        if (!isRunning) return;
        if (localId == Constants.UNASSIGNED_LEADER_ID) return; // hb needs to know the "local" id
        try {
            ZK.Record hb = ZK.Record.newBuilder()
                    .setType(RequestType.ZOOKEEPER_HEARTBEAT.toString())
                    .setHostId(localId)
                    .setHostname(node.getLocalHostname())
                    .setZkPort(zkPort)
                    .setLeader(membershipTable.getLeaderId())
                    .setMembershipTable(membershipTable.toProtobuf())
                    .build();

            conn.send(hb.toByteArray());
        } catch (IOException e) {
            // we are not handling this here... we are going to keep
            // trying sending. It is up to the failure detector to
            // close the connection.
        }
    }

    // Runnable in thread
    @Override
    public void run() {
        send();
    }
}
