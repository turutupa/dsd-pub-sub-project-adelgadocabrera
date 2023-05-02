package common;

import broker.Broker;
import zookeeper.ZooKeeper;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Motivation: <a href="https://refactoring.guru/design-patterns/state">https://refactoring.guru/design-patterns/state</a>
 * Motivation: <a href="https://refactoring.guru/design-patterns/strategy">https://refactoring.guru/design-patterns/strategy</a>
 * <p>
 * Design pattern for executing a strategy in a given state
 */
public interface StateStrategy {
    void execute(Broker broker, ZooKeeper zooKeeper);
}
