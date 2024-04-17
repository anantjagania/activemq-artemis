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
package org.apache.activemq.artemis.tests.soak.replicationAndMirror;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.tests.soak.SoakTestBase;
import org.apache.activemq.artemis.util.ServerUtil;
import org.apache.activemq.artemis.utils.ReusableLatch;
import org.apache.activemq.artemis.utils.cli.helper.HelperCreate;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReplicationFlowControlTest extends SoakTestBase {


   public static final String SERVER_NAME_A_1 = "replicated-mirror-A1";
   public static final String SERVER_NAME_A_2 = "replicated-mirror-A2";
   public static final String SERVER_NAME_B_1 = "replicated-mirror-B1";
   public static final String SERVER_NAME_B_2 = "replicated-mirror-B2";


   private static void createServer(String serverName) throws Exception{
      File serverLocation = getFileServerLocation(serverName);
      deleteDirectory(serverLocation);

      HelperCreate cliCreateServer = new HelperCreate();
      cliCreateServer.setAllowAnonymous(true).setNoWeb(true).setArtemisInstance(serverLocation);
      cliCreateServer.setConfiguration("./src/main/resources/servers/" + serverName);
      cliCreateServer.createServer();
   }

   @BeforeClass
   public static void createServers() throws Exception {
      createServer(SERVER_NAME_A_1);
      createServer(SERVER_NAME_A_2);
      createServer(SERVER_NAME_B_1);
      createServer(SERVER_NAME_B_2);
   }

   private static Process serverA1;
   private static Process serverA2;
   private static Process serverB1;
   private static Process serverB2;


   @Before
   public void before() throws Exception {
      cleanupData(SERVER_NAME_A_1);
      cleanupData(SERVER_NAME_A_2);
      cleanupData(SERVER_NAME_B_1);
      cleanupData(SERVER_NAME_B_2);
      disableCheckThread();
   }

   @After
   @Override
   public void after() throws Exception {
      super.after();
      cleanupData(SERVER_NAME_A_1);
      cleanupData(SERVER_NAME_A_2);
      cleanupData(SERVER_NAME_B_1);
      cleanupData(SERVER_NAME_B_2);
   }
}
