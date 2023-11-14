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

   CopyOnWriteArrayList<MetaData<T>> metaData = new CopyOnWriteArrayList<>();

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
          type != Double.class &&
          type != Float.class) {
         throw new IllegalArgumentException("invalid type " + type);
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
      this.forEach((type, name, setter, getter, gate) -> {
         logger.debug("Parsing {} {} {} {} {}", type, name, setter, getter, gate);
         if (gate == null || gate.test(object)) {
            Object value = getter.apply(object);

            if (logger.isTraceEnabled()) {

               if (gate != null) {
                  logger.trace("Gate passed for {}", name);
               }

               if (value == null) {
                  logger.debug("Result for {} = IS NULL", name);
               } else {
                  logger.debug("Result for {} = {}, type={}", name, value, value.getClass());
               }
            }

            if (value == null) {
               logger.trace("Setting {} as null", name);
               builder.addNull(name);
            } else if (type == String.class || type == SimpleString.class) {
               logger.trace("Setting {} as String {}", name, value);
               builder.add(name, String.valueOf(value));
            } else if (Number.class.isAssignableFrom(type) && value instanceof Number) {
               if (value instanceof Double || value instanceof Float) {
                  logger.trace("Setting {} as double {}", name, value);
                  builder.add(name, ((Number) value).doubleValue());
               } else {
                  logger.trace("Setting {} as long {}", name, value);
                  builder.add(name, ((Number) value).longValue());
               }
            } else {
               builder.add(name, String.valueOf(value));
            }
         } else {
            logger.debug("Gate ignored on {}", name);
         }
      });
   }

   public void forEach(MetadataListener listener) {
      metaData.forEach(m -> {
         listener.metaItem(m.type, m.name, m.setter, m.getter, m.getGate);
      });
   }

   public void fromJSON(T resultObject, String jsonString) {

      logger.debug("Parsing JSON {}", jsonString);

      JsonObject json = JsonLoader.readObject(new StringReader(jsonString));

      this.forEach((type, name, setter, getter, gate) -> {
         if (json.containsKey(name)) {
            if (json.isNull(name)) {
               setter.accept(resultObject, null);
            } else if (type == String.class) {
               setter.accept(resultObject, json.getString(name));
            } else if (type == SimpleString.class) {
               setter.accept(resultObject, SimpleString.toSimpleString(json.getString(name)));
            } else if (type == Integer.class) {
               setter.accept(resultObject, json.getInt(name));
            } else if (type == Long.class) {
               setter.accept(resultObject, json.getJsonNumber(name).longValue());
            } else if (type == Double.class) {
               setter.accept(resultObject, json.getJsonNumber(name).doubleValue());
            } else if (type == Float.class) {
               setter.accept(resultObject, json.getJsonNumber(name).numberValue().floatValue());
            }
         }
      });
   }



   static class MetaData<T> {
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

   public interface MetadataListener<T> {
      void metaItem(Class type, String name, BiConsumer<T, Object> setter, Function<T, Object> getter, Predicate<T> getGate);
   }


}
