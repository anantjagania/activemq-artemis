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
package org.apache.activemq.artemis.tests.db.journal;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.io.IOCriticalErrorListener;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.PreparedTransactionInfo;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.jdbc.store.drivers.JDBCUtils;
import org.apache.activemq.artemis.jdbc.store.journal.JDBCJournalImpl;
import org.apache.activemq.artemis.jdbc.store.sql.SQLProvider;
import org.apache.activemq.artemis.tests.db.common.DBTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.activemq.artemis.utils.TestParameters.testProperty;

@RunWith(Parameterized.class)
public class JDBCJournalTest extends DBTestBase {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   private static final String TEST_NAME = "PGDB";

   // you can use ./start-${database}-podman.sh scripts from ./src/test/scripts to start the databases.
   // support values are derby, mysql and postgres
   private static final String DB_LIST = testProperty(TEST_NAME, "DB_LIST", "mssql,oracle");

   private JDBCJournalImpl journal;

   private ScheduledExecutorService scheduledExecutorService;

   private ExecutorService executorService;

   private SQLProvider sqlProvider;

   private DatabaseStorageConfiguration dbConf;

   @Parameterized.Parameter
   public String database;

   @Parameterized.Parameters(name = "database={0}")
   public static Collection<Object[]> parameters() {
      String[] databases = DB_LIST.split(",");

      ArrayList<Object[]> parameters = new ArrayList<>();
      for (String str : databases) {
         logger.info("Adding {} to the list for the test", str);
         try {
            dropDatabase(str);
         } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            continue;
         }
         parameters.add(new Object[]{str});
      }

      return parameters;
   }

   @After
   @Override
   public void tearDown() throws Exception {
      super.tearDown();
      journal.destroy();
      scheduledExecutorService.shutdownNow();
      scheduledExecutorService = null;
      executorService.shutdown();
      executorService = null;
   }

   @Override
   protected String getJDBCUser() {
      return null;
   }

   @Override
   protected String getJDBCPassword() {
      return null;
   }


   @Override
   protected final String getTestJDBCConnectionUrl() {

      try {
         registerDriver(database);
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
         Assert.fail("Failed to register driver");
      }
      return System.getProperty(database + ".uri");
   }


   @Before
   public void setup() throws Exception {
      disableCheckThread(); // it's ok as tests are isolated on this module
      registerDriver(database);
      dbConf = createDefaultDatabaseStorageConfiguration();
      String driverName = System.getProperty(database + ".class");
      dbConf.setJdbcDriverClassName(driverName);
      sqlProvider = JDBCUtils.getSQLProvider(
         dbConf.getJdbcDriverClassName(),
         dbConf.getMessageTableName(),
         SQLProvider.DatabaseStoreType.MESSAGE_JOURNAL);
      scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
      executorService = Executors.newSingleThreadExecutor();
      journal = new JDBCJournalImpl(dbConf.getConnectionProvider(), sqlProvider, scheduledExecutorService, executorService, new IOCriticalErrorListener() {
         @Override
         public void onIOException(Throwable code, String message, String file) {

         }
      }, 5);
      journal.start();
   }

   @Test
   public void testRestartEmptyJournal() throws SQLException {
      Assert.assertTrue(journal.isStarted());
      Assert.assertEquals(0, journal.getNumberOfRecords());
      journal.stop();
      journal.start();
      Assert.assertTrue(journal.isStarted());
   }

   @Test
   public void testConcurrentEmptyJournal() throws SQLException {
      Assert.assertTrue(journal.isStarted());
      Assert.assertEquals(0, journal.getNumberOfRecords());
      final JDBCJournalImpl secondJournal = new JDBCJournalImpl(dbConf.getConnectionProvider(),
                                                                          sqlProvider, scheduledExecutorService,
                                                                          executorService, (code, message, file) -> {
         Assert.fail(message);
      }, 5);
      secondJournal.start();
      try {
         Assert.assertTrue(secondJournal.isStarted());
      } finally {
         secondJournal.stop();
      }
   }

   @Test
   public void testInsertRecords() throws Exception {
      int noRecords = 10;
      for (int i = 0; i < noRecords; i++) {
         journal.appendAddRecord(i, (byte) 1, new byte[0], true);
      }

      assertEquals(noRecords, journal.getNumberOfRecords());
   }

   @Test
   public void testCleanupTxRecords() throws Exception {
      journal.appendDeleteRecordTransactional(1, 1);
      journal.appendCommitRecord(1, true);
      assertEquals(0, journal.getNumberOfRecords());
   }

   @Test
   public void testCleanupTxRecords4TransactionalRecords() throws Exception {
      // add committed transactional record e.g. paging
      journal.appendAddRecordTransactional(152, 154, (byte) 13, new byte[0]);
      journal.appendCommitRecord(152, true);
      assertEquals(2, journal.getNumberOfRecords());

      // delete transactional record in new transaction e.g. depaging
      journal.appendDeleteRecordTransactional(236, 154);
      journal.appendCommitRecord(236, true);
      assertEquals(0, journal.getNumberOfRecords());
   }

   @Test
   public void testCallbacks() throws Exception {
      final int noRecords = 10;
      final CountDownLatch done = new CountDownLatch(noRecords);

      IOCompletion completion = new IOCompletion() {
         @Override
         public void storeLineUp() {
         }

         @Override
         public void done() {
            done.countDown();
         }

         @Override
         public void onError(int errorCode, String errorMessage) {

         }
      };

      for (int i = 0; i < noRecords; i++) {
         journal.appendAddRecord(1, (byte) 1, new FakeEncodingSupportImpl(new byte[0]), true, completion);
      }
      journal.sync();

      done.await(5, TimeUnit.SECONDS);
      assertEquals(done.getCount(), 0);
   }

   @Test
   public void testReadJournal() throws Exception {
      int noRecords = 100;

      // Standard Add Records
      for (int i = 0; i < noRecords; i++) {
         journal.appendAddRecord(i, (byte) i, new byte[i], true);
      }

      // TX Records
      int noTx = 10;
      int noTxRecords = 100;
      for (int i = 1000; i < 1000 + noTx; i++) {
         for (int j = 0; j < noTxRecords; j++) {
            journal.appendAddRecordTransactional(i, Long.valueOf(i + "" + j), (byte) 1, new byte[0]);
         }
         journal.appendPrepareRecord(i, new byte[0], true);
         journal.appendCommitRecord(i, true);
      }

      Thread.sleep(2000);
      List<RecordInfo> recordInfos = new ArrayList<>();
      List<PreparedTransactionInfo> txInfos = new ArrayList<>();

      journal.load(recordInfos, txInfos, null);

      assertEquals(noRecords + (noTxRecords * noTx), recordInfos.size());
   }

}
