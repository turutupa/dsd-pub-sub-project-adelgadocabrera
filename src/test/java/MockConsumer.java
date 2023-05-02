import common.Properties;
import common.SerializeableItems;
import consumer.Consumer;
import models.ConsumerRecord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alberto Delgado on 3/11/22
 * @project dsd-pub-sub
 */
public class MockConsumer {
    private static void runConsumer() {
        Properties props = new Properties();
        props.put("hostname", Mock.HOSTNAME);
        props.put("port", String.valueOf(Mock.PORT));
        props.put("key.deserializer", SerializeableItems.STRING.name());
        props.put("value.deserializer", SerializeableItems.STRING.name());
        props.put("poll.method.consumer", "poll.consumer");
        props.put("timeout.consumer", "50");

        Consumer<String, String> consumer = new Consumer<>(props);
        consumer.subscribe(List.of(Mock.TOPIC));

        ArrayList<ConsumerRecord<String, String>> allData = new ArrayList<>();
        while (allData.size() < 50) {
            ArrayList<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(record.toString());
                allData.add(record);
            }
        }
        consumer.close();
    }
}
