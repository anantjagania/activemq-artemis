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

package org.apache.activemq.artemis.core.settings.impl;

import java.lang.invoke.MethodHandles;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.json.dynamic.DynamicJSON;
import org.apache.activemq.artemis.utils.RandomUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressSettingsRandomTest {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   @Test
   public void testRandomGeneration() {

      DynamicJSON json = AddressSettings.jsonParser;

      AddressSettings randomSettings = new AddressSettings();

      json.forEach((type, name, setter, getter, gate) -> {
         if (Enum.class.isAssignableFrom(type)) {
            Object [] enumValues = type.getEnumConstants();
            int randomInt = RandomUtil.randomInterval(0, enumValues.length - 1);
            setter.accept(randomSettings, enumValues[randomInt]);
         } else if (type == String.class) {
            setter.accept(randomSettings, RandomUtil.randomString());
         } else if (type == SimpleString.class) {
            setter.accept(randomSettings, RandomUtil.randomSimpleString());
         } else if (type == Integer.class) {
            setter.accept(randomSettings, RandomUtil.randomPositiveInt());
         } else if (type == Long.class) {
            setter.accept(randomSettings, RandomUtil.randomPositiveLong());
         } else if (type == Double.class) {
            setter.accept(randomSettings, RandomUtil.randomDouble());
         } else if (type == Float.class) {
            setter.accept(randomSettings, RandomUtil.randomFloat());
         } else if (type == Boolean.class) {
            setter.accept(randomSettings, RandomUtil.randomBoolean());
         }
      });

      String jsonOutput = randomSettings.toJSON();
      logger.info(jsonOutput);

      AddressSettings outputSettings = AddressSettings.fromJSON(jsonOutput);
      Assert.assertEquals(randomSettings, outputSettings);


   }

}
