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

package org.apache.activemq.artemis.utils.collections;

import java.util.Arrays;

public class ExpapandableLongArray {

   long[] elements;

   public ExpapandableLongArray() {
   }

   public ExpapandableLongArray(long initialElement) {
      this.elements = new long[] {initialElement};
   }

   public ExpapandableLongArray(long[] elements) {
      this.elements = elements;
   }

   public void addElement(long element) {
      if (elements == null) {
         elements = new long[1];
         elements[0] = element;
      } else {
         if (contains(element)) {
            return;
         }
         long[] newElements = Arrays.copyOf(elements, elements.length + 1);
         newElements[elements.length] = element;
         this.elements = newElements;
      }
   }

   public final boolean contains(long el) {
      final int size = elements != null ? elements.length : 0;
      for (int i = 0; i < size; i++) {
         if (elements[i] == el) {
            return true;
         }
      }
      return false;
   }

   public final int size() {
      return elements == null ? 0 : elements.length;
   }

   public final long[] elements() {
      return elements;
   }

}
