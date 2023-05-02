package broker.connectionHandler;

import common.Connection;
import protos.Kafka;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * In every state of the broker, these are the requests
 * each state should be ready to tackle.
 */
public abstract class ConnectionHandlerState {
    ConnectionHandler connectionHandler;

    ConnectionHandlerState(ConnectionHandler ch) {
        this.connectionHandler = ch;
    }

    // Handles producer publishing
    abstract void handleProducerPublish(Connection conn, Kafka.Record record);

    // Handles consumer polling segments
    abstract void handleConsumerPoll(Connection conn, Kafka.Record record);

    // Handles consumer (push based) subscription
    abstract void handleConsumerSubscribe(Connection conn, Kafka.Record record);

    // Handles broker sync request
    abstract void handleBrokerSync(Connection conn, Kafka.Record record);
}
