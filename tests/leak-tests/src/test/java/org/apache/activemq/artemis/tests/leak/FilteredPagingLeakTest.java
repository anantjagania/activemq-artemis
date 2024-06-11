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
package org.apache.activemq.artemis.tests.leak;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.checkleak.core.CheckLeak;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.paging.PagingStore;
import org.apache.activemq.artemis.core.paging.cursor.impl.PageSubscriptionImpl;
import org.apache.activemq.artemis.core.protocol.core.impl.RemotingConnectionImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.server.impl.ServerStatus;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.utils.Wait;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.activemq.artemis.tests.leak.MemoryAssertions.assertMemory;
import static org.apache.activemq.artemis.tests.leak.MemoryAssertions.basicMemoryAsserts;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/* at the time this test was written JournalFileImpl was leaking through JournalFileImpl::negative creating a linked list (or leaked-list, pun intended) */
public class FilteredPagingLeakTest extends AbstractLeakTest {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   ActiveMQServer server;

   @BeforeAll
   public static void beforeClass() throws Exception {
      assumeTrue(CheckLeak.isLoaded());
   }

   @AfterEach
   public void validateServer() throws Exception {
      CheckLeak checkLeak = new CheckLeak();

      // I am doing this check here because the test method might hold a client connection
      // so this check has to be done after the test, and before the server is stopped
      assertMemory(checkLeak, 0, RemotingConnectionImpl.class.getName());

      server.stop();

      server = null;

      clearServers();
      ServerStatus.clear();

      assertMemory(checkLeak, 0, ActiveMQServerImpl.class.getName());
   }

   @Override
   @BeforeEach
   public void setUp() throws Exception {
      server = createServer(true, createDefaultConfig(1, true));
      server.getConfiguration().setJournalPoolFiles(4).setJournalMinFiles(2);
      server.start();
   }

   @Test
   public void testCore() throws Exception {
      doTest("CORE");
   }

   private void doTest(String protocol) throws Exception {
      int MESSAGES = 10_000;
      int MESSAGE_SIZE = 104;
      int COMMIT_INTERVAL = 100;
      int FILTERED_A = 100;

      ExecutorService executorService = Executors.newFixedThreadPool(2);
      runAfter(executorService::shutdownNow);

      ConnectionFactory cf = CFUtil.createConnectionFactory(protocol, "tcp://localhost:61616");

      server.addAddressInfo(new AddressInfo(getName()).addRoutingType(RoutingType.MULTICAST));
      server.createQueue(QueueConfiguration.of("A").setAddress(getName()).setRoutingType(RoutingType.MULTICAST).setDurable(true).setFilterString("i >= " + (MESSAGES - FILTERED_A)));
      server.createQueue(QueueConfiguration.of("B").setAddress(getName()).setRoutingType(RoutingType.MULTICAST).setDurable(true));

      {
         Queue serverQueueA = server.locateQueue("A");

         PagingStore pagingStore = serverQueueA.getPagingStore();
         pagingStore.startPaging();

         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageProducer producer = session.createProducer(session.createTopic(getName()));
            connection.start();

            for (int i = 0; i < MESSAGES; i++) {
               BytesMessage message = session.createBytesMessage();
               message.writeBytes(new byte[MESSAGE_SIZE]);
               message.setIntProperty("i", i);
               producer.send(message);
               if (i > 0 && i % COMMIT_INTERVAL == 0) {
                  session.commit();
                  pagingStore.forceAnotherPage();
               }
            }
            session.commit();
         }
         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue("A"));
            connection.start();

            BytesMessage message = (BytesMessage) consumer.receive(5000);
            Assertions.assertNotNull(message);
            session.rollback();
         }

         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue("A"));
            connection.start();

            BytesMessage message = (BytesMessage) consumer.receive(5000);
            Assertions.assertNotNull(message);
            session.rollback();
         }

      }

      CheckLeak checkLeak = new CheckLeak();
      logger.info("There are {} PageCursorInfo elements in the heap", checkLeak.getAllObjects(PageSubscriptionImpl.PageCursorInfo.class).length);

      server.stop();
      server.start();


      {
         Queue serverQueueA = server.locateQueue("A");


         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue("A"));
            connection.start();

            BytesMessage message = (BytesMessage) consumer.receive(5000);
            Assertions.assertNotNull(message);
            session.rollback();
         }

         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue("B"));
            connection.start();

            for (int i = 0; i < MESSAGES; i++) {
               BytesMessage message = (BytesMessage) consumer.receive(5000);
               Assertions.assertNotNull(message);
               logger.info("Received {}", message.getIntProperty("i"));
               if (i % 100 == 0) {
                  session.commit();
               }
            }
            session.commit();
         }

         try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue("A"));
            connection.start();

            for (int i = 0; i < FILTERED_A; i++) {
               BytesMessage message = (BytesMessage) consumer.receive(5000);
               Assertions.assertNotNull(message);
               logger.info("Received {}", message.getIntProperty("i"));
               if (i % 100 == 0) {
                  session.commit();
               }
            }
            Assertions.assertNull(consumer.receiveNoWait());
            session.commit();
         }

         Wait.assertFalse(serverQueueA.getPagingStore()::isPaging);
      }


   }
}