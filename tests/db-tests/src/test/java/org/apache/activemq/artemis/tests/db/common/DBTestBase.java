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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.activemq.artemis.utils.RealServerTestBase;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBTestBase extends RealServerTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   // There is one artemis server for each database we provide on the tests
   public static ClassLoader defineClassLoader(String database) throws Exception {
      String serverLocation = getServerLocation(database);
      File lib = new File(serverLocation + "/lib");
      return defineClassLoader(lib);
   }

   public static ClassLoader defineClassLoader(File location) throws Exception {
      File[] jars = location.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

      URL[] url = new URL[jars.length];

      for (int i = 0; i < jars.length; i++) {
         url[i] = jars[i].toURI().toURL();
      }

      return new URLClassLoader(url, Thread.currentThread().getContextClassLoader());
   }

   public void registerDriver(String database) throws Exception {
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      runAfter(() -> Thread.currentThread().setContextClassLoader(originalClassLoader));
      String clazzName = System.getProperty(database + ".class");
      Assert.assertNotNull(clazzName);
      ClassLoader loader = defineClassLoader(database);
      Thread.currentThread().setContextClassLoader(loader);
      Class clazz = loader.loadClass(clazzName);
      DriverManager.registerDriver((Driver)clazz.getDeclaredConstructor().newInstance());
   }

   public static Connection getConnection(String database) throws SQLException {
      try {
         String uri = System.getProperty(database + ".uri");
         String clazzName = System.getProperty(database + ".class");
         Assert.assertNotNull(uri);
         Assert.assertNotNull(clazzName);
         uri = uri.replace("&#38;", "&");
         logger.info("uri {}", uri);

         String serverLocation = getServerLocation(database);
         File lib = new File(serverLocation + "/lib");
         ClassLoader loader = defineClassLoader(lib);
         Class clazz = loader.loadClass(clazzName);
         Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();
         return driver.connect(uri, null);
      } catch (Exception e) {
         throw new SQLException(e.getMessage(), e);
      }
   }

   public static void dropDatabase(String database) throws Exception {
      //new Exception("Dropping").printStackTrace();
      try (Connection connection = getConnection(database)) {
         ResultSet data = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
         while (data.next()) {
            try {
               connection.prepareStatement("DROP TABLE " + data.getString("TABLE_NAME")).execute();
               logger.info("Dropped {}", data.getString("TABLE_NAME"));
            } catch (Exception e) {
               logger.debug("Error dropping table {}", data.getString("TABLE_NAME"), e);
            }
         }
      }
   }

}
