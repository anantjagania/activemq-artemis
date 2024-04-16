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

package org.apache.activemq.artemis.core.deleteThis;

import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.io.SequentialFileFactory;
import org.apache.activemq.artemis.core.io.nio.NIOSequentialFileFactory;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.journal.impl.JournalFile;
import org.apache.activemq.artemis.core.journal.impl.JournalImpl;
import org.apache.activemq.artemis.core.journal.impl.JournalReaderCallback;
import org.apache.activemq.artemis.core.persistence.impl.journal.JournalRecordIds;
import org.apache.activemq.artemis.core.persistence.impl.nullpm.NullStorageManager;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessagePersisterV3;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPStandardMessage;
import org.apache.activemq.artemis.spi.core.protocol.MessagePersister;
import org.apache.activemq.artemis.tests.util.CFUtil;
import org.apache.activemq.artemis.utils.ByteUtil;
import org.apache.qpid.protonj2.buffer.ProtonBuffer;
import org.apache.qpid.protonj2.buffer.ProtonBufferAllocator;
import org.apache.qpid.protonj2.buffer.ProtonBufferInputStream;
import org.apache.qpid.protonj2.buffer.impl.ProtonByteArrayBuffer;
import org.apache.qpid.protonj2.buffer.impl.ProtonByteArrayBufferAllocator;
import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.Connection;
import org.apache.qpid.protonj2.client.Delivery;
import org.apache.qpid.protonj2.client.Receiver;
import org.apache.qpid.protonj2.client.ReceiverOptions;
import org.apache.qpid.protonj2.codec.DecodeException;
import org.apache.qpid.protonj2.codec.StreamDecoder;
import org.apache.qpid.protonj2.codec.StreamDecoderState;
import org.apache.qpid.protonj2.codec.decoders.ProtonDecoder;
import org.apache.qpid.protonj2.codec.decoders.ProtonDecoderFactory;
import org.apache.qpid.protonj2.codec.decoders.ProtonDecoderState;
import org.apache.qpid.protonj2.codec.decoders.ProtonStreamDecoderFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteThisTest {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   @Test
   public void testReadMessage() throws Exception {
      try (Connection connection = Client.create().connect("localhost", 5672)) {
         final ReceiverOptions options = new ReceiverOptions().creditWindow(0)
            .autoAccept(false)
            .autoSettle(false);
         final Receiver receiver = connection.openReceiver("amqp-demo-topic::sub1.amqp-demo-topic:global", options);
         int messageCount = 0;
         while (true) {
            logger.info("===== Starting read of message {} =======", ++messageCount);
            receiver.addCredit(1);
            final Delivery delivery = receiver.receive(5, TimeUnit.SECONDS);
            if (delivery == null) {
               logger.trace("Timed out waiting for next message, likely all are consumed");
               break;
            }
            final InputStream stream = delivery.rawInputStream();
            final StreamDecoder decoder = ProtonStreamDecoderFactory.create();
            final StreamDecoderState decoderState = decoder.newDecoderState();
            try {
               long skipAmount = 0;
               while (stream.available() > 0) {
                  try {
                     stream.mark(stream.available());
                     final Object decoded = decoder.readObject(stream, decoderState.reset());
                     logger.info("Read object: {}", decoded);
                  } catch (DecodeException de) {
                     stream.reset();
                     stream.skip(++skipAmount);
                     if (stream.available() == 0) {
                        break;
                     }
                  }
               }
            } catch (Exception e) {
               logger.warn("Caught Error during decode: {}", e.getMessage());
            } finally {
               delivery.accept();
            }
         }
      }
   }

   @Test
   public void testSendMessage() throws Exception {
      ConnectionFactory cf = CFUtil.createConnectionFactory("AMQP", "tcp://localhost:61616");
      try (javax.jms.Connection connection = cf.createConnection()) {
         connection.setClientID("connection1");
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic("myTopic");
         MessageConsumer consumer = session.createDurableConsumer(topic, "myTopic1");
      }
      try (javax.jms.Connection connection = cf.createConnection()) {
         connection.setClientID("connection2");
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic("myTopic");
         MessageConsumer consumer = session.createDurableConsumer(topic, "myTopic1");
      }

      try (javax.jms.Connection connection = cf.createConnection()) {
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
         Topic topic = session.createTopic("myTopic");
         MessageProducer producer = session.createProducer(topic);

         for (int i = 0; i < 100; i++) {
            producer.send(session.createTextMessage("Test " + i));
         }

         session.commit();

      }

   }

   @Test
   public void testDirectReading() throws Exception {

      // on my test
      /*
Result::Header{ durable=true, priority=5, ttl=null, firstAcquirer=null, deliveryCount=null }
Result::DeliveryAnnotations{ {x-opt-amq-mr-id=55} }
Result::MessageAnnotations{ {x-opt-jms-dest=1, x-opt-jms-msg-type=5} }
Result::Properties{messageId=ID:9df46af5-cb67-4e94-a72d-3df891786d6b:3:1:1-1, userId=null, to='TestTopic', subject='null', replyTo='null', correlationId=null, contentType=null, contentEncoding=null, absoluteExpiryTime=null, creationTime=1713194190646, groupId='null', groupSequence=null, replyToGroupId='null' }
Result::ApplicationProperties{ {count=0} }
       */

      // on broken data
      /*
Result::DeliveryAnnotations{ {x-opt-amq-mr-dst=amqp-demo-topic, x-opt-amq-mr-id=2266799} }
Result::Header{ durable=true, priority=null, ttl=null, firstAcquirer=null, deliveryCount=null }
Result::MessageAnnotations{ {x-opt-jms-dest=1, x-opt-jms-msg-type=5} }
Result::Properties{messageId=ID:02fcc6a3-1f57-4794-850f-68f5f52b4773:138:2:141-1, userId=null, to='amqp-demo-topic', subject='null', replyTo='null', correlationId=null, contentType=null, contentEncoding=null, absoluteExpiryTime=null, creationTime=1712309424161, groupId='null', groupSequence=null, replyToGroupId='null' }
Result::ApplicationProperties{ {CamelMessageTimestamp=1712309422791, firedTime=Fri Apr 05 09:30:22 GMT 2024, TMPL_time=1712309423844, TMPL_uuid=0d52defe-1307-4d04-84bc-a316a1386c19, TMPL_hostName=mqperf-amqp-pub-7cb5c8b899-z2n6x, TMPL_counter=30445} }

       */

      String location = "/Users/clebertsuconic/Downloads/luberto/artemis-main/data/journal";
      //String location = "/Users/clebertsuconic/work/apache/activemq-artemis/tests/soak-tests/target/idempotentMirror/DC2/data/journal";
      //String location = "/Users/clebertsuconic/work/apache/activemq-artemis/tests/compatibility-tests/target/tmp/junit6543151053525563765/2/data/journal";
      SequentialFileFactory fileFactory = new NIOSequentialFileFactory(new File(location), null, 1);
      // Will use only default values. The load function should adapt to anything different
      JournalImpl journal = new JournalImpl(10 * 1024 * 1024, 10, 2, 0, 0, fileFactory, "activemq-data", "amq", 1);
      journal.start();

      List<JournalFile> files = journal.orderFiles();

      NullStorageManager storageManager = new NullStorageManager();

      for (JournalFile file : files) {
         System.out.println("*******************************************************************************************************************************");
         System.out.println("File:: " + file);
         JournalImpl.readJournalFile(fileFactory, file, new JournalReaderCallback() {
            @Override
            public void onReadEventRecord(RecordInfo info) throws Exception {
            }

            @Override
            public void done() {
            }

            void printAddRecord(RecordInfo info) {
               if (info.userRecordType == JournalRecordIds.ADD_MESSAGE_PROTOCOL) {
                  StreamDecoder streamDecoder = ProtonStreamDecoderFactory.create();
                  StreamDecoderState streamDecoderState = streamDecoder.newDecoderState();

                  ActiveMQBuffer buffer = ActiveMQBuffers.wrappedBuffer(info.data);
                  byte protocol = buffer.readByte();

                  if (protocol == (byte)5) {
                     long id = buffer.readLong();
                     long format = buffer.readLong();
                     // this instance is being used only if there are no extraProperties or just for debugging purposes:
                     // on journal loading pool shouldn't be null so it shouldn't create any garbage.
                     final SimpleString address;
                     address = SimpleString.readNullableSimpleString(buffer.byteBuf());
                     System.out.println("Id::" + id + ", format:" + format + ", address=" + address);
                     int size = buffer.readInt();
                     System.out.println("Size::" + size);
                     byte[] bytesMessage = new byte[size];
                     buffer.readBytes(bytesMessage);

                     System.out.println("Bytes on message::" + ByteUtil.bytesToHex(bytesMessage));

                     ProtonBuffer protonBuffer = ProtonByteArrayBufferAllocator.wrapped(bytesMessage);
                     ProtonBufferInputStream stream = new ProtonBufferInputStream(protonBuffer);

                     try {
                        while (stream.available() > 0) {
                           Object result = streamDecoder.readObject(stream, streamDecoderState);
                           System.out.println("Result::" + result);
                        }
                     } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                     }

                  }

                  buffer.readerIndex(0);

                  Message message = MessagePersister.getInstance().decode(buffer, null, null, storageManager);
                  System.out.println("File " + file + " has message " + message);

               }
            }

            @Override
            public void onReadAddRecord(RecordInfo info) throws Exception {
               printAddRecord(info);
            }

            @Override
            public void onReadUpdateRecord(RecordInfo recordInfo) throws Exception {
            }

            @Override
            public void onReadDeleteRecord(long recordID) throws Exception {
            }

            @Override
            public void onReadAddRecordTX(long transactionID, RecordInfo recordInfo) throws Exception {
               printAddRecord(recordInfo);
            }

            @Override
            public void onReadUpdateRecordTX(long transactionID, RecordInfo recordInfo) throws Exception {
            }

            @Override
            public void onReadDeleteRecordTX(long transactionID, RecordInfo recordInfo) throws Exception {
            }

            @Override
            public void onReadPrepareRecord(long transactionID,
                                            byte[] extraData,
                                            int numberOfRecords) throws Exception {
            }

            @Override
            public void onReadCommitRecord(long transactionID, int numberOfRecords) throws Exception {
            }

            @Override
            public void onReadRollbackRecord(long transactionID) throws Exception {
            }

            @Override
            public void markAsDataFile(JournalFile file) {
            }
         });
      }



   }

}
