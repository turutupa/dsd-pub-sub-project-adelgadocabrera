package common;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Runs a standard ServerSocket. Will submit each new connection
 * into connection pool.
 * <p>
 * Provide server logic to handle connections.
 */
public class Server implements Runnable {
    private ServerSocket server;
    private final int CONNECTION_POOL_TIMEOUT_IN_SECONDS = 30; // time to wait for broker until termination
    private final int PORT;
    private final String TAG;
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private final ServerConnectionHandler serverLogic;
    private boolean isRunning = true; // broker status
    private final Set<Socket> activeConnections = new HashSet<>();

    public Server(int id, int port, ServerConnectionHandler serverLogic) {
        this.PORT = port;
        this.TAG = "[SERVER " + id + ":" + port + "] ";
        this.serverLogic = serverLogic;
    }

    public void setContext(Context ctxt) {
        serverLogic.setContext(ctxt);
    }

    /**
     * Starts the Broker Server to listen for connections.
     */
    private ServerSocket startServer() {
        ServerSocket server;
        try {
            server = new ServerSocket(PORT);
            System.out.println(TAG + "Listening on port " + PORT + ".");
        } catch (IOException e) {
            System.out.println(TAG + e.getMessage());
            return null;
        }

        return server;
    }

    /**
     * Closes the Broker.
     */
    public void close() {
        isRunning = false;
        try {
            server.close();
        } catch (IOException e) {
            System.out.println(TAG + "IOException closing server");
        }

        new Thread(() -> {
            for (Socket socket : activeConnections) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // we are closing.
                    // we no care about no one
                }
            }
            connectionPool.shutdownNow();
            try {
                if (!connectionPool.awaitTermination(CONNECTION_POOL_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
                    System.err.println(TAG + "Connection pool didn't finish in " + CONNECTION_POOL_TIMEOUT_IN_SECONDS + " seconds");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Runs main server
     */
    @Override
    public void run() {
        server = startServer();
        if (server == null) return;

        while (isRunning) {
            try {
                Socket sock = server.accept();
                connectionPool.execute(() -> serverLogic.handle(new Connection(sock)));
                synchronized (activeConnections) {
                    activeConnections.add(sock);
                }
            } catch (IOException e) {
                System.out.println(TAG + "Connection rejected. Server is closed.");
            }
        }
    }
}
