package common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Alberto Delgado on 3/8/22
 * @project dsd-pub-sub
 * <p>
 * Establishes a connection via socket. You may send and receive
 * data through this connection.
 */
public class Connection {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    public final int PORT;
    public final String HOSTNAME;
    private final String TAG = "[CONNECTION] ";
    public boolean hasConnected;

    public Connection(String hostname, int port) {
        HOSTNAME = hostname;
        PORT = port;
        connect(hostname, port);
    }

    public Connection(Socket socket) {
        this.PORT = socket.getPort();
        this.HOSTNAME = socket.getInetAddress().getHostName();

        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // creates new connection
    public void reconnect() {
        try {
            if (socket != null) socket.close();
            connect(HOSTNAME, PORT);
        } catch (IOException ignored) {
            // not going to handle this
        }
    }

    // establishes socket connection to hostname and port
    private void connect(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            hasConnected = true;
        } catch (IOException e) {
            System.err.println(TAG + "Failed establishing connection to " + hostname + ":" + port + ".");
            hasConnected = false;
        }
    }

    // sends byte array
    public synchronized void send(byte[] data) throws IOException {
        if (socket == null || socket.isClosed()) return;
        out.writeInt(data.length);
        out.write(data);
    }

    // receives byte array
    public synchronized byte[] receive() {
        try {
            int len = in.readInt();
            byte[] data = new byte[len];
            in.readFully(data, 0, len);
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    // returns the local host name
    public String getLocalHostname() {
        return socket.getLocalAddress().getHostName();
    }

    // returns local port
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    // returns remote hostname
    public String getHostname() {
        return socket.getInetAddress().getHostName();
    }

    // returns remote port
    public int getRemotePort() {
        return socket.getPort();
    }

    // checks if conn is closed
    public boolean isClosed() {
        if (socket == null) return true;
        return socket.isClosed();
    }

    // closes connection
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
