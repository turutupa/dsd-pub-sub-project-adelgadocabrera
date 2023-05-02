package zookeeper;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Alberto Delgado on 4/6/22
 * @project dsd-pub-sub
 * <p>
 * <p>
 * Source:
 * https://martinfowler.com/articles/patterns-of-distributed-systems/heartbeat.html
 * <p>
 * Schedule tasks - mainly used to schedule heartbeats and to schedule
 * a failure detector
 */
class HeartbeatScheduler {
    long heartbeatIntervalMs;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final Runnable action;
    private ScheduledFuture<?> scheduledTask;

    HeartbeatScheduler(Runnable action, long heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.action = action;
    }

    // Starts the scheduler
    HeartbeatScheduler start() {
        scheduledTask = executor.scheduleWithFixedDelay(action, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
        return this;
    }

    // Cancels the scheduler
    void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }
        executor.shutdownNow();
    }
}
