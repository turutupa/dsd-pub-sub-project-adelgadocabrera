import broker.Broker;
import common.Properties;
import common.SerializeableItems;
import consumer.Consumer;
import models.ConsumerRecord;
import models.ProducerRecord;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import producer.Producer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alberto Delgado on 3/5/22
 * @project dsd-pub-sub
 */
public class AppTest {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 5000;
    private static final String TOPIC = "images";
    private static final int ID = 0;

    @Test
    @DisplayName("should receive record after running bunch of consumers and one producer")
    public void testApp() throws InterruptedException {
        runBroker();
        new Thread(() -> runPushConsumer("A")).start();
        Thread.sleep(200);
        runProducer(0, 1000);
        runPollConsumer("B");
    }

    private static void runBroker() {
        Broker broker = new Broker(ID, PORT);
        Thread t = new Thread(broker);
        t.start();
    }

    private static void runProducer(int start, int end) {
        Properties props = new Properties();
        props.put("hostname", HOSTNAME);
        props.put("port", String.valueOf(PORT));
        props.put("key.serializer", SerializeableItems.STRING.name());
        props.put("value.serializer", SerializeableItems.STRING.name());
        Producer<String, String> producer = new Producer<>(props);

        Thread t = new Thread(() -> {
            for (int i = start; i <= end; i++) {
                String key = String.valueOf(i);
                String value = "Alberto " + i + " ";
                producer.publish(new ProducerRecord<>(TOPIC, key, value));
            }
        });
        t.start();
    }

    private static void runPushConsumer(String name) {
        Properties props = new Properties();
        props.put("hostname", HOSTNAME);
        props.put("port", String.valueOf(PORT));
        props.put("key.deserializer", SerializeableItems.STRING.name());
        props.put("value.deserializer", SerializeableItems.STRING.name());
        props.put("poll.method.consumer", "push.consumer");

        Consumer<String, String> consumer = new Consumer<>(props);
        consumer.subscribe(List.of(TOPIC));

        while (true) {
            ArrayList<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (record == null) continue;
                Assertions.assertNotNull(record);
                Assertions.assertInstanceOf(ConsumerRecord.class, record);
                Assertions.assertNotNull(record.getOffset());
                Assertions.assertNotNull(record.getValue());
            }
        }
    }

    private static void runPollConsumer(String name) {
        Properties props = new Properties();
        props.put("hostname", HOSTNAME);
        props.put("port", String.valueOf(PORT));
        props.put("key.deserializer", SerializeableItems.STRING.name());
        props.put("value.deserializer", SerializeableItems.STRING.name());
        props.put("poll.method.consumer", "poll.consumer");
        props.put("timeout.consumer", "50");

        Consumer<String, String> consumer = new Consumer<>(props);
        consumer.subscribe(List.of(TOPIC));

        while (true) {
            ArrayList<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (record == null) continue;
                Assertions.assertNotNull(record);
                Assertions.assertInstanceOf(ConsumerRecord.class, record);
                Assertions.assertNotNull(record.getOffset());
                Assertions.assertNotNull(record.getValue());
                return;
            }
        }
    }
}
