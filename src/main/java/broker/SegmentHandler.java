package broker;

import com.google.protobuf.ByteString;
import protos.Kafka;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alberto Delgado on 3/11/22
 * @project dsd-pub-sub
 * <p>
 * This class handles all segments. Segments are files that contain
 * data for a certain topic. The idea would be to be able to create n segments
 * for each topic, for example of 500MB of size per segment, but for simplicity
 * each topic will have one unique segment.
 */
public class SegmentHandler {
    private String SEGMENTS_DIR = "./segments/";
    // stores the writers for each segment
    private Map<String, SegmentWriter> segmentWriters = new HashMap<>();
    // stores the offsets for each record (in each segment). The key of the TreeMap
    // is the record offset start position and the value contains the record offset
    // end position
    private Map<String, TreeMap<Integer, SegmentOffset>> segmentOffsets = new HashMap<>();
    // lock to access the segmentOffsets Treemap
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    SegmentHandler(String dir) {
        SEGMENTS_DIR += dir;
    }

    /**
     * Gets last offsets. Comes handy in order to request sync with other brokers.
     *
     * @return
     */
    Set<Kafka.Record> getOffsets() {
        lock.readLock().lock();
        Set<Kafka.Record> offsets = new HashSet<>();
        try {
            for (String topic : segmentOffsets.keySet()) {
                TreeMap<Integer, SegmentOffset> segmentTopic = segmentOffsets.get(topic);
                Map.Entry<Integer, SegmentOffset> lastEntry = segmentTopic.lastEntry();
                Kafka.Record record = Kafka.Record.newBuilder()
                        .setTopic(topic)
                        .setOffset(lastEntry.getValue().offset)
                        .build();

                offsets.add(record);
            }
        } finally {
            lock.readLock().unlock();
            return offsets;
        }
    }

    /**
     * Gets all the records available for a certain topic from a specified
     * offset onwards
     *
     * @param topic
     * @param requestedOffset
     * @return
     */
    List<Kafka.Record> get(String topic, int requestedOffset) {
        SegmentWriter segmentWriter = getWriter(topic);
        segmentWriter.lock.readLock().lock();
        List<Kafka.Record> data = new ArrayList<>();
        try {
            byte[] segmentData = readSegment(topic);
            TreeMap<Integer, SegmentOffset> offsets = segmentOffsets.get(topic);
            Map.Entry<Integer, SegmentOffset> lastOffset = offsets.higherEntry(requestedOffset);

            while (lastOffset != null) {
                int lastOffsetStart = lastOffset.getKey();
                SegmentOffset segmentOffset = lastOffset.getValue();
                int lastOffsetEnd = segmentOffset.offset;
                long timestamp = segmentOffset.timestamp;
                ByteString key = segmentOffset.key;

                byte[] recordValue = new byte[lastOffsetEnd - lastOffsetStart + 1];

                // read bytes from persisted segment data
                int j = 0;
                for (int i = lastOffsetStart; i <= lastOffsetEnd; i++) {
                    recordValue[j++] = segmentData[i];
                }

                Kafka.Record record = Kafka.Record.newBuilder()
                        .setTopic(topic)
                        .setKey(key)
                        .setValue(ByteString.copyFrom(recordValue))
                        .setOffset(lastOffsetEnd)
                        .setTimestamp(timestamp)
                        .build();

                data.add(record);

                lastOffset = offsets.higherEntry(lastOffsetEnd);
            }
        } catch (IOException e) {
            System.out.println("[SEGMENT HANDLER] Topic requested has yet not been persisted");
        } finally {
            segmentWriter.lock.readLock().unlock();
            return data;
        }
    }

    /**
     * Adds a new record to a segment file. It also has to update the offsets of the record
     *
     * @param topic
     * @param records
     * @throws FileNotFoundException
     */
    void add(String topic, List<Kafka.Record> records) {
        SegmentWriter segmentWriter = getWriter(topic);
        segmentWriter.lock.writeLock().lock();
        try {
            for (Kafka.Record record : records) {
                byte[] value = record.getValue().toByteArray();
                segmentWriter.write(value);
                updateOffset(topic, record.getKey(), value.length, record.getTimestamp());
            }
        } finally {
            segmentWriter.lock.writeLock().unlock();
        }
    }

    /**
     * Reads a segment and returns it in bytes
     *
     * @param topic
     * @return
     * @throws IOException
     */
    private byte[] readSegment(String topic) throws IOException {
        Path path = Paths.get(SEGMENTS_DIR + topic);
        return Files.readAllBytes(path);
    }

    /**
     * Helper method to get the writer for a topic. If non-extant then
     * it creates a new one.
     *
     * @param topic
     * @return
     * @throws FileNotFoundException
     */
    private synchronized SegmentWriter getWriter(String topic) {
        SegmentWriter segmentWriter = segmentWriters.get(topic);
        if (segmentWriter == null) {
            segmentWriter = new SegmentWriter(topic);
            segmentWriters.put(topic, segmentWriter);
        }

        return segmentWriter;
    }

    /**
     * Logic to update the offset of a new record.
     *
     * @param topic
     * @param key
     * @param recordLength
     * @param timestamp
     */
    private synchronized void updateOffset(String topic, ByteString key, int recordLength, long timestamp) {
        lock.writeLock().lock();
        try {
            TreeMap<Integer, SegmentOffset> topicOffsets = segmentOffsets.get(topic);

            if (topicOffsets == null) {
                topicOffsets = new TreeMap<>();
                segmentOffsets.put(topic, topicOffsets);
            }

            Map.Entry<Integer, SegmentOffset> lastOffset = topicOffsets.lastEntry();

            if (lastOffset == null) {
                topicOffsets.put(0, new SegmentOffset(key, recordLength - 1, timestamp));
            } else {
                int lastOffsetEnds = lastOffset.getValue().offset;
                int newOffsetStarts = lastOffsetEnds + 1;
                int newOffsetEnds = newOffsetStarts + recordLength - 1;
                topicOffsets.put(newOffsetStarts, new SegmentOffset(key, newOffsetEnds, timestamp));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Helper Class to store Offset information
     */
    private class SegmentOffset {
        final ByteString key;
        final int offset; // this refers to ending offset
        final long timestamp;

        SegmentOffset(ByteString key, int offset, long timestamp) {
            this.key = key;
            this.offset = offset;
            this.timestamp = timestamp;
        }
    }

    /**
     * Writes data and holds a lock so only one thread
     * is writing at a time
     */
    private class SegmentWriter {
        public FileOutputStream writer;
        public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        SegmentWriter(String topic) {
            File dir = new File(SEGMENTS_DIR);
            if (!dir.exists()) {
                dir.mkdir();
                System.out.println("[SEGMENT HANDLER] Creating segments folder.");
            }

            try {
                // then create file output stream for that topic
                writer = new FileOutputStream(SEGMENTS_DIR + topic);
            } catch (FileNotFoundException e) {
                // dir is created so it won't fail
                e.printStackTrace();
            }
        }

        /**
         * Persists data to file
         *
         * @param data
         */
        private void write(byte[] data) {
            try {
                writer.write(data);
            } catch (IOException e) {
                System.err.println("[SEGMENT HANDLER] Something went wrong writing record to segment");
            }
        }
    }
}
