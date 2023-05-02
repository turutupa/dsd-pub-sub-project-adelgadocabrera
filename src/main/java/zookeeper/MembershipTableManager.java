package zookeeper;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Alberto Delgado on 4/18/22
 * @project dsd-pub-sub
 * <p>
 * Motivation: <a href="https://refactoring.guru/design-patterns/observer">https://refactoring.guru/design-patterns/observer</a>
 * <p>
 * Manager of the membership table.
 * If you want to know of subscription/unsubscription nodes simply
 * add your listener here.
 */
public class MembershipTableManager {
    Set<MembershipTableListener> listeners = new HashSet<>();

    // Subscribe to listen for changes
    public void subscribe(MembershipTableListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    // Unsubscribe to stop listening for changes
    public void unsubscribe(MembershipTableListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    // Notify all listeners of new node
    void notifySubscribed(ZKNode node) {
        synchronized (listeners) {
            for (MembershipTableListener listener : listeners)
                listener.subscribedNode(node);
        }
    }

    // Notify all listeners of remove node
    void notifyUnsubscribed(ZKNode node) {
        synchronized (listeners) {
            for (MembershipTableListener listener : listeners)
                listener.unsubscribedNode(node);
        }
    }
}
