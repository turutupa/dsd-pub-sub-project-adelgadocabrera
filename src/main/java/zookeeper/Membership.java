package zookeeper;

import common.RequestType;
import models.Node;
import protos.ZK;

import java.io.IOException;

/**
 * @author Alberto Delgado on 4/11/22
 * @project dsd-pub-sub
 * <p>
 * Handles ZooKeeper membership
 * Coordinates the logic to subscribe new brokers and to request
 * membership in remote node
 */
class Membership {
    final ZKProperties props;
    final MembershipTable membershipTable;

    Membership(ZKProperties props, MembershipTable membershipTable) {
        this.props = props;
        this.membershipTable = membershipTable;
    }


    public String toString() {
        return membershipTable.toString();
    }

    // Attempts to subscribe a new node.
    // Will check if already subscribed, and if new node
    // is one-self then it will be added to membership table
    // but won't establish connection
    synchronized ZKNode subscribe(Node node) {
        if (isSubscribed(node)) return membershipTable.getById(node.ID);

        ZKNode newNode;
        if (props.ID == node.ID) {
            Node n = new Node(props.ID, props.HOSTNAME, props.ZK_PORT, props.BROKER_PORT);
            newNode = new ZKNode(n, false);
        } else {
            newNode = new ZKNode(node);
            boolean subscribed = subscribe(newNode, 0);
            if (!subscribed) return null;
        }

        membershipTable.addNode(newNode);
        return newNode;
    }

    // sends the subscribe request to remote node
    private boolean subscribe(ZKNode node, int attempts) {
        ZK.Record request = ZK.Record.newBuilder()
                .setType(RequestType.ZOOKEEPER_MEMBERSHIP.name())
                .setHostId(props.ID)
                .setZkPort(props.ZK_PORT)
                .setBrokerPort(props.BROKER_PORT)
                .setHostname(props.HOSTNAME)
                .build();

        try {
            node.send(request.toByteArray());
        } catch (IOException e) {
            if (attempts < 3) {
                System.out.println("[HEARTBEAT] Attempting connect with " + node.ID + " failed. Retrying.");
                System.out.println("[HEARTBEAT] Node hostname " + props.HOSTNAME + ":" + node.ZK_PORT);
                node.reconnect();
                return subscribe(node, ++attempts);
            } else return false;
        }

        return true;
    }

    // checks if node is already subscribed
    private boolean isSubscribed(Node n) {
        return membershipTable.has(n.ID);
    }
}
