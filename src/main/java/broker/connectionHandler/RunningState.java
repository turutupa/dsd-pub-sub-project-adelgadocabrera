package broker.connectionHandler;

import broker.ConnectionHelpers;
import common.Connection;
import protos.Kafka;
import utils.Demo;

import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 4/16/22
 * @project dsd-pub-sub
 * <p>
 * State behaviors under "normal" running state. All requests
 * are expected to be handled
 */
public class RunningState extends ConnectionHandlerState {

    RunningState(ConnectionHandler connectionHandler) {
        super(connectionHandler);
    }

    // Handles producer publishing
    @Override
    public void handleProducerPublish(Connection conn, Kafka.Record record) {
        waitWhileSyncing();

        if (record.getRole().equals(Kafka.Record.Role.PRODUCER)) {
            connectionHandler.addProducer(record.getNodeId(), conn.getHostname(), record.getPort());
            Demo.printReplication("Data from " + record.getRole() + ":" + record.getTimestamp());
        }

        if (record.getRole().equals(Kafka.Record.Role.BROKER))
            Demo.printReplication("[BROKER] Receiving replicated data:" + record.getTimestamp());

        connectionHandler.storeRecord(record);
        connectionHandler.sendToPushBasedConsumers(record);
        connectionHandler.sendToReplicas(record);
        ConnectionHelpers.ack(conn, record);
    }

    // Handles consumer polling segments
    @Override
    public void handleConsumerPoll(Connection conn, Kafka.Record record) {
        Kafka.Record.Role role = record.getRole();
        if (role.equals(Kafka.Record.Role.CONSUMER)) {
            connectionHandler.addConsumer(record.getNodeId(), conn.getHostname(), record.getPort());
        }
        connectionHandler.sendSegment(conn, record);
    }

    // Handles consumer (push based) subscription
    @Override
    public void handleConsumerSubscribe(Connection conn, Kafka.Record record) {
        if (record.getRole().equals(Kafka.Record.Role.CONSUMER))
            connectionHandler.addConsumer(record.getNodeId(), conn.getHostname(), conn.getRemotePort());

        connectionHandler.subscribeConsumer(conn, record.getTopic());
    }

    // Handles broker sync request
    @Override
    void handleBrokerSync(Connection conn, Kafka.Record record) {
        connectionHandler.handleSyncRequest(conn, record);
    }

    // Waits (non-blocking) until broker has finished
    // syncing with another broker
    private void waitWhileSyncing() {
        boolean brokerIsSyncing = connectionHandler.isSyncing.get();
        while (brokerIsSyncing) {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            long delayMs = 100L;
            Future<Boolean> futureSyncState = scheduler.schedule(
                    () -> connectionHandler.isSyncing.get(),
                    delayMs,
                    TimeUnit.MILLISECONDS
            );

            try {
                brokerIsSyncing = futureSyncState.get();
            } catch (ExecutionException | InterruptedException e) {
                // not handling this
            }
        }
    }
}
