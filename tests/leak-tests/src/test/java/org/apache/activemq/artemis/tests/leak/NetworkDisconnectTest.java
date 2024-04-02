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
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import io.github.checkleak.core.CheckLeak;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
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

public class NetworkDisconnectTest extends ActiveMQTestBase {

   @BeforeClass
   public static void start() {
      NetUtil.skipIfNotSudo();
   }

   // 192.0.2.0 is reserved for documentation (and testing on this case).
   private static final String LIVE_IP = "192.0.2.0";

   @Test
   public void testLeakAfterDisconnect() throws Exception {
      CheckLeak checkLeak = new CheckLeak();

      NetUtil.netUp(LIVE_IP, "lo0");

      runAfter(() -> NetUtil.netDown(LIVE_IP));

      ActiveMQServer server = createServer(true, createDefaultConfig(true), AddressSettings.DEFAULT_PAGE_SIZE, AddressSettings.DEFAULT_MAX_SIZE_BYTES, -1, -1);
      server.getConfiguration().clearAcceptorConfigurations();
      server.getConfiguration().addAcceptorConfiguration("live", "tcp://" + LIVE_IP + ":61616");
      server.start();

      ConnectionFactory factory = CFUtil.createConnectionFactory("CORE", "tcp://" + LIVE_IP + ":61616?ha=true&reconnectAttempts=-1&connectionTTL=1000&callTimeout=1000&clientFailureCheckPeriod=1000");
      Assert.assertTrue(((ActiveMQConnectionFactory)factory).isHA());
      Assert.assertEquals(-1, ((ActiveMQConnectionFactory)factory).getReconnectAttempts());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getConnectionTTL());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getClientFailureCheckPeriod());
      Assert.assertEquals(1000, ((ActiveMQConnectionFactory)factory).getCallTimeout());

      Connection conn = factory.createConnection();

      Session session = conn.createSession();
      MessageProducer producer = session.createProducer(session.createQueue(getName()));
      producer.send(session.createTextMessage("hello"));
      conn.start();
      MessageConsumer consumer = session.createConsumer(session.createQueue(getName()));
      Assert.assertNotNull(consumer.receive(5000));
      Queue queue = server.locateQueue(getName());
      for (int i = 0; i < 10; i++) {
         MessageConsumer emptyConsumer = session.createConsumer(session.createQueue(getName()));
         Assert.assertNull(emptyConsumer.receiveNoWait());
      }

      NetUtil.netDown(LIVE_IP);

      Wait.assertEquals(0, queue::getConsumerCount);

      int numberOfIterators = checkLeak.getAllObjects(LinkedListImpl.Iterator.class).length;
      Assert.assertEquals("Redistributors are leaking " + LinkedListImpl.Iterator.class.getName(), 0, numberOfIterators);
   }


   @Test
   public void testLeakAfterStop() throws Exception {
      CheckLeak checkLeak = new CheckLeak();

      NetUtil.netUp(LIVE_IP, "lo0");

      runAfter(() -> NetUtil.netDown(LIVE_IP));

      ActiveMQServer server = createServer(true, createDefaultConfig(true), AddressSettings.DEFAULT_PAGE_SIZE, AddressSettings.DEFAULT_MAX_SIZE_BYTES, -1, -1);
      server.getConfiguration().clearAcceptorConfigurations();
      server.getConfiguration().addAcceptorConfiguration("live", "tcp://" + LIVE_IP + ":61616");
      server.start();

      ConnectionFactory factory = CFUtil.createConnectionFactory("CORE", "tcp://" + LIVE_IP + ":61616?ha=true&reconnectAttempts=-1&connectionTTL=1000&callTimeout=1000&clientFailureCheckPeriod=1000");
      Assert.assertTrue(((ActiveMQConnectionFactory)factory).isHA());
      Assert.assertEquals(-1, ((ActiveMQConnectionFactory)factory).getReconnectAttempts());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getConnectionTTL());
      Assert.assertEquals(1000L, ((ActiveMQConnectionFactory)factory).getClientFailureCheckPeriod());
      Assert.assertEquals(1000, ((ActiveMQConnectionFactory)factory).getCallTimeout());

      Connection conn = factory.createConnection();

      Session session = conn.createSession();
      MessageProducer producer = session.createProducer(session.createQueue(getName()));
      producer.send(session.createTextMessage("hello"));
      conn.start();
      MessageConsumer consumer = session.createConsumer(session.createQueue(getName()));
      Assert.assertNotNull(consumer.receive(5000));
      Queue queue = server.locateQueue(getName());
      for (int i = 0; i < 10; i++) {
         MessageConsumer emptyConsumer = session.createConsumer(session.createQueue(getName()));
         Assert.assertNull(emptyConsumer.receiveNoWait());
      }

      server.stop();

      int numberOfIterators = checkLeak.getAllObjects(LinkedListImpl.Iterator.class).length;
      Assert.assertEquals("Redistributors are leaking " + LinkedListImpl.Iterator.class.getName(), 0, numberOfIterators);

   }

}
