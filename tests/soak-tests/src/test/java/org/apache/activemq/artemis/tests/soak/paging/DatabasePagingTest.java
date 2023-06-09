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

package org.apache.activemq.artemis.tests.soak.paging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.tests.soak.SoakTestBase;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.activemq.artemis.tests.soak.TestParameters.testProperty;

public class DatabasePagingTest extends SoakTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   private static final String TEST_NAME = "DB";

   // if you set this property to true, you can use the ./start-mysql-podman.sh from ./src/test/scripts
   private static final boolean USE_MYSQL = Boolean.parseBoolean(testProperty(TEST_NAME, "USE_MYSQL", "true"));

   public static final String SERVER_NAME_0 = "database-paging/" + (USE_MYSQL ? "mysql" : "derby");

   Process serverProcess;

   @Before
   public void before() throws Exception {
      cleanupData(SERVER_NAME_0);

      serverProcess = startServer(SERVER_NAME_0, 0, 60_000);
   }


   @Test
   public void testPaging() throws Exception {
      testPaging("CORE");
   }

   public void testPaging(String protocol) throws Exception {
      logger.info("performing paging test on {}", protocol);

      ConnectionFactory connectionFactory = CFUtil.createConnectionFactory(protocol, "tcp://localhost:61616");

      try (Connection connection = connectionFactory.createConnection()) {
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Queue queue = session.createQueue("MY_QUEUE");
         MessageProducer producer = session.createProducer(queue);
         for (int i = 0; i < 2000; i++) {
            TextMessage message = session.createTextMessage("message " + i);
            message.setIntProperty("i", i);
            producer.send(message);
            if (i % 100 == 0) {
               session.commit();
            }
         }
         session.commit();

      }

      serverProcess.destroyForcibly();
      serverProcess.waitFor(1, TimeUnit.MINUTES);
      Assert.assertFalse(serverProcess.isAlive());

      serverProcess = startServer(SERVER_NAME_0, 0, 60_000);


      try (Connection connection = connectionFactory.createConnection()) {
         connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue queue = session.createQueue("MY_QUEUE");
         MessageConsumer consumer = session.createConsumer(queue);
         for (int i = 0; i < 2000; i++) {
            TextMessage message = (TextMessage) consumer.receive(5000);
            Assert.assertNotNull(message);
            Assert.assertEquals(i, message.getIntProperty("i"));
         }
      }


   }

}
