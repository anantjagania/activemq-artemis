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
package org.apache.activemq.artemis.jms.client;

import jakarta.jms.IllegalStateRuntimeException;
import jakarta.jms.InvalidClientIDException;
import jakarta.jms.InvalidClientIDRuntimeException;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.InvalidDestinationRuntimeException;
import jakarta.jms.InvalidSelectorException;
import jakarta.jms.InvalidSelectorRuntimeException;
import jakarta.jms.JMSException;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.JMSSecurityException;
import jakarta.jms.JMSSecurityRuntimeException;
import jakarta.jms.MessageFormatException;
import jakarta.jms.MessageFormatRuntimeException;
import jakarta.jms.MessageNotWriteableException;
import jakarta.jms.MessageNotWriteableRuntimeException;
import jakarta.jms.ResourceAllocationException;
import jakarta.jms.ResourceAllocationRuntimeException;
import jakarta.jms.TransactionInProgressException;
import jakarta.jms.TransactionInProgressRuntimeException;
import jakarta.jms.TransactionRolledBackException;
import jakarta.jms.TransactionRolledBackRuntimeException;

/**
 *
 */
public final class JmsExceptionUtils {

   private JmsExceptionUtils() {
      // utility class
   }

   /**
    * Converts instances of sub-classes of {@link JMSException} into the corresponding sub-class of
    * {@link JMSRuntimeException}.
    *
    * @param e
    * @return
    */
   public static JMSRuntimeException convertToRuntimeException(JMSException e) {
      if (e instanceof jakarta.jms.IllegalStateException) {
         return new IllegalStateRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof InvalidClientIDException) {
         return new InvalidClientIDRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof InvalidDestinationException) {
         return new InvalidDestinationRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof InvalidSelectorException) {
         return new InvalidSelectorRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof JMSSecurityException) {
         return new JMSSecurityRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof MessageFormatException) {
         return new MessageFormatRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof MessageNotWriteableException) {
         return new MessageNotWriteableRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof ResourceAllocationException) {
         return new ResourceAllocationRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof TransactionInProgressException) {
         return new TransactionInProgressRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      if (e instanceof TransactionRolledBackException) {
         return new TransactionRolledBackRuntimeException(e.getMessage(), e.getErrorCode(), e);
      }
      return new JMSRuntimeException(e.getMessage(), e.getErrorCode(), e);
   }
}
