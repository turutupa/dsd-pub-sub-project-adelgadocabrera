package common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alberto Delgado on 4/21/22
 * @project dsd-pub-sub
 * <p>
 * Class used by producer and consumer to host a server which
 * will be used to be notified by a broker when he is proclaimed
 * new leader and has to inform of his new address
 */
public class Client implements Runnable {
    protected final int ID;
    protected final int PORT;
    protected int remotePort; // for testing purposes
    protected final Server server;
    protected ExecutorService serverThread = Executors.newSingleThreadExecutor();
    protected Connection conn;

    public Client(Properties props) {
        remotePort = props.getPort();
        ID = props.getId();
        PORT = props.getLocalPort();
        String hostname = props.getHostname();
        conn = new Connection(hostname, remotePort);
        server = new Server(ID, PORT, new ClientConnectionHandler(this));
    }

    // updates the new connection
    public void setConnection(Connection connection) {
        conn = connection;
    }

    // runs the server
    @Override
    public void run() {
        serverThread.submit(server);
    }
}
