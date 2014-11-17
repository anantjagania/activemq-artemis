/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.apache.activemq.tests.integration.management;
import org.apache.activemq.api.core.ActiveMQException;
import org.junit.Before;
import org.junit.After;

import org.junit.Test;

import static org.apache.activemq.api.core.management.CoreNotificationType.SECURITY_AUTHENTICATION_VIOLATION;
import static org.apache.activemq.api.core.management.CoreNotificationType.SECURITY_PERMISSION_VIOLATION;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.api.core.client.ClientConsumer;
import org.apache.activemq.api.core.client.ClientMessage;
import org.apache.activemq.api.core.client.ClientSession;
import org.apache.activemq.api.core.client.ClientSessionFactory;
import org.apache.activemq.api.core.client.HornetQClient;
import org.apache.activemq.api.core.client.ServerLocator;
import org.apache.activemq.api.core.management.ManagementHelper;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.core.security.CheckType;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.server.HornetQServer;
import org.apache.activemq.core.server.HornetQServers;
import org.apache.activemq.spi.core.security.HornetQSecurityManager;
import org.apache.activemq.tests.util.RandomUtil;
import org.apache.activemq.tests.util.UnitTestCase;

/**
 * A SecurityNotificationTest
 *
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 *
 */
public class SecurityNotificationTest extends UnitTestCase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private HornetQServer server;

   private ClientSession adminSession;

   private ClientConsumer notifConsumer;

   private SimpleString notifQueue;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   @Test
   public void testSECURITY_AUTHENTICATION_VIOLATION() throws Exception
   {
      String unknownUser = RandomUtil.randomString();

      SecurityNotificationTest.flush(notifConsumer);

      ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(UnitTestCase.INVM_CONNECTOR_FACTORY));
      ClientSessionFactory sf = createSessionFactory(locator);

      try
      {
         sf.createSession(unknownUser, RandomUtil.randomString(), false, true, true, false, 1);
         Assert.fail("authentication must fail and a notification of security violation must be sent");
      }
      catch (Exception e)
      {
      }

      ClientMessage[] notifications = SecurityNotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(SECURITY_AUTHENTICATION_VIOLATION.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals(unknownUser, notifications[0].getObjectProperty(ManagementHelper.HDR_USER).toString());
   }

   @Test
   public void testSECURITY_PERMISSION_VIOLATION() throws Exception
   {
      SimpleString queue = RandomUtil.randomSimpleString();
      SimpleString address = RandomUtil.randomSimpleString();

      // guest can not create queue
      Role role = new Role("roleCanNotCreateQueue", true, true, false, true, false, true, true);
      Set<Role> roles = new HashSet<Role>();
      roles.add(role);
      server.getSecurityRepository().addMatch(address.toString(), roles);
      HornetQSecurityManager securityManager = server.getSecurityManager();
      securityManager.addRole("guest", "roleCanNotCreateQueue");

      SecurityNotificationTest.flush(notifConsumer);

      ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(UnitTestCase.INVM_CONNECTOR_FACTORY));
      ClientSessionFactory sf = createSessionFactory(locator);
      ClientSession guestSession = sf.createSession("guest", "guest", false, true, true, false, 1);

      try
      {
         guestSession.createQueue(address, queue, true);
         Assert.fail("session creation must fail and a notification of security violation must be sent");
      }
      catch (Exception e)
      {
      }

      ClientMessage[] notifications = SecurityNotificationTest.consumeMessages(1, notifConsumer);
      Assert.assertEquals(SECURITY_PERMISSION_VIOLATION.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_NOTIFICATION_TYPE).toString());
      Assert.assertEquals("guest", notifications[0].getObjectProperty(ManagementHelper.HDR_USER).toString());
      Assert.assertEquals(address.toString(), notifications[0].getObjectProperty(ManagementHelper.HDR_ADDRESS)
                                                              .toString());
      Assert.assertEquals(CheckType.CREATE_DURABLE_QUEUE.toString(),
                          notifications[0].getObjectProperty(ManagementHelper.HDR_CHECK_TYPE).toString());

      guestSession.close();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      Configuration conf = createBasicConfig()
         .setSecurityEnabled(true)
         // the notifications are independent of JMX
         .setJMXManagementEnabled(false)
         .addAcceptorConfiguration(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      server = HornetQServers.newHornetQServer(conf, false);
      server.start();

      notifQueue = RandomUtil.randomSimpleString();

      HornetQSecurityManager securityManager = server.getSecurityManager();
      securityManager.addUser("admin", "admin");
      securityManager.addUser("guest", "guest");
      securityManager.setDefaultUser("guest");

      Role role = new Role("notif", true, true, true, true, true, true, true);
      Set<Role> roles = new HashSet<Role>();
      roles.add(role);
      server.getSecurityRepository().addMatch(ActiveMQDefaultConfiguration.getDefaultManagementNotificationAddress().toString(),
                                              roles);

      securityManager.addRole("admin", "notif");

      ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(UnitTestCase.INVM_CONNECTOR_FACTORY));
      ClientSessionFactory sf = createSessionFactory(locator);
      adminSession = sf.createSession("admin", "admin", false, true, true, false, 1);
      adminSession.start();

      adminSession.createTemporaryQueue(ActiveMQDefaultConfiguration.getDefaultManagementNotificationAddress(), notifQueue);

      notifConsumer = adminSession.createConsumer(notifQueue);
   }

   @Override
   @After
   public void tearDown() throws Exception
   {
      notifConsumer.close();

      adminSession.deleteQueue(notifQueue);
      adminSession.close();

      server.stop();

      super.tearDown();
   }

   // Private -------------------------------------------------------

   private static void flush(final ClientConsumer notifConsumer) throws ActiveMQException
   {
      ClientMessage message = null;
      do
      {
         message = notifConsumer.receive(500);
      }
      while (message != null);
   }

   protected static ClientMessage[] consumeMessages(final int expected, final ClientConsumer consumer) throws Exception
   {
      ClientMessage[] messages = new ClientMessage[expected];

      ClientMessage m = null;
      for (int i = 0; i < expected; i++)
      {
         m = consumer.receive(500);
         if (m != null)
         {
            for (SimpleString key : m.getPropertyNames())
            {
               System.out.println(key + "=" + m.getObjectProperty(key));
            }
         }
         Assert.assertNotNull("expected to received " + expected + " messages, got only " + i, m);
         messages[i] = m;
         m.acknowledge();
      }
      m = consumer.receiveImmediate();
      if (m != null)
      {
         for (SimpleString key : m.getPropertyNames())

         {
            System.out.println(key + "=" + m.getObjectProperty(key));
         }
      }
      Assert.assertNull("received one more message than expected (" + expected + ")", m);

      return messages;
   }

   // Inner classes -------------------------------------------------

}
