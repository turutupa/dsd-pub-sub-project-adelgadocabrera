For this project, you will implement a distributed publish/subscribe system similar to the original Kafka design.

---

# Required Functionality (worth up to 85 points)

<aside>
⚠️ To earn full credit for your demonstration you must have ***thorough***  logging that clearly demonstrates what is happening at each component of your system. It is recommended that you download the Apache Kafka implementation to see the kinds of logs it generates.

</aside>

## **producer.producer**

You will implement a `producer.producer` API that may be used by an application running on any node that publishes
messages to a broker. At minimum, your `producer.producer` will allow the application to do the following:

1. Connect to a `broker.broker`
2. Send data to the `broker.broker` by providing a `byte[]` containing the data and a `String` containing the topic.

Following is an example of how the [Kafka](https://kafka.apache.org/) API supports this functionality. You are
encouraged to download Kafka and play around with the real implementation!

```java
// Create a properties object that specifies where to find the broker
// and how to serialize the data
Properties props=new Properties();
        props.put("bootstrap.servers","localhost:9092");
        props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");

        String topic="my-topic"; //set the topic

// Create a producer using the properties specified
        producer.producer<String, String> producer=new KafkaProducer<>(props);

// Use the send method to publish records to the topic
// Records are of the form key->value
// In this example records look like {"1" -> "1"}, {"2" -> "2"}, and so on
        for(int i=1;i< 100;i++){
        producer.send(new models.ProducerRecord<String, String>(topic,Integer.toString(i),Integer.toString(i)));
        }

        producer.close();
```

It is *not* required that you implement the exact API described above. It is intended to give you a more clear idea of
how your API will be used. You may omit the use of the `Properties` object and simply pass the relevant information into
the `producer.producer` constructor. You may also omit the use of generic types and assume that the `send` method
accepts a `byte[]` of data. An example of a *suggested* simplified API is as follows:

```java
// Open a connection to the broker.broker by creating a new producer.producer object
String brokerLocation="localhost:9092";
        producer.producer producer=new producer.producer(brokerLocation);

// Set the data and topic
        byte[]data=...;
        String topic="my-topic";

// Send data
        producer.send(topic,data);
// Leave connection open until all data sent

// Close the connection 
        producer.close();
```

Your final demonstration must include at least three `producer.producer` applications running on three separate nodes.
Each application will mimic a front-end web server by *replaying* data from logs. Here are a couple of data sets that
you may use for this purpose:

- [https://www.kaggle.com/eliasdabbas/web-server-access-logs](https://www.kaggle.com/eliasdabbas/web-server-access-logs)
- https://github.com/logpai/loghub

## consumer.consumer

You will implement a `consumer.consumer` API that may be used by an application running on any node that consumes
messages from a broker. At minimum, your `consumer.consumer` will allow the application to do the following:

1. Connect to a `broker.broker`
2. Retrieve data from the `broker.broker` using a pull-based approach by specifying a topic of interest and a starting
   position in the message stream

Following is an example of how the Kafka API supports this functionality. In this example, the `subscribe` method is how
the consumer specifies one *or more topics* of interest and the `auto.offset.reset` property is how the consumer
specifies to start at the beginning of the message stream.

```java
// Create a properties object that specifies where to find the broker
// and how to serialize the data
Properties props=new Properties();
        props.setProperty("bootstrap.servers","localhost:9092");
        props.setProperty("group.id","test");
        props.setProperty("enable.auto.commit","false");
        props.setProperty("auto.offset.reset","earliest");
        props.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");

        String topic="my-topic";

// Create a consumer using the properties specified
        KafkaConsumer<String, String> consumer=new KafkaConsumer<>(props);

// Subscribe the consumer to a list of topics
        consumer.subscribe(Arrays.asList(topic));

// Forever...poll the next set of records from the consumer stream
        while(true){
        ConsumerRecords<String, String> records=consumer.poll(Duration.ofMillis(100));
        for(models.ConsumerRecord<String, String> record:records)
        System.out.printf("offset = %d, key = %s, value = %s%n",record.offset(),record.key(),record.value());
        }
        }
```

Recall that Kafka uses the log offset rather than a message ID to identify a position in the message stream. In your
solution, you may simplify this assumption and give each message a monotonically increasing integer ID. Below is a *
suggested* simplified API for your `consumer.consumer`.

```java
// Specify the location of the broker, topic of interest for this specific
// consumer object, and a starting position in the message stream.
String brokerLocation="localhost:9092";
        String topic="my-topic";
        int startingPosition=20;

// Connect to the consumer
        consumer.consumer consumer=new consumer.consumer(brokerLocation,topic,startingPosition);

// Continue to pull messages...forever
        while(true){
        byte[]message=consumer.poll(Duration.ofMillis(100));
        // do something with this data!
        }

// When forever finally finishes...
        consumer.close();
```

In a real application, your  `consumer.consumer` would do something with the data consumed. For demonstration purposes
you may just save it to a file.

Your final demonstration must include at least three `consumer.consumer`applications running on three separate nodes.
Your choice of topic(s) should fully demonstrate the features of your application.

## broker.broker

The `broker.broker` will accept an unlimited number of connection requests from producers and consumers. The
basic `broker.broker` implementation* will maintain a thread-safe, in-memory data structure that stores all messages.
The basic `broker.broker` will be stateless with respect to the `consumer.consumer` nodes.

The assignment description specifies the application API that you must implement. It is up to you to design the
communication protocol between the components of the system. An API-level method call will be translated into a message
that will be sent over a connection between `broker.broker` and either `producer.producer` or `consumer.consumer`.

**See below for possible additional features.*

# Additional Features

The following features may be implemented to get to the 100% mark and earn extra credit. Note that each option (
intentionally!) requires you to think carefully about how to design the functionality. I am happy to discuss your
proposed designs during office hour, and I do encourage you to share your designs with one another.

## Interoperability (5 points)

You may earn up to 5 additional points for implementing a solution that interoperates with another student’s solution. *
Each student **must** implement their own independent solution;* however, you will team up to develop the protocol used
for `producer.producer/broker.broker` communication and `broker.broker/consumer.consumer` communication. To earn full
credit, the team will sign up for a 30-minute demonstration slot and will demonstrate student 1’s `broker.broker` and
student 2’s `producer.producer/consumer.consumer` and vice versa.

For my own planning purposes, I would appreciate it if you would let me know your team as soon as you decide that you
are pursing this feature. You may later change your mind without penalty.

## Persist utils.Demo to Disk and Use Byte Offsets as Message IDs (5 points)

You may earn up to 5 additional points for saving the log to permanent storage and using byte offsets rather than
integer message IDs for identifying the appropriate consumer read location. For full credit, you will be expected to
implement the following design described in the original paper: *“For better performance, we flush the segment files to
disk only after a configurable number of messages have been published or a certain amount of time has elapsed. A message
is only exposed to the consumers after it is flushed.”*

## Push-based consumer.consumer (10 points)

You may earn up to 10 additional points for implementing the ability for a `consumer.consumer` to be push-based. This
option will require that the `broker.broker` be stateful. You will need to design and implement a mechanism for
a `consumer.consumer` to register to receive updates to a topic. The `broker.broker` will proactively push out new
messages to any registered consumers.

## Partitioning (10 points)

You may earn up to 10 additional points for implementing partitioning of topics. This does *not* require you to
implement replication (that’s your next project!); however, to earn full credit for this option you must demonstrate
with multiple instances of the `broker.broker` running on separate nodes. Each topic may have multiple partitions, and
each partition may be handled by a different `broker.broker`. When a new message is posted you will specify both the
topic and a key. Like in the real Kafka implementation, the key will be hashed to determine which `broker.broker` is
managing the partition for that <key, topic>. You will need to design the mechanism for directing a request to the
appropriate `broker.broker`. You may use ZooKeeper for this purpose, or you may implement a custom load balancer that is
essentially just another service that accepts a request containing a key and returns the node information of
the `broker.broker` that manages that partition.

## Other Additional Features

Other additional features, e.g., consumer.consumer Groups, may be considered. See me during office hour to propose your
ideas, and I will tell you how many points you will earn for a well designed and robustly implemented feature.

# Other Requirements

1. **Java:** Your solution must be implemented using Java. You may use maven, gradle, or any other dependency manager.
2. **Dependencies:** Your solution may not use any dependencies other than JUnit, protobuf, and gson unless explicitly
   approved by the instructor. If there is a library you wish to use send me a Slack DM, and I will consider your
   request.
3. **Thorough Logging:** Your demonstration ***must*** include thorough logging that clearly demonstrates what is
   happening in your solution.

# **Hints and Suggestions**

***Start early!*** Projects are designed to take roughly 20 hours/week of your time for the duration of the assignment.
It would not be surprising for this assignment to take ~80 hours in total. Though the basic required features do not
require a significant amount of complicated logic, there are lots of moving pieces to this one, and you are certain to
encounter bugs that cannot be easily solved.

# Submission

1. Your complete solution must be committed to your GitHub repository prior to the deadline of **1pm on 3/24/2022**.
2. *Interactive grading is required for this assignment.* A signup sheet will be provided prior to the deadline. You ***
   must*** demonstrate your solution to an instructor during one of the available signup times in order to earn credit
   for this assignment. Failure to complete a demonstration will result in a score of 0.

## Rubric

<div style=""><table style="margin: 8px 18px 18px 8px;"><div data-block-id="bcb085b9-69c3-437b-a546-215135e7b721" class="notion-selectable notion-table-block notion-table-tbody-selectable" style="position: relative;"><tbody><tr class="notion-table-row"><td style="background: rgb(247, 246, 243); font-weight: 500; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Points</div></div></td><td style="background: rgb(247, 246, 243); font-weight: 500; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Category</div></div></td><td style="background: rgb(247, 246, 243); font-weight: 500; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Description</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">utils.Demo - Basic Functionality</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Solution behaves as expected with three Producers, three Consumers, and one broker.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">5</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">utils.Demo - Logging</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">utils.Demo output clearly demonstrates functionality of all components of system.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">5</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">utils.Demo - Instructor Questions</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Instructor questions during demonstration are answered correctly and thoroughly.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Implementation - producer.Producer</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">producer.Producer API, communication protocol, and test application are implemented correctly and robustly.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10 </div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Implementation - consumer.Consumer</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">consumer.Consumer API, communication protocol, and test application are implemented correctly and robustly.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">15</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Implementation - broker.Broker</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">broker.Broker communication protocol, data structure, and logic are implemented correctly and robustly.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">15</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Additional Features</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Design and implementation of additional features.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Design - APIs</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">This is a <span style="font-style:italic" data-token-index="1" class="notion-enable-hover">subjective</span> assessment of the design of your APIs and framework.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Design - Code Quality</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">This is a <span style="font-style:italic" data-token-index="1" class="notion-enable-hover">subjective</span> assessment of the quality of your solution. Make sure you apply good Java practices and solid design principles. Points will be deducted for failing to follow the class <a href="/e6aec203ec06464c88db4c51f5befdc2" style="cursor:pointer;color:inherit;word-wrap:break-word;font-weight:500;text-decoration:inherit" class="notion-link-token notion-enable-hover" target="_blank" rel="noopener noreferrer" data-token-index="3"><span style="border-bottom:0.05em solid;border-color:rgba(55,53,47,0.4);opacity:0.7" class="link-annotation-e3c93d05-e624-47a4-9545-ab9bed475d8d-14953939">Java Style Guidelines</span></a>.</div></div></td></tr><tr class="notion-table-row"><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 119px; max-width: 119px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">10</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Test harness</div></div></td><td style="color: inherit; fill: inherit; border: 1px solid rgb(233, 233, 231); position: relative; vertical-align: top; min-width: 120px; max-width: 240px; min-height: 32px;"><div class="notion-table-cell"><div class="notion-table-cell-text" spellcheck="true" placeholder=" " data-content-editable-leaf="true" style="max-width: 100%; width: 100%; white-space: pre-wrap; word-break: break-word; caret-color: rgb(55, 53, 47); padding: 7px 9px; background-color: transparent; font-size: 14px; line-height: 20px;" contenteditable="false">Yes, you must test this and all projects!</div></div></td></tr></tbody></div></table></div>
