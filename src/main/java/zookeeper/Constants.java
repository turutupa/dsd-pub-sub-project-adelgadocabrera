package zookeeper;

/**
 * @author Alberto Delgado on 4/11/22
 * @project dsd-pub-sub
 * <p>
 * Main constants of ZooKeeper
 */
public class Constants {
    public static final int UNASSIGNED_LEADER_ID = -1;
    public static final long HEARTBEAT_INTERVAL_MS = 1000L;
    public static final long HEARTBEAT_CHECK_INTERVAL_MS = 1000L;
    public static final long FAILURE_DETECTOR_TIMEOUT_MS = 4000L;
    public static final int MAX_NODE_FAILURE_DETECTIONS = 3;

    public static final long LEADER_DISCOVERY_PHASE_MS = 3000L;
    public static final long ALIVE_TIMEOUT_MS = 2000L;
}
