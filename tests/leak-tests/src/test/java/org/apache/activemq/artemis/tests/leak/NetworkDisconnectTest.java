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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.checkleak.core.CheckLeak;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.server.impl.QueueImpl;
import org.apache.activemq.artemis.core.server.impl.ServerConsumerImpl;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.utils.Wait;
import org.apache.activemq.artemis.utils.collections.LinkedListImpl;
import org.apache.activemq.artemis.utils.network.NetUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkDisconnectTest extends ActiveMQTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   @BeforeClass
   public static void start() {
      NetUtil.skipIfNotSudo();
   }

   // 192.0.2.0 is reserved for documentation (and testing on this case).
   private static final String LIVE_IP = "192.0.2.0";

   ConnectionFactory createCF(String host) {
      ConnectionFactory factory = CFUtil.createConnectionFactory("CORE", "tcp://" + host + ":61616?ha=true&reconnectAttempts=-1&connectionTTL=1000&callTimeout=1000&clientFailureCheckPeriod=1000&callFailoverTimeout=1000");
      Assert.assertTrue(((ActiveMQConnectionFactory)factory).isHA());
      Assert.assertEquals(-1, ((ActiveMQConnectionFactory)factory).getReconnectAttempts());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getConnectionTTL());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getClientFailureCheckPeriod());
      Assert.assertEquals(1000, ((ActiveMQConnectionFactory)factory).getCallTimeout());
      Assert.assertEquals(1000, ((ActiveMQConnectionFactory)factory).getCallFailoverTimeout());
      return factory;
   }

   @Test
   public void testLeakAfterDisconnect() throws Exception {
      CheckLeak checkLeak = new CheckLeak();

      NetUtil.netUp(LIVE_IP, "lo0");

      runAfter(() -> NetUtil.netDown(LIVE_IP));

      ActiveMQServer server = createServer(true, createDefaultConfig(true), AddressSettings.DEFAULT_PAGE_SIZE, AddressSettings.DEFAULT_MAX_SIZE_BYTES, -1, -1);
      server.getConfiguration().clearAcceptorConfigurations();
      server.getConfiguration().addAcceptorConfiguration("consumer", "tcp://" + LIVE_IP + ":61616");
      server.getConfiguration().addAcceptorConfiguration("producer", "tcp://localhost:61616");
      server.start();

      server.addAddressInfo(new AddressInfo(getName()).addRoutingType(RoutingType.ANYCAST));
      server.createQueue(new QueueConfiguration(getName()).setRoutingType(RoutingType.ANYCAST));

      ConnectionFactory factoryConsumers = createCF(LIVE_IP);
      ConnectionFactory factoryProducer = createCF("localhost");

      final int consumers = 9;

      AtomicBoolean running = new AtomicBoolean(true);

      ExecutorService service = Executors.newFixedThreadPool(consumers + 1);
      runAfter(() -> running.set(false));
      runAfter(service::shutdownNow);

      for (int i = 0; i < consumers; i++) {
         int clientID = i;
         service.execute(() -> {
            while (running.get()) {
               try {
                  Connection conn = factoryConsumers.createConnection();
                  conn.start();

                  Session session = conn.createSession();
                  MessageConsumer consumer = session.createConsumer(session.createQueue(getName()));
                  while (running.get()) {
                     Message message = consumer.receive(5000);
                     logger.info("client {} received {}", clientID, message);
                  }
               } catch (Throwable e) {
                  logger.warn(e.getMessage(), e);
               }
            }
         });
      }

      service.execute(() -> {
         while (running.get()) {
            try {
               Connection conn = factoryProducer.createConnection();

               Session session = conn.createSession();
               MessageProducer producer = session.createProducer(session.createQueue(getName()));
               while (running.get()) {
                  producer.send(session.createTextMessage("hello"));
               }
            } catch (Throwable e) {
               logger.warn(e.getMessage(), e);
            }
         }
      });

      QueueImpl queue = (QueueImpl) server.locateQueue(getName());

      for (int repeat = 0; repeat < 5; repeat++) {
         logger.info("Repeat {}", repeat);
         if (repeat > 0) {
            NetUtil.netUp(LIVE_IP, "lo0");
         }

         Wait.assertEquals(consumers, queue::getConsumerCount);
         Thread.sleep(1000);

         NetUtil.netDown(LIVE_IP);

         Wait.assertEquals(0, queue::getConsumerCount);
         validateNoIterators(checkLeak);
      }

      NetUtil.netUp(LIVE_IP, "lo0");
      Wait.assertEquals(consumers, queue::getConsumerCount);

      server.stop();

      validateNoIterators(checkLeak);

      running.set(false);
   }

   private static void validateNoIterators(CheckLeak checkLeak) {
      int numberOfIterators = checkLeak.getAllObjects(LinkedListImpl.Iterator.class).length;
      int numberOfConsumers = checkLeak.getAllObjects(ServerConsumerImpl.class).length;
      Assert.assertEquals("Redistributors are leaking " + LinkedListImpl.Iterator.class.getName(), 0, numberOfIterators);
      Assert.assertEquals("ServerConsumers are leaking " + ServerConsumerImpl.class.getName(), 0, numberOfConsumers);
   }

}
