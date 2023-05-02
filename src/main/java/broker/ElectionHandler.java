package broker;

import common.Connection;
import common.RequestType;
import models.Node;
import protos.ZK;
import zookeeper.ZKNode;
import zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 4/19/22
 * @project dsd-pub-sub
 * <p>
 * Handles the election process. Uses bully-algo, meaning
 * it will send request to higher priority nodes, if they respond
 * (alive) it will do nothing. Otherwise, will time out and
 * send victory message.
 */
class ElectionHandler extends BrokerService {
    private boolean running = false;
    private final String TAG;

    public ElectionHandler(Broker broker, ZooKeeper zooKeeper) {
        super(broker, zooKeeper);

        TAG = "[ELECTION HANDLER " + broker.ID + "] ";
    }

    // Handles the bully election process
    public void handleElection() {
        if (running) return; // make sure only one election at a time
        running = true;
        Set<ZKNode> electionNodes = requestLeadership();
        boolean existsBetterCandidate = existsAliveNode(electionNodes);

        if (existsBetterCandidate) {
            running = false;
            return;
        }

        System.out.println(TAG + "Woohoo! I am the leader now!");
        notifyVictory();
        zooKeeper.setLeader(broker.ID);
        running = false;
    }

    // Sends leadership request to higher order nodes
    private Set<ZKNode> requestLeadership() {
        ZK.Record electionInit = ZK.Record.newBuilder()
                .setHostId(broker.ID)
                .setType(RequestType.ZOOKEEPER_LEADER_CANDIDATE.name())
                .build();

        Set<ZKNode> electionNodes = new HashSet<>();
        for (ZKNode node : zooKeeper.getNodes()) {
            if (node.getHasCrashed()) continue;
            try {
                if (node.ID < broker.ID) {
                    node.send(electionInit.toByteArray());
                    electionNodes.add(node);
                }
            } catch (IOException e) {
                System.out.println(broker.TAG + " failed candidating to " + node.ID);
                // We are not handling this here. It will be
                // up to the ElectionStrategy to timeout if
                // the other node doesn't respond.
            }
        }
        return electionNodes;
    }

    // Checks if nodes respond with "alive"
    private boolean existsAliveNode(Set<ZKNode> nodes) {
        List<Future<Boolean>> responses = new ArrayList<>();
        for (ZKNode node : nodes) {
            long delayMs = zookeeper.Constants.ALIVE_TIMEOUT_MS;
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Future<Boolean> future = scheduler.schedule(
                    () -> {
                        node.receive();
                        return true;
                    },
                    delayMs,
                    TimeUnit.MILLISECONDS
            );
            responses.add(future);
        }

        for (Future<Boolean> response : responses) {
            try {
                return response.get();
            } catch (ExecutionException | InterruptedException e) {
            }
        }
        return false;
    }

    // Notifies of victory to nodes, consumer and producers
    public void notifyVictory() {
        for (ZKNode node : zooKeeper.getNodes())
            notifyBrokers(node);

        for (Node node : zooKeeper.getProducers().values())
            notifyClient(node);

        for (Node node : zooKeeper.getConsumers().values())
            notifyClient(node);
    }

    // Helper to notify brokers
    private void notifyBrokers(ZKNode node) {
        ZK.Record victory = ZK.Record.newBuilder()
                .setType(RequestType.ZOOKEEPER_LEADER_VICTORY.name())
                .setHostId(broker.ID)
                .build();

        try {
            node.send(victory.toByteArray());
        } catch (IOException e) {
            // continue
        }
    }

    // Helper to notify client
    private void notifyClient(Node node) {
        System.out.println("Trying to acquire leadership of " + node.HOSTNAME + ":" + node.PORT);
        Connection conn = new Connection(node.HOSTNAME, node.PORT);

        ZK.Record victory = ZK.Record.newBuilder()
                .setHostId(broker.ID)
                .setHostname(broker.HOSTNAME)
                .setBrokerPort(zooKeeper.BROKER_PORT)
                .build();

        try {
            conn.send(victory.toByteArray());
        } catch (IOException e) {
            // continue
        }
    }
}