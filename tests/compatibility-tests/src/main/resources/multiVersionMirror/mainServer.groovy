package multiVersionReplica
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.activemq.artemis.api.core.QueueConfiguration
import org.apache.activemq.artemis.api.core.RoutingType
import org.apache.activemq.artemis.api.core.SimpleString
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration
import org.apache.activemq.artemis.core.config.amqpBrokerConnectivity.AMQPBrokerConnectConfiguration
import org.apache.activemq.artemis.core.config.amqpBrokerConnectivity.AMQPMirrorBrokerConnectionElement
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl

// starts an artemis server
import org.apache.activemq.artemis.core.server.JournalType
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ

String folder = arg[0];
String id = arg[1];

configuration = new ConfigurationImpl();
configuration.setJournalType(JournalType.NIO);
configuration.setBrokerInstance(new File(folder + "/" + id));
configuration.addAcceptorConfiguration("artemis", "tcp://localhost:61616");
configuration.setSecurityEnabled(false);
configuration.setPersistenceEnabled(true);

configuration.addAddressConfiguration(new CoreAddressConfiguration().setName("TestQueue").addRoutingType(RoutingType.ANYCAST));
configuration.addQueueConfiguration(new QueueConfiguration("TestQueue").setAddress("TestQueue").setRoutingType(RoutingType.ANYCAST));
configuration.addAddressConfiguration(new CoreAddressConfiguration().setName("TestTopic").addRoutingType(RoutingType.MULTICAST));

try {
    AMQPBrokerConnectConfiguration connection = new AMQPBrokerConnectConfiguration("mirror", "tcp://localhost:61617").setReconnectAttempts(-1).setRetryInterval(100);
    AMQPMirrorBrokerConnectionElement replication = new AMQPMirrorBrokerConnectionElement().setDurable(true).setSync(false).setMessageAcknowledgements(true).setDurable(true);
    connection.addElement(replication);
    configuration.addAMQPConnection(connection);
} catch (Throwable ignored) {
}

theMainServer = new EmbeddedActiveMQ();
theMainServer.setConfiguration(configuration);
theMainServer.start();
theMainServer.getActiveMQServer().getPagingManager().getPageStore(SimpleString.toSimpleString("TestTopic")).startPaging();
theMainServer.getActiveMQServer().getPagingManager().getPageStore(SimpleString.toSimpleString("TestQueue")).startPaging();
