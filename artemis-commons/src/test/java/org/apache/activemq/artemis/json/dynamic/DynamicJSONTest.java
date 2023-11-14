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

package org.apache.activemq.artemis.json.dynamic;

import java.lang.invoke.MethodHandles;

import org.apache.activemq.artemis.json.JsonObject;
import org.apache.activemq.artemis.utils.RandomUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicJSONTest {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   @Test
   public void testToJson() throws Exception {

      DynamicJSON<MYClass> dynamicJSON = new DynamicJSON<>();
      dynamicJSON.addMetadata(String.class, "a", (obj, value) -> obj.setA(String.valueOf(value)),  obj -> obj.getA());
      dynamicJSON.addMetadata(Integer.class, "b", (obj, value) -> obj.setB((Integer) value),  obj -> obj.getB());
      dynamicJSON.addMetadata(Integer.class, "c", (obj, value) -> obj.setC((Integer) value),  obj -> obj.getC());
      dynamicJSON.addMetadata(String.class, "d", (obj, value) -> obj.setD((String) value),  obj -> obj.getD());
      dynamicJSON.addMetadata(Integer.class, "IdCacheSize", (obj, value) -> obj.setIdCacheSize((Integer) value),  obj -> obj.getIdCacheSize());

      MYClass myObject = new MYClass();
      myObject.setA(RandomUtil.randomString());
      myObject.setB(RandomUtil.randomInt());
      myObject.setC(RandomUtil.randomInt());
      myObject.setD(null);
      myObject.setIdCacheSize(333);

      JsonObject jsonObject = dynamicJSON.toJSON(myObject);
      logger.debug("Result::" + jsonObject.toString());

      MYClass result = new MYClass();

      Assert.assertEquals(null, result.getD());
      Assert.assertNotNull(result.getIdCacheSize());
      Assert.assertEquals(333, result.getIdCacheSize().intValue());

      Assert.assertEquals(myObject, result);
   }

   public static class MYClass {
      String a;
      int b;
      Integer c;
      String d = "defaultString";
      Integer idCacheSize;

      public String getA() {
         return a;
      }

      public MYClass setA(String a) {
         this.a = a;
         return this;
      }

      public int getB() {
         return b;
      }

      public MYClass setB(int b) {
         this.b = b;
         return this;
      }

      public Integer getC() {
         return c;
      }

      public MYClass setC(Integer c) {
         this.c = c;
         return this;
      }

      public String getD() {
         return d;
      }

      public MYClass setD(String d) {
         this.d = d;
         return this;
      }

      public Integer getIdCacheSize() {
         return idCacheSize;
      }

      public MYClass setIdCacheSize(Integer idCacheSize) {
         this.idCacheSize = idCacheSize;
         return this;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o)
            return true;
         if (o == null || getClass() != o.getClass())
            return false;

         MYClass myClass = (MYClass) o;

         if (b != myClass.b)
            return false;
         if (a != null ? !a.equals(myClass.a) : myClass.a != null)
            return false;
         if (c != null ? !c.equals(myClass.c) : myClass.c != null)
            return false;
         if (d != null ? !d.equals(myClass.d) : myClass.d != null)
            return false;
         return idCacheSize != null ? idCacheSize.equals(myClass.idCacheSize) : myClass.idCacheSize == null;
      }

      @Override
      public int hashCode() {
         int result = a != null ? a.hashCode() : 0;
         result = 31 * result + b;
         result = 31 * result + (c != null ? c.hashCode() : 0);
         result = 31 * result + (d != null ? d.hashCode() : 0);
         result = 31 * result + (idCacheSize != null ? idCacheSize.hashCode() : 0);
         return result;
      }

      @Override
      public String toString() {
         return "MYClass{" + "a='" + a + '\'' + ", b=" + b + ", c=" + c + ", d='" + d + '\'' + ", idCacheSize=" + idCacheSize + '}';
      }
   }

}
