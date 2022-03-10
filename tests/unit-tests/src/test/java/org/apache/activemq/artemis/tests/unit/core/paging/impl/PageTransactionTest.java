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
package org.apache.activemq.artemis.tests.unit.core.paging.impl;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.paging.PageTransactionInfo;
import org.apache.activemq.artemis.core.paging.impl.PageTransactionInfoImpl;
import org.apache.activemq.artemis.core.paging.impl.PageTransactionInfoImplTestAccessor;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.utils.RandomUtil;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

public class PageTransactionTest extends ActiveMQTestBase {

   private static final Logger log = Logger.getLogger(PageTransactionTest.class);

   @Test
   public void testAddAndRemoveMessages() {
      long id1 = RandomUtil.randomLong();
      long id2 = RandomUtil.randomLong();
      PageTransactionInfo trans = new PageTransactionInfoImpl(id2);

      trans.setRecordID(id1);

      ActiveMQBuffer buffer = ActiveMQBuffers.fixedBuffer(trans.getEncodeSize());

      trans.encode(buffer);

      PageTransactionInfo trans2 = new PageTransactionInfoImpl(id1);
      trans2.decode(buffer);

      Assert.assertEquals(id2, trans2.getTransactionID());

   }

   @Test
   public void testAddPages() {
      long id1 = RandomUtil.randomLong();
      long id2 = RandomUtil.randomLong();
      PageTransactionInfo trans = new PageTransactionInfoImpl(id2);

      trans.setRecordID(id1);
      SimpleString address = RandomUtil.randomSimpleString();
      // Maybe I could as well return null on non filled addresses... feel free to change the rule if you need it
      Assert.assertNull( trans.getPages(address));
      PageTransactionInfoImplTestAccessor.addPage(trans, null, address, 10);
      PageTransactionInfoImplTestAccessor.addPage(trans, null, address, 11);

      Assert.assertEquals(2, trans.getPages(address).length);
      Assert.assertEquals(10, trans.getPages(address)[0]);
      Assert.assertEquals(11, trans.getPages(address)[1]);

      ActiveMQBuffer buffer = ActiveMQBuffers.fixedBuffer(trans.getEncodeSize());

      trans.encode(buffer);

      PageTransactionInfo trans2 = new PageTransactionInfoImpl(id1);
      trans2.decode(buffer);

      Assert.assertEquals(id2, trans2.getTransactionID());

      Assert.assertEquals(2, trans2.getPages(address).length);
      Assert.assertEquals(10, trans2.getPages(address)[0]);
      Assert.assertEquals(11, trans2.getPages(address)[1]);

      Assert.assertEquals(0, buffer.readableBytes());
   }

}