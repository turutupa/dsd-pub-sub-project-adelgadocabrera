package common;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Every server connection is expected to provide a handle
 * method, which is the logic to be performed in each connection;
 * and a setContext in order to be able to have a ServerConnectionHandler
 * with matching State actions for each strategy
 */
public abstract class ServerConnectionHandler {
    abstract protected void handle(Connection conn);

    abstract protected void setContext(Context ctxt);
}
