package zookeeper;

/**
 * @author Alberto Delgado on 4/18/22
 * @project dsd-pub-sub
 * <p>
 * Observer design pattern - methods to be implemented if you
 * want to subscribe to the membership table (subscribes/unsubscribes)
 */
public interface MembershipTableListener {
    void subscribedNode(ZKNode node);

    void unsubscribedNode(ZKNode node);
}
