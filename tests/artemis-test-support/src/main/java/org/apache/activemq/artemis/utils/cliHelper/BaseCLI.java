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

package org.apache.activemq.artemis.utils.cliHelper;

import java.io.File;

public class BaseCLI {

   BaseCLI() {
      String propertyHome = System.getProperty("artemis.bin.output");
      if (propertyHome != null) {
         artemisHome = new File(propertyHome);
      }
      System.out.println("home::" + artemisHome);
   }


   File artemisHome;
   File artemisInstance;

   public File getArtemisHome() {
      return artemisHome;
   }

   public BaseCLI setArtemisHome(File artemisHome) {
      this.artemisHome = artemisHome;
      return this;
   }

   public File getArtemisInstance() {
      return artemisInstance;
   }

   public BaseCLI setArtemisInstance(File artemisInstance) {
      this.artemisInstance = artemisInstance;
      return this;
   }

   public String[] getArgs() {
      return args;
   }

   public BaseCLI setArgs(String... args) {

      this.args = args;
      return this;
   }

   String[] args = new String[0];
}
