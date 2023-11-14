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

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.json.JsonObject;
import org.apache.activemq.artemis.json.JsonObjectBuilder;
import org.apache.activemq.artemis.json.JsonString;
import org.apache.activemq.artemis.json.JsonValue;
import org.apache.activemq.artemis.utils.JsonLoader;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Receives a metadata about a class with methods to read, write and certain gates.
 *  And provides a generic logic to convert to and from JSON.
 *
 *  As a historical context the first try to make a few objects more dynamic (e.g AddressSettings) was
 *  around BeanUtils however there was some implicit logic on when certain settins were Null or default values.
 *  for that reason I decided for a meta-data approach where extra semantic could be applied for each individual attributes
 *  rather than a generic BeanUtils parser.*/
public class DynamicJSON <T> {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   class MetaData {
      Class type;
      String name;
      BiConsumer<T, Object> setter;
      Function<T, Object> getter;
      Predicate<T>  getGate;


      public MetaData(Class type, String name, BiConsumer<T, Object> setter, Function<T, Object> getter, Predicate<T> getGate) {
         this.type = type;
         this.name = name;
         this.setter = setter;
         this.getter = getter;
         this.getGate = getGate;
      }

      @Override
      public String toString() {
         return "MetaData{" + "type=" + type + ", name='" + name + '\'' + '}';
      }
   }

   CopyOnWriteArrayList<MetaData> metaData = new CopyOnWriteArrayList<>();

   /**
    * Accepted types:
    * String.class
    * SimpleString.class
    * Integer.class
    * Long.class
    * Double.class
    * Float.class
    */
   public DynamicJSON addMetadata(Class type, String name, BiConsumer<T, Object> setter, Function<T, Object> getter, Predicate<T> getGate) {
      if (type != String.class &&
          type != SimpleString.class &&
          type != Integer.class &&
          type != Long.class &&
          ) {

      }


      metaData.add(new MetaData(type, name, setter, getter, getGate));
      return this;
   }

   public DynamicJSON addMetadata(Class type, String name, BiConsumer<T, Object> setter, Function<T, Object> getter) {
      metaData.add(new MetaData(type, name, setter, getter, null));
      return this;
   }

   public JsonObject toJSON(T object) {
      JsonObjectBuilder builder = JsonLoader.createObjectBuilder();
      parseToJSON(object, builder);
      return builder.build();
   }

   public void parseToJSON(T object, JsonObjectBuilder builder) {
      logger.debug("Parsing object {}", object);
      metaData.forEach(m -> {
         logger.debug("Parsing {}", m);
         if (m.getGate == null || m.getGate.test(object)) {
            Object value = m.getter.apply(object);

            if (logger.isTraceEnabled()) {

               if (m.getGate != null) {
                  logger.trace("Gate passed for {}", m.name);
               }

               if (value == null) {
                  logger.debug("Result for {} = IS NULL", m.name);
               } else {
                  logger.debug("Result for {} = {}, type={}", m.name, value, value.getClass());
               }
            }

            if (value == null) {
               logger.trace("Setting {} as null", m.name);
               builder.addNull(m.name);
            } else if (m.type == String.class || m.type == SimpleString.class) {
               logger.trace("Setting {} as String {}", m.name, value);
               builder.add(m.name, String.valueOf(value));
            } else if (Number.class.isAssignableFrom(m.type) && value instanceof Number) {
               if (value instanceof Double || value instanceof Float) {
                  logger.trace("Setting {} as double {}", m.name, value);
                  builder.add(m.name, ((Number) value).doubleValue());
               } else {
                  logger.trace("Setting {} as long {}", m.name, value);
                  builder.add(m.name, ((Number) value).longValue());
               }
            } else {
               builder.add(m.name, String.valueOf(value));
            }
         } else {
            logger.debug("Gate ignored on {}", m);
         }
      });
   }


   public void fromJSON(T resulttObject, String jsonString) throws Exception {

      logger.debug("Parsing JSON {}", jsonString);

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
