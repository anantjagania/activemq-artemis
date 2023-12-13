# Examples

The Apache ActiveMQ Artemis distribution comes with over 90 run out-of-the-box
examples demonstrating many of the features.

The examples are available in both the binary and source distribution under the
`examples` directory. Examples are split by the following source tree:

- features - Examples containing broker specific features.
    - clustered - examples showing load balancing and distribution capabilities.
    - ha - examples showing failover and reconnection capabilities.
    - perf - examples allowing you to run a few performance tests on the server
    - standard - examples demonstrating various broker features.
    - sub-modules - examples of integrated external modules.
- protocols - Protocol specific examples
    - amqp
    - mqtt
    - openwire
    - stomp

## Running the Examples

To run any example, simply `cd` into the appropriate example directory and type
`mvn verify` or `mvn install` (For details please read the readme.html in each
example directory).

You can use the profile `-Pexamples` to run multiple examples under any example
tree.

For each example, you will have a created server under `./target/server0` (some
examples use more than one server).

You have the option to prevent the example from starting the server (e.g. if
you want to start the server manually) by simply specifying the `-PnoServer`
profile, e.g.:

```sh
# running an example without running the server
mvn verify -PnoServer
```

Also under `./target` there will be a script repeating the commands to create
each server. Here is the `create-server0.sh` generated by the `Queue` example.
This is useful to see exactly what command(s) are required to configure the
server(s).

```sh
# These are the commands used to create server0
/myInstallDirectory/apache-artemis/bin/artemis create --allow-anonymous --silent --force --no-web --user guest --password guest --role guest --port-offset 0 --data ./data --allow-anonymous --no-autotune --verbose /myInstallDirectory/apache-artemis-1.1.0/examples/features/standard/queue/target/server0
```

Several examples use UDP clustering which may not work in your environment by
default. On linux the command would be:
 
```sh
route add -net 224.0.0.0 netmask 240.0.0.0 dev lo
```
 
This command should be run as root. This will redirect any traffic directed to
`224.0.0.0` to the loopback interface.  On Mac OS X, the command is slightly
different:

```sh
sudo route add 224.0.0.0 127.0.0.1 -netmask 240.0.0.0
```

All the examples use the [Maven plugin](maven-plugin.md), which can be useful
for running your test servers as well.

This is the common output when running an example. On this case taken from the
`Queue` example:

```sh
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building ActiveMQ Artemis JMS Queue Example 2.5.0
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-maven) @ queue ---
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-java) @ queue ---
[INFO]
[INFO] --- maven-remote-resources-plugin:1.5:process (process-resource-bundles) @ queue ---
[INFO]
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ queue ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] Copying 3 resources
[INFO]
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ queue ---
[INFO] Nothing to compile - all classes are up to date
[INFO]
[INFO] --- maven-checkstyle-plugin:2.17:check (default) @ queue ---
[INFO]
[INFO] --- apache-rat-plugin:0.12:check (default) @ queue ---
[INFO] RAT will not execute since it is configured to be skipped via system property 'rat.skip'.
[INFO]
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ queue ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /home/user/activemq-artemis/examples/features/standard/queue/src/test/resources
[INFO] Copying 3 resources
[INFO]
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ queue ---
[INFO] No sources to compile
[INFO]
[INFO] --- maven-surefire-plugin:2.18.1:test (default-test) @ queue ---
[INFO]
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ queue ---
[INFO] Building jar: /home/user/activemq-artemis/examples/features/standard/queue/target/queue-2.5.0.jar
[INFO]
[INFO] --- maven-site-plugin:3.3:attach-descriptor (attach-descriptor) @ queue ---
[INFO]
[INFO] >>> maven-source-plugin:2.2.1:jar (attach-sources) > generate-sources @ queue >>>
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-maven) @ queue ---
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-java) @ queue ---
[INFO]
[INFO] <<< maven-source-plugin:2.2.1:jar (attach-sources) < generate-sources @ queue <<<
[INFO]
[INFO]
[INFO] --- maven-source-plugin:2.2.1:jar (attach-sources) @ queue ---
[INFO] Building jar: /home/user/activemq-artemis/examples/features/standard/queue/target/queue-2.5.0-sources.jar
[INFO]
[INFO] >>> maven-source-plugin:2.2.1:jar (default) > generate-sources @ queue >>>
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-maven) @ queue ---
[INFO]
[INFO] --- maven-enforcer-plugin:1.4:enforce (enforce-java) @ queue ---
[INFO]
[INFO] <<< maven-source-plugin:2.2.1:jar (default) < generate-sources @ queue <<<
[INFO]
[INFO]
[INFO] --- maven-source-plugin:2.2.1:jar (default) @ queue ---
[INFO]
[INFO] --- dependency-check-maven:1.4.3:check (default) @ queue ---
[INFO] Skipping dependency-check
[INFO]
[INFO] --- artemis-maven-plugin:2.5.0:create (create) @ queue ---
[INFO] Local       id: local
      url: file:///home/user/.m2/repository/
   layout: default
snapshots: [enabled => true, update => always]
 releases: [enabled => true, update => always]

[INFO] Entries.size 2
[INFO] ... key=project = MavenProject: org.apache.activemq.examples.broker:queue:2.5.0 @ /home/user/activemq-artemis/examples/features/standard/queue/pom.xml
[INFO] ... key=pluginDescriptor = Component Descriptor: role: 'org.apache.maven.plugin.Mojo', implementation: 'org.apache.activemq.artemis.maven.ArtemisCLIPlugin', role hint: 'org.apache.activemq:artemis-maven-plugin:2.5.0:cli'
role: 'org.apache.maven.plugin.Mojo', implementation: 'org.apache.activemq.artemis.maven.ArtemisCreatePlugin', role hint: 'org.apache.activemq:artemis-maven-plugin:2.5.0:create'
role: 'org.apache.maven.plugin.Mojo', implementation: 'org.apache.activemq.artemis.maven.ArtemisDependencyScanPlugin', role hint: 'org.apache.activemq:artemis-maven-plugin:2.5.0:dependency-scan'
role: 'org.apache.maven.plugin.Mojo', implementation: 'org.apache.activemq.artemis.maven.ArtemisClientPlugin', role hint: 'org.apache.activemq:artemis-maven-plugin:2.5.0:runClient'
---
Executing org.apache.activemq.artemis.cli.commands.Create create --allow-anonymous --silent --force --user guest --password guest --role guest --port-offset 0 --data ./data --allow-anonymous --no-web --no-autotune --verbose --aio /home/user/activemq-artemis/examples/features/standard/queue/target/server0 
Home::/home/user/activemq-artemis/examples/features/standard/queue/../../../../artemis-distribution/target/apache-artemis-2.5.0-bin/apache-artemis-2.5.0, Instance::null
Creating ActiveMQ Artemis instance at: /home/user/activemq-artemis/examples/features/standard/queue/target/server0

You can now start the broker by executing:

   "/home/user/activemq-artemis/examples/features/standard/queue/target/server0/bin/artemis" run

Or you can run the broker in the background using:

   "/home/user/activemq-artemis/examples/features/standard/queue/target/server0/bin/artemis-service" start

[INFO] ###################################################################################################
[INFO] create-server0.sh created with commands to reproduce server0
[INFO] under /home/user/activemq-artemis/examples/features/standard/queue/target
[INFO] ###################################################################################################
[INFO]
[INFO] --- artemis-maven-plugin:2.5.0:cli (start) @ queue ---
[INFO] awaiting server to start
server-out:     _        _               _
server-out:    / \  ____| |_  ___ __  __(_) _____
server-out:   / _ \|  _ \ __|/ _ \  \/  | |/  __/
server-out:  / ___ \ | \/ |_/  __/ |\/| | |\___ \
server-out: /_/   \_\|   \__\____|_|  |_|_|/___ /
server-out: Apache ActiveMQ Artemis 2.5.0
server-out:
server-out:
server-out:2018-03-13 09:06:37,980 WARN  [org.apache.activemq.artemis.core.server] AMQ222018: AIO was not located on this platform, it will fall back to using pure Java NIO. If your platform is Linux, install LibAIO to enable the AIO journal
server-out:2018-03-13 09:06:38,052 INFO  [org.apache.activemq.artemis.integration.bootstrap] AMQ101000: Starting ActiveMQ Artemis Server
[INFO] awaiting server to start
server-out:2018-03-13 09:06:38,123 INFO  [org.apache.activemq.artemis.core.server] AMQ221000: live Message Broker is starting with configuration Broker Configuration (clustered=false,journalDirectory=./data/journal,bindingsDirectory=./data/bindings,largeMessagesDirectory=./data/large-messages,pagingDirectory=./data/paging)
server-out:2018-03-13 09:06:38,146 INFO  [org.apache.activemq.artemis.core.server] AMQ221013: Using NIO Journal
server-out:2018-03-13 09:06:38,178 INFO  [org.apache.activemq.artemis.core.server] AMQ221057: Global Max Size is being adjusted to 1/2 of the JVM max size (-Xmx). being defined as 1,073,741,824
server-out:2018-03-13 09:06:38,197 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-server]. Adding protocol support for: CORE
server-out:2018-03-13 09:06:38,198 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-amqp-protocol]. Adding protocol support for: AMQP
server-out:2018-03-13 09:06:38,198 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-hornetq-protocol]. Adding protocol support for: HORNETQ
server-out:2018-03-13 09:06:38,198 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-mqtt-protocol]. Adding protocol support for: MQTT
server-out:2018-03-13 09:06:38,199 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-openwire-protocol]. Adding protocol support for: OPENWIRE
server-out:2018-03-13 09:06:38,199 INFO  [org.apache.activemq.artemis.core.server] AMQ221043: Protocol module found: [artemis-stomp-protocol]. Adding protocol support for: STOMP
server-out:2018-03-13 09:06:38,261 INFO  [org.apache.activemq.artemis.core.server] AMQ221034: Waiting indefinitely to obtain live lock
server-out:2018-03-13 09:06:38,262 INFO  [org.apache.activemq.artemis.core.server] AMQ221035: Live Server Obtained live lock
server-out:2018-03-13 09:06:38,386 INFO  [org.apache.activemq.artemis.core.server] AMQ221003: Deploying queue DLQ on address DLQ
server-out:2018-03-13 09:06:38,445 INFO  [org.apache.activemq.artemis.core.server] AMQ221003: Deploying queue ExpiryQueue on address ExpiryQueue
[INFO] awaiting server to start
server-out:2018-03-13 09:06:38,739 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started EPOLL Acceptor at 0.0.0.0:61616 for protocols [CORE,MQTT,AMQP,STOMP,HORNETQ,OPENWIRE]
server-out:2018-03-13 09:06:38,741 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started EPOLL Acceptor at 0.0.0.0:5445 for protocols [HORNETQ,STOMP]
server-out:2018-03-13 09:06:38,742 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started EPOLL Acceptor at 0.0.0.0:5672 for protocols [AMQP]
server-out:2018-03-13 09:06:38,744 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started EPOLL Acceptor at 0.0.0.0:1883 for protocols [MQTT]
server-out:2018-03-13 09:06:38,746 INFO  [org.apache.activemq.artemis.core.server] AMQ221020: Started EPOLL Acceptor at 0.0.0.0:61613 for protocols [STOMP]
server-out:2018-03-13 09:06:38,752 INFO  [org.apache.activemq.artemis.core.server] AMQ221007: Server is now live
server-out:2018-03-13 09:06:38,752 INFO  [org.apache.activemq.artemis.core.server] AMQ221001: Apache ActiveMQ Artemis Message Broker version 2.5.0 [0.0.0.0, nodeID=bf1853a1-26c7-11e8-9378-d96702a756ed] 
[INFO] Server started
[INFO]
[INFO] --- artemis-maven-plugin:2.5.0:runClient (runClient) @ queue ---
Sent message: This is a text message
Received message: This is a text message
[INFO]
[INFO] --- artemis-maven-plugin:2.5.0:cli (stop) @ queue ---
server-out:2018-03-13 09:06:40,888 INFO  [org.apache.activemq.artemis.core.server] AMQ221002: Apache ActiveMQ Artemis Message Broker version 2.5.0 [bf1853a1-26c7-11e8-9378-d96702a756ed] stopped, uptime 2.786 seconds
server-out:Server stopped!
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.641 s
[INFO] Finished at: 2018-03-13T09:06:40-05:00
[INFO] Final Memory: 43M/600M
[INFO] ------------------------------------------------------------------------
```


This includes a preview list of a few examples that we distribute with Artemis.
Please refer to the distribution for a more accurate list.

## Applet

This example shows you how to send and receive JMS messages from an Applet.

## Application-Layer Failover

Apache ActiveMQ Artemis also supports Application-Layer failover, useful in the
case that replication is not enabled on the server side.

With Application-Layer failover, it's up to the application to register a JMS
`ExceptionListener` with Apache ActiveMQ Artemis which will be called by Apache
ActiveMQ Artemis in the event that connection failure is detected.

The code in the `ExceptionListener` then recreates the JMS connection, session,
etc on another node and the application can continue.

Application-layer failover is an alternative approach to High Availability
(HA). Application-layer failover differs from automatic failover in that some
client side coding is required in order to implement this. Also, with
Application-layer failover, since the old session object dies and a new one is
created, any uncommitted work in the old session will be lost, and any
unacknowledged messages might be redelivered.

## Core Bridge Example

The `bridge` example demonstrates a core bridge deployed on one server, which
consumes messages from a local queue and forwards them to an address on a
second server.

Core bridges are used to create message flows between any two Apache ActiveMQ
Artemis servers which are remotely separated. Core bridges are resilient and
will cope with temporary connection failure allowing them to be an ideal choice
for forwarding over unreliable connections, e.g. a WAN.

## Browser

The `browser` example shows you how to use a JMS `QueueBrowser` with Apache
ActiveMQ Artemis.

Queues are a standard part of JMS, please consult the JMS 2.0 specification for
full details.

A `QueueBrowser` is used to look at messages on the queue without removing
them. It can scan the entire content of a queue or only messages matching a
message selector.

## Camel

The `camel` example demonstrates how to build and deploy a Camel route to the
broker using a web application archive (i.e. `war` file).

## Client Kickoff

The `client-kickoff` example shows how to terminate client connections given an
IP address using the JMX management API.

## Client side failover listener

The `client-side-failoverlistener` example shows how to register a listener to
monitor failover events

## Client-Side Load-Balancing

The `client-side-load-balancing` example demonstrates how sessions created from
a single JMS `Connection` can be created to different nodes of the cluster. In
other words it demonstrates how Apache ActiveMQ Artemis does client-side
load-balancing of sessions across the cluster.

## Clustered Durable Subscription

This example demonstrates a clustered JMS durable subscription

## Clustered Grouping

This is similar to the message grouping example except that it demonstrates it
working over a cluster. Messages sent to different nodes with the same group id
will be sent to the same node and the same consumer.

## Clustered Queue

The `clustered-queue` example demonstrates a queue deployed on two different
nodes. The two nodes are configured to form a cluster. We then create a
consumer for the queue on each node, and we create a producer on only one of
the nodes. We then send some messages via the producer, and we verify that both
consumers receive the sent messages in a round-robin fashion.

## Clustering with JGroups

The `clustered-jgroups` example demonstrates how to form a two node cluster
using JGroups as its underlying topology discovery technique, rather than the
default UDP broadcasting. We then create a consumer for the queue on each node,
and we create a producer on only one of the nodes. We then send some messages
via the producer, and we verify that both consumers receive the sent messages
in a round-robin fashion.

## Clustered Standalone

The `clustered-standalone` example demonstrates how to configure and starts 3
cluster nodes on the same machine to form a cluster. A subscriber for a JMS
topic is created on each node, and we create a producer on only one of the
nodes. We then send some messages via the producer, and we verify that the 3
subscribers receive all the sent messages.

## Clustered Static Discovery

This example demonstrates how to configure a cluster using a list of connectors
rather than UDP for discovery

## Clustered Static Cluster One Way

This example demonstrates how to set up a cluster where cluster connections are
one way, i.e. server A -\> Server B -\> Server C

## Clustered Topic

The `clustered-topic` example demonstrates a JMS topic deployed on two
different nodes. The two nodes are configured to form a cluster. We then create
a subscriber on the topic on each node, and we create a producer on only one of
the nodes. We then send some messages via the producer, and we verify that both
subscribers receive all the sent messages.

## Message Consumer Rate Limiting

With Apache ActiveMQ Artemis you can specify a maximum consume rate at which a
JMS MessageConsumer will consume messages. This can be specified when creating
or deploying the connection factory.

If this value is specified then Apache ActiveMQ Artemis will ensure that
messages are never consumed at a rate higher than the specified rate. This is a
form of consumer throttling.

## Dead Letter

The `dead-letter` example shows you how to define and deal with dead letter
messages. Messages can be delivered unsuccessfully (e.g. if the transacted
session used to consume them is rolled back).

Such a message goes back to the JMS destination ready to be redelivered.
However, this means it is possible for a message to be delivered again and
again without any success and remain in the destination, clogging the system.

To prevent this, messaging systems define dead letter messages: after a
specified unsuccessful delivery attempts, the message is removed from the
destination and put instead in a dead letter destination where they can be
consumed for further investigation.

## Delayed Redelivery

The `delayed-redelivery` example demonstrates how Apache ActiveMQ Artemis can
be configured to provide a delayed redelivery in the case a message needs to be
redelivered.

Delaying redelivery can often be useful in the case that clients regularly fail
or roll-back. Without a delayed redelivery, the system can get into a
"thrashing" state, with delivery being attempted, the client rolling back, and
delivery being re-attempted in quick succession, using up valuable CPU and
network resources.

## Divert

Apache ActiveMQ Artemis diverts allow messages to be transparently "diverted"
or copied from one address to another with just some simple configuration
defined on the server side.

## Durable Subscription

The `durable-subscription` example shows you how to use a durable subscription
with Apache ActiveMQ Artemis. Durable subscriptions are a standard part of JMS,
please consult the JMS 1.1 specification for full details.

Unlike non-durable subscriptions, the key function of durable subscriptions is
that the messages contained in them persist longer than the lifetime of the
subscriber - i.e. they will accumulate messages sent to the topic even if there
is no active subscriber on them. They will also survive server restarts or
crashes. Note that for the messages to be persisted, the messages sent to them
must be marked as durable messages.

## Embedded

The `embedded` example shows how to embed a broker within your own code using
POJO instantiation and no config files.

## Embedded Simple

The `embedded-simple` example shows how to embed a broker within your own code
using regular Apache ActiveMQ Artemis XML files.

## Exclusive Queue

The `exlusive-queue` example shows you how to use exclusive queues, that route
all messages to only one consumer at a time.

## Message Expiration

The `expiry` example shows you how to define and deal with message expiration.
Messages can be retained in the messaging system for a limited period of time
before being removed. JMS specification states that clients should not receive
messages that have been expired (but it does not guarantee this will not
happen).

Apache ActiveMQ Artemis can assign an expiry address to a given queue so that
when messages are expired, they are removed from the queue and sent to the
expiry address. These "expired" messages can later be consumed from the expiry
address for further inspection.

## Apache ActiveMQ Artemis Resource Adapter example

This examples shows how to build the activemq resource adapters a rar for
deployment in other Application Server's

## HTTP Transport

The `http-transport` example shows you how to configure Apache ActiveMQ Artemis
to use the HTTP protocol as its transport layer.

## Instantiate JMS Objects Directly

Usually, JMS Objects such as `ConnectionFactory`, `Queue` and `Topic` instances
are looked up from JNDI before being used by the client code.  This objects are
called "administered objects" in JMS terminology.

However, in some cases a JNDI server may not be available or desired. To come
to the rescue Apache ActiveMQ Artemis also supports the direct instantiation of
these administered objects on the client side so you don't have to use JNDI for
JMS.

## Interceptor

Apache ActiveMQ Artemis allows an application to use an interceptor to hook
into the messaging system. Interceptors allow you to handle various message
events in Apache ActiveMQ Artemis.

## Interceptor AMQP

Similar to the [Interceptor](#interceptor) example, but using AMQP interceptors.

## Interceptor Client

Similar to the [Interceptor](#interceptor) example, but using interceptors on
the **client** rather than the broker.

## Interceptor MQTT

Similar to the [Interceptor](#interceptor) example, but using MQTT interceptors.

## JAAS

The `jaas` example shows you how to configure Apache ActiveMQ Artemis to use
JAAS for security. Apache ActiveMQ Artemis can leverage JAAS to delegate user
authentication and authorization to existing security infrastructure.

## JMS Auto Closable

The `jms-auto-closeable` example shows how JMS resources, such as connections,
sessions and consumers, in JMS 2 can be automatically closed on error.

## JMS Completion Listener

The `jms-completion-listener` example shows how to send a message
asynchronously to Apache ActiveMQ Artemis and use a CompletionListener to be
notified of the Broker receiving it.

## JMS Bridge

The `jms-bridge` example shows how to setup a bridge between two standalone
Apache ActiveMQ Artemis servers.

## JMS Context

The `jms-context` example shows how to send and receive a message to/from an
address/queue using Apache ActiveMQ Artemis by using a JMS Context.

A JMSContext is part of JMS 2.0 and combines the JMS Connection and Session
Objects into a simple Interface.

## JMS Shared Consumer

The `jms-shared-consumer` example shows you how can use shared consumers to
share a subscription on a topic. In JMS 1.1 this was not allowed and so caused
a scalability issue. In JMS 2 this restriction has been lifted so you can share
the load across different threads and connections.

## JMX Management

The `jmx` example shows how to manage Apache ActiveMQ Artemis using JMX.

## Large Message

The `large-message` example shows you how to send and receive very large
messages with Apache ActiveMQ Artemis. Apache ActiveMQ Artemis supports the
sending and receiving of huge messages, much larger than can fit in available
RAM on the client or server. Effectively the only limit to message size is the
amount of disk space you have on the server.

Large messages are persisted on the server so they can survive a server
restart. In other words Apache ActiveMQ Artemis doesn't just do a simple socket
stream from the sender to the consumer.

## Last-Value Queue

The `last-value-queue` example shows you how to define and deal with last-value
queues. Last-value queues are special queues which discard any messages when a
newer message with the same value for a well-defined last-value property is put
in the queue. In other words, a last-value queue only retains the last value.

A typical example for last-value queue is for stock prices, where you are only
interested by the latest price for a particular stock.

## Management

The `management` example shows how to manage Apache ActiveMQ Artemis using JMS
Messages to invoke management operations on the server.

## Management Notification

The `management-notification` example shows how to receive management
notifications from Apache ActiveMQ Artemis using JMS messages. Apache ActiveMQ
Artemis servers emit management notifications when events of interest occur
(consumers are created or closed, addresses are created or deleted, security
authentication fails, etc.).

## Message Counter

The `message-counters` example shows you how to use message counters to obtain
message information for a queue.

## Message Group

The `message-group` example shows you how to configure and use message groups
with Apache ActiveMQ Artemis. Message groups allow you to pin messages so they
are only consumed by a single consumer. Message groups are sets of messages
that has the following characteristics:

- Messages in a message group share the same group id, i.e. they have same
  JMSXGroupID string property values

- The consumer that receives the first message of a group will receive all the
  messages that belongs to the group

## Message Group

The `message-group2` example shows you how to configure and use message groups
with Apache ActiveMQ Artemis via a connection factory.

## Message Priority

Message Priority can be used to influence the delivery order for messages.

It can be retrieved by the message's standard header field 'JMSPriority' as
defined in JMS specification version 1.1.

The value is of type integer, ranging from 0 (the lowest) to 9 (the highest).
When messages are being delivered, their priorities will effect their order of
delivery. Messages of higher priorities will likely be delivered before those
of lower priorities.

Messages of equal priorities are delivered in the natural order of their
arrival at their destinations. Please consult the JMS 1.1 specification for
full details.

## Multiple Failover

This example demonstrates how to set up a live server with multiple backups

## Multiple Failover Failback

This example demonstrates how to set up a live server with multiple backups but
forcing failover back to the original live server

## No Consumer Buffering

By default, Apache ActiveMQ Artemis consumers buffer messages from the server
in a client side buffer before you actually receive them on the client side.
This improves performance since otherwise every time you called receive() or
had processed the last message in a `MessageListener onMessage()` method, the
Apache ActiveMQ Artemis client would have to go the server to request the next
message, which would then get sent to the client side, if one was available.

This would involve a network round trip for every message and reduce
performance. Therefore, by default, Apache ActiveMQ Artemis pre-fetches
messages into a buffer on each consumer.

In some case buffering is not desirable, and Apache ActiveMQ Artemis allows it
to be switched off. This example demonstrates that.

## Non-Transaction Failover With Server Data Replication

The `non-transaction-failover` example demonstrates two servers coupled as a
live-backup pair for high availability (HA), and a client using a
*non-transacted* JMS session failing over from live to backup when the live
server is crashed.

Apache ActiveMQ Artemis implements failover of client connections between live
and backup servers. This is implemented by the replication of state between
live and backup nodes. When replication is configured and a live node crashes,
the client connections can carry and continue to send and consume messages.
When non-transacted sessions are used, once and only once message delivery is
not guaranteed and it is possible that some messages will be lost or delivered
twice.

## OpenWire

The `Openwire` example shows how to configure an Apache ActiveMQ Artemis server
to communicate with an Apache ActiveMQ Artemis JMS client that uses open-wire
protocol.

You will find the queue example for open wire, and the chat example. The virtual-topic-mapping examples shows how to
map the ActiveMQ 5.x Virtual Topic naming convention to work with the Artemis Address model.

## Paging

The `paging` example shows how Apache ActiveMQ Artemis can support huge queues
even when the server is running in limited RAM. It does this by transparently
*paging* messages to disk, and *depaging* them when they are required.

## Pre-Acknowledge

Standard JMS supports three acknowledgement modes:` AUTO_ACKNOWLEDGE`,
`CLIENT_ACKNOWLEDGE`, and `DUPS_OK_ACKNOWLEDGE`. For a full description on
these modes please consult the JMS specification, or any JMS tutorial.

All of these standard modes involve sending acknowledgements from the client to
the server. However in some cases, you really don't mind losing messages in
event of failure, so it would make sense to acknowledge the message on the
server before delivering it to the client. This example demonstrates how Apache
ActiveMQ Artemis allows this with an extra acknowledgement mode.

## Message Producer Rate Limiting

The `producer-rte-limit` example demonstrates how, with Apache ActiveMQ
Artemis, you can specify a maximum send rate at which a JMS message producer
will send messages.

## Queue

A simple example demonstrating a queue.

## Message Redistribution

The `queue-message-redistribution` example demonstrates message redistribution
between queues with the same name deployed in different nodes of a cluster.

## Queue Requestor

A simple example demonstrating a JMS queue requestor.

## Queue with Message Selector

The `queue-selector` example shows you how to selectively consume messages
using message selectors with queue consumers.

## Reattach Node example

The `Reattach Node` example shows how a client can try to reconnect to the same
server instead of failing the connection immediately and notifying any user
ExceptionListener objects. Apache ActiveMQ Artemis can be configured to
automatically retry the connection, and reattach to the server when it becomes
available again across the network.

## Replicated Failback example

An example showing how failback works when using replication, In this example a
live server will replicate all its Journal to a backup server as it updates it.
When the live server crashes the backup takes over from the live server and the
client reconnects and carries on from where it left off.

## Replicated Failback static example

An example showing how failback works when using replication, but this time
with static connectors

## Replicated multiple failover example

An example showing how to configure multiple backups when using replication

## Replicated Failover transaction example

An example showing how failover works with a transaction when using replication

## Request-Reply example

A simple example showing the JMS request-response pattern.

## Scheduled Message

The `scheduled-message` example shows you how to send a scheduled message to an
address/queue with Apache ActiveMQ Artemis. Scheduled messages won't get
delivered until a specified time in the future.

## Security

The `security` example shows you how configure and use role based security with
Apache ActiveMQ Artemis.

## Security LDAP

The `security-ldap` example shows you how configure and use role based security
with Apache ActiveMQ Artemis & an embedded instance of the Apache DS LDAP
server.

## Security keycloak

The `security-keycloak` example shows you how to delegate security
with Apache ActiveMQ Artemis & an external Keycloak. Using
OAuth of the web console and direct access for JMS clients.

## Send Acknowledgements

The `send-acknowledgements` example shows you how to use Apache ActiveMQ
Artemis's advanced *asynchronous send acknowledgements* feature to obtain
acknowledgement from the server that sends have been received and processed in
a separate stream to the sent messages.

## Slow Consumer

The `slow-consumer` example shows you how to detect slow consumers and
configure a slow consumer policy in Apache ActiveMQ Artemis's

## Spring Integration

This example shows how to use embedded JMS using Apache ActiveMQ Artemis's
Spring integration.

## SSL Transport

The `ssl-enabled` shows you how to configure SSL with Apache ActiveMQ Artemis
to send and receive message.

## Static Message Selector

The `static-selector` example shows you how to configure an Apache ActiveMQ
Artemis core queue with static message selectors (filters).

## Static Message Selector Using JMS

The `static-selector-jms` example shows you how to configure an Apache ActiveMQ
Artemis queue with static message selectors (filters) using JMS.

## Stomp

The `stomp` example shows you how to configure an Apache ActiveMQ Artemis
server to send and receive Stomp messages.

## Stomp1.1

The `stomp` example shows you how to configure an Apache ActiveMQ Artemis
server to send and receive Stomp messages via a Stomp 1.1 connection.

## Stomp1.2

The `stomp` example shows you how to configure an Apache ActiveMQ Artemis
server to send and receive Stomp messages via a Stomp 1.2 connection.

## Stomp Over Web Sockets

The `stomp-websockets` example shows you how to configure an Apache ActiveMQ
Artemis server to send and receive Stomp messages directly from Web browsers
(provided they support Web Sockets).

## Symmetric Cluster

The `symmetric-cluster` example demonstrates a symmetric cluster set-up with
Apache ActiveMQ Artemis.

Apache ActiveMQ Artemis has extremely flexible clustering which allows you to
set-up servers in many different topologies. The most common topology that
you'll perhaps be familiar with if you are used to application server
clustering is a symmetric cluster.

With a symmetric cluster, the cluster is homogeneous, i.e. each node is
configured the same as every other node, and every node is connected to every
other node in the cluster.

## Temporary Queue

A simple example demonstrating how to use a JMS temporary queue.

## Topic

A simple example demonstrating a JMS topic.

## Topic Hierarchy

Apache ActiveMQ Artemis supports topic hierarchies. With a topic hierarchy you
can register a subscriber with a wild-card and that subscriber will receive any
messages sent to an address that matches the wild card.

## Topic Selector 1

The `topic-selector-example1` example shows you how to send message to a JMS
Topic, and subscribe them using selectors with Apache ActiveMQ Artemis.

## Topic Selector 2

The `topic-selector-example2` example shows you how to selectively consume
messages using message selectors with topic consumers.

## Transaction Failover

The `transaction-failover` example demonstrates two servers coupled as a
live-backup pair for high availability (HA), and a client using a transacted
JMS session failing over from live to backup when the live server is crashed.

Apache ActiveMQ Artemis implements failover of client connections between live
and backup servers. This is implemented by the sharing of a journal between the
servers. When a live node crashes, the client connections can carry and
continue to send and consume messages. When transacted sessions are used, once
and only once message delivery is guaranteed.

## Failover Without Transactions

The `stop-server-failover` example demonstrates failover of the JMS connection
from one node to another when the live server crashes using a JMS
non-transacted session.

## Transactional Session

The `transactional` example shows you how to use a transactional Session with
Apache ActiveMQ Artemis.

## XA Heuristic

The `xa-heuristic` example shows you how to make an XA heuristic decision
through Apache ActiveMQ Artemis Management Interface. A heuristic decision is a
unilateral decision to commit or rollback an XA transaction branch after it has
been prepared.

## XA Receive

The `xa-receive` example shows you how message receiving behaves in an XA
transaction in Apache ActiveMQ Artemis.

## XA Send

The `xa-send` example shows you how message sending behaves in an XA
transaction in Apache ActiveMQ Artemis.