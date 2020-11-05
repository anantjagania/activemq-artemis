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
package org.apache.activemq.artemis.tests.integration.cluster.failover;

import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.client.impl.ClientSessionFactoryImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.tests.util.RandomUtil;
import org.apache.activemq.artemis.tests.util.Wait;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** this test will simulate a case where failures are coming from multiple places. */
public class MultiThreadFailuresTest extends ActiveMQTestBase {

   private ActiveMQServer server;

   @Before
   public void startServer() throws Exception {
      server = createServer(false, true);
      server.start();
   }

   @Test
   public void testNoPesky() throws Exception {
      // This is just testing that the test itself would receive messages without failures.
      // it's just test validation
      testMultiFailureOnClient(0, 0, 0);
   }

   @Test
   public void testManyServer() throws Exception {
      // This is just testing that the test itself would receive messages without failures.
      // it's just test validation
      testMultiFailureOnClient(0, 10, 50_000);
   }

   @Test
   public void testManyClients() throws Exception {
      // This is just testing that the test itself would receive messages without failures.
      // it's just test validation
      testMultiFailureOnClient(10, 0, 50_000);
   }

   @Test
   public void testBothWays() throws Exception {
      // This is just testing that the test itself would receive messages without failures.
      // it's just test validation
      testMultiFailureOnClient(10, 10, 50_000);
   }

   public void testMultiFailureOnClient(int clientPesky, int serverPesky, int timeRunning) throws Exception {
      SimpleString ADDRESS = SimpleString.toSimpleString("testMultiFailure");
      server.addAddressInfo(new AddressInfo(ADDRESS).setAutoCreated(false).addRoutingType(RoutingType.ANYCAST));
      server.createQueue(new QueueConfiguration(ADDRESS).setAddress(ADDRESS).setRoutingType(RoutingType.ANYCAST).setDurable(true));

      ConnectionFactory factory = CFUtil.createConnectionFactory("core", "tcp://localhost:61616?ha=true&reconnectAttempts=-1&retryInterval=100&callTimeout=1000");
      try (ActiveMQConnection connection = (ActiveMQConnection)factory.createConnection()) {
         connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         javax.jms.Queue jmsQueue = session.createQueue(ADDRESS.toString());
         MessageConsumer consumer = session.createConsumer(jmsQueue);

         Queue queue = server.locateQueue(ADDRESS);
         Wait.assertEquals(1, queue::getConsumerCount);

         ClientSessionFactoryImpl factoryImpl = (ClientSessionFactoryImpl) connection.getSessionFactory();
         Assert.assertEquals(-1, factoryImpl.getReconnectAttempts());
         Assert.assertEquals(1000, factoryImpl.getServerLocator().getCallTimeout());

         AtomicBoolean running = new AtomicBoolean(true);
         final int CLIENT_THREADS = clientPesky;
         final int SERVER_THREADS = serverPesky;

         Wait.assertEquals(1, () -> server.getRemotingService().getConnections().size());

         CyclicBarrier cyclicBarrier = CLIENT_THREADS + SERVER_THREADS > 0 ? new CyclicBarrier(CLIENT_THREADS + SERVER_THREADS) : null;
         Runnable clientPeskyRunnable = () -> {
            boolean flip = true;
            try {
               // wait they all to get aligned
               cyclicBarrier.await();
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
            while (running.get()) {
               try {
                  if (flip) {
                     factoryImpl.connectionException(factoryImpl.getConnection().getID(), new ActiveMQException("Is that bothering you"));
                  } else {
                     factoryImpl.connectionDestroyed(factoryImpl.getConnection().getID());
                  }
                  int sleep = RandomUtil.randomPositiveInt() % 50;
                  if (sleep > 0) {
                     Thread.sleep(sleep);
                  }
                  flip = !flip;
               } catch (Throwable dontcare) {
                  dontcare.printStackTrace();
               }
            }
         };

         Runnable serverPeskyRunnable = () -> {
            try {
               cyclicBarrier.await();
            } catch (Exception e) {
               e.printStackTrace();
            }
            while (running.get()) {
               try {
                  Set<RemotingConnection> connectionSet = server.getRemotingService().getConnections();
                  connectionSet.forEach((c) -> c.fail(new ActiveMQException("are you bothered?")));
               } catch (Exception dontCare) {
                  dontCare.printStackTrace();
               }
               int sleep = RandomUtil.randomPositiveInt() % 5000;
               if (sleep > 0) {
                  try {
                     Thread.sleep(sleep);
                  } catch (Throwable e) {
                     e.printStackTrace();
                     return;
                  }
               }
            }
         };

         Thread[] clientThreads = new Thread[CLIENT_THREADS];

         for (int i = 0; i < CLIENT_THREADS; i++) {
            clientThreads[i] = new Thread(clientPeskyRunnable);
            clientThreads[i].start();
         }

         Thread[] serverThreads = new Thread[SERVER_THREADS];

         for (int i = 0; i < SERVER_THREADS; i++) {
            serverThreads[i] = new Thread(serverPeskyRunnable);
            serverThreads[i].start();
         }


         if (timeRunning > 0) {
            Thread.sleep(timeRunning);
         }

         running.set(false);

         for (Thread t : clientThreads) {
            t.join();
         }

         for (Thread t : serverThreads) {
            t.join();
         }

         Wait.assertEquals(1, queue::getConsumerCount);
         Wait.assertEquals(1, () -> server.getRemotingService().getConnections().size());

         ConnectionFactory factorySender = CFUtil.createConnectionFactory("core", "tcp://localhost:61616?ha=true&reconnectAttempts=-1&retryInterval=100");
         ActiveMQConnection connectionSender = (ActiveMQConnection) factorySender.createConnection();
         Session sessionSender = connectionSender.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer producer = sessionSender.createProducer(jmsQueue);

         for (int i = 0; i < 100; i++) {
            producer.send(sessionSender.createTextMessage("hello " + i));
         }

         Wait.assertEquals(100, queue::getMessageCount);

         connection.start();
         for (int i = 0; i < 100; i++) {
            TextMessage message = (TextMessage) consumer.receive(5000);
            Assert.assertNotNull(message);
            Assert.assertEquals("hello " + i, message.getText());
         }

         Assert.assertNull(consumer.receiveNoWait());

         consumer.close();
         connection.close();

         connectionSender.close();

         Wait.assertEquals(0, queue::getConsumerCount);
      }

   }
}
