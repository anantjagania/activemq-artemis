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

package org.apache.activemq.artemis.core.paging.impl.pgctrl;

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.utils.DataConstants;

public class PageControlPGTX extends PageControlDataAbstract {

   private long pgTX;

   public PageControlPGTX(long pgTX) {
      this.pgTX = pgTX;
   }


   @Override
   public int getEncodeSize() {
      return DataConstants.SIZE_BYTE + DataConstants.SIZE_LONG;
   }

   @Override
   public void encode(ByteBuf buffer) {
      buffer.writeByte(PAGE_TX);
      buffer.writeLong(pgTX);
   }

   @Override
   public void decode(ByteBuf buffer) {
      this.pgTX = buffer.readLong();
   }

   @Override
   protected byte getControlByte() {
      return PAGE_TX;
   }
}
