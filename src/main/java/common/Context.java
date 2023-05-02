package common;

import broker.Broker;
import zookeeper.ZooKeeper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Motivation: <a href="https://refactoring.guru/design-patterns/state">https://refactoring.guru/design-patterns/state</a>
 * Motivation: <a href="https://refactoring.guru/design-patterns/strategy">https://refactoring.guru/design-patterns/strategy</a>
 * <p>
 * This is the heart of how the broker and zookeeper behave. You can set a series of strategies
 * that will be triggerred each time the state of the application is chained.
 */
public class Context {
    private State state;
    private Map<State, StateStrategy> strategies = new HashMap<>();
    Broker broker;
    ZooKeeper zooKeeper;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Context(Broker broker, ZooKeeper zooKeeper) {
        this.broker = broker;
        this.zooKeeper = zooKeeper;
    }

    // Adds new state strategy
    public void addStrategy(State st, StateStrategy strategy) {
        lock.writeLock().lock();
        try {
            if (strategies.containsKey(st)) return;
            strategies.put(st, strategy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // changes the state of the application and triggers
    // the State Strategy for that state
    public void setState(State newState) {
        lock.writeLock().lock();
        try {
            state = newState;
        } finally {
            lock.writeLock().unlock();
        }
        StateStrategy strategy = strategies.get(newState);
        if (strategy != null)
            strategy.execute(broker, zooKeeper);
    }

    // returns the current state
    public State getState() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }
}