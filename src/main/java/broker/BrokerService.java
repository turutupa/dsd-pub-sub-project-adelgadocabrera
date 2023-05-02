package broker;

import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/19/22
 * @project dsd-pub-sub
 * <p>
 * All distributed services should implement this interface.
 * Indicating they required ZooKeeper and Broker to function
 * properly
 */

abstract class BrokerService {
    final Broker broker;
    final ZooKeeper zooKeeper;

    BrokerService(Broker broker, ZooKeeper zooKeeper) {
        this.broker = broker;
        this.zooKeeper = zooKeeper;
    }
}
