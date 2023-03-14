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

package org.apache.activemq.artemis.tests.soak.interruptlm;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.management.ObjectNameBuilder;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.tests.soak.SoakTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.tests.util.Wait;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is used to kill a server and make sure the server will remove any pending files.
public class ClusteredLargeMessageInterruptTest extends SoakTestBase {

   public static final String SERVER_NAME_0 = "lmbroker1";
   public static final String SERVER_NAME_1 = "lmbroker2";
   private static final String JMX_SERVER_HOSTNAME = "localhost";
   private static final int JMX_SERVER_PORT_0 = 1099;
   private static final int JMX_SERVER_PORT_1 = 1199;
   private volatile boolean runningSend = true;
   private volatile boolean runningConsumer = true;
   private final AtomicInteger errors = new AtomicInteger(0);
   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   static String firstURI = "service:jmx:rmi:///jndi/rmi://" + JMX_SERVER_HOSTNAME + ":" + JMX_SERVER_PORT_0 + "/jmxrmi";
   static String secondURI = "service:jmx:rmi:///jndi/rmi://" + JMX_SERVER_HOSTNAME + ":" + JMX_SERVER_PORT_1 + "/jmxrmi";
   static ObjectNameBuilder firstNameBuilder = ObjectNameBuilder.create(ActiveMQDefaultConfiguration.getDefaultJmxDomain(), "lmbroker1", true);
   static ObjectNameBuilder secondNameBuilder = ObjectNameBuilder.create(ActiveMQDefaultConfiguration.getDefaultJmxDomain(), "lmbroker2", true);

   static final String largebody = createBody();
   static final int BODY_SIZE = 500 * 1024;

   private static String createBody() {
      StringBuffer buffer = new StringBuffer();
      while (buffer.length() < BODY_SIZE) {
         buffer.append("LOREM IPSUM WHATEVER THEY SAY IN THERE I DON'T REALLY CARE. I'M NOT SURE IF IT'S LOREM, LAUREM, LAUREN, IPSUM OR YPSUM AND I DON'T REALLY CARE ");
      }
      return buffer.toString();
   }

   Process serverProcess;
   Process serverProcess2;

   public ConnectionFactory createConnectionFactory(int broker, String protocol) {
      if (protocol.equals("CORE")) {
         switch (broker) {
            case 0:
               return new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory("tcp://localhost:61616?ha=false&useTopologyForLoadBalancing=false&callTimeout=1000");
            case 1:
               return new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory("tcp://localhost:61716?ha=false&useTopologyForLoadBalancing=false&callTimeout=1000");
            default:
               logger.warn("undefined argument {}", broker);
               throw new IllegalArgumentException("undefined");
         }
      } else {
         return CFUtil.createConnectionFactory(protocol, "tcp://localhost:" + (61616 + broker * 100) + "?ha=false");
      }
   }

   @Before
   public void before() throws Exception {
      cleanupData(SERVER_NAME_0);
      cleanupData(SERVER_NAME_1);
      serverProcess = startServer0();
      serverProcess2 = startServer1();
      disableCheckThread();
   }

   private Process startServer0() throws Exception {
      return startServer(SERVER_NAME_0, 0, 30000);
   }

   private Process startServer1() throws Exception {
      return startServer(SERVER_NAME_1, 100, 30000);
   }

   @Test
   public void testInterruptLargeMessageAMQPTX() throws Throwable {
      testInterruptLM("AMQP", true);
   }

   @Test
   public void testInterruptLargeMessageAMQPNonTX() throws Throwable {
      testInterruptLM("AMQP", false);
   }

   @Test
   public void testInterruptLargeMessageCORETX() throws Throwable {
      testInterruptLM("CORE", true);
   }

   @Test
   public void testInterruptLargeMessageOPENWIRETX() throws Throwable {
      testInterruptLM("OPENWIRE", true);
   }

   @Test
   public void testInterruptLargeMessageCORENonTX() throws Throwable {
      testInterruptLM("CORE", false);
   }

   private CountDownLatch startSendingThreads(Executor executor, String protocol, int broker, int threads, boolean tx, String queueName) {
      runningSend = true;
      CountDownLatch done = new CountDownLatch(threads);

      ConnectionFactory factory = createConnectionFactory(broker, protocol);
      if (factory instanceof ActiveMQConnectionFactory) {
         if (((ActiveMQConnectionFactory)factory).getServerLocator().getUseTopologyForLoadBalancing()) {
            logger.warn("WHAT??");
            System.exit(-1);
         }
      }
      final CyclicBarrier startFlag = new CyclicBarrier(threads);

      for (int i = 0; i < threads; i++) {
         int threadID = i;
         executor.execute(() -> {
            int numberOfMessages = 0;
            try {
               Connection connection = factory.createConnection();
               Session session = connection.createSession(tx, tx ? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE);
               MessageProducer producer = session.createProducer(session.createQueue(queueName));

               startFlag.await(10, TimeUnit.SECONDS);
               while (runningSend) {
                  producer.send(session.createTextMessage(largebody));
                  if (tx) {
                     session.commit();
                  }
                  if (numberOfMessages++ % 10 == 0) {
                     logger.info("Sent {}", numberOfMessages);
                  }
               }
            } catch (Exception e) {
               logger.info("Thread {} got an error {}", threadID, e.getMessage());
            } finally {
               done.countDown();
               logger.info("CountDown:: current Count {}", done.getCount());
            }
         });
      }

      return done;
   }


   private CountDownLatch startConsumingThreads(Executor executor, String protocol, int broker, int threads, boolean tx, String queueName) {
      runningConsumer = true;
      CountDownLatch done = new CountDownLatch(threads);

      ConnectionFactory factory = createConnectionFactory(broker, protocol);
      final CyclicBarrier startFlag = new CyclicBarrier(threads);

      for (int i = 0; i < threads; i++) {
         executor.execute(() -> {
            int numberOfMessages = 0;
            try {
               Connection connection = factory.createConnection();
               connection.start();
               Session session = connection.createSession(tx, tx ? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE);
               MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));

               startFlag.await(10, TimeUnit.SECONDS);
               while (runningConsumer) {
                  TextMessage message = (TextMessage)consumer.receive(100);
                  if (message != null) {
                     if (!message.getText().startsWith(largebody)) {
                        logger.warn("Body does not match!");
                        errors.incrementAndGet();
                     }
                     if (tx) {
                        session.commit();
                     }
                     if (numberOfMessages++ % 10 == 0) {
                        logger.info("Received {}", numberOfMessages);
                     }
                  }
               }
            } catch (Exception e) {
            } finally {
               logger.info("Done sending");
               done.countDown();
            }
         });
      }

      return done;
   }



   // this test has sleeps as the test will send while still active
   // we keep sending all the time.. so the testInterruptLM acts like a controller telling the threads when to stop
   private void testInterruptLM(String protocol, boolean tx) throws Throwable {
      final int SENDING_THREADS = 10;
      final int CONSUMING_THREADS = 10;
      final AtomicInteger errors = new AtomicInteger(0); // I don't expect many errors since this test is disconnecting and reconnecting the server
      final CountDownLatch killAt = new CountDownLatch(40);

      ExecutorService executorService = Executors.newFixedThreadPool(SENDING_THREADS + CONSUMING_THREADS);
      runAfter(executorService::shutdownNow);

      String queueName = "ClusteredLargeMessageInterruptTest";

      Thread.sleep(1000);

      CountDownLatch sendDone = startSendingThreads(executorService, protocol, 0, SENDING_THREADS, tx, queueName);
      CountDownLatch receiverDone = startConsumingThreads(executorService, protocol, 0, CONSUMING_THREADS, tx, queueName);

      Thread.sleep(2000);

      serverProcess.destroyForcibly();
      runningSend = false;
      runningConsumer = false;
      Assert.assertTrue(serverProcess.waitFor(10, TimeUnit.MINUTES));
      Assert.assertTrue(receiverDone.await(10, TimeUnit.SECONDS));
      Assert.assertTrue(sendDone.await(1, TimeUnit.MINUTES));

      logger.info("All receivers and senders are done!!!");

      serverProcess = startServer0();

      Thread.sleep(2000);

      sendDone = startSendingThreads(executorService, protocol, 1, SENDING_THREADS, tx, queueName);
      receiverDone = startConsumingThreads(executorService, protocol, 1, CONSUMING_THREADS, tx, queueName);

      serverProcess2.destroyForcibly();
      Assert.assertTrue(serverProcess2.waitFor(10, TimeUnit.MINUTES));
      runningSend = false;
      runningConsumer = false;
      Assert.assertTrue(sendDone.await(1, TimeUnit.MINUTES));
      Assert.assertTrue(receiverDone.await(10, TimeUnit.SECONDS));

      serverProcess2 = startServer1();

      sendDone = startSendingThreads(executorService, protocol, 1, SENDING_THREADS, tx, queueName);
      receiverDone = startConsumingThreads(executorService, protocol, 1, CONSUMING_THREADS, tx, queueName);

      Thread.sleep(2000);
      runningSend = false;
      Assert.assertTrue(sendDone.await(10, TimeUnit.SECONDS));

      File lmFolder = new File(getServerLocation(SERVER_NAME_0) + "/data/large-messages");
      File lmFolder2 = new File(getServerLocation(SERVER_NAME_1) + "/data/large-messages");

      logger.info("Waiting consumers to receive everything");
      runningConsumer = false;
      Assert.assertTrue(receiverDone.await(10, TimeUnit.SECONDS));

      Wait.assertEquals(0, () -> lmFolder.listFiles().length);
      Wait.assertEquals(0, () -> lmFolder2.listFiles().length);
      Assert.assertEquals(0, errors.get());

   }

}