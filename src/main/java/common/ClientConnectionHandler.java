package common;

import com.google.protobuf.InvalidProtocolBufferException;
import protos.ZK;

/**
 * @author Alberto Delgado on 4/20/22
 * @project dsd-pub-sub
 * <p>
 * Handles the server logic. Only expects to be called by a new
 * leader broker with its new address.
 */
public class ClientConnectionHandler extends ServerConnectionHandler {
    private final Client client;

    ClientConnectionHandler(Client client) {
        this.client = client;
    }

    // Used for Brokers - skip
    @Override
    protected void setContext(Context ctxt) {
    }

    // Server logic - updates leader connection
    @Override
    protected void handle(Connection conn) {
        while (!conn.isClosed()) {
            byte[] data = conn.receive();
            if (data == null) return; // connection closed?

            ZK.Record record;
            try {
                record = ZK.Record.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                conn.close();
                return;
            }

            int hostId = record.getHostId();
            String hostname = record.getHostname();
            int port = record.getBrokerPort();

            client.remotePort = port;
            System.out.println("[CLIENT " + client.ID + "] New leader. Syncing with node " + hostId);
            client.conn.close();
            client.setConnection(new Connection(hostname, port));
        }
    }
}
