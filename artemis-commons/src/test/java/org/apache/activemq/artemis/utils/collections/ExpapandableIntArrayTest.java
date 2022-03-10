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

import org.junit.Assert;
import org.junit.Test;

public class ExpapandableIntArrayTest {


   @Test
   public void testAdds() {
      ExpapandableLongArray arrayVar = new ExpapandableLongArray();
      Assert.assertEquals(0, arrayVar.size());
      arrayVar.addElement(33L);
      Assert.assertEquals(1, arrayVar.size());
      Assert.assertEquals(33L, arrayVar.elements()[0]);

      arrayVar.addElement(77L);
      Assert.assertEquals(2, arrayVar.size());
      Assert.assertEquals(33L, arrayVar.elements()[0]);
      Assert.assertEquals(77L, arrayVar.elements()[1]);

      Assert.assertTrue(arrayVar.contains(33));
      Assert.assertTrue(arrayVar.contains(77));
      Assert.assertFalse(arrayVar.contains(50));
      arrayVar.addElement(50L);
      Assert.assertTrue(arrayVar.contains(50L));


      arrayVar.addElement(50);

      Assert.assertEquals(3, arrayVar.size());
   }

   @Test
   public void testInitialAdd() {
      ExpapandableLongArray arrayVar = new ExpapandableLongArray(77L);
      Assert.assertEquals(1, arrayVar.size());
      arrayVar.addElement(33L);
      Assert.assertEquals(2, arrayVar.size());
      Assert.assertEquals(77L, arrayVar.elements()[0]);
      Assert.assertEquals(33L, arrayVar.elements()[1]);

      arrayVar.addElement(77L);
      Assert.assertEquals(2, arrayVar.size());
      Assert.assertEquals(77L, arrayVar.elements()[0]);
      Assert.assertEquals(33L, arrayVar.elements()[1]);
   }

}
