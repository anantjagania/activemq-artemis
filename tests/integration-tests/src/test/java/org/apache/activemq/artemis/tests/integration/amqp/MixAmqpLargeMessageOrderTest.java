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

package org.apache.activemq.artemis.tests.integration.amqp;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.tests.util.CFUtil;
import org.junit.Assert;
import org.junit.Test;

public class MixAmqpLargeMessageOrderTest extends AmqpClientTestSupport {

   private static int OK = 1;
   private static int NUMBER_OF_MESSAGES = 1000;

   @Test
   public void testMixSize() throws Exception {


      ConnectionFactory factory = CFUtil.createConnectionFactory("AMQP", "tcp://localhost:5672");
      try (Connection connection = factory.createConnection()) {
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         MessageProducer producer = session.createProducer(session.createQueue(getName()));
         String largeBody;
         {
            StringBuilder builder = new StringBuilder();
            while (builder.length() < 150_000) {
               builder.append("O Rato roeu a roupa do rei de roma.. Sally Sells see shell by the sea shore!  Lorem Ipsum Whatever!! ");
            }
            largeBody = builder.toString();
         }

         for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            Message message;
            if (i % 50 == 0) {
               message = session.createTextMessage(largeBody);
            } else {
               message = session.createTextMessage("small body");
            }
            message.setIntProperty("i", i);
            producer.send(message);
            if (i % 1000 == 0 && i > 0) {
               session.commit();
            }
         }
         session.commit();

         connection.start();
         for (int repeat = 0; repeat < 3; repeat++) {
            MessageConsumer consumer = session.createConsumer(session.createQueue(getName()));
            for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
               TextMessage message = (TextMessage) consumer.receive(1000);
               Assert.assertNotNull(message);
               Assert.assertEquals(i, message.getIntProperty("i"));
            }
            Assert.assertNull(consumer.receiveNoWait());
            session.rollback();
            consumer.close();
         }
      }

   }

}
