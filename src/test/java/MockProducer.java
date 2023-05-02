import common.Properties;
import common.SerializeableItems;
import models.ProducerRecord;
import producer.Producer;

/**
 * @author Alberto Delgado on 3/11/22
 * @project dsd-pub-sub
 */
public class MockProducer {
    private static void runProducer() {
        Properties props = new Properties();
        props.put("hostname", Mock.HOSTNAME);
        props.put("port", String.valueOf(Mock.PORT));
        props.put("key.serializer", SerializeableItems.STRING.name());
        props.put("value.serializer", SerializeableItems.STRING.name());
        Producer<String, String> producer = new Producer<>(props);

        Thread t = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                String key = String.valueOf(i);
                String value = String.valueOf(i);
                producer.publish(new ProducerRecord<>(Mock.TOPIC, key, value));
            }
        });
        t.start();
    }
}
