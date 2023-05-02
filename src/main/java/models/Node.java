package models;

import protos.ZK;

/**
 * @author Alberto Delgado on 4/6/22
 * @project dsd-pub-sub
 * <p>
 * Json file to be read. Basic properties of a node
 */

public class Node {
    public int ID;
    public String HOSTNAME;
    public int ZK_PORT;
    public int BROKER_PORT;
    public int PORT;

    public Node(int id, String hostname, int port) {
        ID = id;
        PORT = port;
        HOSTNAME = hostname;
    }

    public Node(int id, String hostname, int zkPort, int brokerPort) {
        this.ID = id;
        this.HOSTNAME = hostname;
        this.ZK_PORT = zkPort;
        this.BROKER_PORT = brokerPort;
    }

    public ZK.Node toProtobuf() {
        return ZK.Node.newBuilder()
                .setId(ID)
                .setHostname(HOSTNAME)
                .setPort(PORT)
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
        out += "}";
        return out;
    }
}

