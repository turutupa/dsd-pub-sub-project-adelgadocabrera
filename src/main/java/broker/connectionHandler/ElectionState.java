package broker.connectionHandler;

import common.Connection;
import protos.Kafka;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * Connection handler for Election state.
 * We do not wish for the broker to respond to anyone. Only to sync.
 */
public class ElectionState extends ConnectionHandlerState {
    ElectionState(ConnectionHandler connectionHandler) {
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
        connectionHandler.handleSyncRequest(conn, record);
    }
}
