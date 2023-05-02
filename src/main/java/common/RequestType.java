package common;

/**
 * @author Alberto Delgado on 3/3/22
 * @project dsd-pub-sub
 * <p>
 * These are the expected requests from the broker && zookeeeper
 */
public enum RequestType {
    PRODUCER_PUBLISH,
    CONSUMER_POLL,
    CONSUMER_SUBSCRIBE,
    BROKER_SYNC,
    ZOOKEEPER_HEARTBEAT,
    ZOOKEEPER_MEMBERSHIP,
    ZOOKEEPER_LEADER_VICTORY,
    ZOOKEEPER_LEADER_CANDIDATE,
    ZOOKEEPER_ALIVE
}
