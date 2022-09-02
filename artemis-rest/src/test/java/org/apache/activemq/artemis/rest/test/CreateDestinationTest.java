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
package org.apache.activemq.artemis.rest.test;

import org.apache.activemq.artemis.rest.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.Link;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateDestinationTest extends MessageTestBase {
   private static final Logger log = LoggerFactory.getLogger(CreateDestinationTest.class);

   @BeforeClass
   public static void reg() {
      server.getJaxrsServer().getDeployment().getProviderFactory().registerProvider(org.jboss.resteasy.plugins.providers.DocumentProvider.class);
   }

   @Test
   public void testCreateQueue() throws Exception {
      String queueConfig = "<queue name=\"testQueue\"><durable>true</durable></queue>";
      ClientRequest create = new ClientRequest(TestPortProvider.generateURL("/queues"));
      ClientResponse cRes = create.body("application/activemq.jms.queue+xml", queueConfig).post();
      cRes.releaseConnection();
      Assert.assertEquals(201, cRes.getStatus());
      log.debug("Location: " + cRes.getLocationLink());
      ClientRequest request = cRes.getLocationLink().request();

      ClientResponse<?> response = request.head();
      response.releaseConnection();
      Assert.assertEquals(200, response.getStatus());
      Link sender = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), response, "create");
      log.debug("create: " + sender);
      Link consumers = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), response, "pull-consumers");
      log.debug("pull: " + consumers);
      response = Util.setAutoAck(consumers, true);
      Link consumeNext = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), response, "consume-next");
      log.debug("poller: " + consumeNext);

      ClientResponse<?> res = sender.request().body("text/plain", Integer.toString(1)).post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());

      res = consumeNext.request().post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("1", res.getEntity(String.class));
      res.releaseConnection();
      Link session = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), res, "consumer");
      log.debug("session: " + session);
      consumeNext = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), res, "consume-next");
      log.debug("consumeNext: " + consumeNext);

      res = sender.request().body("text/plain", Integer.toString(2)).post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());

      log.debug("{}", consumeNext);
      res = consumeNext.request().header(Constants.WAIT_HEADER, "10").post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("2", res.getEntity(String.class));
      res.releaseConnection();
      session = getLinkByTitle(manager.getQueueManager().getLinkStrategy(), res, "consumer");
      log.debug("session: " + session);
      getLinkByTitle(manager.getQueueManager().getLinkStrategy(), res, "consume-next");
      log.debug("consumeNext: " + consumeNext);

      res = session.request().delete();
      res.releaseConnection();
      Assert.assertEquals(204, res.getStatus());
   }

   @Test
   public void testCreateTopic() throws Exception {
      String queueConfig = "<topic name=\"testTopic\"></topic>";
      ClientRequest create = new ClientRequest(TestPortProvider.generateURL("/topics"));
      ClientResponse cRes = create.body("application/activemq.jms.topic+xml", queueConfig).post();
      cRes.releaseConnection();
      Assert.assertEquals(201, cRes.getStatus());

      ClientRequest request = cRes.getLocationLink().request();

      ClientResponse<?> response = request.head();
      response.releaseConnection();
      Assert.assertEquals(200, response.getStatus());
      Link sender = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), response, "create");
      Link subscriptions = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), response, "pull-subscriptions");

      ClientResponse<?> res = subscriptions.request().post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());
      Link sub1 = res.getLocationLink();
      Assert.assertNotNull(sub1);
      Link consumeNext1 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");
      Assert.assertNotNull(consumeNext1);
      log.debug("consumeNext1: " + consumeNext1);

      res = subscriptions.request().post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());
      Link sub2 = res.getLocationLink();
      Assert.assertNotNull(sub2);
      Link consumeNext2 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");
      Assert.assertNotNull(consumeNext1);

      res = sender.request().body("text/plain", Integer.toString(1)).post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());
      res = sender.request().body("text/plain", Integer.toString(2)).post();
      res.releaseConnection();
      Assert.assertEquals(201, res.getStatus());

      res = consumeNext1.request().post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("1", res.getEntity(String.class));
      res.releaseConnection();
      consumeNext1 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");

      res = consumeNext1.request().post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("2", res.getEntity(String.class));
      res.releaseConnection();
      consumeNext1 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");

      res = consumeNext2.request().post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("1", res.getEntity(String.class));
      res.releaseConnection();
      consumeNext2 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");

      res = consumeNext2.request().post(String.class);
      Assert.assertEquals(200, res.getStatus());
      Assert.assertEquals("2", res.getEntity(String.class));
      res.releaseConnection();
      consumeNext2 = getLinkByTitle(manager.getTopicManager().getLinkStrategy(), res, "consume-next");
      res = sub1.request().delete();
      res.releaseConnection();
      Assert.assertEquals(204, res.getStatus());
      res = sub2.request().delete();
      res.releaseConnection();
      Assert.assertEquals(204, res.getStatus());
   }

   @Test
   public void testCreateQueueWithBadContentType() throws Exception {
      String queueConfig = "<queue name=\"testQueue\"><durable>true</durable></queue>";
      ClientRequest create = new ClientRequest(TestPortProvider.generateURL("/queues"));
      ClientResponse cRes = create.body("application/x-www-form-urlencoded", queueConfig).post();
      cRes.releaseConnection();

      Assert.assertEquals(415, cRes.getStatus());
   }

   @Test
   public void testCreateTopicWithBadContentType() throws Exception {
      String queueConfig = "<topic name=\"testTopic\"></topic>";
      ClientRequest create = new ClientRequest(TestPortProvider.generateURL("/topics"));
      ClientResponse cRes = create.body("application/x-www-form-urlencoded", queueConfig).post();
      cRes.releaseConnection();
      Assert.assertEquals(415, cRes.getStatus());
   }
}