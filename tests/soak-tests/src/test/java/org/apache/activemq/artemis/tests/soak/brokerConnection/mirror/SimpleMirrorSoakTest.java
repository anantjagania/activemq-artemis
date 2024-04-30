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

package org.apache.activemq.artemis.tests.soak.brokerConnection.mirror;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.io.File;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.management.SimpleManagement;
import org.apache.activemq.artemis.core.config.amqpBrokerConnectivity.AMQPBrokerConnectionAddressType;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.tests.soak.SoakTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.tests.util.RandomUtil;
import org.apache.activemq.artemis.util.ServerUtil;
import org.apache.activemq.artemis.utils.FileUtil;
import org.apache.activemq.artemis.utils.Wait;
import org.apache.activemq.artemis.utils.actors.OrderedExecutor;
import org.apache.activemq.artemis.utils.cli.helper.HelperCreate;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleMirrorSoakTest extends SoakTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   private static final String TOPIC_NAME = "topicTest";
   private static final String QUEUE_NAME = "myQueue";

   private static String largeBody;
   private static String mediumBody;
   private static String smallBody = "This is a small body";

   static {
      StringWriter writer = new StringWriter();
      while (writer.getBuffer().length() < 1024 * 1024) {
         writer.append("This is a large string ..... ");
         if (mediumBody == null && writer.getBuffer().length() > 30 * 1024) {
            mediumBody = writer.toString();
         }
      }
      largeBody = writer.toString();
   }

   public static final String DC1_NODE_A = "SimpleMirrorSoakTest/DC1";
   public static final String DC2_NODE_A = "SimpleMirrorSoakTest/DC2";

   volatile Process processDC1;
   volatile Process processDC2;

   private static String DC1_NODEA_URI = "tcp://localhost:61616";
   private static String DC2_NODEA_URI = "tcp://localhost:61618";

   private static void createServer(String serverName, String connectionName, String mirrorURI, int porOffset, boolean paging) throws Exception {
      File serverLocation = getFileServerLocation(serverName);
      deleteDirectory(serverLocation);

      HelperCreate cliCreateServer = new HelperCreate();
      cliCreateServer.setAllowAnonymous(true).setArtemisInstance(serverLocation);
      cliCreateServer.setMessageLoadBalancing("ON_DEMAND");
      cliCreateServer.setClustered(false);
      cliCreateServer.setNoWeb(false);
      cliCreateServer.setArgs("--no-stomp-acceptor", "--no-hornetq-acceptor", "--no-mqtt-acceptor", "--no-amqp-acceptor", "--max-hops", "1", "--name", DC1_NODE_A);
      cliCreateServer.addArgs("--addresses", TOPIC_NAME);
      cliCreateServer.addArgs("--queues", QUEUE_NAME);
      cliCreateServer.setPortOffset(porOffset);
      cliCreateServer.createServer();

      Properties brokerProperties = new Properties();
      brokerProperties.put("AMQPConnections." + connectionName + ".uri", mirrorURI);
      brokerProperties.put("AMQPConnections." + connectionName + ".retryInterval", "1000");
      brokerProperties.put("AMQPConnections." + connectionName + ".type", AMQPBrokerConnectionAddressType.MIRROR.toString());
      brokerProperties.put("AMQPConnections." + connectionName + ".connectionElements.mirror.sync", "false");
      brokerProperties.put("largeMessageSync", "false");
      brokerProperties.put("mirrorAckManagerMaxPageAttempts", "10");
      brokerProperties.put("mirrorAckManagerRetryDelay", "1000");
      File brokerPropertiesFile = new File(serverLocation, "broker.properties");
      saveProperties(brokerProperties, brokerPropertiesFile);

      File brokerXml = new File(serverLocation, "/etc/broker.xml");
      Assert.assertTrue(brokerXml.exists());
      // Adding redistribution delay to broker configuration
      Assert.assertTrue(FileUtil.findReplace(brokerXml, "<address-setting match=\"#\">", "<address-setting match=\"#\">\n\n" + "            <redistribution-delay>0</redistribution-delay> <!-- added by SimpleMirrorSoakTest.java --> \n"));
      if (paging) {
         Assert.assertTrue(FileUtil.findReplace(brokerXml, "<max-size-messages>-1</max-size-messages>", "<max-size-messages>1</max-size-messages>"));
         Assert.assertTrue(FileUtil.findReplace(brokerXml, "<max-read-page-bytes>20M</max-read-page-bytes>", "<max-read-page-bytes>-1</max-read-page-bytes>"));
         Assert.assertTrue(FileUtil.findReplace(brokerXml, "<max-read-page-messages>-1</max-read-page-messages>", "<max-read-page-messages>100000</max-read-page-messages>\n" + "            <prefetch-page-messages>10000</prefetch-page-messages>"));
      }
   }

   public static void createRealServers(boolean paging) throws Exception {
      createServer(DC1_NODE_A, "mirror",  DC2_NODEA_URI, 0, paging);
      createServer(DC2_NODE_A, "mirror", DC1_NODEA_URI, 2, paging);
   }

   private void startServers() throws Exception {
      processDC1 = startServer(DC1_NODE_A, -1, -1, new File(getServerLocation(DC1_NODE_A), "broker.properties"));
      processDC2 = startServer(DC2_NODE_A, -1, -1, new File(getServerLocation(DC2_NODE_A), "broker.properties"));

      ServerUtil.waitForServerToStart(0, 10_000);
      ServerUtil.waitForServerToStart(2, 10_000);
   }

   @Test
   public void testMirroredTopics() throws Exception {
      createRealServers(true);
      startServers();

      final int numberOfMessages = 10_000;
      final int receiveCommitInterval = 100;
      final int sendCommitInterval = 100;
      final int killInterval = 500;
      Assert.assertTrue(killInterval > sendCommitInterval);

      Assert.assertTrue("numberOfMessages must be even", numberOfMessages % 2 == 0);

      String clientIDA = "nodeA";
      String clientIDB = "nodeB";
      String subscriptionID = "my-order";
      String snfQueue = "$ACTIVEMQ_ARTEMIS_MIRROR_mirror";

      ConnectionFactory connectionFactoryDC1A = CFUtil.createConnectionFactory("amqp", DC1_NODEA_URI);
      ConnectionFactory connectionFactoryDC2A = CFUtil.createConnectionFactory("amqp", DC2_NODEA_URI);

      consume(connectionFactoryDC1A, clientIDA, subscriptionID, 0, 0, false, false, receiveCommitInterval);
      consume(connectionFactoryDC1A, clientIDB, subscriptionID, 0, 0, false, false, receiveCommitInterval);

      SimpleManagement managementDC1 = new SimpleManagement(DC1_NODEA_URI, null, null);
      SimpleManagement managementDC2 = new SimpleManagement(DC2_NODEA_URI, null, null);

      Wait.assertEquals(0, () -> getCount(managementDC1, clientIDA + "." + subscriptionID));
      Wait.assertEquals(0, () -> getCount(managementDC2, clientIDA + "." + subscriptionID));
      Wait.assertEquals(0, () -> getCount(managementDC1, clientIDB + "." + subscriptionID));
      Wait.assertEquals(0, () -> getCount(managementDC2, clientIDB + "." + subscriptionID));

      ExecutorService executorService = Executors.newFixedThreadPool(3);
      runAfter(executorService::shutdownNow);
      executorService.execute(() -> {
         try {
            consume(connectionFactoryDC1A, clientIDA, subscriptionID, 0, numberOfMessages, true, false, receiveCommitInterval);
         } catch (Exception e) {
            logger.warn(e.getMessage(), e);
         }
      });
      executorService.execute(() -> {
         try {
            consume(connectionFactoryDC1A, clientIDB, subscriptionID, 0, numberOfMessages, true, false, receiveCommitInterval);
         } catch (Exception e) {
            logger.warn(e.getMessage(), e);
         }
      });

      OrderedExecutor restartExeuctor = new OrderedExecutor(executorService);
      AtomicBoolean running = new AtomicBoolean(true);
      runAfter(() -> running.set(false));

      try (Connection connection = connectionFactoryDC1A.createConnection()) {
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         MessageProducer producer = session.createProducer(session.createTopic(TOPIC_NAME));
         for (int i = 0; i < numberOfMessages; i++) {
            TextMessage message = session.createTextMessage(mediumBody);
            message.setIntProperty("i", i);
            message.setBooleanProperty("large", false);
            producer.send(message);
            if (i > 0 && i % sendCommitInterval == 0) {
               logger.info("Sent {} messages", i);
               session.commit();
            }
            if (i > 0 && i % killInterval == 0) {
               restartExeuctor.execute(() -> {
                  if (running.get()) {
                     try {
                        System.out.println("Restarting target");
                        if (processDC2 != null) {
                           processDC2.destroyForcibly();
                        }
                        processDC2 = startServer(DC2_NODE_A, 2, 10_000, new File(getServerLocation(DC2_NODE_A), "broker.properties"));
                     } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                     }
                  }
               });
            }
         }
         session.commit();
         running.set(false);
      }

      while (true) {
         try {
            System.out.println("SNF DC1 count = " + managementDC1.getMessageCountOnQueue(snfQueue));
            System.out.println("SNF DC2 count = " + managementDC2.getMessageCountOnQueue(snfQueue));
            System.out.println("DC2 count = " + managementDC2.getMessageCountOnQueue(snfQueue));

            System.out.println("DC1 ClientA - " + getCount(managementDC1, clientIDA + "." + subscriptionID));
            System.out.println("DC1 ClientA - " + getCount(managementDC1, clientIDB + "." + subscriptionID));
            System.out.println("DC2 ClientA - " + getCount(managementDC2, clientIDA + "." + subscriptionID));
            System.out.println("DC2 ClientA - " + getCount(managementDC2, clientIDB + "." + subscriptionID));
            System.out.println("looping...");
         } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
         }
         Thread.sleep(10_000);
      }
   }


   private static void consume(ConnectionFactory factory, String clientID, String subscriptionID, int start, int numberOfMessages, boolean expectEmpty, boolean assertBody, int batchCommit) throws Exception {
      try (Connection connection = factory.createConnection()) {
         connection.setClientID(clientID);
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic(TOPIC_NAME);
         connection.start();
         MessageConsumer consumer = session.createDurableConsumer(topic, subscriptionID);
         boolean failed = false;

         int pendingCommit = 0;

         for (int i = start; i < start + numberOfMessages; i++) {
            TextMessage message = (TextMessage) consumer.receive(10_000);
            Assert.assertNotNull(message);
            logger.debug("Received message {}, large={}", message.getIntProperty("i"), message.getBooleanProperty("large"));
            if (message.getIntProperty("i") != i) {
               failed = true;
               logger.warn("Expected message {} but got {}", i, message.getIntProperty("i"));
            }
            if (assertBody) {
               if (message.getBooleanProperty("large")) {
                  Assert.assertEquals(largeBody, message.getText());
               } else {
                  Assert.assertEquals(smallBody, message.getText());
               }
            }
            logger.debug("Consumed {}, large={}", i, message.getBooleanProperty("large"));
            pendingCommit++;
            if (pendingCommit >= batchCommit) {
               logger.info("received {}", i);
               session.commit();
               pendingCommit = 0;
            }
         }
         session.commit();

         Assert.assertFalse(failed);

         if (expectEmpty) {
            Assert.assertNull(consumer.receiveNoWait());
         }
      }
   }

   public long getCount(SimpleManagement simpleManagement, String queue) throws Exception {
      try {
         long value = simpleManagement.getMessageCountOnQueue(queue);
         logger.debug("count on queue {} is {}", queue, value);
         return value;
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
         return -1;
      }
   }
}
