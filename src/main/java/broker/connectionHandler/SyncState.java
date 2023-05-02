package broker.connectionHandler;

import common.Connection;
import protos.Kafka;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * This state is only used on first boot. Broker will try to SYNC
 * with leader broker. One time use.
 */
public class SyncState extends ConnectionHandlerState {
    SyncState(ConnectionHandler connectionHandler) {
        super(connectionHandler);
    }

    // Handles producer publishing
    @Override
    void handleProducerPublish(Connection conn, Kafka.Record record) {
    }

    // Handles consumer polling segments
    @Override
    void handleConsumerPoll(Connection conn, Kafka.Record record) {
    }

    // Handles consumer (push based) subscription
    @Override
    void handleConsumerSubscribe(Connection conn, Kafka.Record record) {
    }

    // Handles broker sync request
    @Override
    void handleBrokerSync(Connection conn, Kafka.Record record) {
    }
}
