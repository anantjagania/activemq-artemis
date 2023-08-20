/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.db.common;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;

import org.apache.activemq.artemis.utils.RealServerTestBase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBTestBase extends RealServerTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public ClassLoader defineClassLoader(File location) throws Exception {
      String classPathValue = null;
      File[] jars = location.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

      URL[] url = new URL[jars.length];

      for (int i = 0; i < jars.length; i++) {
         url[i] = jars[i].toURI().toURL();
      }

      return new URLClassLoader(url, Thread.currentThread().getContextClassLoader());
   }

   public void dropDatabase(String serverName) throws Exception {
      String uri = System.getProperty(serverName + ".uri");
      String clazzName = System.getProperty(serverName + ".class");
      Assert.assertNotNull(uri);
      Assert.assertNotNull(clazzName);

      uri = uri.replace("&#38;", "&");
      logger.info("uri {}", uri);

      String serverLocation = getServerLocation(serverName);
      File lib = new File(serverLocation + "/lib");
      ClassLoader loader = defineClassLoader(lib);
      Class clazz = loader.loadClass(clazzName);
      Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
      try (Connection connection = driver.connect(uri, null)) {
         ResultSet data = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
         while (data.next()) {
            try {
               connection.prepareStatement("DROP TABLE " + data.getString("TABLE_NAME")).execute();
               logger.info("Dropped {}", data.getString("TABLE_NAME"));
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

}
