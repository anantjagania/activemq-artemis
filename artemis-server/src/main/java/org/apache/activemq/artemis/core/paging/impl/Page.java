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
package org.apache.activemq.artemis.core.paging.impl;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
import org.apache.activemq.artemis.core.io.IOCallback;
import org.apache.activemq.artemis.core.io.SequentialFile;
import org.apache.activemq.artemis.core.io.SequentialFileFactory;
import org.apache.activemq.artemis.core.paging.PagedMessage;
import org.apache.activemq.artemis.core.paging.cursor.PageSubscriptionCounter;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.server.ActiveMQMessageBundle;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.LargeServerMessage;
import org.apache.activemq.artemis.utils.DataConstants;
import org.apache.activemq.artemis.utils.Env;
import org.apache.activemq.artemis.utils.ReferenceCounterUtil;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.apache.activemq.artemis.utils.collections.EmptyList;
import org.apache.activemq.artemis.utils.collections.LinkedList;
import org.apache.activemq.artemis.utils.collections.LinkedListImpl;
import org.apache.activemq.artemis.utils.collections.LinkedListIterator;
import org.jboss.logging.Logger;

public final class Page implements Comparable<Page> {

   private static final AtomicInteger factory = new AtomicInteger(0);

   private final int seqInt = factory.incrementAndGet();

   private static final Logger logger = Logger.getLogger(Page.class);

   public static final int SIZE_RECORD = DataConstants.SIZE_BYTE + DataConstants.SIZE_INT + DataConstants.SIZE_BYTE;

   private static final byte START_BYTE = (byte) '{';

   private static final byte END_BYTE = (byte) '}';

   private final ReferenceCounterUtil referenceCounter = new ReferenceCounterUtil();

   public void usageReset() {
      referenceCounter.reset();
   }

   public int usageUp() {
      return referenceCounter.increment();
   }

   public int usageDown() {
      return referenceCounter.decrement();
   }


   /** This is an utility method to help you call usageDown while using a try (closeable) call.
    *  */
   public AutoCloseable refCloseable() {
      return referenceCounter;
   }

   /** to be called when the page is supposed to be released */
   public void releaseTask(Consumer<Page> releaseTask) {
      referenceCounter.setTask(() -> releaseTask.accept(this));
   }

   private final long pageId;

   private boolean suspiciousRecords = false;

   private int numberOfMessages;

   private final SequentialFile file;

   private final SequentialFileFactory fileFactory;

   private volatile LinkedList<PagedMessage> messages;

   /**
    * The page cache that will be filled with data as we write more data
    */
   private volatile Consumer<PagedMessage> callback;

   private final AtomicInteger size = new AtomicInteger(0);

   private final StorageManager storageManager;

   private final SimpleString storeName;

   /**
    * A list of subscriptions containing pending counters (with non tx adds) on this page
    */
   private Set<PageSubscriptionCounter> pendingCounters;

   private ByteBuffer readFileBuffer;

   public Page(final SimpleString storeName,
               final StorageManager storageManager,
               final SequentialFileFactory factory,
               final SequentialFile file,
               final long pageId) throws Exception {
      this.pageId = pageId;
      this.file = file;
      fileFactory = factory;
      this.storageManager = storageManager;
      this.storeName = storeName;
      resetReadMessageStatus();
   }

   public long getPageId() {
      return pageId;
   }

   public void setCallback(Consumer<PagedMessage> callback) {
      this.callback = callback;
   }

   public Consumer<PagedMessage> getCallback() {
      return callback;
   }

   private synchronized void resetReadMessageStatus() {
   }

   public LinkedListIterator<PagedMessage> iterator() throws Exception {
      LinkedList<PagedMessage> messages = getMessages();
      return messages.iterator();
   }

   public synchronized LinkedList<PagedMessage> getMessages() throws Exception {
      if (messages == null) {
         boolean wasOpen = file.isOpen();
         if (!wasOpen) {
            if (!file.exists()) {
               return EmptyList.getEmptyList();
            }
            file.open();
         }
         messages = read(storageManager);
         if (!wasOpen) {
            file.close();
         }
      }

      return messages;
   }

   private void addMessage(PagedMessage message) {
      if (messages == null) {
         messages = new LinkedListImpl<>();
      }
      message.setMessageNr(messages.size());
      message.setPageNr(this.pageId);
      messages.addTail(message);
   }

   public synchronized LinkedList<PagedMessage> read() throws Exception {
      return read(storageManager);
   }

   public synchronized LinkedList<PagedMessage> read(StorageManager storage) throws Exception {
      return read(storage, false);
   }

   public synchronized LinkedList<PagedMessage> read(StorageManager storage, boolean onlyLargeMessages) throws Exception {
      if (logger.isDebugEnabled()) {
         logger.debugf("reading page %d on address = %s onlyLargeMessages = %b",
            new Object[] {pageId, storeName, onlyLargeMessages});
      }

      if (!file.isOpen()) {
         if (!file.exists()) {
            return EmptyList.getEmptyList();
         }
         throw ActiveMQMessageBundle.BUNDLE.invalidPageIO();
      }

      size.lazySet((int) file.size());

      final LinkedList<PagedMessage> messages = new LinkedListImpl<>();

      final int totalMessageCount = readFromSequentialFile(storage, messages::addTail, onlyLargeMessages ? ONLY_LARGE : NO_SKIP);

      numberOfMessages = totalMessageCount;

      return messages;
   }

   public String debugMessages() throws Exception {
      StringBuffer buffer = new StringBuffer();
      LinkedListIterator<PagedMessage> iter = getMessages().iterator();
      while (iter.hasNext()) {
         PagedMessage message = iter.next();
         buffer.append(message.toString() + "\n");
      }
      iter.close();
      return buffer.toString();
   }

   private ByteBuffer allocateAndReadIntoFileBuffer(ByteBuffer fileBuffer, int requiredBytes, boolean direct) throws Exception {
      ByteBuffer newFileBuffer;
      if (direct) {
         newFileBuffer = fileFactory.allocateDirectBuffer(Math.max(requiredBytes, MIN_CHUNK_SIZE));
         newFileBuffer.put(fileBuffer);
         fileFactory.releaseDirectBuffer(fileBuffer);
      } else {
         newFileBuffer = fileFactory.newBuffer(Math.max(requiredBytes, MIN_CHUNK_SIZE));
         newFileBuffer.put(fileBuffer);
         fileFactory.releaseBuffer(fileBuffer);
      }
      fileBuffer = newFileBuffer;
      //move the limit to allow reading as much as possible from the file
      fileBuffer.limit(fileBuffer.capacity());
      file.read(fileBuffer);
      fileBuffer.position(0);
      return fileBuffer;
   }

   /**
    * It returns a {@link ByteBuffer} that has {@link ByteBuffer#remaining()} bytes >= {@code requiredBytes}
    * of valid data from {@link #file}.
    */
   private ByteBuffer readIntoFileBufferIfNecessary(ByteBuffer fileBuffer, int requiredBytes, boolean direct) throws Exception {
      final int remaining = fileBuffer.remaining();
      //fileBuffer::remaining is the current size of valid data
      final int bytesToBeRead = requiredBytes - remaining;
      if (bytesToBeRead > 0) {
         final int capacity = fileBuffer.capacity();
         //fileBuffer has enough overall capacity to hold all the required bytes?
         if (capacity >= requiredBytes) {
            //we do not care to use the free space between
            //fileBuffer::limit and fileBuffer::capacity
            //to save compactions, because fileBuffer
            //is very unlikely to not be completely full
            //after each file::read
            if (fileBuffer.limit() > 0) {
               //the previous check avoid compact
               //to attempt a copy of 0 bytes
               fileBuffer.compact();
            } else {
               //compact already set the limit == capacity
               fileBuffer.limit(capacity);
            }
            file.read(fileBuffer);
            fileBuffer.position(0);
         } else {
            fileBuffer = allocateAndReadIntoFileBuffer(fileBuffer, requiredBytes, direct);
         }
      }
      return fileBuffer;
   }

   private static boolean validateLargeMessageStorageManager(PagedMessage msg) {
      if (!(msg.getMessage() instanceof LargeServerMessage)) {
         return true;
      }
      LargeServerMessage largeServerMessage = ((LargeServerMessage) msg.getMessage());

      boolean storageManager = largeServerMessage.getStorageManager() != null;
      if (!storageManager) {
         logger.warn("storage manager is null at " + msg);
      }

      return storageManager;
   }

   private static ChannelBufferWrapper wrapWhole(ByteBuffer fileBuffer) {
      final int position = fileBuffer.position();
      final int limit = fileBuffer.limit();
      final int capacity = fileBuffer.capacity();
      try {
         fileBuffer.clear();
         final ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(fileBuffer);
         //this check is important to avoid next ByteBuf::setIndex
         //to fail due to ByteBuf::capacity == ByteBuffer::remaining bytes
         assert wrappedBuffer.readableBytes() == capacity;
         final ChannelBufferWrapper fileBufferWrapper = new ChannelBufferWrapper(wrappedBuffer);
         return fileBufferWrapper;
      } finally {
         fileBuffer.position(position);
         fileBuffer.limit(limit);
      }
   }

   //sizeOf(START_BYTE) + sizeOf(MESSAGE LENGTH) + sizeOf(END_BYTE)
   private static final int HEADER_AND_TRAILER_SIZE = DataConstants.SIZE_INT + 2;
   private static final int MINIMUM_MSG_PERSISTENT_SIZE = HEADER_AND_TRAILER_SIZE;
   private static final int HEADER_SIZE = HEADER_AND_TRAILER_SIZE - 1;
   private static final int MIN_CHUNK_SIZE = Env.osPageSize();

   interface SkipRecord {
      boolean skip(ActiveMQBuffer buffer);
   }

   private static final SkipRecord ONLY_LARGE = new SkipRecord() {
      @Override
      public boolean skip(ActiveMQBuffer buffer) {
         return !PagedMessageImpl.isLargeMessage(buffer);
      }
   };

   private static final SkipRecord NO_SKIP = new SkipRecord() {
      @Override
      public boolean skip(ActiveMQBuffer buffer) {
         return false;
      }
   };

   private static final SkipRecord SKIP_ALL = new SkipRecord() {
      @Override
      public boolean skip(ActiveMQBuffer buffer) {
         return false;
      }
   };


   private int readFromSequentialFile(StorageManager storage,
                                                     Consumer<PagedMessage> messages,
                                                     SkipRecord skipRecord) throws Exception {
      final int fileSize = (int) file.size();
      file.position(0);
      int processedBytes = 0;
      ByteBuffer fileBuffer = null;
      ChannelBufferWrapper fileBufferWrapper;
      int totalMessageCount = 0;
      try {
         int remainingBytes = fileSize - processedBytes;
         if (remainingBytes >= MINIMUM_MSG_PERSISTENT_SIZE) {
            fileBuffer = fileFactory.newBuffer(Math.min(remainingBytes, MIN_CHUNK_SIZE));
            //the wrapper is reused to avoid unnecessary allocations
            fileBufferWrapper = wrapWhole(fileBuffer);
            //no content is being added yet
            fileBuffer.limit(0);
            do {
               final ByteBuffer oldFileBuffer = fileBuffer;
               fileBuffer = readIntoFileBufferIfNecessary(fileBuffer, MINIMUM_MSG_PERSISTENT_SIZE, false);
               //change wrapper if fileBuffer has changed
               if (fileBuffer != oldFileBuffer) {
                  fileBufferWrapper = wrapWhole(fileBuffer);
               }
               final byte startByte = fileBuffer.get();
               if (startByte == Page.START_BYTE) {
                  final int encodedSize = fileBuffer.getInt();
                  final int nextPosition = processedBytes + HEADER_AND_TRAILER_SIZE + encodedSize;
                  if (nextPosition <= fileSize) {
                     final ByteBuffer currentFileBuffer = fileBuffer;
                     fileBuffer = readIntoFileBufferIfNecessary(fileBuffer, encodedSize + 1, false);
                     //change wrapper if fileBuffer has changed
                     if (fileBuffer != currentFileBuffer) {
                        fileBufferWrapper = wrapWhole(fileBuffer);
                     }
                     final int endPosition = fileBuffer.position() + encodedSize;
                     //this check must be performed upfront decoding
                     if (fileBuffer.remaining() >= (encodedSize + 1) && fileBuffer.get(endPosition) == Page.END_BYTE) {
                        fileBufferWrapper.setIndex(fileBuffer.position(), endPosition);
                        final boolean skipMessage = skipRecord.skip(fileBufferWrapper);
                        if (!skipMessage) {
                           final PagedMessageImpl msg = new PagedMessageImpl(encodedSize, storageManager);
                           msg.decode(fileBufferWrapper);
                           assert fileBuffer.get(endPosition) == Page.END_BYTE : "decoding cannot change end byte";
                           msg.initMessage(storage);
                           assert validateLargeMessageStorageManager(msg);
                           if (logger.isTraceEnabled()) {
                              logger.tracef("Reading message %s on pageId=%d for address=%s", msg, pageId, storeName);
                           }
                           if (messages != null) {
                              messages.accept(msg);
                           }
                           msg.setPageNr(this.pageId).setMessageNr(totalMessageCount);
                        }
                        totalMessageCount++;
                        fileBuffer.position(endPosition + 1);
                        processedBytes = nextPosition;
                     } else {
                        markFileAsSuspect(file.getFileName(), processedBytes, totalMessageCount + 1);
                        return totalMessageCount;
                     }
                  } else {
                     markFileAsSuspect(file.getFileName(), processedBytes, totalMessageCount + 1);
                     return totalMessageCount;
                  }
               } else {
                  markFileAsSuspect(file.getFileName(), processedBytes, totalMessageCount + 1);
                  return totalMessageCount;
               }
               remainingBytes = fileSize - processedBytes;
            }
            while (remainingBytes >= MINIMUM_MSG_PERSISTENT_SIZE);
         }
         //ignore incomplete messages at the end of the file
         if (logger.isTraceEnabled()) {
            logger.tracef("%s has %d bytes of unknown data at position = %d", file.getFileName(), remainingBytes, processedBytes);
         }
         return totalMessageCount;
      } finally {
         if (fileBuffer != null) {
            fileFactory.releaseBuffer(fileBuffer);
         }
         size.lazySet(processedBytes);
         if (file.position() != processedBytes) {
            file.position(processedBytes);
         }
      }
   }

   public synchronized void write(final PagedMessage message) throws Exception {
      writeDirect(message);
      storageManager.pageWrite(message, pageId);
   }

   /** This write will not interact back with the storage manager.
    *  To avoid ping pongs with Journal retaining events and any other stuff. */
   public void writeDirect(PagedMessage message) throws Exception {
      if (!file.isOpen()) {
         throw ActiveMQMessageBundle.BUNDLE.cannotWriteToClosedFile(file);
      }
      final int messageEncodedSize = message.getEncodeSize();
      final int bufferSize = messageEncodedSize + Page.SIZE_RECORD;
      final ByteBuffer buffer = fileFactory.newBuffer(bufferSize);
      ChannelBufferWrapper activeMQBuffer = new ChannelBufferWrapper(Unpooled.wrappedBuffer(buffer));
      activeMQBuffer.clear();
      activeMQBuffer.writeByte(Page.START_BYTE);
      activeMQBuffer.writeInt(messageEncodedSize);
      message.encode(activeMQBuffer);
      activeMQBuffer.writeByte(Page.END_BYTE);
      assert (activeMQBuffer.readableBytes() == bufferSize) : "messageEncodedSize is different from expected";
      //buffer limit and position are the same
      assert (buffer.remaining() == bufferSize) : "buffer position or limit are changed";
      file.writeDirect(buffer, false);
      if (callback != null) {
         callback.accept(message);
      }
      addMessage(message);
      //lighter than addAndGet when single writer
      numberOfMessages++;
      size.lazySet(size.get() + bufferSize);
   }

   public void sync() throws Exception {
      file.sync();
   }

   public boolean isOpen() {
      return file != null && file.isOpen();
   }


   public boolean open(boolean createFile) throws Exception {
      boolean isOpen = false;
      if (!file.isOpen() && (createFile || file.exists())) {
         file.open();
         isOpen = true;
      }
      if (file.isOpen()) {
         isOpen = true;
         size.set((int) file.size());
         file.position(0);
      }
      return isOpen;
   }

   public void close(boolean sendEvent) throws Exception {
      close(sendEvent, true);
      this.callback = null;
   }

   /**
    * sendEvent means it's a close happening from a major event such moveNext.
    * While reading the cache we don't need (and shouldn't inform the backup
    */
   public synchronized void close(boolean sendEvent, boolean waitSync) throws Exception {
      if (readFileBuffer != null) {
         fileFactory.releaseDirectBuffer(readFileBuffer);
         readFileBuffer = null;
      }

      if (sendEvent && storageManager != null) {
         storageManager.pageClosed(storeName, pageId);
      }
      file.close(waitSync, waitSync);

      Set<PageSubscriptionCounter> counters = getPendingCounters();
      if (counters != null) {
         for (PageSubscriptionCounter counter : counters) {
            counter.cleanupNonTXCounters(this.getPageId());
         }
      }
   }

   public boolean delete(final LinkedList<PagedMessage> messages) throws Exception {
      if (storageManager != null) {
         storageManager.pageDeleted(storeName, pageId);
      }

      if (logger.isDebugEnabled()) {
         logger.debugf("Deleting pageNr=%d on store %s", pageId, storeName);
      }

      if (messages != null) {
         try (LinkedListIterator<PagedMessage> iter = messages.iterator()) {
            while (iter.hasNext()) {
               PagedMessage msg = iter.next();
               if ((msg.getMessage()).isLargeMessage()) {
                  ((LargeServerMessage)(msg.getMessage())).deleteFile();
                  msg.getMessage().usageDown();
               }
            }
         }
      }

      storageManager.afterCompleteOperations(new IOCallback() {
         @Override
         public void done() {
            try {
               if (suspiciousRecords) {
                  ActiveMQServerLogger.LOGGER.pageInvalid(file.getFileName(), file.getFileName());
                  file.renameTo(file.getFileName() + ".invalidPage");
               } else {
                  file.delete();
               }
               referenceCounter.reset();
            } catch (Exception e) {
               ActiveMQServerLogger.LOGGER.pageDeleteError(e);
            }
         }

         @Override
         public void onError(int errorCode, String errorMessage) {

         }
      });

      return true;
   }

   public int readNumberOfMessages() throws Exception {
      boolean wasOpen = isOpen();

      if (!wasOpen) {
         if (!open(false)) {
            return 0;
         }
      }

      try {
         int numberOfMessages = readFromSequentialFile(null, null, SKIP_ALL);
         if (logger.isDebugEnabled()) {
            logger.debug(">>> Reading numberOfMessages page " + this.pageId + ", returning " + numberOfMessages);
         }
         return numberOfMessages;
      } finally {
         if (!wasOpen) {
            close(false);
         }
      }
   }

   public int getNumberOfMessages() {
      return numberOfMessages;
   }

   public int getSize() {
      return size.intValue();
   }

   @Override
   public String toString() {
      return "Page::seqCreation=" + seqInt + ", pageNr=" + this.pageId + ", file=" + this.file;
   }

   @Override
   public int compareTo(Page o) {
      return 0;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      Page page = (Page) o;

      return pageId == page.pageId;
   }

   @Override
   public int hashCode() {
      return (int) (pageId ^ (pageId >>> 32));
   }

   /**
    * @param position
    * @param msgNumber
    */
   private void markFileAsSuspect(final String fileName, final int position, final int msgNumber) {
      ActiveMQServerLogger.LOGGER.pageSuspectFile(fileName, position, msgNumber);
      suspiciousRecords = true;
   }

   public SequentialFile getFile() {
      return file;
   }

   /**
    * This will indicate a page that will need to be called on cleanup when the page has been closed and confirmed
    *
    * @param pageSubscriptionCounter
    */
   public void addPendingCounter(PageSubscriptionCounter pageSubscriptionCounter) {
      getOrCreatePendingCounters().add(pageSubscriptionCounter);
   }

   private synchronized Set<PageSubscriptionCounter> getPendingCounters() {
      return pendingCounters;
   }

   private synchronized Set<PageSubscriptionCounter> getOrCreatePendingCounters() {
      if (pendingCounters == null) {
         pendingCounters = new ConcurrentHashSet<>();
      }

      return pendingCounters;
   }
}
