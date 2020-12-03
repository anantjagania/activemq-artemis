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

package org.apache.activemq.artemis.protocol.amqp.broker;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.core.io.SequentialFile;
import org.apache.activemq.artemis.utils.DataConstants;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;


public class FileBasedMappedBuffer implements ReadableBuffer {
   final SequentialFile file;
   ByteBuf workingBuffer;
   // Position of the working buffer in the file
   int offset = 0;
   int currentFilePosition = -1;
   int maxPosition = -1;
   int limit;
   int readPosition = 0;


   private FileBasedMappedBuffer(FileBasedMappedBuffer copy) {
      this.file = copy.file;
      this.workingBuffer = copy.workingBuffer;
      this.currentFilePosition = copy.currentFilePosition;
      this.maxPosition = copy.maxPosition;
      this.limit = copy.limit;
   }

   private void checkWorkingBuffer(int filePosition, int requiredBytes) {

      try {
         if (currentFilePosition < 0) {
            // we never read anything from the file, so we read it now
            readFromFile(filePosition);
         } else {
            if (filePosition < currentFilePosition || filePosition + requiredBytes > maxPosition) {
               readFromFile(filePosition);
            }
         }

         workingBuffer.readerIndex(filePosition - currentFilePosition);

      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   public SequentialFile getFile() {
      return file;
   }

   public void freeDirectBuffer() {
      if (workingBuffer != null) {
         workingBuffer.release();
         workingBuffer = null;
      }
   }

   private void readFromFile(int filePosition) throws Exception {
      // never read, so we read it now
      file.position(filePosition);
      workingBuffer.resetReaderIndex().writerIndex(workingBuffer.capacity());
      int maxRead = file.read(workingBuffer.nioBuffer());
      workingBuffer.setIndex(0, maxRead);
      currentFilePosition = filePosition;
      maxPosition = currentFilePosition + maxRead;
   }

   public FileBasedMappedBuffer(SequentialFile file, ByteBuf workingBuffer) {
      try {
         this.file = file;
         this.workingBuffer = workingBuffer;
         this.limit = (int) file.size();
      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   @Override
   public int capacity() {
      return 0;
   }

   @Override
   public boolean hasArray() {
      return false;
   }

   @Override
   public byte[] array() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int arrayOffset() {
      throw new UnsupportedOperationException();
   }

   @Override
   public ReadableBuffer reclaimRead() {
      return this;
   }

   @Override
   public byte get() {
      checkWorkingBuffer(offset + readPosition++, 1);
      return workingBuffer.readByte();
   }

   @Override
   public byte get(int i) {
      checkWorkingBuffer(offset + i, 1);
      return workingBuffer.getByte(i - currentFilePosition);
   }

   @Override
   public int getInt() {
      checkWorkingBuffer(offset + readPosition, DataConstants.SIZE_INT);
      readPosition += DataConstants.SIZE_INT;
      return workingBuffer.readInt();
   }

   @Override
   public long getLong() {
      checkWorkingBuffer(offset + readPosition, DataConstants.SIZE_LONG);
      readPosition += DataConstants.SIZE_LONG;
      return workingBuffer.readInt();
   }

   @Override
   public short getShort() {
      checkWorkingBuffer(offset + readPosition, DataConstants.SIZE_SHORT);
      readPosition += DataConstants.SIZE_SHORT;
      return workingBuffer.readShort();
   }

   @Override
   public float getFloat() {
      checkWorkingBuffer(offset + readPosition, DataConstants.SIZE_FLOAT);
      readPosition += DataConstants.SIZE_FLOAT;
      return workingBuffer.readFloat();
   }

   @Override
   public double getDouble() {
      checkWorkingBuffer(offset + readPosition, DataConstants.SIZE_DOUBLE);
      readPosition += DataConstants.SIZE_DOUBLE;
      return workingBuffer.readDouble();
   }

   @Override
   public ReadableBuffer get(byte[] bytes, final int offset, final int length) {
      try {
         file.read(bytes, offset, length);
         return this;
      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   @Override
   public ReadableBuffer get(byte[] bytes) {
      return get(bytes, 0, bytes.length);
   }

   @Override
   public ReadableBuffer get(WritableBuffer writableBuffer) {
      return null;
   }

   @Override
   public ReadableBuffer slice() {
      FileBasedMappedBuffer copy = new FileBasedMappedBuffer(this);
      copy.offset = this.readPosition + offset;
      copy.limit = this.offset + this.limit - copy.offset;
      return copy;
   }

   @Override
   public ReadableBuffer flip() {
      throw new UnsupportedOperationException();
   }

   @Override
   public ReadableBuffer limit(int limit) {
      this.limit = limit;
      return this;
   }

   @Override
   public int limit() {
      return limit;
   }

   @Override
   public ReadableBuffer position(int position) {
      this.readPosition = position;
      return this;
   }

   @Override
   public int position() {
      return readPosition;
   }

   @Override
   public ReadableBuffer mark() {
      return this;
   }

   @Override
   public ReadableBuffer reset() {
      position(0);
      return this;
   }

   @Override
   public ReadableBuffer rewind() {
      position(0);
      return this;
   }

   @Override
   public ReadableBuffer clear() {
      position(0);
      return this;
   }

   @Override
   public int remaining() {
      try {
         return limit - readPosition;
      } catch (Exception e) {
         throw new RuntimeException(e.getMessage(), e);
      }
   }

   @Override
   public boolean hasRemaining() {
      return remaining() > 0;
   }

   @Override
   public ReadableBuffer duplicate() {
      return new FileBasedMappedBuffer(this);
   }

   @Override
   public ByteBuffer byteBuffer() {
      throw new UnsupportedOperationException();
   }

   @Override
   public String readUTF8() throws CharacterCodingException {
      return readString(StandardCharsets.UTF_8.newDecoder());
   }

   @Override
   public String readString(CharsetDecoder charsetDecoder) throws CharacterCodingException {
      checkWorkingBuffer(offset + readPosition, 0);

      int bytesToRead = limit - readPosition;

      if (bytesToRead <= workingBuffer.readableBytes()) {
         workingBuffer.writerIndex(bytesToRead + workingBuffer.readerIndex());
         String str = workingBuffer.toString(charsetDecoder.charset());
         workingBuffer.writerIndex(workingBuffer.capacity());
         readPosition += bytesToRead;
         return str;
      } else {
         return readStringFromFile(charsetDecoder);
      }
   }

   // Adapted from CompositeReadableBuffer::readSringFromComponents
   private String readStringFromFile(CharsetDecoder decoder) throws CharacterCodingException {
      int size = (int)(remaining() * decoder.averageCharsPerByte());
      CharBuffer decoded = CharBuffer.allocate(size);

      CoderResult step;

      checkWorkingBuffer(offset + readPosition, 1);
      ByteBuffer currentRead = workingBuffer.nioBuffer();

      int upperLimit = limit - readPosition;
      if (upperLimit < currentRead.limit()) {
         currentRead.limit(upperLimit);
      }


      //currentRead.limit(workingBuffer.writerIndex());
      //currentRead.position(workingBuffer.readerIndex());
      readPosition += currentRead.remaining();
      boolean endOfInput = !hasRemaining();

      do {

         step = decoder.decode(currentRead, decoded, endOfInput);

         if (step.isUnderflow()) {
            if (endOfInput) {
               step = decoder.flush(decoded);
               break;
            } else {
               checkWorkingBuffer(offset + readPosition, 1);
               currentRead = workingBuffer.nioBuffer();
               currentRead.position(workingBuffer.readerIndex());
               readPosition += currentRead.remaining();
               if (readPosition > limit) {
                  int overFlow = readPosition - limit;
                  readPosition -= overFlow;
                  currentRead.limit(currentRead.limit() - overFlow);
               }
               endOfInput = !hasRemaining();
            }
         } else if (step.isOverflow()) {
            size = 2 * size + 1;
            CharBuffer upsized = CharBuffer.allocate(size);
            decoded.flip();
            upsized.put(decoded);
            decoded = upsized;
            continue;
         }
      }
      while (!step.isError());

      if (step.isError()) {
         step.throwException();
      }

      return ((CharBuffer) decoded.flip()).toString();
   }


}
