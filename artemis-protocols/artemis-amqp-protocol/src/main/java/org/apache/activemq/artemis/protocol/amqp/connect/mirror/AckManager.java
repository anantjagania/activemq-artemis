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

package org.apache.activemq.artemis.protocol.amqp.connect.mirror;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

import io.netty.util.collection.LongObjectHashMap;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.io.IOCriticalErrorListener;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.journal.collections.JournalHashMap;
import org.apache.activemq.artemis.core.journal.collections.JournalHashMapProvider;
import org.apache.activemq.artemis.core.paging.PagingStore;
import org.apache.activemq.artemis.core.paging.cursor.PageSubscription;
import org.apache.activemq.artemis.core.paging.cursor.PagedReference;
import org.apache.activemq.artemis.core.paging.impl.Page;
import org.apache.activemq.artemis.core.persistence.impl.journal.JournalRecordIds;
import org.apache.activemq.artemis.core.persistence.impl.journal.OperationContextImpl;
import org.apache.activemq.artemis.core.persistence.impl.journal.codec.AckRetry;
import org.apache.activemq.artemis.core.postoffice.impl.LocalQueueBinding;
import org.apache.activemq.artemis.core.server.ActiveMQComponent;
import org.apache.activemq.artemis.core.server.ActiveMQScheduledComponent;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.RoutingContext;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.artemis.core.server.mirror.MirrorController;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.core.transaction.impl.TransactionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AckManager implements ActiveMQComponent {

   // we first retry on the queue a few tiems
   public static final short MIN_QUEUE_ATTEMPT = 4;

   public static final short MAX_PAGE_ATTEMPT = 10;

   private static DisabledAckMirrorController disabledAckMirrorController = new DisabledAckMirrorController();

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   final Journal journal;
   final LongSupplier sequenceGenerator;
   final JournalHashMapProvider<AckRetry, AckRetry, Queue> journalHashMapProvider;
   final ActiveMQServer server;
   final ReferenceIDSupplier referenceIDSupplier;
   final IOCriticalErrorListener ioCriticalErrorListener;
   volatile MultiStepProgress progress;
   int period;
   ActiveMQScheduledComponent scheduledComponent;

   public AckManager(ActiveMQServer server, int period) {
      this.server = server;
      this.period = period;
      this.ioCriticalErrorListener = server.getIoCriticalErrorListener();
      this.journal = server.getStorageManager().getMessageJournal();
      this.sequenceGenerator = server.getStorageManager()::generateID;
      journalHashMapProvider = new JournalHashMapProvider<>(sequenceGenerator, journal, AckRetry.getPersister(), JournalRecordIds.ACK_RETRY, OperationContextImpl::getContext, server.getPostOffice()::findQueue, server.getIoCriticalErrorListener());
      this.referenceIDSupplier = new ReferenceIDSupplier(server);
   }

   public void reload(RecordInfo recordInfo) {
      journalHashMapProvider.reload(recordInfo);
   }

   @Override
   public synchronized void stop() {
      if (scheduledComponent != null) {
         scheduledComponent.stop();
         scheduledComponent = null;
      }
      AckManagerProvider.remove(this.server);
      logger.debug("Stopping ackmanager on server {}", server);
   }

   @Override
   public synchronized boolean isStarted() {
      return scheduledComponent != null && scheduledComponent.isStarted();
   }

   @Override
   public synchronized void start() {
      logger.debug("Starting ACKManager on {} with period = {}", server, period);
      if (!isStarted()) {
         scheduledComponent = new ActiveMQScheduledComponent(server.getScheduledPool(), server.getExecutorFactory().getExecutor(), period, period, TimeUnit.MILLISECONDS, false).setScheduledTask(this::beginRetry);
         scheduledComponent.start();
      } else {
         logger.debug("Starting ignored on server {}", server);
      }
   }

   public synchronized void restart() {
      if (scheduledComponent != null) {
         scheduledComponent.stop();
         scheduledComponent = null;
      }
      start();
   }

   public int getPeriod() {
      return period;
   }

   public AckManager setPeriod(int period) {
      this.period = period;
      return this;
   }

   public void beginRetry() {
      logger.trace("being retry server {}", server);
      if (initRetry()) {
         logger.trace("Starting process to retry, server={}", server);
         progress.nextStep();
      } else {
         logger.trace("Retry already happened");
      }
   }

   public void endRetry() {
      logger.trace("Retry done on server {}", server);
      progress = null;
   }

   public boolean initRetry() {
      if (progress != null) {
         logger.trace("Retry already in progress, we will wait next time, server={}", server);
         return false;
      }

      HashMap<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> retries = sortRetries();

      if (retries.isEmpty()) {
         logger.trace("Nothing to retry!, server={}", server);
         return false;
      }

      progress = new MultiStepProgress(sortRetries());
      return true;
   }

   // Sort the ACK list by address
   // We have the retries by queue, we need to sort them by address
   // as we will perform all the retries on the same addresses at the same time (in the Multicast case with multiple queues acking)
   public HashMap<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> sortRetries() {
      // We will group the retries by address,
      // so we perform all of the queues in the same address at once
      HashMap<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> retriesByAddress = new HashMap<>();

      Iterator<JournalHashMap<AckRetry, AckRetry, Queue>> queueRetriesIterator = journalHashMapProvider.getMaps().iterator();

      while (queueRetriesIterator.hasNext()) {
         JournalHashMap<AckRetry, AckRetry, Queue> ackRetries = queueRetriesIterator.next();
         if (!ackRetries.isEmpty()) {
            LocalQueueBinding queueBinding = server.getPostOffice().findLocalBinding(ackRetries.getCollectionId());
            if (queueBinding != null) {
               SimpleString address = queueBinding.getAddress();
               LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>> queueRetriesOnAddress = retriesByAddress.get(address);
               if (queueRetriesOnAddress == null) {
                  queueRetriesOnAddress = new LongObjectHashMap<>();
                  retriesByAddress.put(address, queueRetriesOnAddress);
               }
               queueRetriesOnAddress.put(queueBinding.getQueue().getID(), ackRetries);
            }
         }
      }

      return retriesByAddress;
   }

   private boolean isEmpty(LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>> queuesToRetry) {
      AtomicBoolean empty = new AtomicBoolean(true);

      queuesToRetry.forEach((id, journalHashMap) -> {
         if (!journalHashMap.isEmpty()) {
            empty.set(false);
         }
      });

      return empty.get();

   }

   // to be used with the same executor as the PagingStore executor
   public boolean retryAddress(SimpleString address, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>> queuesToRetry) {
      MirrorController previousController = AMQPMirrorControllerTarget.getControllerInUse();
      boolean retriedPaging = false;
      logger.debug("retrying address {} on server {}", address, server);
      try {
         AMQPMirrorControllerTarget.setControllerInUse(disabledAckMirrorController);
         queuesToRetry.forEach(this::retryQueue);
         AckRetry key = new AckRetry();

         PagingStore store = server.getPagingManager().getPageStore(address);
         for (long pageId = store.getFirstPage(); pageId <= store.getCurrentWritingPage(); pageId++) {
            if (isEmpty(queuesToRetry)) {
               logger.trace("Retry stopped while reading page {} on address {} as the outcome is now empty, server={}", pageId, address, server);
               break;
            }
            Page page = store.usePage(pageId, true, false);
            if (page == null) {
               continue;
            }
            try {
               if (retryPage(queuesToRetry, page, key)) {
                  retriedPaging = true;
               }
            } finally {
               page.usageDown();
            }
         }

         validateExpiredSet(queuesToRetry);
      } catch (Throwable e) {
         logger.warn(e.getMessage(), e);
      } finally {
         AMQPMirrorControllerTarget.setControllerInUse(previousController);
      }
      return retriedPaging;
   }

   private void validateExpiredSet(LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>> queuesToRetry) {
      queuesToRetry.forEach(this::validateExpireSet);
   }

   private void validateExpireSet(long queueID, JournalHashMap<AckRetry, AckRetry, Queue> retries) {
      for (AckRetry retry : retries.values()) {
         if (retry.getQueueAttempts() > MIN_QUEUE_ATTEMPT) {
            if (retry.attemptedPage() > MAX_PAGE_ATTEMPT) {
               logger.debug("Retry {} too many times, giving up on the entry now", retry);
               retries.remove(retry);
            }
         }
      }
   }

   private boolean retryPage(LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>> queuesToRetry,
                          Page page,
                          AckRetry key) throws Exception {
      AtomicBoolean retriedPaging = new AtomicBoolean(false);
      TransactionImpl transaction = new TransactionImpl(server.getStorageManager()).setAsync(true);
      // scan each page for acks
      page.getMessages().forEach(pagedMessage -> {
         for (int i = 0; i < pagedMessage.getQueueIDs().length; i++) {
            long queueID = pagedMessage.getQueueIDs()[i];
            JournalHashMap<AckRetry, AckRetry, Queue> retries = queuesToRetry.get(queueID);
            if (retries != null) {
               String serverID = referenceIDSupplier.getServerID(pagedMessage.getMessage());
               if (serverID == null) {
                  serverID = referenceIDSupplier.getDefaultNodeID();
               }
               long id = referenceIDSupplier.getID(pagedMessage.getMessage());

               logger.debug("Looking for retry on serverID={}, id={} on server={}", serverID, id, server);
               key.setNodeID(serverID).setMessageID(id);
               AckRetry foundRetry = retries.get(key);
               if (foundRetry != null && foundRetry.getQueueAttempts() > MIN_QUEUE_ATTEMPT) {

                  Queue queue = retries.getContext();

                  if (queue != null) {
                     PageSubscription subscription = queue.getPageSubscription();
                     if (!subscription.isAcked(pagedMessage)) {
                        PagedReference reference = retries.getContext().getPagingStore().getCursorProvider().newReference(pagedMessage, subscription);
                        try {
                           subscription.ackTx(transaction, reference);
                           retriedPaging.set(true);
                        } catch (Exception e) {
                           logger.warn(e.getMessage(), e);
                           if (ioCriticalErrorListener != null) {
                              ioCriticalErrorListener.onIOException(e, e.getMessage(), null);
                           }
                        }
                     }
                     retries.remove(foundRetry, transaction.getID());
                     transaction.setContainsPersistent();
                     logger.debug("retry found = {} for message={} on queue", foundRetry, pagedMessage);
                  }
               }
            } else {
               logger.trace("Retry key={} not found server={}", key, server);
            }
         }
      });

      try {
         transaction.commit();
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
         if (ioCriticalErrorListener != null) {
            ioCriticalErrorListener.onIOException(e, e.getMessage(), null);
         }
      }

      return retriedPaging.get();
   }

   /** This method will try a regular attempt at acking, by removing the message from the queue as a consumer would do. */
   private void retryQueue(long queueID, JournalHashMap<AckRetry, AckRetry, Queue> retries) {
      Queue queue = server.getPostOffice().findQueue(queueID);
      for (AckRetry retry : retries.values()) {
         if (ack(retry.getNodeID(), queue, retry.getMessageID(), retry.getReason(), false)) {
            logger.debug("Removing retry {} as the retry went ok", retry);
            retries.remove(retry);
         } else {
            int retried = retry.attemptedQueue();
            logger.debug("retry {}  attempted {} times", retry, retried);
         }
      }
   }

   public void addRetry(String nodeID, Queue queue, long messageID, AckReason reason) {
      if (nodeID == null) {
         nodeID = referenceIDSupplier.getDefaultNodeID();
      }
      AckRetry retry = new AckRetry(nodeID, messageID, reason);
      journalHashMapProvider.getMap(queue.getID(), queue).put(retry, retry);
   }

   public boolean ack(String nodeID, Queue targetQueue, long messageID, AckReason reason, boolean allowRetry) {
      if (logger.isTraceEnabled()) {
         logger.trace("performAck (nodeID={}, messageID={}), targetQueue={}, allowRetry={})", nodeID, messageID, targetQueue.getName(), allowRetry);
      }

      MessageReference reference = targetQueue.removeWithSuppliedID(nodeID, messageID, referenceIDSupplier);

      if (reference == null) {
         logger.debug("Could not find retry on reference nodeID={} (while localID={}), messageID={} on queue {}, server={}", nodeID, referenceIDSupplier.getDefaultNodeID(), messageID, targetQueue.getName(), server);
         if (allowRetry) {
            addRetry(nodeID, targetQueue, messageID, reason);
         }
         return false;
      } else  {
         if (logger.isTraceEnabled()) {
            logger.trace("ack {} worked well for messageID={} nodeID={} queue={}, targetQueue={}", server, messageID, nodeID, reference.getQueue(), targetQueue);
         }
         doACK(targetQueue, reference, reason);
         return true;
      }
   }


   private void doACK(Queue targetQueue, MessageReference reference, AckReason reason) {
      try {
         switch (reason) {
            case EXPIRED:
               targetQueue.expire(reference, null, false);
               break;
            default:
               TransactionImpl transaction = new TransactionImpl(server.getStorageManager()).setAsync(true);
               targetQueue.acknowledge(transaction, reference, reason, null, false);
               transaction.commit();
               break;
         }
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
      }
   }
   /** The ACKManager will perform the retry on each address's pageStore executor.
    *  it will perform each address individually, one by one. */
   class MultiStepProgress {
      HashMap<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> retryList;

      Iterator<Map.Entry<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>>> retryIterator;

      boolean retriedPaging = false;


      MultiStepProgress(HashMap<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> retryList) {
         this.retryList = retryList;
         retryIterator = retryList.entrySet().iterator();
      }

      public void nextStep() {
         try {
            if (!retryIterator.hasNext()) {
               if (retriedPaging) {
                  logger.debug("Retried acks on paging, better to rebuild the page counters");
                  server.getPagingManager().rebuildCounters(null);
               }

               logger.trace("Iterator is done on retry, server={}", server);
               AckManager.this.endRetry();
            } else {
               Map.Entry<SimpleString, LongObjectHashMap<JournalHashMap<AckRetry, AckRetry, Queue>>> entry = retryIterator.next();
               PagingStore pagingStore = server.getPagingManager().getPageStore(entry.getKey());
               pagingStore.execute(() -> {
                  if (AckManager.this.retryAddress(entry.getKey(), entry.getValue())) {
                     retriedPaging = true;
                  }
                  nextStep();
               });
            }
         } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
            // there was an exception, I'm clearing the current progress to allow a new one
            AckManager.this.endRetry();
            ioCriticalErrorListener.onIOException(e, e.getMessage(), null);
         }
      }
   }


   private static class DisabledAckMirrorController implements MirrorController {

      @Override
      public boolean isAllowACK() {
         return false;
      }

      @Override
      public void addAddress(AddressInfo addressInfo) throws Exception {

      }

      @Override
      public void deleteAddress(AddressInfo addressInfo) throws Exception {

      }

      @Override
      public void createQueue(QueueConfiguration queueConfiguration) throws Exception {

      }

      @Override
      public void deleteQueue(SimpleString addressName, SimpleString queueName) throws Exception {

      }

      @Override
      public void sendMessage(Transaction tx, Message message, RoutingContext context) {

      }

      @Override
      public void postAcknowledge(MessageReference ref, AckReason reason) throws Exception {

      }

      @Override
      public void preAcknowledge(Transaction tx, MessageReference ref, AckReason reason) throws Exception {

      }

      @Override
      public String getRemoteMirrorId() {
         return null;
      }
   }

}
