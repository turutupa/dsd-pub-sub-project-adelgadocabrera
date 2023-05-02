package broker;

import common.Connection;
import protos.Kafka;

import java.io.IOException;

/**
 * @author Alberto Delgado on 4/18/22
 * @project dsd-pub-sub
 * <p>
 * Helpers to avoid send data to remote node
 */
public class ConnectionHelpers {

    // Sends kafa record
    public static boolean sendRecord(Connection conn, Kafka.Record record) {
        byte[] protoBytes = record.toByteArray();

        try {
            conn.send(protoBytes);
            return true;
        } catch (IOException e) {
            // Something went wrong. Perhaps consumer closed request.
            // Stop sending data and wait for another request.
            return false;
        }
    }

    // ACKs kafka record
    public static void ack(Connection conn, Kafka.Record record) {
        Kafka.Record ack = Kafka.Record.newBuilder()
                .setKey(record.getKey())
                .build();

        sendRecord(conn, ack);
    }
}
