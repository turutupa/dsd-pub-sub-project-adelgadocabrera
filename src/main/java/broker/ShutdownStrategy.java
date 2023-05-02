package broker;

import common.StateStrategy;
import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/20/22
 * @project dsd-pub-sub
 * <p>
 * Actions to take on "shutdown" mode.
 * Close broker.
 * Close zooKeeper.
 * <p>
 * Try to close all connections mainly.
 */
public class ShutdownStrategy implements StateStrategy {
    @Override
    public void execute(Broker broker, ZooKeeper zooKeeper) {
        System.out.println(broker.TAG + "Closing ZooKeeper.");
        zooKeeper.close();

        System.out.println(broker.TAG + "Closing Broker server.");
        broker.server.close();

        System.out.println(broker.TAG + "Shutting down threads.");
        broker.serverThread.shutdownNow();
        broker.zooKeeperThread.shutdownNow();
    }
}
