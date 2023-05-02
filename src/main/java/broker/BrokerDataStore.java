package broker;

import common.Connection;
import protos.Kafka;

import java.util.*;

import static broker.ConnectionHelpers.sendRecord;

/**
 * @author Alberto Delgado on 4/17/22
 * @project dsd-pub-sub
 * <p>
 * Handles the internal broker data. Additionally, has methods
 * to add and extract data.
 */
public class BrokerDataStore {
    final int MAX_CACHED_SIZE = Constants.BROKER_DATASTORE_CACHE_CAPACITY; // number of records to be stored until persisted
    final Map<String, List<Kafka.Record>> topics = new HashMap<>(); // temporarily stored data before persistence
    final SegmentHandler segmentHandler;

    public BrokerDataStore(SegmentHandler segmentHandler) {
        this.segmentHandler = segmentHandler;
    }

    public Set<String> getTopics() {
        synchronized (topics) {
            return topics.keySet();
        }
    }

    // Adds record to in-memory. If cache is full
    // it persists it locally;
    public void storeRecord(Kafka.Record record) {
        synchronized (topics) {
            String topic = record.getTopic();

            List<Kafka.Record> requestedTopic;
            if (topics.containsKey(topic)) {
                requestedTopic = topics.get(record.getTopic());

                for (Kafka.Record r : requestedTopic) {
                    // TODO: optimize performance
                    if (r.getTimestamp() == record.getTimestamp()) return;
                }
            } else {
                requestedTopic = new ArrayList<>();
                topics.put(record.getTopic(), requestedTopic);
            }
            requestedTopic.add(record);

            // lock only records for requested topic
            if (requestedTopic.size() == MAX_CACHED_SIZE) {
                System.out.println("[BROKER DATA STORE] Cache full. Persisting locally.");
                segmentHandler.add(record.getTopic(), requestedTopic);
                requestedTopic.clear();
            }
        }
    }

    // Receives an offset indicating how much it is desired from that topic.
    // It will read the segment and send all the data from that offset onwards
    public void sendSegment(Connection conn, Kafka.Record record) {
        sendSegment(conn, record.getTopic(), record.getOffset(), record.getRole());
    }

    // Receives an offset indicating how much it is desired from that topic.
    // It will read the segment and send all the data from that offset onwards
    private void sendSegment(Connection conn, String topic, int offset, Kafka.Record.Role role) {
        if (conn == null) return;
        if (conn.isClosed()) return; // if socket closed don't even start;
        List<Kafka.Record> requestedRecords = segmentHandler.get(topic, offset);

        if (requestedRecords.size() == 0) {
            Kafka.Record record = Kafka.Record.newBuilder()
                    .setTopic(Constants.SEGMENT_HANDLER_EMPTY)
                    .build();
            sendRecord(conn, record);
            return;
        }

        for (Kafka.Record requestedRecord : requestedRecords)
            sendRecord(conn, requestedRecord);

        if (role == null) return;
        if (role.equals(Kafka.Record.Role.CONSUMER)) {
            Kafka.Record eot = Kafka.Record.newBuilder()
                    .setTopic(Constants.EOT)
                    .build();
            sendRecord(conn, eot);
        }
    }

    // Sends all the data accumulated in the broker to the requester
    // Including in-memory data
    public void syncDataStore(Connection conn, Kafka.Record record) {
        System.out.println("[BROKER DATASTORE] Initiating sync with broker");
        synchronized (topics) {
            Set<String> topicsList = new HashSet();
            // copy of topics
            for (String topic : topics.keySet())
                topicsList.add(topic);

            // First sync all records that the requester broker
            // already has.
            List<Kafka.Record> recordList = record.getRecordsList();
            for (Kafka.Record rec : recordList) {
                sendSegment(conn, rec);
                topicsList.remove(rec.getTopic());
            }

            // Then sync all persisted records that requester doesn't have
            for (String topic : topicsList) {
                sendSegment(conn, topic, -1, null);
                topicsList.remove(topic);
            }

            // Then sync all remaining records that may be in memory
            // but not yet persisted
            for (String topic : topics.keySet()) {
                List<Kafka.Record> requestedTopic;
                requestedTopic = topics.get(topic);

                synchronized (requestedTopic) {
                    for (Kafka.Record rec : requestedTopic) {
                        sendRecord(conn, rec);
                    }
                }
            }
        }
    }
}
