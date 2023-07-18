/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.tests.smoke.brokerConnection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.tests.smoke.common.SimpleManagement;
import org.apache.activemq.artemis.tests.smoke.common.SmokeTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.util.ServerUtil;
import org.apache.activemq.artemis.utils.Wait;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirroredSubscriptionTest extends SmokeTestBase {

   public static final String SERVER_NAME_A = "mirrored-subscriptions/broker1";
   public static final String SERVER_NAME_B = "mirrored-subscriptions/broker2";
   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   Process processB;
   Process processA;

   @Before
   public void beforeClass() throws Exception {
      cleanupData(SERVER_NAME_A);
      cleanupData(SERVER_NAME_B);
      processB = startServer(SERVER_NAME_B, 1, 0);
      processA = startServer(SERVER_NAME_A, 0, 0);

      ServerUtil.waitForServerToStart(1, "B", "B", 30000);
      ServerUtil.waitForServerToStart(0, "A", "A", 30000);
   }

   @Test
   public void testSend() throws Throwable {

      int COMMIT_INTERVAL = 100;
      int NUMBER_OF_MESSAGES = 1000;
      int CLIENTS = 5;
      String mainURI = "tcp://localhost:61616";
      String secondURI = "tcp://localhost:61617";

      String topicName = "myTopic";

      ConnectionFactory cf = CFUtil.createConnectionFactory("amqp", mainURI);

      for (int i = 0; i < CLIENTS; i++) {
         try (Connection connection = cf.createConnection()) {
            connection.setClientID("client" + i);
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Topic topic = session.createTopic(topicName);
            session.createDurableSubscriber(topic, "subscription" + i);
         }
      }

      try (Connection connection = cf.createConnection()) {
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic(topicName);
         MessageProducer producer = session.createProducer(topic);
         for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            producer.send(session.createTextMessage("hello " + i));
            if (i % COMMIT_INTERVAL == 0) {
               session.commit();
            }
         }
         session.commit();
      }

      Map<String, Integer> result = SimpleManagement.listQueues(mainURI, null, null, 100);
      result.entrySet().forEach(entry -> logger.info("Queue {} = {}", entry.getKey(), entry.getValue()));

      checkMessages(NUMBER_OF_MESSAGES, CLIENTS, mainURI, secondURI);

      ExecutorService executorService = Executors.newFixedThreadPool(CLIENTS);
      runAfter(executorService::shutdownNow);

      CountDownLatch done = new CountDownLatch(CLIENTS);
      AtomicInteger errors = new AtomicInteger(0);

      for (int i = 0; i < CLIENTS; i++) {
         final int clientID = i;
         executorService.execute(() -> {
            try (Connection connection = cf.createConnection()) {
               connection.setClientID("client" + clientID);
               connection.start();
               Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
               Topic topic = session.createTopic(topicName);
               TopicSubscriber subscriber = session.createDurableSubscriber(topic, "subscription" + clientID);
               for (int messageI = 0; messageI < NUMBER_OF_MESSAGES; messageI++) {
                  TextMessage message = (TextMessage) subscriber.receive(5000);
                  Assert.assertNotNull(message);
                  if (messageI % COMMIT_INTERVAL == 0) {
                     session.commit();
                     logger.info("Received {} messages on receiver {}", messageI, clientID);
                  }
               }
               session.commit();
            } catch (Throwable e) {
               logger.warn(e.getMessage(), e);
               errors.incrementAndGet();
            } finally {
               done.countDown();
            }
         });
      }

      Assert.assertTrue(done.await(300, TimeUnit.SECONDS));
      Assert.assertEquals(0, errors.get());
      checkMessages(0, CLIENTS, mainURI, secondURI);
   }

   private void checkMessages(int NUMBER_OF_MESSAGES, int CLIENTS, String mainURI, String secondURI) throws Exception {
      for (int i = 0; i < CLIENTS; i++) {
         final int clientID = i;
         Wait.assertEquals(NUMBER_OF_MESSAGES, () -> getMessageCount(mainURI, "client" + clientID + ".subscription" + clientID));
         Wait.assertEquals(NUMBER_OF_MESSAGES, () -> getMessageCount(secondURI, "client" + clientID + ".subscription" + clientID));
      }
   }

   int getMessageCount(String uri, String queueName) throws Exception {
      try {
         Map<String, Integer> result = SimpleManagement.listQueues(uri, null, null, 100);

         if (result == null) {
            return 0;
         }

         Integer resultReturn = result.get(queueName);

         logger.debug("Result = {}, queueName={}, returnValue = {}", result, queueName, resultReturn);
         return resultReturn == null ? 0 : resultReturn;
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
         // if an exception happened during a retry
         // we just return -1, so the retries will keep coming
         return -1;
      }

   }

}
