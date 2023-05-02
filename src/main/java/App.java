import broker.Broker;
import common.Properties;
import consumer.Consumer;
import models.*;
import producer.Producer;
import utils.ConfigReader;
import utils.ConfigReader.Config;
import utils.Demo;
import utils.KaggleParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alberto Delgado on 3/5/22
 * @project dsd-pub-sub
 * <p>
 * Runs the main logic of the application:
 * - Create dynamically a broker/producer/consumer
 * <p>
 * - In case of broker it will simply act as a generic broker.
 * It expects data from producers or requests from consumers.
 * <p>
 * - In case of producer it goes through Kaggle data
 * and filters the data relevant to the specified topic.
 * Example: if images topic specified it will go through
 * kaggle data looking for images and will send those.
 * <p>
 * - In case of consumer it will poll/push for the specified
 * topic.
 */
public class App {
    // hard-coded filenames
    private static final String FILENAME = "access.log";
    private static final String CONFIG_FILENAME = "config.json";
    // time between each record sent by the producer to simulate the clicks
    private static final int SIMULATED_WAITING_TIME_MS = 50;

    /**
     * Creates the objects specified in the config file and
     * runs them.
     *
     * @param args
     */
    public static void main(String[] args) {
        Demo.parseArgs(args);

        Config config = ConfigReader.get(CONFIG_FILENAME);
        System.out.println(config.toString());

        BrokerConfig brokerConfig = config.brokerConfig;
        BrokerConfig leaderConfig = config.leaderConfig;
        ProducerConfig producerConfig = config.producerConfig;
        List<ConsumerConfig> consumersConfig = config.consumersConfig;


        if (brokerConfig != null) {
            createBroker(brokerConfig, leaderConfig);
            // make some time for broker to boot up
            // before producers and consumers
            if (producerConfig != null || consumersConfig != null)
                Demo.printAndDelay();
        }

        if (producerConfig != null)
            createProducer(producerConfig);

        if (consumersConfig != null)
            for (ConsumerConfig consumerConfig : consumersConfig)
                createConsumer(consumerConfig);
    }

    /**
     * Creates a Broker object given a broker config
     *
     * @param brokerConfig
     */
    private static void createBroker(BrokerConfig brokerConfig, BrokerConfig leaderConfig) {
        if (brokerConfig == null) return;

        int brokerPort = brokerConfig.brokerPort;
        int zkPort = brokerConfig.zkPort;
        if (brokerPort == 0 || zkPort == 0) {
            System.out.println("[BROKER] You must specify a port.");
        } else {
            System.out.println("[BROKER] Creating broker in port " + brokerPort + ".");
            runBroker(brokerConfig, leaderConfig);
        }
    }

    /**
     * Creates a Producer object given a producer config
     *
     * @param producerConfig
     */
    private static void createProducer(ProducerConfig producerConfig) {
        if (producerConfig == null) return;

        int id = producerConfig.id;
        String hostname = producerConfig.hostname;
        int port = producerConfig.port;
        int localPort = producerConfig.localPort;
        String keySerializer = producerConfig.keySerializer;
        String valueSerializer = producerConfig.valueSerializer;
        List<String> topics = producerConfig.topics;
        String tag = "[PRODUCER] ";
        String end = " Aborting producer creation.";

        if (hostname == null) {
            System.out.println(tag + "You must specify a hostname." + end);
        } else if (port == 0) {
            System.out.println(tag + "You must specify a port number." + end);
        } else if (localPort == 0) {
            System.out.println(tag + "You must specify a local port number." + end);
        } else if (keySerializer == null) {
            System.out.println(tag + "You must specify a key serializer." + end);
        } else if (valueSerializer == null) {
            System.out.println(tag + "You must specify a value serializer." + end);
        } else if (topics.isEmpty()) {
            System.out.println(tag + "You must specify at least one topic" + end);
        } else {
            System.out.println(tag + "New producer: \n" + producerConfig);
            runProducer(id,
                    hostname,
                    port,
                    localPort,
                    topics,
                    keySerializer,
                    valueSerializer);
        }
    }

    /**
     * Creates a Consumer object given a consumer config
     *
     * @param consumerConfig
     */
    private static void createConsumer(ConsumerConfig consumerConfig) {
        if (consumerConfig == null) return;

        String hostname = consumerConfig.hostname;
        int port = consumerConfig.port;
        int localPort = consumerConfig.localPort;
        String keySerializer = consumerConfig.keyDeserializer;
        String valueSerializer = consumerConfig.valueDeserializer;
        List<String> topics = consumerConfig.topics;
        String pollMethodConsumer = consumerConfig.pollMethodConsumer;
        int timeout = consumerConfig.timeout;
        int offset = consumerConfig.offset;

        String tag = "[CONSUMER] ";
        String end = " Aborting consumer creation.";

        if (hostname == null) {
            System.out.println(tag + "You must specify a hostname." + end);
        } else if (port == 0) {
            System.out.println(tag + "You must specify a port number." + end);
        } else if (localPort == 0) {
            System.out.println(tag + "You must specify a local port number." + end);
        } else if (keySerializer == null) {
            System.out.println(tag + "You must specify a key serializer." + end);
        } else if (valueSerializer == null) {
            System.out.println(tag + "You must specify a value serializer." + end);
        } else if (topics.isEmpty()) {
            System.out.println(tag + "You must specify at least one topic" + end);
        } else if (pollMethodConsumer == null) {
            System.out.println(tag + "You must specify a polling method" + end);
        } else if (timeout == 0) {
            System.out.println(tag + "You must specify a timeout" + end);
        } else {
            System.out.println(tag + "New consumer: \n" + consumerConfig);
            runConsumer(
                    consumerConfig.id,
                    hostname,
                    port,
                    localPort,
                    topics,
                    keySerializer,
                    valueSerializer,
                    pollMethodConsumer,
                    timeout,
                    offset
            );
        }
    }

    /**
     * Initiates a Broker
     */
    private static void runBroker(BrokerConfig brokerConfig, BrokerConfig leaderConfig) {
        Broker broker = new Broker(brokerConfig.id, brokerConfig.brokerPort);
        broker.addZooKeeper(brokerConfig.zkPort);

        if (leaderConfig != null) {
            // Add leader
            broker.addNode(new Node(leaderConfig.id,
                    leaderConfig.hostname,
                    leaderConfig.zkPort,
                    leaderConfig.brokerPort));
        }

        Thread t = new Thread(broker);
        t.start();

        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }

    /**
     * Initiates the Producer logic
     *
     * @param hostname
     * @param port
     * @param topics
     * @param keySerializer
     * @param valueSerializer
     */
    private static void runProducer(
            int id,
            String hostname,
            int port,
            int localPort,
            List<String> topics,
            String keySerializer,
            String valueSerializer
    ) {
        Properties props = new Properties();
        props.put("id", String.valueOf(id));
        props.put("hostname", hostname);
        props.put("port", String.valueOf(port));
        props.put("local.port", String.valueOf(localPort));
        props.put("key.serializer", keySerializer);
        props.put("value.serializer", valueSerializer);
        Producer<String, String> producer = new Producer<>(props);
        producer.run();

        for (String topic : topics) {
            Thread t = new Thread(() -> {
                KaggleParser kp = KaggleParser.from(FILENAME, topic);
                String value;
                int j = 0;
                while ((value = kp.next()) != null) {
                    Demo.printAndDelay("Publishing record " + j);
                    producer.publish(new ProducerRecord<>(topic, String.valueOf(j++), value));
                }
            });
            t.start();
        }
    }

    /**
     * Initiates the Consumer logic
     *
     * @param hostname
     * @param port
     * @param topics
     * @param keyDeserializer
     * @param valueDeserializer
     * @param pollMethodConsumer
     */
    private static void runConsumer(
            int id,
            String hostname,
            int port,
            int localPort,
            List<String> topics,
            String keyDeserializer,
            String valueDeserializer,
            String pollMethodConsumer,
            int timeout,
            int offset
    ) {
        Properties props = new Properties();
        props.put("id", String.valueOf(id));
        props.put("hostname", hostname);
        props.put("port", String.valueOf(port));
        props.put("local.port", String.valueOf(localPort));
        props.put("key.deserializer", keyDeserializer);
        props.put("value.deserializer", valueDeserializer);
        props.put("poll.method.consumer", pollMethodConsumer);
        props.put("timeout.consumer", String.valueOf(timeout));
        props.put("offset.consumer", String.valueOf(offset));

        Consumer<String, String> consumer = new Consumer<>(props);
        consumer.run();
        consumer.subscribe(topics);

        new Thread(() -> {
            while (true) {
                ArrayList<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, String> record : records) {
                    if (record == null) continue;
                    System.out.println("[CONSUMER] " + record + "\n");
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}