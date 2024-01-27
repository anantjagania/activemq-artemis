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
package org.apache.activemq.artemis.tests.integration.paging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.utils.collections.LinkedListIterator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueFilterPagingTest extends ActiveMQTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public static final SimpleString ADDRESS = new SimpleString("QueueFilterPagingTest");


   private ActiveMQServer server;

   protected boolean isNetty() {
      return true;
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      server = newActiveMQServer();

      server.start();
      waitForServerToStart(server);
   }

   private ActiveMQServer newActiveMQServer() throws Exception {
      ActiveMQServer server = createServer(true, isNetty());

      AddressSettings defaultSetting = new AddressSettings().setPageSizeBytes(10 * 1024).setMaxSizeBytes(20 * 1024).setMaxReadPageBytes(-1).setMaxReadPageMessages(-1);

      server.getAddressSettingsRepository().addMatch("#", defaultSetting);

      return server;
   }


   @Test
   public void testFilteredQueue() throws Exception {
      server.addAddressInfo(new AddressInfo(ADDRESS).addRoutingType(RoutingType.MULTICAST));

      String protocol = "CORE";
      String clientID = "SOME_ID";

      ConnectionFactory factory = CFUtil.createConnectionFactory(protocol, "tcp://localhost:61616");
      try (Connection connection = factory.createConnection()) {
         connection.setClientID(clientID);
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Session sessionSpread = connection.createSession(true, Session.SESSION_TRANSACTED);
         Session sessionLast = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic(ADDRESS.toString() + "a");
         MessageConsumer spreadConsumer = sessionSpread.createDurableSubscriber(topic, "spread", "id=9 OR id >= 900", false);
         MessageConsumer fullThing = session.createDurableSubscriber(topic, "full");
         MessageConsumer lastOnes = sessionLast.createDurableSubscriber(topic, "last", "id >= 900", false);
         MessageConsumer never = session.createDurableSubscriber(topic, "never", "id >= 1000000", false);

         MessageProducer producer = session.createProducer(topic);

         String body;
         {
            StringWriter stringWriter = new StringWriter();

            while (stringWriter.getBuffer().length() < 1024) {
               stringWriter.append("Lorem Ypsum Whatever is that thing...");
            }
            body = stringWriter.toString();
         }

         for (int i = 0; i < 1_000; i++) {
            Message message = session.createTextMessage(body);
            message.setIntProperty("id", i);
            producer.send(message);
            if ((i + 1) % 5 == 0) {
               logger.info("Sent {}", i);
               session.commit();
            }
         }
         session.commit();

         String subscriptionName = "SOME_ID.spread";

         QueueControl queueControl = (QueueControl) server.getManagementService().getResource("queue." + subscriptionName);
         Assert.assertNotNull(queueControl);

         Map<String, Object>[] mapResult = queueControl.listMessages("");
         System.out.println("Size = " + mapResult.length);

         for (int i = 0; i < 1; i++) {
            Assert.assertNotNull(lastOnes.receive(5000));
         }
         sessionLast.commit();

         /*for (int i = 0; i < 11; i++) {
            Message message = spreadConsumer.receive(5000);
            Assert.assertNotNull(message);
            logger.info("Received {}", message.getIntProperty("id"));
         }
         sessionSpread.rollback(); */
         //Assert.assertNull(spreadConsumer.receiveNoWait());

         for (int i = 0; i < 1_000; i++) {
            Message message = fullThing.receive(5000);
            Assert.assertNotNull(message);
            if (i % 100 == 0) {
               session.commit();
            }
         }

         session.commit();

      }

      while (true) {
         System.out.println("sleeping...");
         Thread.sleep(10_000);
      }
   }
}
