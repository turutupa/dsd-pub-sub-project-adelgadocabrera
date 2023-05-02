package zookeeper;

/**
 * Data structure holding the received times,
 * the id and relevant information to detect whether
 * a heartbeat has been delayed too long (failure detection)
 */
public class HeartbeatRecord {
    public final int id;
    long lastReceived;
    int misses = 0;
    boolean hasCrashed = false;

    HeartbeatRecord(int nodeId, long lastReceived) {
        this.id = nodeId;
        this.lastReceived = lastReceived;
    }

    // sets the Heartbeat to crashed
    public void setHasCrashed(boolean hasCrashed) {
        this.hasCrashed = hasCrashed;
    }

    @Override
    public String toString() {
        String out = "HeartbeatRecord {\n";
        out += "   id=" + id + "\n";
        out += "   lastReceived=" + lastReceived + "\n";
        out += "   misses=" + misses + "\n";
        out += "}";
        return out;
    }
}