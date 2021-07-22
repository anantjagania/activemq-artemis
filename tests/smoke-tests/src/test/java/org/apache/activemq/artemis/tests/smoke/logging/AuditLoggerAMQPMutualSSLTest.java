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
package org.apache.activemq.artemis.tests.smoke.logging;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.Test;

/**
 * The various keystore files used for this test were generated using the following commands:
 *
 * keytool -genkey -keystore server-side-keystore.jks -storepass secureexample -keypass secureexample -dname "CN=ActiveMQ Artemis Server, OU=Artemis, O=ActiveMQ, L=AMQ, S=AMQ, C=AMQ" -keyalg RSA
 * keytool -export -keystore server-side-keystore.jks -file server-side-cert.cer -storepass secureexample
 * keytool -import -keystore client-side-truststore.jks -file server-side-cert.cer -storepass secureexample -keypass secureexample -noprompt
 * keytool -genkey -keystore client-side-keystore.jks -storepass secureexample -keypass secureexample -dname "CN=ActiveMQ Artemis Client, OU=Artemis, O=ActiveMQ, L=AMQ, S=AMQ, C=AMQ" -keyalg RSA
 * keytool -export -keystore client-side-keystore.jks -file client-side-cert.cer -storepass secureexample
 * keytool -import -keystore server-side-truststore.jks -file client-side-cert.cer -storepass secureexample -keypass secureexample -noprompt
 */
public class AuditLoggerAMQPMutualSSLTest extends AuditLoggerTestBase {

   @Override
   protected String getServerName() {
      return "audit-logging-amqp-mutual-ssl";
   }

   @Test
   public void testAuditForProducerAndConsumer() throws Exception {
      String sslhost = "amqps://localhost:5500";

      int maxInactivityDurationInitialDelay = 30000;
      int idleTimeout = 120000;
      boolean verifyHost = false;
      String keyStoreLocation = "target/" + getServerName() + "/etc/client-side-keystore.jks";
      String keyStorePassword = "secureexample";
      String trustStoreLocation = "target/" + getServerName() + "/etc/client-side-truststore.jks";
      String trustStorePassword = "secureexample";

      String remoteUri = sslhost +
         "?maxInactivityDurationInitialDelay=" + maxInactivityDurationInitialDelay +
         "&amqp.idleTimeout=" + idleTimeout +
         "&transport.verifyHost=" + verifyHost +
         "&transport.keyStoreLocation=" + keyStoreLocation +
         "&transport.keyStorePassword=" + keyStorePassword +
         "&transport.trustStoreLocation=" + trustStoreLocation +
         "&transport.trustStorePassword=" + trustStorePassword;

      ConnectionFactory connectionFactory = new JmsConnectionFactory(remoteUri);
      try (Connection connection = connectionFactory.createConnection()) {
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue queue = session.createQueue("exampleQueue");
         MessageProducer sender = session.createProducer(queue);
         TextMessage stm = session.createTextMessage("Hello world ");
         stm.setStringProperty("foo", "bar");
         sender.send(stm);
         MessageConsumer consumer = session.createConsumer(queue);
         connection.start();
         Message m = consumer.receive(500);
         assertNotNull(m);
      }

      checkAuditLogRecord(true, "AMQ601715: User myUser(producers)@", "successfully authenticated");
      checkAuditLogRecord(true, "AMQ601267: User myUser(producers)@", "is creating a core session");
      checkAuditLogRecord(true, "AMQ601500: User myUser(producers)@", "is sending a message AMQPStandardMessage");
      checkAuditLogRecord(true, "AMQ601265: User myUser(producers)@", "is creating a core consumer");
      checkAuditLogRecord(true, "AMQ601501: User myUser(producers)@", "is consuming a message from exampleQueue");
   }
}
