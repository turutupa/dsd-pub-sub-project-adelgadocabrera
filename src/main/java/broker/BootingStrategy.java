package broker;

import common.StateStrategy;
import zookeeper.Constants;
import zookeeper.ZKNode;
import zookeeper.ZooKeeper;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Handles the actions to take on Boot.
 * Will start server/broker and will try to search for
 * a leader.
 * If no other leader, self assign leadership.
 */
class BootingStrategy implements StateStrategy {
    @Override
    public void execute(Broker broker, ZooKeeper zooKeeper) {
        System.out.println(broker.TAG + "Booting up broker");
        broker.serverThread = Executors.newSingleThreadExecutor();
        broker.serverThread.submit(broker.server);

        // if no zooKeeper running we are running a non-replication
        // broker. Return;
        if (zooKeeper == null) return;

        int leaderId = getLeader(zooKeeper);
        // there is no other node running, therefore
        // self-proclaim leader
        if (leaderId == Constants.UNASSIGNED_LEADER_ID
                || leaderId == broker.ID) {
            System.out.println(broker.TAG + "No leader detected. Self-proclaim as leader [" + broker.ID + "]");
            zooKeeper.acquireLeadershipOnBoot(broker.ID);
            return;
        }

        System.out.println(broker.TAG + "Node " + leaderId + " detected as leader.");
        zooKeeper.setLeader(leaderId);
    }

    /**
     * On first boot, there is no leader assigned. Therefore, diff
     * scenarios may happen:
     * - Boot up only one node: it will not find any other nodes on
     * membership table therefore it will self proclaim as leader.
     * - Boot up several nodes at the same time: it will search for
     * a leader, because all of them are new and there is no leader,
     * node with the lowest ID will be proclaimed as leader.
     * - Boot up and there are already running distributed brokers. Then
     * a node will be found as leader. Set this node as broker leader.
     *
     * @param zooKeeper
     * @return
     */
    private int getLeader(ZooKeeper zooKeeper) {
        int leaderId = Constants.UNASSIGNED_LEADER_ID;
        long delayMs = Constants.LEADER_DISCOVERY_PHASE_MS;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Future<Integer> futureLeader = scheduler.schedule(
                () -> {
                    if (zooKeeper.getLeaderId() != Constants.UNASSIGNED_LEADER_ID) {
                        return zooKeeper.getLeaderId();
                    }

                    List<ZKNode> nodes = zooKeeper.getNodes();
                    int leader = Integer.MAX_VALUE;
                    for (ZKNode node : nodes) {
                        if (node.getIsLeader()) return node.ID;
                        if (node.ID < leader) leader = node.ID;
                    }

                    return leader;
                },
                delayMs,
                TimeUnit.MILLISECONDS
        );

        try {
            leaderId = futureLeader.get();
        } catch (ExecutionException | InterruptedException e) {
            // not handling this
        }

        if (leaderId == Integer.MAX_VALUE) return Constants.UNASSIGNED_LEADER_ID;
        return leaderId;
    }
}


