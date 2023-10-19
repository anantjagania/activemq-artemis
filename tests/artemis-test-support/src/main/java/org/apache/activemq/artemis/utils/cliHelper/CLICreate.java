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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.artemis.cli.Artemis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLICreate extends BaseCLI {

   public CLICreate() {
      super();
   }

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   private File configuration;

   private String[] replacePairs;

   private boolean noWeb = true;

   private String user = "guest";

   private String password = "guest";

   private String role = "guest";

   private String javaOptions = "";

   private int portOffset = 0;

   private boolean allowAnonymous = true;

   private boolean replicated = false;

   private boolean sharedStore = false;

   private boolean clustered = false;

   private boolean slave = false;

   private String staticCluster;

   String dataFolder = "./data";

   private boolean failoverOnShutdown = false;

   /**
    * it will disable auto-tune
    */
   private boolean noAutoTune = true;

   private String messageLoadBalancing = "ON_DEMAND";

   public String[] getReplacePairs() {
      return replacePairs;
   }

   public CLICreate setReplacePairs(String[] replacePairs) {
      this.replacePairs = replacePairs;
      return this;
   }

   public boolean isNoWeb() {
      return noWeb;
   }

   public CLICreate setNoWeb(boolean noWeb) {
      this.noWeb = noWeb;
      return this;
   }

   public String getUser() {
      return user;
   }

   public CLICreate setUser(String user) {
      this.user = user;
      return this;
   }

   public String getPassword() {
      return password;
   }

   public CLICreate setPassword(String password) {
      this.password = password;
      return this;
   }

   public String getRole() {
      return role;
   }

   public CLICreate setRole(String role) {
      this.role = role;
      return this;
   }

   public String getJavaOptions() {
      return javaOptions;
   }

   public CLICreate setJavaOptions(String javaOptions) {
      this.javaOptions = javaOptions;
      return this;
   }

   public int getPortOffset() {
      return portOffset;
   }

   public CLICreate setPortOffset(int portOffset) {
      this.portOffset = portOffset;
      return this;
   }

   public boolean isAllowAnonymous() {
      return allowAnonymous;
   }

   public CLICreate setAllowAnonymous(boolean allowAnonymous) {
      this.allowAnonymous = allowAnonymous;
      return this;
   }

   public boolean isReplicated() {
      return replicated;
   }

   public CLICreate setReplicated(boolean replicated) {
      this.replicated = replicated;
      return this;
   }

   public boolean isSharedStore() {
      return sharedStore;
   }

   public CLICreate setSharedStore(boolean sharedStore) {
      this.sharedStore = sharedStore;
      return this;
   }

   public boolean isClustered() {
      return clustered;
   }

   public CLICreate setClustered(boolean clustered) {
      this.clustered = clustered;
      return this;
   }

   public boolean isSlave() {
      return slave;
   }

   public CLICreate setSlave(boolean slave) {
      this.slave = slave;
      return this;
   }

   public String getStaticCluster() {
      return staticCluster;
   }

   public CLICreate setStaticCluster(String staticCluster) {
      this.staticCluster = staticCluster;
      return this;
   }

   public String getDataFolder() {
      return dataFolder;
   }

   public CLICreate setDataFolder(String dataFolder) {
      this.dataFolder = dataFolder;
      return this;
   }

   public boolean isFailoverOnShutdown() {
      return failoverOnShutdown;
   }

   public CLICreate setFailoverOnShutdown(boolean failoverOnShutdown) {
      this.failoverOnShutdown = failoverOnShutdown;
      return this;
   }

   public boolean isNoAutoTune() {
      return noAutoTune;
   }

   public CLICreate setNoAutoTune(boolean noAutoTune) {
      this.noAutoTune = noAutoTune;
      return this;
   }

   public String getMessageLoadBalancing() {
      return messageLoadBalancing;
   }

   public CLICreate setMessageLoadBalancing(String messageLoadBalancing) {
      this.messageLoadBalancing = messageLoadBalancing;
      return this;
   }

   @Override
   public CLICreate setArgs(String... args) {
      return (CLICreate) super.setArgs(args);
   }

   @Override
   public CLICreate setArtemisHome(File artemisHome) {
      return (CLICreate) super.setArtemisHome(artemisHome);
   }

   @Override
   public CLICreate setArtemisInstance(File artemisInstance) {
      return (CLICreate) super.setArtemisInstance(artemisInstance);
   }

   private void add(List<String> list, String... str) {
      for (String s : str) {
         list.add(s);
      }
   }

   public File getConfiguration() {
      return configuration;
   }

   public CLICreate setConfiguration(File configuration) {
      this.configuration = configuration;
      return this;
   }

   public CLICreate setConfiguration(String configuration) {
      setConfiguration(new File(configuration));
      return this;
   }

   public void createServer() throws Exception {

      ArrayList<String> listCommands = new ArrayList<>();

      add(listCommands, "create", "--silent", "--force", "--user", user, "--password", password, "--role", role, "--port-offset", "" + portOffset, "--data", dataFolder);

      if (allowAnonymous) {
         add(listCommands, "--allow-anonymous");
      } else {
         add(listCommands, "--require-login");
      }

      if (staticCluster != null) {
         add(listCommands, "--static-cluster", staticCluster);
      }

      if (!javaOptions.isEmpty()) {
         add(listCommands, "--java-options", javaOptions);
      }

      if (noWeb) {
         add(listCommands, "--no-web");
      }

      if (slave) {
         add(listCommands, "--slave");
      }

      if (replicated) {
         add(listCommands, "--replicated");
      }

      if (sharedStore) {
         add(listCommands, "--shared-store");
      }

      if (clustered) {
         add(listCommands, "--clustered");
         add(listCommands, "--message-load-balancing", messageLoadBalancing);
      }

      if (failoverOnShutdown) {
         add(listCommands, "--failover-on-shutdown");
      }

      if (noAutoTune) {
         add(listCommands, "--no-autotune");
      }

      add(listCommands, "--verbose");

      if ("Linux".equals(System.getProperty("os.name"))) {
         add(listCommands, "--aio");
      }

      for (String str : args) {
         add(listCommands, str);
      }

      add(listCommands, artemisInstance.getAbsolutePath());

      logger.debug("server created at {} with home = {}", artemisInstance, artemisHome);
      artemisInstance.mkdirs();

      Artemis.execute(false, true, artemisHome, null, null, (String[]) listCommands.toArray(new String[listCommands.size()]));

      if (configuration != null) {
         String[] list = configuration.list();

         if (list != null) {
            copyConfigurationFiles(list, configuration.toPath(), artemisInstance.toPath().resolve("etc"));
         }
      }
   }

   private void copyWithReplacements(Path original, Path target) throws IOException {
      Charset charset = StandardCharsets.UTF_8;

      String content = new String(Files.readAllBytes(original), charset);
      for (int i = 0; i + 1 < replacePairs.length; i += 2) {
         content = content.replaceAll(replacePairs[i], replacePairs[i + 1]);
      }
      Files.write(target, content.getBytes(charset));
   }

   private void copyConfigurationFiles(String[] list,
                                       Path sourcePath,
                                       Path targetPath) throws IOException {
      boolean hasReplacements = false;
      if (replacePairs != null && replacePairs.length > 0) {
         hasReplacements = true;
         if (replacePairs.length % 2 == 1) {
            throw new IllegalArgumentException("You need to pass an even number of replacement pairs");
         }
      }
      for (String file : list) {
         Path target = targetPath.resolve(file);

         Path originalFile = sourcePath.resolve(file);

         if (hasReplacements) {
            copyWithReplacements(originalFile, target);
         } else {
            Files.copy(originalFile, target, StandardCopyOption.REPLACE_EXISTING);
         }

         if (originalFile.toFile().isDirectory()) {
            copyConfigurationFiles(originalFile.toFile().list(), originalFile, target);
         }
      }
   }

}
