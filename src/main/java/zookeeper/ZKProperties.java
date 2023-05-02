package zookeeper;

import models.Node;

/**
 * @author Alberto Delgado on 4/13/22
 * @project dsd-pub-sub
 * <p>
 * Ideally I would simply have these properties in the ZooKeeper class publicly,
 * but they would be "static", so every class belonging to the ZooKeeper
 * package can access it.
 * <p>
 * PROBLEM:
 * When testing, I am running more than one broker, and each one has an instance
 * of ZooKeeper. Therefore, if these properties were to be "static" then I would
 * overwrite them each time a new ZooKeeper instance is created.
 * <p>
 * SOLUTION:
 * Create ZKProperties and pass them to each component that needs them in the
 * ZooKeeper package.
 * <p>
 * ALTERNATIVE:
 * Pass the ZooKeeper object. Perhaps I over-did the ZooKeeper passing. Makes it
 * complicated to debug
 */
public class ZKProperties extends Node {
    public ZKProperties(int id, String hostname, int zkPort, int brokerPort) {
        super(id, hostname, zkPort, brokerPort);
    }
}
