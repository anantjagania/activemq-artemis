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

package org.apache.activemq.artemis.protocol.amqp.broker;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.apache.activemq.artemis.core.io.SequentialFile;
import org.apache.activemq.artemis.core.io.nio.NIOSequentialFileFactory;
import org.apache.activemq.artemis.protocol.amqp.util.NettyReadable;
import org.apache.activemq.artemis.protocol.amqp.util.NettyWritable;
import org.apache.activemq.artemis.protocol.amqp.util.TLSEncode;
import org.apache.activemq.artemis.utils.DataConstants;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.codec.EncoderImpl;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.message.Message;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileBasedMappedBufferTest {

   @Rule
   public final TemporaryFolder temporaryFolder;

   public FileBasedMappedBufferTest() {
      File place = new File("./target/tmp/FileBasedMappedBufferTest");
      place.mkdirs();

      temporaryFolder = new TemporaryFolder(place);
   }

   @Test
   public void testGetBytes() throws Exception {
      int size = 1024 * 100;
      Assert.assertTrue(temporaryFolder.getRoot().exists());
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);

      for (int i = 0; i < size; i++) {
         fileOutputStream.write((byte) (i % 255));
      }
      fileOutputStream.close();

      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();

      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(10 * 1024);

      try {
         FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);

         for (int i = 0; i < size; i++) {
            Assert.assertEquals("wrong at " + i, (byte) (i % 255), fileBasedMappedBuffer.get());
         }

         for (int i = 0; i < size; i++) {
            Assert.assertEquals("wrong at " + i, (byte) (i % 255), fileBasedMappedBuffer.get(i));
         }
      } finally {
         buf.release();
      }


   }

   @Test
   public void testGetInteger() throws Exception {
      int size = 10_000;
      Assert.assertTrue(temporaryFolder.getRoot().exists());
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

      for (int i = 0; i < size; i++) {
         dataOutputStream.writeInt(i);
      }
      fileOutputStream.close();

      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();

      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(333); // some really odd number to make calculations more challenging

      try {
         FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);

         for (int i = 0; i < size; i++) {
            Assert.assertEquals("wrong at " + i, i, fileBasedMappedBuffer.getInt());
         }

         for (int i = 0; i < size; i++) {
            fileBasedMappedBuffer.position(i * 4);
            Assert.assertEquals("wrong at " + i, i, fileBasedMappedBuffer.getInt());
         }
      } finally {
         buf.release();
      }
   }

   @Test
   public void testGetFloat() throws Exception {
      int size = 10_000;
      System.out.println("Folder::" + temporaryFolder.getRoot());
      Assert.assertTrue(temporaryFolder.getRoot().exists());
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

      for (int i = 0; i < size; i++) {
         dataOutputStream.writeFloat((float)i);
      }
      fileOutputStream.close();

      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();

      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(333); // some really odd number to make calculations more challenging

      try {
         FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);

         for (int i = 0; i < size; i++) {
            float floatRead = fileBasedMappedBuffer.getFloat();
            Assert.assertEquals("wrong at " + i + " read was " + floatRead, (float)i, floatRead, 0);
         }

         for (int i = 0; i < size; i++) {
            fileBasedMappedBuffer.position(i * DataConstants.SIZE_FLOAT);
            Assert.assertEquals("wrong at " + i, i, fileBasedMappedBuffer.getFloat(), (float) 0);
         }
      } finally {
         buf.release();
      }
   }

   @Test
   public void testGetShort() throws Exception {
      int size = 10_000;
      Assert.assertTrue(temporaryFolder.getRoot().exists());
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

      for (int i = 0; i < size; i++) {
         dataOutputStream.writeShort((short)i);
      }
      fileOutputStream.close();

      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();

      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(333); // some really odd number to make calculations more challenging

      try {
         FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);

         for (int i = 0; i < size; i++) {
            short shortRead = fileBasedMappedBuffer.getShort();
            Assert.assertEquals("wrong at " + i + " read was " + shortRead, (short)i, shortRead);
         }

         for (int i = 0; i < size; i++) {
            fileBasedMappedBuffer.position(i * DataConstants.SIZE_SHORT);
            Assert.assertEquals("wrong at " + i, i, fileBasedMappedBuffer.getShort());
         }
      } finally {
         buf.release();
      }
   }
   @Test
   public void testSliceOverBuffer() throws Exception {
      int size = 250;
      ByteBuffer buffer = ByteBuffer.allocate(size);
      for (int i = 0; i < size; i++) {
         buffer.put((byte)i);
      }

      buffer.position(100);

      buffer = buffer.slice();

      Assert.assertEquals(150, buffer.limit());

      for (int i = 100; i < size; i++) {
         Assert.assertEquals((byte)i, buffer.get());
      }
   }

   @Test
   public void testSliceOverFile() throws Exception {
      int size = 250;
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);

      for (int i = 0; i < size; i++) {
         fileOutputStream.write(i);
      }

      fileOutputStream.close();


      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();

      ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(333); // some really odd number to make calculations more challenging

      try {
         FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);

         fileBasedMappedBuffer.position(100);
         ReadableBuffer sliced = fileBasedMappedBuffer.slice();
         Assert.assertEquals(150, sliced.limit());

         for (int i = 100; i < size - 50; i++) {
            Assert.assertEquals((byte)i, sliced.get());
         }

         sliced = sliced.slice(); // slice of the slice

         Assert.assertEquals(50, sliced.limit());

         for (int i = size - 50; i < size; i++) {
            Assert.assertEquals((byte)i, sliced.get());
         }

         fileBasedMappedBuffer.rewind();

         Assert.assertEquals(size, fileBasedMappedBuffer.remaining());
         Assert.assertTrue(fileBasedMappedBuffer.hasRemaining());
         fileBasedMappedBuffer.position(10);

         Assert.assertEquals(size - 10, fileBasedMappedBuffer.remaining());

         sliced = fileBasedMappedBuffer.slice();

         Assert.assertEquals(fileBasedMappedBuffer.remaining(), sliced.remaining());

         sliced.limit(10);

         Assert.assertEquals(10, sliced.remaining());

         for (int i = 10; i < 20; i++) {
            Assert.assertEquals((byte)i, sliced.get());
         }

         Assert.assertFalse(sliced.hasRemaining());
         Assert.assertEquals(0, sliced.remaining());


      } finally {
         buf.release();
      }


   }

   @Test
   public void testDecodeLargeApplicationProperties() throws Exception {
      Message message = Proton.message();
      byte[] payload = new byte[10 * 1024];
      for (int i = 0; i < payload.length; i++) {
         payload[i] = ' ';
      }
      message.setBody(new AmqpValue(payload));

      StringBuffer strbuffer = new StringBuffer();
      for (int i = 0; i < 1024 * 1024; i++) {
         strbuffer.append(" ");
      }
      HashMap applicationPropertiesMap = new HashMap<>();
      ApplicationProperties properties = new ApplicationProperties(applicationPropertiesMap);
      applicationPropertiesMap.put("str", strbuffer.toString());

      EncoderImpl encoder = TLSEncode.getEncoder();
      final ByteBuffer bufferOutput = ByteBuffer.allocate(2 * 1024 * 1024);
      encoder.setByteBuffer(bufferOutput);
      encoder.writeObject(properties);

      int position = bufferOutput.position();
      message.setApplicationProperties(properties);

      FileBasedMappedBuffer fileBasedMappedBuffer = createFileFromBuffer(bufferOutput);

      bufferOutput.position(0);
      ByteBuf nettyBuffer = Unpooled.wrappedBuffer(bufferOutput);
      nettyBuffer.setIndex(0, position);

      //TLSEncode.getDecoder().setBuffer(fileBasedMappedBuffer);
      TLSEncode.getDecoder().setBuffer(new NettyReadable(nettyBuffer));
      ApplicationProperties applicationProperties = (ApplicationProperties)TLSEncode.getDecoder().readObject();

      String stringProperty = (String)applicationProperties.getValue().get("str");
      System.out.println("String property:" + stringProperty);



      Assert.assertEquals(strbuffer.toString(), applicationProperties.getValue().get("str"));
      Assert.assertNotSame(strbuffer.toString(), applicationProperties.getValue().get("str"));

      //bufferOutput.get
   }

   @Test
   public void testWithWriteStringWithProtonEncoder() throws Exception {
      EncoderImpl encoder = TLSEncode.getEncoder();
      ByteBuffer bufferOutput = ByteBuffer.allocate(10 * 1024);
      encoder.setByteBuffer(bufferOutput);
      encoder.writeString("str1");
      encoder.writeString("str2");


      int position = bufferOutput.position();
      FileBasedMappedBuffer fileBasedMappedBuffer = createFileFromBuffer(bufferOutput);

      bufferOutput.position(0);
      ByteBuf nettyBuffer = Unpooled.wrappedBuffer(bufferOutput);
      nettyBuffer.setIndex(0, position);

      for (int i = 0; i < 2; i++) {
         if (i == 0) {
            // the only purpose of this is to sanity check the test.
            // FileBasedMappedBuffer should have the same semantic here as NettyBuffer
            // so this is to verify the test is correct
            TLSEncode.getDecoder().setBuffer(new NettyReadable(nettyBuffer));
         } else {
            TLSEncode.getDecoder().setBuffer(fileBasedMappedBuffer);
         }

         String str1 = TLSEncode.getDecoder().readString();
         Assert.assertNotNull(str1);
         Assert.assertEquals("str1", str1);
         String str2 = TLSEncode.getDecoder().readString();
         Assert.assertNotNull(str2);
         Assert.assertEquals("str2", str2);
      }

      fileBasedMappedBuffer.getFile().close();
   }

   private void addString(WritableBuffer buffer, String added) {
      int originalPosition = buffer.position();
      buffer.putInt(0);
      int stringPosition = buffer.position();
      buffer.put(added);
      int endPosition = buffer.position();
      buffer.position(originalPosition);
      buffer.putInt(endPosition - stringPosition);
      buffer.position(endPosition);
   }

   private String getString(ReadableBuffer buffer) throws Exception {
      int size = buffer.getInt();
      ReadableBuffer sliced = buffer.slice().limit(size);
      buffer.position(buffer.position() + size);
      return sliced.readUTF8();
   }

   @Test
   public void testWithPureString() throws Exception {

      String largeString;
      {
         StringBuffer strBuffer = new StringBuffer();
         while (strBuffer.length() < 100 * 1024) {
            strBuffer.append("aaaaaaa");
         }
         largeString = strBuffer.toString();
      }

      ByteBuf nettyBuffer = Unpooled.buffer(512 * 1024);
      NettyWritable nettyWritable = new NettyWritable(nettyBuffer);
      // writing 4 bytes to later replace on
      addString(nettyWritable, "smallString");
      addString(nettyWritable, largeString);
      addString(nettyWritable, "thirdString");
      // addString(nettyWritable, "thirdString");
      ByteBuffer bufPass = nettyBuffer.nioBuffer();
      bufPass.position(nettyBuffer.writerIndex());
      FileBasedMappedBuffer fileBasedMappedBuffer = createFileFromBuffer(bufPass);

      for (int i = 0; i < 2; i++) {

         ReadableBuffer readableBuffer;
         if (i == 0) {
            // the only purpose of this is to sanity check the test.
            // FileBasedMappedBuffer should have the same semantic here as NettyBuffer
            // so this is to verify the test is correct
            readableBuffer = new NettyReadable(nettyBuffer);
         } else {
            readableBuffer = fileBasedMappedBuffer;
         }
         Assert.assertEquals("smallString", getString(readableBuffer));
         Assert.assertEquals(largeString, getString(readableBuffer));
         Assert.assertEquals("thirdString", getString(readableBuffer));
      }

      fileBasedMappedBuffer.getFile().close();
   }

   protected FileBasedMappedBuffer createFileFromBuffer(ByteBuffer bufferOutput) throws Exception {
      int position = bufferOutput.position();
      bufferOutput.position(0);
      byte[] bytesOutput = new byte[position];
      bufferOutput.get(bytesOutput);
      File file = new File(temporaryFolder.getRoot(), "test.bin");
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      fileOutputStream.write(bytesOutput);
      fileOutputStream.close();
      NIOSequentialFileFactory nioSequentialFileFactory = new NIOSequentialFileFactory(temporaryFolder.getRoot(), 1);
      SequentialFile sequentialFile = nioSequentialFileFactory.createSequentialFile("test.bin");
      sequentialFile.open();
      ByteBuf buf = Unpooled.buffer(333);
      FileBasedMappedBuffer fileBasedMappedBuffer = new FileBasedMappedBuffer(sequentialFile, buf);
      return fileBasedMappedBuffer;
   }
}
