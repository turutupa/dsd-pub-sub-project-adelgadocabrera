package zookeeper;

import java.util.HashMap;
import java.util.Map;

import static zookeeper.Constants.HEARTBEAT_INTERVAL_MS;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Handles running heartbeats - it may
 * - start more heartbeats if missing for a node
 * - cancel crashed nodes heartbeats
 */
class HeartbeatManager {
    private final ZooKeeper zooKeeper;
    private final MembershipTable membershipTable;
    private final Map<Integer, HeartbeatScheduler> heartbeats = new HashMap<>();

    public HeartbeatManager(ZooKeeper zooKeeper, MembershipTable membershipTable) {
        this.zooKeeper = zooKeeper;
        this.membershipTable = membershipTable;
    }

    // Starts a new heartbeat to specified node
    public synchronized void init(int to, Heartbeat heartbeat) {
        synchronized (heartbeats) {
            if (heartbeats.containsKey(to))
                return; // already sending hbs

            heartbeat.setMembershipTable(membershipTable);
            heartbeat.setLocalId(zooKeeper.ID);
            heartbeat.setZkPort(zooKeeper.ZK_PORT);
            HeartbeatScheduler hbScheduler = new HeartbeatScheduler(heartbeat, HEARTBEAT_INTERVAL_MS);
            hbScheduler.start();
            heartbeats.put(to, hbScheduler);
        }
    }

    // checks for existing heartbeat by id
    public boolean hasHeartbeat(int id) {
        synchronized (heartbeats) {
            return heartbeats.containsKey(id);
        }
    }

    // cancels heartbeat by id
    void cancel(Integer nodeId) {
        HeartbeatScheduler hbScheduler = heartbeats.get(nodeId);
        hbScheduler.cancel();
    }

    // closes all heartbeats - mainly for shutdown
    public void close() {
        synchronized (heartbeats) {
            for (int heartbeatId : heartbeats.keySet())
                cancel(heartbeatId);
        }
    }
}
