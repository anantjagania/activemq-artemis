/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.protocol.amqp.connect.mirror;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.core.transaction.impl.TransactionImpl;
import org.apache.activemq.artemis.utils.collections.LinkedListIterator;

public class MirrorSourceCompactor {

   final StorageManager storageManager;
   final Queue queue;

   public MirrorSourceCompactor(StorageManager storageManager, Queue queue) {
      this.storageManager = storageManager;
      this.queue = queue;
   }

   // To be executed within the Queue's executor
   public void compact() throws Exception {
      Transaction tx = new TransactionImpl(storageManager);
      try (LinkedListIterator<MessageReference> messages = queue.iterator()) {
         while (messages.hasNext()) {
            MessageReference ref = messages.next();
            Message message = ref.getMessage();
            System.out.println("message " + message);
         }
      }
   }

}
