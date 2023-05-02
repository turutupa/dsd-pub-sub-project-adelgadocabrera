package zookeeper;

/**
 * @author Alberto Delgado on 4/8/22
 * @project dsd-pub-sub
 * <p>
 * Source:
 * <a href="https://martinfowler.com/articles/patterns-of-distributed-systems/heartbeat.html">https://martinfowler.com/articles/patterns-of-distributed-systems/heartbeat.html</a>
 * <p>
 * Detects whether a heartbeat has been missing for to long and if
 * it has to be marked for take down
 */
class FailureDetector implements Runnable {
    final ZooKeeper zk;
    final HeartbeatScheduler heartbeatScheduler;
    final HeartbeatReceivedTimes receivedTimes;

    FailureDetector(ZooKeeper zk, HeartbeatReceivedTimes receivedTimes) {
        this.zk = zk;
        this.receivedTimes = receivedTimes;
        heartbeatScheduler = new HeartbeatScheduler(new HeartbeatCheck(receivedTimes, zk), Constants.HEARTBEAT_CHECK_INTERVAL_MS).start();
    }

    // updates the received times table
    void heartbeatReceived(int nodeId) {
        receivedTimes.update(nodeId);
    }

    // closes the scheduler to stop checking on heartbeats
    void close() {
        heartbeatScheduler.cancel();
    }

    // No need to run. Starts running upon creation
    @Override
    public void run() {
    }

    // Logic to check on the heartbeats (if they have to be marked
    // for take down)
    static class HeartbeatCheck implements Runnable {
        ZooKeeper zk;
        HeartbeatReceivedTimes receivedTimes;
        private final int msToNano = 1000000;

        HeartbeatCheck(HeartbeatReceivedTimes receivedTimes, ZooKeeper zooKeeper) {
            this.zk = zooKeeper;
            this.receivedTimes = receivedTimes;
        }

        // checks how much time has been delayed since last heartbeat
        public void run() {
            synchronized (receivedTimes) {
                long currentTime = System.nanoTime();
                for (HeartbeatRecord record : receivedTimes.getRecords()) {
                    if (record.hasCrashed) continue;
                    long delay = currentTime - record.lastReceived;
                    if (delay > Constants.FAILURE_DETECTOR_TIMEOUT_MS * msToNano) {
                        zk.markNodeDown(record);
                    } else {
                        zk.markNodeUp(record);
                    }
                }
            }
        }

    }
}
