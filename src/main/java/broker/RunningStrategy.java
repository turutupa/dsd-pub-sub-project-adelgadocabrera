package broker;

import common.StateStrategy;
import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Actions to take when on "running mode". It mainly
 * affects the RunningState (in the server connection handler).
 */
class RunningStrategy implements StateStrategy {
    @Override
    public void execute(Broker broker, ZooKeeper zooKeeper) {
        System.out.println(broker.TAG + "Broker running.");
    }
}
