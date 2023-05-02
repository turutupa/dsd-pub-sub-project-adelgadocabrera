package broker;

import common.State;
import common.StateStrategy;
import zookeeper.ZKNode;
import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/11/22
 * @project dsd-pub-sub
 * <p>
 * Handles the actions to be taken the moment the state
 * changes to Election mode.
 * <p>
 * Will initiate election (bully-algo) plus will sync up with
 * rest of nodes before become operative
 */
class ElectionStrategy implements StateStrategy {

    // Main logic: election + sync
    public void execute(Broker broker, ZooKeeper zooKeeper) {
        ElectionHandler electionHandler = broker.electionHandler;
        System.out.println(broker.TAG + "Election time!");
        electionHandler.handleElection();

        if (zooKeeper.getLeaderId() == broker.ID) {
            sync(broker, zooKeeper);
        }

        // go back to running!
        broker.context.setState(State.RUNNING);
    }

    // Syncs with brokers to have data up to date
    private void sync(Broker broker, ZooKeeper zooKeeper) {
        for (ZKNode node : zooKeeper.getNodes()) {
            if (node.getHasCrashed()) continue;
            if (node.ID != broker.ID) {
                broker.syncHandler.requestSync(node);
                broker.replicationHandler.setNodeAsSynced(node.ID);
            }

        }
    }
}
