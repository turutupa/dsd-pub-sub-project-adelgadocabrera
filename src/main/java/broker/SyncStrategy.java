package broker;

import common.StateStrategy;
import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Handles the "Sync" mode of the application. It will only
 * request to sync data to the leader. Assumes leader has
 * up-to-date data.
 */
class SyncStrategy implements StateStrategy {
    @Override
    public void execute(Broker broker, ZooKeeper zooKeeper) {
        System.out.println(broker.TAG + "Sync stage.");
        // No need to sync if not running distributed broker
        if (zooKeeper == null) return;
        if (zooKeeper.getLeaderId() == zooKeeper.ID) return;

        // Time to sync-up!
        System.out.println(broker.TAG + "Initiating data SYNC.");
        broker.syncHandler.requestSync(zooKeeper.getLeader());
        System.out.println(broker.TAG + "Syncing complete.");
    }
}
