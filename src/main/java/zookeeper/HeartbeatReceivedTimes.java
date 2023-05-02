package zookeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alberto Delgado on 4/14/22
 * @project dsd-pub-sub
 * <p>
 * Data Structure to store the Received Times of the heartbeats as well
 * as a centralized method to define actions on how to update/remove items
 */
public class HeartbeatReceivedTimes {
    private final Map<Integer, HeartbeatRecord> receivedTimes = new HashMap<>();

    List<HeartbeatRecord> getRecords() {
        synchronized (receivedTimes) {
            return new ArrayList<>(receivedTimes.values());
        }
    }

    // Updates the received time of a specified node id
    synchronized void update(int nodeId) {
        long currentTime = System.nanoTime();
        if (receivedTimes.containsKey(nodeId)) {
            HeartbeatRecord record = receivedTimes.get(nodeId);
            record.lastReceived = currentTime;
        } else {
            HeartbeatRecord record = new HeartbeatRecord(nodeId, currentTime);
            receivedTimes.put(nodeId, record);
        }
    }

    // Sets to crashed a specific node so,it is no
    // longer necessary to get if missing hb
    synchronized void remove(int nodeId) {
        receivedTimes.get(nodeId).setHasCrashed(true);
    }
}
