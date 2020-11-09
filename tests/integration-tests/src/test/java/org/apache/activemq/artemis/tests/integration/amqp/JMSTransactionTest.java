/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.amqp;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.tests.util.Wait;
import org.junit.Assert;
import org.junit.Test;

public class JMSTransactionTest extends JMSClientTestSupport {

   protected String getConfiguredProtocols() {
      return "AMQP,OPENWIRE,CORE";
   }

   @Test(timeout = 60000)
   public void testProduceMessageAndCommit() throws Throwable {
      Connection connection = createConnection();
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      instanceLog.debug("queue:" + queue.getQueueName());
      MessageProducer p = session.createProducer(queue);
      for (int i = 0; i < 10; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("Message:" + i);
         p.send(message);
      }

      session.commit();
      session.close();

      Queue queueView = getProxyToQueue(getQueueName());

      Wait.assertEquals(10, queueView::getMessageCount);
   }

   @Test(timeout = 60000)
   public void testProduceMessageAndRollback() throws Throwable {
      Connection connection = createConnection();
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      instanceLog.debug("queue:" + queue.getQueueName());
      MessageProducer p = session.createProducer(queue);
      for (int i = 0; i < 10; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("Message:" + i);
         p.send(message);
      }

      session.rollback();
      session.close();

      Queue queueView = getProxyToQueue(getQueueName());
      Wait.assertEquals(0, queueView::getMessageCount);
   }

   @Test(timeout = 60000)
   public void testProducedMessageAreRolledBackOnSessionClose() throws Exception {
      int numMessages = 10;

      Connection connection = createConnection();
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      MessageProducer p = session.createProducer(queue);
      byte[] bytes = new byte[2048];
      new Random().nextBytes(bytes);
      for (int i = 0; i < numMessages; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("msg:" + i);
         p.send(message);
      }

      session.close();

      Queue queueView = getProxyToQueue(getQueueName());
      Wait.assertEquals(0, queueView::getMessageCount);
   }

   @Test(timeout = 60000)
   public void testConsumeMessagesAndCommit() throws Throwable {
      Connection connection = createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      instanceLog.debug("queue:" + queue.getQueueName());
      MessageProducer p = session.createProducer(queue);
      for (int i = 0; i < 10; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("Message:" + i);
         p.send(message);
      }
      session.close();

      session = connection.createSession(true, Session.SESSION_TRANSACTED);
      MessageConsumer cons = session.createConsumer(queue);
      connection.start();

      for (int i = 0; i < 10; i++) {
         TextMessage message = (TextMessage) cons.receive(5000);
         Assert.assertNotNull(message);
         Assert.assertEquals("Message:" + i, message.getText());
      }
      session.commit();
      session.close();

      Queue queueView = getProxyToQueue(getQueueName());
      Wait.assertEquals(0, queueView::getMessageCount);
   }

   @Test(timeout = 60000)
   public void testConsumeMessagesAndRollback() throws Throwable {
      Connection connection = createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      MessageProducer p = session.createProducer(queue);
      for (int i = 0; i < 10; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("Message:" + i);
         p.send(message);
      }
      session.close();

      session = connection.createSession(true, Session.SESSION_TRANSACTED);
      MessageConsumer cons = session.createConsumer(queue);
      connection.start();

      for (int i = 0; i < 10; i++) {
         TextMessage message = (TextMessage) cons.receive(5000);
         Assert.assertNotNull(message);
         Assert.assertEquals("Message:" + i, message.getText());
      }

      session.rollback();

      Queue queueView = getProxyToQueue(getQueueName());
      Wait.assertEquals(10, queueView::getMessageCount);
   }

   @Test(timeout = 60000)
   public void testAckAndSendWithDuplicateAMQP() throws Throwable {
      testAckAndSendWithDuplicate("AMQP");
   }

   /** the main purpose of this test is to validate the test itself.
    *  I am more concerned to make sure the broker will behave similarly to both Core AND AMQP
    *  then I am to find bugs on the core protocol for this one. */
   @Test(timeout = 60000)
   public void testAckAndSendWithDuplicateCORE() throws Throwable {
      testAckAndSendWithDuplicate("CORE");
   }

   public void testAckAndSendWithDuplicate(String protocol) throws Throwable {
      String inputQueue = getQueueName() + ".input";
      String outputQueue = getQueueName() + ".output";
      server.addAddressInfo(new AddressInfo(inputQueue).addRoutingType(RoutingType.ANYCAST).setAutoCreated(false));
      server.addAddressInfo(new AddressInfo(outputQueue).addRoutingType(RoutingType.ANYCAST).setAutoCreated(false));
      server.createQueue(new QueueConfiguration(inputQueue).setAddress(inputQueue).setRoutingType(RoutingType.ANYCAST).setDurable(true));
      server.createQueue(new QueueConfiguration(outputQueue).setAddress(outputQueue).setRoutingType(RoutingType.ANYCAST).setDurable(true));

      ConnectionFactory factory = CFUtil.createConnectionFactory(protocol, "tcp://localhost:5672");
      Connection connection = factory.createConnection();
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      javax.jms.Queue queueinput = session.createQueue(inputQueue);
      javax.jms.Queue queueouptut = session.createQueue(outputQueue);
      try (MessageProducer producerinput = session.createProducer(queueinput)) {
         for (int i = 0; i < 10; i++) {
            TextMessage message = session.createTextMessage();
            message.setText("Message:" + i);
            producerinput.send(message);
         }
         session.commit();
      }

      connection.start();
      try (MessageConsumer consumer = session.createConsumer(queueinput); MessageProducer producer = session.createProducer(queueouptut)) {
         for (int i = 0; i < 10; i++) {
            TextMessage received = (TextMessage) consumer.receive(5000);
            Assert.assertNotNull(received);
            TextMessage sending = session.createTextMessage();
            sending.setStringProperty("_AMQ_DUPL_ID", "itwillcertainlyfail");
            producer.send(sending);
         }
         session.commit();
      } catch (Exception e) {
         e.printStackTrace();
      }

      session.rollback();

      try (MessageConsumer consumer = session.createConsumer(queueinput); MessageProducer producer = session.createProducer(queueouptut)) {
         for (int i = 0; i < 10; i++) {
            TextMessage received = (TextMessage)consumer.receive(5000);
            Assert.assertNotNull(received);
            TextMessage sending = session.createTextMessage();
            sending.setStringProperty("_AMQ_DUPL_ID", "message:" + i);
            producer.send(sending);
            session.commit();
         }
      }

      try (MessageConsumer consumer = session.createConsumer(queueinput)) {
         Assert.assertNull(consumer.receiveNoWait());
      }

      try (MessageConsumer consumer = session.createConsumer(queueouptut)) {
         for (int i = 0; i < 10; i++) {
            TextMessage received = (TextMessage)consumer.receive(5000);
            Assert.assertNotNull(received);
         }
         Assert.assertNull(consumer.receiveNoWait());
         session.commit();
      }

   }

   @Test(timeout = 60000)
   public void testRollbackSomeThenReceiveAndCommit() throws Exception {
      final int MSG_COUNT = 5;
      final int consumeBeforeRollback = 2;

      Connection connection = createConnection();
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      javax.jms.Queue queue = session.createQueue(getQueueName());

      MessageProducer p = session.createProducer(queue);
      for (int i = 0; i < MSG_COUNT; i++) {
         TextMessage message = session.createTextMessage();
         message.setText("Message:" + i);
         message.setIntProperty("MESSAGE_NUMBER", i + 1);
         p.send(message);
      }

      session.commit();

      Queue queueView = getProxyToQueue(getQueueName());
      Wait.assertEquals(MSG_COUNT, queueView::getMessageCount);

      MessageConsumer consumer = session.createConsumer(queue);

      for (int i = 1; i <= consumeBeforeRollback; i++) {
         Message message = consumer.receive(1000);
         assertNotNull(message);
         assertEquals("Unexpected message number", i, message.getIntProperty("MESSAGE_NUMBER"));
      }

      session.rollback();

      Wait.assertEquals(MSG_COUNT, queueView::getMessageCount);

      // Consume again..check we receive all the messages.
      Set<Integer> messageNumbers = new HashSet<>();
      for (int i = 1; i <= MSG_COUNT; i++) {
         messageNumbers.add(i);
      }

      for (int i = 1; i <= MSG_COUNT; i++) {
         Message message = consumer.receive(1000);
         assertNotNull(message);
         int msgNum = message.getIntProperty("MESSAGE_NUMBER");
         messageNumbers.remove(msgNum);
      }

      session.commit();

      assertTrue("Did not consume all expected messages, missing messages: " + messageNumbers, messageNumbers.isEmpty());
      assertEquals("Queue should have no messages left after commit", 0, queueView.getMessageCount());
   }
}
