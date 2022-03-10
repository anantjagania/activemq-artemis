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
package org.apache.activemq.artemis.tests.integration.paging;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.StoreConfiguration;
import org.apache.activemq.artemis.core.paging.PagingStore;
import org.apache.activemq.artemis.core.paging.impl.PagingStoreTestAccessor;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.logs.AssertionLoggerHandler;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.utils.RetryRule;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PageTransactionCleanupTest extends ActiveMQTestBase {

   protected static final int RECEIVE_TIMEOUT = 5000;
   protected static final int PAGE_MAX = 100 * 1024;
   protected static final int PAGE_SIZE = 10 * 1024;
   static final int MESSAGE_SIZE = 1024; // 1k
   static final int LARGE_MESSAGE_SIZE = 100 * 1024;
   static final SimpleString ADDRESS = new SimpleString("SimpleAddress");
   private static final Logger log = Logger.getLogger(PageTransactionCleanupTest.class);
   private static final Logger logger = Logger.getLogger(PageTransactionCleanupTest.class);
   protected final boolean mapped;
   protected final StoreConfiguration.StoreType storeType;
   @Rule
   public RetryRule retryMethod = new RetryRule(1);
   protected ServerLocator locator;
   protected ActiveMQServer server;
   protected ClientSessionFactory sf;

   public PageTransactionCleanupTest(StoreConfiguration.StoreType storeType, boolean mapped) {
      this.storeType = storeType;
      this.mapped = mapped;
   }

   @Parameterized.Parameters(name = "storeType={0}, mapped={1}")
   public static Collection<Object[]> data() {
      //Object[][] params = new Object[][]{{StoreConfiguration.StoreType.FILE, false}, {StoreConfiguration.StoreType.FILE, true}, {StoreConfiguration.StoreType.DATABASE, false}};
      Object[][] params = new Object[][]{{StoreConfiguration.StoreType.FILE, false}};
      return Arrays.asList(params);
   }

   @Before
   public void checkLoggerStart() throws Exception {
      AssertionLoggerHandler.startCapture();
   }

   @After
   public void checkLoggerEnd() throws Exception {
      try {
         // These are the message errors for the negative size address size
         Assert.assertFalse(AssertionLoggerHandler.findText("222214"));
         Assert.assertFalse(AssertionLoggerHandler.findText("222215"));
      } finally {
         AssertionLoggerHandler.stopCapture();
      }
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      locator = createInVMNonHALocator();
   }

   @Test
   public void testPageTXCleanup() throws Exception {
      AssertionLoggerHandler.startCapture();

      try {
         Configuration config = createDefaultInVMConfig();

         final int PAGE_MAX = 20 * 1024;

         final int PAGE_SIZE = 10 * 1024;

         ActiveMQServer server = createServer(true, config, PAGE_SIZE, PAGE_MAX);
         server.start();

         final int numberOfBytes = 14;

         locator.setBlockOnNonDurableSend(false).setBlockOnDurableSend(false).setBlockOnAcknowledge(false);

         ClientSessionFactory sf = addSessionFactory(createSessionFactory(locator));

         ClientSession session = sf.createSession(null, null, false, false, false, false, 0);

         session.createQueue(new QueueConfiguration(ADDRESS.concat("-0")).setAddress(ADDRESS));

         PagingStore store = server.getPagingManager().getPageStore(ADDRESS);
         store.disableCleanup();
         store.startPaging();

         session.start();

         ClientProducer producer = session.createProducer(ADDRESS);

         ClientConsumer browserConsumer = session.createConsumer(ADDRESS.concat("-0"), true);

         ClientMessage message = null;

         for (int i = 0; i < 100; i++) {
            message = session.createMessage(true);

            message.getBodyBuffer().writerIndex(0);

            message.getBodyBuffer().writeBytes(new byte[numberOfBytes]);

            for (int j = 1; j <= numberOfBytes; j++) {
               message.getBodyBuffer().writeInt(j);
            }

            System.out.println("Sending " + i);
            producer.send(message);
            session.commit();
            if (i % 10 == 0 && i > 0) {
               store.forceAnotherPage();
            }
         }

         PagingStoreTestAccessor.cleanupTransactions(server.getPagingManager().getPageStore(ADDRESS));

         ClientConsumer consumer = session.createConsumer(ADDRESS.concat("-0"));

         session.start();

         for (int i = 0; i < 100; i++) {
            ClientMessage message2 = consumer.receive(1000);

            Assert.assertNotNull(message2);

            message2.acknowledge();

            Assert.assertNotNull(message2);

            session.commit();
         }

         consumer.close();
      } finally {
         AssertionLoggerHandler.stopCapture();
      }
   }

}