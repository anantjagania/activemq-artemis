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

package org.apache.activemq.artemis.utils.beans;

import java.beans.PropertyDescriptor;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.activemq.artemis.json.JsonObject;
import org.apache.activemq.artemis.json.JsonObjectBuilder;
import org.apache.activemq.artemis.json.JsonString;
import org.apache.activemq.artemis.json.JsonValue;
import org.apache.activemq.artemis.utils.JsonLoader;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONConverter {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public static PropertyDescriptor[] getClassDescription(Class clazz) {
      return PropertyUtils.getPropertyDescriptors(clazz);
   }

   public static String toJSON(Object object) throws Exception {
      JsonObjectBuilder builder = JsonLoader.createObjectBuilder();
      BeanSupport.exploreProperties(object, (a, b) -> {
         logger.trace("Setting {}={}", a, b);
         if (b == null) {
            builder.addNull(a);
         } else if (b instanceof String) {
            builder.add(a, (String) b);
         } else if (b instanceof Number) {
            Number number = (Number)b;
            if (b instanceof Double || b instanceof Float) {
               builder.add(a, number.doubleValue());
            } else {
               builder.add(a, number.longValue());
            }
         } else {
            builder.add(a, String.valueOf(b));
         }
      });

      return builder.build().toString();
   }

   public static void fromJSON(Object resultObject, String jsonString) throws Exception {

      JsonObject json = JsonLoader.readObject(new StringReader(jsonString));

      for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
         logger.info("{}={}, type={}", entry.getKey(), entry.getValue(), entry.getValue().getValueType());
         if (entry.getValue().getValueType() == JsonValue.ValueType.NULL) {
            BeanUtils.setProperty(resultObject, entry.getKey(), null);
         } else {
            String value = entry.getValue().getValueType() == JsonValue.ValueType.STRING ? ((JsonString) entry.getValue()).getString() : entry.getValue().toString();
            BeanUtils.setProperty(resultObject, entry.getKey(), value);
         }
      }
   }

}
