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

package org.apache.activemq.artemis.tests.smoke.paging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.tests.smoke.common.SmokeTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.utils.RandomUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FloodServerWithAsyncSendTest extends SmokeTestBase {

   public static final String SERVER_NAME_0 = "paging";

   volatile boolean running = true;

   AtomicInteger errors = new AtomicInteger(0);

   @Before
   public void before() throws Exception {
      cleanupData(SERVER_NAME_0);
      startServer(SERVER_NAME_0, 0, 30000);
   }

   @Test
   public void testAsyncPagingOpenWire() throws Exception {
      String protocol = "OPENWIRE";
      internalTest(protocol);

   }

   ConnectionFactory newCF(String protocol) {
      if (protocol.equalsIgnoreCase("OPENWIRE")) {
         return CFUtil.createConnectionFactory(protocol, "tcp://localhost:61616?jms.useAsyncSend=true");
      } else {
         Assert.fail("unsuported protocol");
         return null;
      }
   }

   private void internalTest(String protocol) throws Exception {

      Thread consume1 = new Thread(() -> consume(protocol, "queue1"), "ProducerQueue1");
      consume1.start();
      Thread consume2 = new Thread(() -> consume(protocol, "queue2"), "ProducerQueue2");
      consume2.start();

      Thread produce1 = new Thread(() -> produce(protocol, "queue1"), "ConsumerQueue1");
      produce1.start();
      Thread produce2 = new Thread(() -> produce(protocol, "queue2"), "ConsumerQueue2");
      produce2.start();

      Thread.sleep(10_000);

      running = false;

      consume1.join();
      consume2.join();
      produce1.join();
      produce2.join();

      ConnectionFactory factory = newCF("openwire");
      Connection connection = factory.createConnection();
      connection.start();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue("queue3");
      MessageConsumer consumer = session.createConsumer(queue);

      MessageProducer producer = session.createProducer(queue);

      String random = RandomUtil.randomString();

      producer.send(session.createTextMessage(random));
      TextMessage message = (TextMessage) consumer.receive(1000);
      Assert.assertNotNull(message);
      Assert.assertEquals(random, message.getText());
      connection.close();

   }

   protected void consume(String protocol, String queueName) {
      ConnectionFactory factory = newCF(protocol);
      Connection connection = null;
      try {
         connection = factory.createConnection();
         connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue queue = session.createQueue(queueName);
         MessageConsumer consumer = session.createConsumer(queue);
         while (running) {
            consumer.receive(5000);
         }
      } catch (Throwable e) {
         e.printStackTrace();
         errors.incrementAndGet();
      } finally {
         try {
            connection.close();
         } catch (Exception ignored) {
         }
      }
   }

   protected void produce(String protocol, String queueName) {

      int produced = 0;
      ConnectionFactory factory = newCF(protocol);
      Connection connection = null;
      try {

         connection = factory.createConnection();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue queue = session.createQueue(queueName);
         MessageProducer producer = session.createProducer(queue);
         String randomString;
         {
            StringBuffer buffer = new StringBuffer();
            while (buffer.length() < 10000) {
               buffer.append(RandomUtil.randomString());
            }
            randomString = buffer.toString();
         }

         while (running) {
            if (++produced % 100 == 0) {
               System.out.println(queueName + " produced " + produced + " messages");
            }
            producer.send(session.createTextMessage(randomString));
         }

      } catch (Throwable e) {
         e.printStackTrace();
         errors.incrementAndGet();
      } finally {
         try {
            connection.close();
         } catch (Exception ignored) {
         }
      }
   }

}
