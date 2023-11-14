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
import java.util.Objects;

import org.apache.activemq.artemis.api.core.SimpleString;
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
      dynamicJSON.addMetadata(SimpleString.class, "gated", (obj, value) -> obj.setGated((SimpleString) value),  obj -> obj.getGated(), obj -> obj.gated != null);
      dynamicJSON.addMetadata(Integer.class, "IdCacheSize", (obj, value) -> obj.setIdCacheSize((Integer) value),  obj -> obj.getIdCacheSize());
      dynamicJSON.addMetadata(SimpleString.class, "simpleString", (obj, value) -> obj.setSimpleString((SimpleString) value), obj -> obj.getSimpleString());
      dynamicJSON.addMetadata(Long.class, "longValue", (obj, value) -> obj.setLongValue((Long) value), obj -> obj.getLongValue());
      dynamicJSON.addMetadata(Double.class, "doubleValue", (obj, value) -> obj.setDoubleValue((Double) value), obj -> obj.getDoubleValue());
      dynamicJSON.addMetadata(Float.class, "floatValue", (obj, value) -> obj.setFloatValue((Float) value), obj -> obj.getFloatValue());

      MYClass sourceObject = new MYClass();
      sourceObject.setA(RandomUtil.randomString());
      sourceObject.setB(RandomUtil.randomInt());
      sourceObject.setC(RandomUtil.randomInt());
      sourceObject.setD(null);
      sourceObject.setIdCacheSize(333);
      sourceObject.setSimpleString(SimpleString.toSimpleString("mySimpleString"));
      sourceObject.setFloatValue(33.33f);
      sourceObject.setDoubleValue(11.11);


      JsonObject jsonObject = dynamicJSON.toJSON(sourceObject);
      Assert.assertFalse(jsonObject.containsKey("gated"));

      logger.debug("Result::" + jsonObject.toString());

      MYClass result = new MYClass();
      dynamicJSON.fromJSON(result, jsonObject.toString());
      Assert.assertEquals(sourceObject, result);


      Assert.assertEquals(null, result.getD());
      Assert.assertNotNull(result.getIdCacheSize());
      Assert.assertEquals(333, result.getIdCacheSize().intValue());
      Assert.assertEquals(33.33f, result.getFloatValue().floatValue(), 0);
      Assert.assertEquals(11.11, result.getDoubleValue().doubleValue(), 0);

      sourceObject.setGated(SimpleString.toSimpleString("gated-now-has-value"));
      jsonObject = dynamicJSON.toJSON(sourceObject);
      Assert.assertTrue(jsonObject.containsKey("gated"));
      Assert.assertEquals("gated-now-has-value", jsonObject.getString("gated"));


   }

   public static class MYClass {
      String a;
      int b;
      Integer c;
      String d = "defaultString";
      Integer idCacheSize;
      SimpleString simpleString;
      SimpleString gated;

      Long longValue;
      Double doubleValue;
      Float floatValue;

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

      public Long getLongValue() {
         return longValue;
      }

      public MYClass setLongValue(Long longValue) {
         this.longValue = longValue;
         return this;
      }

      public Double getDoubleValue() {
         return doubleValue;
      }

      public MYClass setDoubleValue(Double doubleValue) {
         this.doubleValue = doubleValue;
         return this;
      }

      public Float getFloatValue() {
         return floatValue;
      }

      public MYClass setFloatValue(Float floatValue) {
         this.floatValue = floatValue;
         return this;
      }

      public Integer getIdCacheSize() {
         return idCacheSize;
      }

      public MYClass setIdCacheSize(Integer idCacheSize) {
         this.idCacheSize = idCacheSize;
         return this;
      }

      public SimpleString getSimpleString() {
         return simpleString;
      }

      public MYClass setSimpleString(SimpleString simpleString) {
         this.simpleString = simpleString;
         return this;
      }

      public SimpleString getGated() {
         return gated;
      }

      public MYClass setGated(SimpleString gated) {
         this.gated = gated;
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
         if (!Objects.equals(a, myClass.a))
            return false;
         if (!Objects.equals(c, myClass.c))
            return false;
         if (!Objects.equals(d, myClass.d))
            return false;
         if (!Objects.equals(idCacheSize, myClass.idCacheSize))
            return false;
         if (!Objects.equals(simpleString, myClass.simpleString))
            return false;
         if (!Objects.equals(gated, myClass.gated))
            return false;
         if (!Objects.equals(longValue, myClass.longValue))
            return false;
         if (!Objects.equals(doubleValue, myClass.doubleValue))
            return false;
         return Objects.equals(floatValue, myClass.floatValue);
      }

      @Override
      public int hashCode() {
         int result = a != null ? a.hashCode() : 0;
         result = 31 * result + b;
         result = 31 * result + (c != null ? c.hashCode() : 0);
         result = 31 * result + (d != null ? d.hashCode() : 0);
         result = 31 * result + (idCacheSize != null ? idCacheSize.hashCode() : 0);
         result = 31 * result + (simpleString != null ? simpleString.hashCode() : 0);
         result = 31 * result + (gated != null ? gated.hashCode() : 0);
         result = 31 * result + (longValue != null ? longValue.hashCode() : 0);
         result = 31 * result + (doubleValue != null ? doubleValue.hashCode() : 0);
         result = 31 * result + (floatValue != null ? floatValue.hashCode() : 0);
         return result;
      }

      @Override
      public String toString() {
         return "MYClass{" + "a='" + a + '\'' + ", b=" + b + ", c=" + c + ", d='" + d + '\'' + ", idCacheSize=" + idCacheSize + ", simpleString=" + simpleString + ", gated=" + gated + ", longValue=" + longValue + ", doubleValue=" + doubleValue + ", floatValue=" + floatValue + '}';
      }

   }

}
