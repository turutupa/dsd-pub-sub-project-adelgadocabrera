package common;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * The possible States of the application.
 */
public enum State {
    BOOTING,
    RUNNING,
    ELECTION,
    SYNC,
    SHUTDOWN
}
