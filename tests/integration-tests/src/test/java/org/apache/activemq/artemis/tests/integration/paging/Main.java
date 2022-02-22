/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.tests.integration.paging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.Wait;
import org.checkerframework.checker.units.qual.A;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Main extends ActiveMQTestBase {

   private final static String QUEUE = "service.images.dev::service.images.dev";

   ActiveMQServer server;


   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();

      Configuration config = createDefaultConfig(0, true).setJournalSyncNonTransactional(false);

      config.setMessageExpiryScanPeriod(-1);
      server = createServer(true, config, 100 * 1024 * 1024, 10 * 1024 * 1024);

      server.getAddressSettingsRepository().clear();

      AddressSettings defaultSetting = new AddressSettings().setPageSizeBytes(100 * 1024 * 1024).setMaxSizeBytes(10 * 1024 * 1024).setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE).setAutoCreateAddresses(false).setAutoCreateQueues(false);

      server.getAddressSettingsRepository().addMatch("#", defaultSetting);


      server.start();


      server.addAddressInfo(new AddressInfo(QUEUE).addRoutingType(RoutingType.ANYCAST));
      server.createQueue(new QueueConfiguration(QUEUE).setRoutingType(RoutingType.ANYCAST));

   }

   @Test
   public void testSending() throws Exception {

      final String username = null;
      final String password = null;

      var serverLocator = ActiveMQClient.createServerLocator("tcp://localhost:61616").setBlockOnDurableSend(true).setBlockOnNonDurableSend(true).setMinLargeMessageSize(1024);

      final var sessionFactory = serverLocator.createSessionFactory();

      final var xa = false;
      final var autoCommitSends = true;
      final var autoCommitAcks = true;
      final var ackBatchSize = serverLocator.getAckBatchSize();
      final var preAcknowledge = serverLocator.isPreAcknowledge();
      final var clientSession = sessionFactory.createSession(username, password, xa, autoCommitSends, autoCommitAcks, preAcknowledge, ackBatchSize);

      var queueQueryResult = clientSession.queueQuery(SimpleString.toSimpleString(QUEUE));
      if (!queueQueryResult.isExists()) {
         clientSession.createQueue(_ServiceQueueConfiguration(new SimpleString(QUEUE)));
      }

      final var consumer = clientSession.createConsumer(QUEUE);

      clientSession.start();

      AtomicInteger errors = new AtomicInteger(0);
      AtomicInteger received = new AtomicInteger(0);

      consumer.setMessageHandler((msg) -> {
         try {
            msg.getDataBuffer();
            received.incrementAndGet();
         } catch (Throwable e) {
            e.printStackTrace();
            errors.incrementAndGet();
         }
      });


      try (ClientSession producerSession = sessionFactory.createSession()) {
         ClientProducer producer = producerSession.createProducer(QUEUE);
         for (int i = 0; i < 100; i++) {
            ClientMessage message = producerSession.createMessage(true);
            message.getBodyBuffer().writeBytes(new byte[1024 * 1024]);
            producer.send(message);
         }
      }

      Wait.assertEquals(100, received::get);
      Assert.assertEquals(0, errors.get());
   }

   @Test
   public void testWithJMSListener() throws Exception {

      final String username = null;
      final String password = null;

      ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
      factory.setMinLargeMessageSize(1024);
      Connection connection = factory.createConnection();
      Session sessionProducer = connection.createSession(true, Session.SESSION_TRANSACTED);

      Session sessionConsumer = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue jmsQueue = sessionProducer.createQueue(QUEUE);

      MessageConsumer consumer = sessionConsumer.createConsumer(jmsQueue);


      connection.start();

      AtomicInteger errors = new AtomicInteger(0);
      AtomicInteger received = new AtomicInteger(0);

      consumer.setMessageListener((msg) -> {
         try {
            System.out.println("Received: " + ((TextMessage)msg).getText().length());
            received.incrementAndGet();
         } catch (Throwable e) {
            e.printStackTrace();
            errors.incrementAndGet();
         }

      });

      MessageProducer producer = sessionProducer.createProducer(jmsQueue);

      StringBuffer buffer = new StringBuffer();
      while (buffer.length() < 100 * 1024) {
         buffer.append("*****");
      }

      for (int i = 0; i < 100; i++) {
         TextMessage message = sessionProducer.createTextMessage(buffer.toString());
         producer.send(message);
      }
      sessionProducer.commit();

      Wait.assertEquals(100, received::get);
      Assert.assertEquals(0, errors.get());
   }

   private static QueueConfiguration _ServiceQueueConfiguration(SimpleString queueName) {
      final var config = new QueueConfiguration(queueName);
      config.setMaxConsumers(1);
      config.setPurgeOnNoConsumers(false);
      config.setDurable(false);
      config.setAutoDelete(false);
      config.setRoutingType(RoutingType.MULTICAST);
      return config;
   }
}