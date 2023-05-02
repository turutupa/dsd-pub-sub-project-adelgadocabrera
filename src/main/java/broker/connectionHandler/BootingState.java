package broker.connectionHandler;

import common.Connection;
import protos.Kafka;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * On booting state we don't want the server to be responding to any
 * requests. First we need it to sync up with other Brokers if we are
 * running a distributed broker system.
 */
public class BootingState extends ConnectionHandlerState {
    BootingState(ConnectionHandler connectionHandler) {
        super(connectionHandler);
    }

    // Handles producer publishing
    @Override
    void handleProducerPublish(Connection conn, Kafka.Record record) {
        System.out.println("someone contacting while in boot ");
    }

    // Handles consumer polling
    @Override
    void handleConsumerPoll(Connection conn, Kafka.Record record) {

        System.out.println("someone contacting while in boot ");
    }

    // Handles consumer subscribing
    @Override
    void handleConsumerSubscribe(Connection conn, Kafka.Record record) {
        System.out.println("someone contacting while in boot ");

    }

    // Handles broker syncing
    @Override
    void handleBrokerSync(Connection conn, Kafka.Record record) {
        System.out.println("someone contacting while in boot ");

    }
}
