package common;

/**
 * @author Alberto Delgado on 4/21/22
 * @project dsd-pub-sub
 * <p>
 * Implemented by third party Clients to make sure they
 * can update to new connection when new broker becomes leader
 */
public interface ClientUpdateable {
    void setConnection(Connection conn);
}
