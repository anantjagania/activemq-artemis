/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.apache.activemq.core.deployers.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.api.core.BroadcastEndpointFactoryConfiguration;
import org.apache.activemq.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.api.core.JGroupsBroadcastGroupConfiguration;
import org.apache.activemq.api.core.Pair;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.api.core.UDPBroadcastGroupConfiguration;
import org.apache.activemq.api.core.client.HornetQClient;
import org.apache.activemq.core.config.BridgeConfiguration;
import org.apache.activemq.core.config.ClusterConnectionConfiguration;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.core.config.CoreQueueConfiguration;
import org.apache.activemq.core.config.DivertConfiguration;
import org.apache.activemq.core.config.HAPolicyConfiguration;
import org.apache.activemq.core.config.ScaleDownConfiguration;
import org.apache.activemq.core.config.ha.ColocatedPolicyConfiguration;
import org.apache.activemq.core.config.ha.LiveOnlyPolicyConfiguration;
import org.apache.activemq.core.config.ha.ReplicaPolicyConfiguration;
import org.apache.activemq.core.config.ha.ReplicatedPolicyConfiguration;
import org.apache.activemq.core.config.ha.SharedStoreMasterPolicyConfiguration;
import org.apache.activemq.core.config.ha.SharedStoreSlavePolicyConfiguration;
import org.apache.activemq.core.config.impl.ConfigurationImpl;
import org.apache.activemq.core.config.impl.FileConfiguration;
import org.apache.activemq.core.config.impl.Validators;
import org.apache.activemq.core.journal.impl.AIOSequentialFileFactory;
import org.apache.activemq.core.journal.impl.JournalConstants;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.server.HornetQServerLogger;
import org.apache.activemq.core.server.JournalType;
import org.apache.activemq.core.server.group.impl.GroupingHandlerConfiguration;
import org.apache.activemq.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.core.settings.impl.AddressSettings;
import org.apache.activemq.core.settings.impl.SlowConsumerPolicy;
import org.apache.activemq.utils.DefaultSensitiveStringCodec;
import org.apache.activemq.utils.PasswordMaskingUtil;
import org.apache.activemq.utils.SensitiveDataCodec;
import org.apache.activemq.utils.XMLConfigurationUtil;
import org.apache.activemq.utils.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses an XML document according to the {@literal activemq-configuration.xsd} schema.
 *
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 */
public final class FileConfigurationParser extends XMLConfigurationUtil
{

   // Constants -----------------------------------------------------

   private static final String CONFIGURATION_SCHEMA_URL = "schema/activemq-configuration.xsd";

   // Security Parsing
   public static final String SECURITY_ELEMENT_NAME = "security-setting";

   private static final String PERMISSION_ELEMENT_NAME = "permission";

   private static final String TYPE_ATTR_NAME = "type";

   private static final String ROLES_ATTR_NAME = "roles";

   static final String CREATEDURABLEQUEUE_NAME = "createDurableQueue";

   private static final String DELETEDURABLEQUEUE_NAME = "deleteDurableQueue";

   private static final String CREATE_NON_DURABLE_QUEUE_NAME = "createNonDurableQueue";

   private static final String DELETE_NON_DURABLE_QUEUE_NAME = "deleteNonDurableQueue";

   // HORNETQ-309 we keep supporting these attribute names for compatibility
   private static final String CREATETEMPQUEUE_NAME = "createTempQueue";

   private static final String DELETETEMPQUEUE_NAME = "deleteTempQueue";

   private static final String SEND_NAME = "send";

   private static final String CONSUME_NAME = "consume";

   private static final String MANAGE_NAME = "manage";

   // Address parsing

   private static final String DEAD_LETTER_ADDRESS_NODE_NAME = "dead-letter-address";

   private static final String EXPIRY_ADDRESS_NODE_NAME = "expiry-address";

   private static final String EXPIRY_DELAY_NODE_NAME = "expiry-delay";

   private static final String REDELIVERY_DELAY_NODE_NAME = "redelivery-delay";

   private static final String REDELIVERY_DELAY_MULTIPLIER_NODE_NAME = "redelivery-delay-multiplier";

   private static final String MAX_REDELIVERY_DELAY_NODE_NAME = "max-redelivery-delay";

   private static final String MAX_DELIVERY_ATTEMPTS = "max-delivery-attempts";

   private static final String MAX_SIZE_BYTES_NODE_NAME = "max-size-bytes";

   private static final String ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME = "address-full-policy";

   private static final String PAGE_SIZE_BYTES_NODE_NAME = "page-size-bytes";

   private static final String PAGE_MAX_CACHE_SIZE_NODE_NAME = "page-max-cache-size";

   private static final String MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME = "message-counter-history-day-limit";

   private static final String LVQ_NODE_NAME = "last-value-queue";

   private static final String REDISTRIBUTION_DELAY_NODE_NAME = "redistribution-delay";

   private static final String SEND_TO_DLA_ON_NO_ROUTE = "send-to-dla-on-no-route";

   private static final String SLOW_CONSUMER_THRESHOLD_NODE_NAME = "slow-consumer-threshold";

   private static final String SLOW_CONSUMER_CHECK_PERIOD_NODE_NAME = "slow-consumer-check-period";

   private static final String SLOW_CONSUMER_POLICY_NODE_NAME = "slow-consumer-policy";

   // Attributes ----------------------------------------------------

   private boolean validateAIO = false;

   /**
    * @return the validateAIO
    */
   public boolean isValidateAIO()
   {
      return validateAIO;
   }

   /**
    * @param validateAIO the validateAIO to set
    */
   public void setValidateAIO(final boolean validateAIO)
   {
      this.validateAIO = validateAIO;
   }

   public Configuration parseMainConfig(final InputStream input) throws Exception
   {

      Reader reader = new InputStreamReader(input);
      String xml = org.apache.activemq.utils.XMLUtil.readerToString(reader);
      xml = XMLUtil.replaceSystemProps(xml);
      Element e = org.apache.activemq.utils.XMLUtil.stringToElement(xml);

      Configuration config = new ConfigurationImpl();

      parseMainConfig(e, config);

      return config;
   }

   public void parseMainConfig(final Element e, final Configuration config) throws Exception
   {
      XMLUtil.validate(e, FileConfigurationParser.CONFIGURATION_SCHEMA_URL);

      config.setName(getString(e, "name", config.getName(), Validators.NO_CHECK));

      NodeList haPolicyNodes = e.getElementsByTagName("ha-policy");

      boolean containsHAPolicy = false;

      if (haPolicyNodes.getLength() > 0)
      {
         parseHAPolicyConfiguration((Element) haPolicyNodes.item(0), config);
         containsHAPolicy = true;
         // remove <ha-policy> from the DOM so later when we look for deprecated elements using parameterExists() we don't get false positives
         e.removeChild(haPolicyNodes.item(0));
      }

      NodeList elems = e.getElementsByTagName("clustered");
      if (elems != null && elems.getLength() > 0)
      {
         HornetQServerLogger.LOGGER.deprecatedConfigurationOption("clustered");
      }

      // these are combined because they are both required for setting the correct HAPolicyConfiguration
      if (parameterExists(e, "backup") || parameterExists(e, "shared-store"))
      {
         boolean backup = getBoolean(e, "backup", false);
         boolean sharedStore = getBoolean(e, "shared-store", true);

         if (containsHAPolicy)
         {
            if (parameterExists(e, "backup"))
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("backup");
            }

            if (parameterExists(e, "shared-store"))
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("shared-store");
            }
         }
         else
         {
            if (parameterExists(e, "backup"))
            {
               HornetQServerLogger.LOGGER.deprecatedConfigurationOption("backup");
            }

            if (parameterExists(e, "shared-store"))
            {
               HornetQServerLogger.LOGGER.deprecatedConfigurationOption("shared-store");
            }

            if (backup && sharedStore)
            {
               config.setHAPolicyConfiguration(new SharedStoreSlavePolicyConfiguration());
            }
            else if (backup && !sharedStore)
            {
               config.setHAPolicyConfiguration(new ReplicaPolicyConfiguration());
            }
            else if (!backup && sharedStore)
            {
               config.setHAPolicyConfiguration(new SharedStoreMasterPolicyConfiguration());
            }
            else if (!backup && !sharedStore)
            {
               config.setHAPolicyConfiguration(new ReplicatedPolicyConfiguration());
            }
         }
      }

      HAPolicyConfiguration haPolicyConfig = config.getHAPolicyConfiguration();

      if (parameterExists(e, "check-for-live-server"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("check-for-live-server");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("check-for-live-server");

            if (haPolicyConfig instanceof ReplicatedPolicyConfiguration)
            {
               ReplicatedPolicyConfiguration hapc = (ReplicatedPolicyConfiguration) haPolicyConfig;
               hapc.setCheckForLiveServer(getBoolean(e, "check-for-live-server", hapc.isCheckForLiveServer()));
            }
         }
      }

      if (parameterExists(e, "allow-failback"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("allow-failback");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("allow-failback");

            if (haPolicyConfig instanceof ReplicaPolicyConfiguration)
            {
               ReplicaPolicyConfiguration hapc = (ReplicaPolicyConfiguration) haPolicyConfig;
               hapc.setAllowFailBack(getBoolean(e, "allow-failback", hapc.isAllowFailBack()));
            }
            else if (haPolicyConfig instanceof SharedStoreSlavePolicyConfiguration)
            {
               SharedStoreSlavePolicyConfiguration hapc = (SharedStoreSlavePolicyConfiguration) haPolicyConfig;
               hapc.setAllowFailBack(getBoolean(e, "allow-failback", hapc.isAllowFailBack()));
            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("check-for-live-server");
            }
         }
      }

      if (parameterExists(e, "backup-group-name"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("backup-group-name");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("backup-group-name");

            if (haPolicyConfig instanceof ReplicaPolicyConfiguration)
            {
               ReplicaPolicyConfiguration hapc = (ReplicaPolicyConfiguration) haPolicyConfig;
               hapc.setGroupName(getString(e, "backup-group-name", hapc.getGroupName(), Validators.NO_CHECK));
            }
            else if (haPolicyConfig instanceof ReplicatedPolicyConfiguration)
            {
               ReplicatedPolicyConfiguration hapc = (ReplicatedPolicyConfiguration) haPolicyConfig;
               hapc.setGroupName(getString(e, "backup-group-name", hapc.getGroupName(), Validators.NO_CHECK));
            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("backup-group-name");
            }
         }
      }

      if (parameterExists(e, "failback-delay"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("failback-delay");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("failback-delay");

            if (haPolicyConfig instanceof ReplicaPolicyConfiguration)
            {
               ReplicaPolicyConfiguration hapc = (ReplicaPolicyConfiguration) haPolicyConfig;
               hapc.setFailbackDelay(getLong(e, "failback-delay", hapc.getFailbackDelay(), Validators.GT_ZERO));
            }
            else if (haPolicyConfig instanceof SharedStoreMasterPolicyConfiguration)
            {
               SharedStoreMasterPolicyConfiguration hapc = (SharedStoreMasterPolicyConfiguration) haPolicyConfig;
               hapc.setFailbackDelay(getLong(e, "failback-delay", hapc.getFailbackDelay(), Validators.GT_ZERO));
            }
            else if (haPolicyConfig instanceof SharedStoreSlavePolicyConfiguration)
            {
               SharedStoreSlavePolicyConfiguration hapc = (SharedStoreSlavePolicyConfiguration) haPolicyConfig;
               hapc.setFailbackDelay(getLong(e, "failback-delay", hapc.getFailbackDelay(), Validators.GT_ZERO));
            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("failback-delay");
            }
         }
      }

      if (parameterExists(e, "failover-on-shutdown"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("failover-on-shutdown");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("failover-on-shutdown");

            if (haPolicyConfig instanceof SharedStoreMasterPolicyConfiguration)
            {
               SharedStoreMasterPolicyConfiguration hapc = (SharedStoreMasterPolicyConfiguration) haPolicyConfig;
               hapc.setFailoverOnServerShutdown(getBoolean(e, "failover-on-shutdown", hapc.isFailoverOnServerShutdown()));
            }
            else if (haPolicyConfig instanceof SharedStoreSlavePolicyConfiguration)
            {
               SharedStoreSlavePolicyConfiguration hapc = (SharedStoreSlavePolicyConfiguration) haPolicyConfig;
               hapc.setFailoverOnServerShutdown(getBoolean(e, "failover-on-shutdown", hapc.isFailoverOnServerShutdown()));
            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("failover-on-shutdown");
            }
         }
      }

      if (parameterExists(e, "replication-clustername"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("replication-clustername");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("replication-clustername");

            if (haPolicyConfig instanceof ReplicaPolicyConfiguration)
            {
               ReplicaPolicyConfiguration hapc = (ReplicaPolicyConfiguration) haPolicyConfig;
               hapc.setClusterName(getString(e, "replication-clustername", null, Validators.NO_CHECK));
            }
            else if (haPolicyConfig instanceof ReplicatedPolicyConfiguration)
            {
               ReplicatedPolicyConfiguration hapc = (ReplicatedPolicyConfiguration) haPolicyConfig;
               hapc.setClusterName(getString(e, "replication-clustername", null, Validators.NO_CHECK));
            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("replication-clustername");
            }
         }
      }

      if (parameterExists(e, "max-saved-replicated-journals-size"))
      {
         if (containsHAPolicy)
         {
            HornetQServerLogger.LOGGER.incompatibleWithHAPolicy("max-saved-replicated-journals-size");
         }
         else
         {
            HornetQServerLogger.LOGGER.deprecatedConfigurationOption("max-saved-replicated-journals-size");

            if (haPolicyConfig instanceof ReplicaPolicyConfiguration)
            {
               ReplicaPolicyConfiguration hapc = (ReplicaPolicyConfiguration) haPolicyConfig;
               hapc.setMaxSavedReplicatedJournalsSize(getInteger(e, "max-saved-replicated-journals-size",
                     hapc.getMaxSavedReplicatedJournalsSize(), Validators.MINUS_ONE_OR_GE_ZERO));

            }
            else
            {
               HornetQServerLogger.LOGGER.incompatibleWithHAPolicyChosen("max-saved-replicated-journals-size");
            }
         }
      }

      //if we aren already set then set to default
      if (config.getHAPolicyConfiguration() == null)
      {
         config.setHAPolicyConfiguration(new LiveOnlyPolicyConfiguration());
      }


      config.setResolveProtocols(getBoolean(e, "resolve-protocols", config.isResolveProtocols()));

      // Defaults to true when using FileConfiguration
      config.setFileDeploymentEnabled(getBoolean(e, "file-deployment-enabled", config instanceof FileConfiguration));

      config.setPersistenceEnabled(getBoolean(e, "persistence-enabled",
                                              config.isPersistenceEnabled()));

      config.setPersistDeliveryCountBeforeDelivery(getBoolean(e, "persist-delivery-count-before-delivery",
                                                              config.isPersistDeliveryCountBeforeDelivery()));

      config.setScheduledThreadPoolMaxSize(getInteger(e, "scheduled-thread-pool-max-size",
                                                      config.getScheduledThreadPoolMaxSize(), Validators.GT_ZERO));

      config.setThreadPoolMaxSize(getInteger(e, "thread-pool-max-size", config.getThreadPoolMaxSize(),
                                             Validators.MINUS_ONE_OR_GT_ZERO));

      config.setSecurityEnabled(getBoolean(e, "security-enabled", config.isSecurityEnabled()));

      config.setJMXManagementEnabled(getBoolean(e, "jmx-management-enabled", config.isJMXManagementEnabled()));

      config.setJMXDomain(getString(e, "jmx-domain", config.getJMXDomain(), Validators.NOT_NULL_OR_EMPTY));

      config.setSecurityInvalidationInterval(getLong(e, "security-invalidation-interval",
                                                     config.getSecurityInvalidationInterval(),
                                                     Validators.GT_ZERO));

      config.setConnectionTTLOverride(getLong(e,
                                              "connection-ttl-override",
                                              config.getConnectionTTLOverride(),
                                              Validators.MINUS_ONE_OR_GT_ZERO));

      config.setEnabledAsyncConnectionExecution(getBoolean(e,
                                                           "async-connection-execution-enabled",
                                                           config.isAsyncConnectionExecutionEnabled()));

      config.setTransactionTimeout(getLong(e,
                                           "transaction-timeout",
                                           config.getTransactionTimeout(),
                                           Validators.GT_ZERO));

      config.setTransactionTimeoutScanPeriod(getLong(e,
                                                     "transaction-timeout-scan-period",
                                                     config.getTransactionTimeoutScanPeriod(),
                                                     Validators.GT_ZERO));

      config.setMessageExpiryScanPeriod(getLong(e,
                                                "message-expiry-scan-period",
                                                config.getMessageExpiryScanPeriod(),
                                                Validators.MINUS_ONE_OR_GT_ZERO));

      config.setMessageExpiryThreadPriority(getInteger(e,
                                                       "message-expiry-thread-priority",
                                                       config.getMessageExpiryThreadPriority(),
                                                       Validators.THREAD_PRIORITY_RANGE));

      config.setIDCacheSize(getInteger(e,
                                       "id-cache-size",
                                       config.getIDCacheSize(),
                                       Validators.GT_ZERO));

      config.setPersistIDCache(getBoolean(e, "persist-id-cache", config.isPersistIDCache()));

      config.setManagementAddress(new SimpleString(getString(e,
                                                             "management-address",
                                                             config.getManagementAddress()
                                                                .toString(),
                                                             Validators.NOT_NULL_OR_EMPTY)));

      config.setManagementNotificationAddress(new SimpleString(getString(e,
                                                                         "management-notification-address",
                                                                         config.getManagementNotificationAddress()
                                                                            .toString(),
                                                                         Validators.NOT_NULL_OR_EMPTY)));

      config.setMaskPassword(getBoolean(e, "mask-password", false));

      config.setPasswordCodec(getString(e, "password-codec", DefaultSensitiveStringCodec.class.getName(),
                                        Validators.NOT_NULL_OR_EMPTY));

      // parsing cluster password
      String passwordText = getString(e, "cluster-password", null, Validators.NO_CHECK);

      final boolean maskText = config.isMaskPassword();

      if (passwordText != null)
      {
         if (maskText)
         {
            SensitiveDataCodec<String> codec = PasswordMaskingUtil.getCodec(config.getPasswordCodec());
            config.setClusterPassword(codec.decode(passwordText));
         }
         else
         {
            config.setClusterPassword(passwordText);
         }
      }

      config.setClusterUser(getString(e,
                                      "cluster-user",
                                      config.getClusterUser(),
                                      Validators.NO_CHECK));

      NodeList interceptorNodes = e.getElementsByTagName("remoting-interceptors");

      ArrayList<String> incomingInterceptorList = new ArrayList<String>();

      if (interceptorNodes.getLength() > 0)
      {
         NodeList interceptors = interceptorNodes.item(0).getChildNodes();

         for (int i = 0; i < interceptors.getLength(); i++)
         {
            if ("class-name".equalsIgnoreCase(interceptors.item(i).getNodeName()))
            {
               String clazz = getTrimmedTextContent(interceptors.item(i));

               incomingInterceptorList.add(clazz);
            }
         }
      }

      NodeList incomingInterceptorNodes = e.getElementsByTagName("remoting-incoming-interceptors");

      if (incomingInterceptorNodes.getLength() > 0)
      {
         NodeList interceptors = incomingInterceptorNodes.item(0).getChildNodes();

         for (int i = 0; i < interceptors.getLength(); i++)
         {
            if ("class-name".equalsIgnoreCase(interceptors.item(i).getNodeName()))
            {
               String clazz = getTrimmedTextContent(interceptors.item(i));

               incomingInterceptorList.add(clazz);
            }
         }
      }

      config.setIncomingInterceptorClassNames(incomingInterceptorList);

      NodeList outgoingInterceptorNodes = e.getElementsByTagName("remoting-outgoing-interceptors");

      ArrayList<String> outgoingInterceptorList = new ArrayList<String>();

      if (outgoingInterceptorNodes.getLength() > 0)
      {
         NodeList interceptors = outgoingInterceptorNodes.item(0).getChildNodes();

         for (int i = 0; i < interceptors.getLength(); i++)
         {
            if ("class-name".equalsIgnoreCase(interceptors.item(i).getNodeName()))
            {
               String clazz = interceptors.item(i).getTextContent();

               outgoingInterceptorList.add(clazz);
            }
         }
      }

      config.setOutgoingInterceptorClassNames(outgoingInterceptorList);

      NodeList connectorNodes = e.getElementsByTagName("connector");

      for (int i = 0; i < connectorNodes.getLength(); i++)
      {
         Element connectorNode = (Element) connectorNodes.item(i);

         TransportConfiguration connectorConfig = parseTransportConfiguration(connectorNode, config);

         if (connectorConfig.getName() == null)
         {
            HornetQServerLogger.LOGGER.connectorWithNoName();

            continue;
         }

         if (config.getConnectorConfigurations().containsKey(connectorConfig.getName()))
         {
            HornetQServerLogger.LOGGER.connectorAlreadyDeployed(connectorConfig.getName());

            continue;
         }

         config.getConnectorConfigurations().put(connectorConfig.getName(), connectorConfig);
      }

      NodeList acceptorNodes = e.getElementsByTagName("acceptor");

      for (int i = 0; i < acceptorNodes.getLength(); i++)
      {
         Element acceptorNode = (Element) acceptorNodes.item(i);

         TransportConfiguration acceptorConfig = parseTransportConfiguration(acceptorNode, config);

         config.getAcceptorConfigurations().add(acceptorConfig);
      }

      NodeList bgNodes = e.getElementsByTagName("broadcast-group");

      for (int i = 0; i < bgNodes.getLength(); i++)
      {
         Element bgNode = (Element) bgNodes.item(i);

         parseBroadcastGroupConfiguration(bgNode, config);
      }

      NodeList dgNodes = e.getElementsByTagName("discovery-group");

      for (int i = 0; i < dgNodes.getLength(); i++)
      {
         Element dgNode = (Element) dgNodes.item(i);

         parseDiscoveryGroupConfiguration(dgNode, config);
      }

      NodeList brNodes = e.getElementsByTagName("bridge");

      for (int i = 0; i < brNodes.getLength(); i++)
      {
         Element mfNode = (Element) brNodes.item(i);

         parseBridgeConfiguration(mfNode, config);
      }

      NodeList gaNodes = e.getElementsByTagName("grouping-handler");

      for (int i = 0; i < gaNodes.getLength(); i++)
      {
         Element gaNode = (Element) gaNodes.item(i);

         parseGroupingHandlerConfiguration(gaNode, config);
      }

      NodeList ccNodes = e.getElementsByTagName("cluster-connection");

      for (int i = 0; i < ccNodes.getLength(); i++)
      {
         Element ccNode = (Element) ccNodes.item(i);

         parseClusterConnectionConfiguration(ccNode, config);
      }

      NodeList dvNodes = e.getElementsByTagName("divert");

      for (int i = 0; i < dvNodes.getLength(); i++)
      {
         Element dvNode = (Element) dvNodes.item(i);

         parseDivertConfiguration(dvNode, config);
      }

      // Persistence config

      config.setLargeMessagesDirectory(getString(e,
                                                 "large-messages-directory",
                                                 config.getLargeMessagesDirectory(),
                                                 Validators.NOT_NULL_OR_EMPTY));

      config.setBindingsDirectory(getString(e,
                                            "bindings-directory",
                                            config.getBindingsDirectory(),
                                            Validators.NOT_NULL_OR_EMPTY));

      config.setCreateBindingsDir(getBoolean(e,
                                             "create-bindings-dir",
                                             config.isCreateBindingsDir()));

      config.setJournalDirectory(getString(e, "journal-directory", config.getJournalDirectory(),
                                           Validators.NOT_NULL_OR_EMPTY));


      config.setPageMaxConcurrentIO(getInteger(e,
                                               "page-max-concurrent-io",
                                               config.getPageMaxConcurrentIO(),
                                               Validators.MINUS_ONE_OR_GT_ZERO));

      config.setPagingDirectory(getString(e,
                                          "paging-directory",
                                          config.getPagingDirectory(),
                                          Validators.NOT_NULL_OR_EMPTY));

      config.setCreateJournalDir(getBoolean(e, "create-journal-dir", config.isCreateJournalDir()));

      String s = getString(e,
                           "journal-type",
                           config.getJournalType().toString(),
                           Validators.JOURNAL_TYPE);

      if (s.equals(JournalType.NIO.toString()))
      {
         config.setJournalType(JournalType.NIO);
      }
      else if (s.equals(JournalType.ASYNCIO.toString()))
      {
         // https://jira.jboss.org/jira/browse/HORNETQ-295
         // We do the check here to see if AIO is supported so we can use the correct defaults and/or use
         // correct settings in xml
         // If we fall back later on these settings can be ignored
         boolean supportsAIO = AIOSequentialFileFactory.isSupported();

         if (supportsAIO)
         {
            config.setJournalType(JournalType.ASYNCIO);
         }
         else
         {
            if (validateAIO)
            {
               HornetQServerLogger.LOGGER.AIONotFound();
            }

            config.setJournalType(JournalType.NIO);
         }
      }

      config.setJournalSyncTransactional(getBoolean(e,
                                                    "journal-sync-transactional",
                                                    config.isJournalSyncTransactional()));

      config.setJournalSyncNonTransactional(getBoolean(e,
                                                       "journal-sync-non-transactional",
                                                       config.isJournalSyncNonTransactional()));

      config.setJournalFileSize(getInteger(e,
                                           "journal-file-size",
                                           config.getJournalFileSize(),
                                           Validators.GT_ZERO));

      int journalBufferTimeout = getInteger(e,
                                            "journal-buffer-timeout",
                                            config.getJournalType() == JournalType.ASYNCIO ? JournalConstants.DEFAULT_JOURNAL_BUFFER_TIMEOUT_AIO
                                               : JournalConstants.DEFAULT_JOURNAL_BUFFER_TIMEOUT_NIO,
                                            Validators.GT_ZERO);

      int journalBufferSize = getInteger(e,
                                         "journal-buffer-size",
                                         config.getJournalType() == JournalType.ASYNCIO ? JournalConstants.DEFAULT_JOURNAL_BUFFER_SIZE_AIO
                                            : JournalConstants.DEFAULT_JOURNAL_BUFFER_SIZE_NIO,
                                         Validators.GT_ZERO);

      int journalMaxIO = getInteger(e,
                                    "journal-max-io",
                                    config.getJournalType() == JournalType.ASYNCIO ? ActiveMQDefaultConfiguration.getDefaultJournalMaxIoAio()
                                       : ActiveMQDefaultConfiguration.getDefaultJournalMaxIoNio(),
                                    Validators.GT_ZERO);

      if (config.getJournalType() == JournalType.ASYNCIO)
      {
         config.setJournalBufferTimeout_AIO(journalBufferTimeout);
         config.setJournalBufferSize_AIO(journalBufferSize);
         config.setJournalMaxIO_AIO(journalMaxIO);
      }
      else
      {
         config.setJournalBufferTimeout_NIO(journalBufferTimeout);
         config.setJournalBufferSize_NIO(journalBufferSize);
         config.setJournalMaxIO_NIO(journalMaxIO);
      }

      config.setJournalMinFiles(getInteger(e, "journal-min-files", config.getJournalMinFiles(), Validators.GT_ZERO));

      config.setJournalCompactMinFiles(getInteger(e, "journal-compact-min-files", config.getJournalCompactMinFiles(),
                                                  Validators.GE_ZERO));

      config.setJournalCompactPercentage(getInteger(e,
                                                    "journal-compact-percentage",
                                                    config.getJournalCompactPercentage(),
                                                    Validators.PERCENTAGE));

      config.setLogJournalWriteRate(getBoolean(e,
                                               "log-journal-write-rate",
                                               ActiveMQDefaultConfiguration.isDefaultJournalLogWriteRate()));

      config.setJournalPerfBlastPages(getInteger(e,
                                                 "perf-blast-pages",
                                                 ActiveMQDefaultConfiguration.getDefaultJournalPerfBlastPages(),
                                                 Validators.MINUS_ONE_OR_GT_ZERO));

      config.setRunSyncSpeedTest(getBoolean(e, "run-sync-speed-test", config.isRunSyncSpeedTest()));

      config.setWildcardRoutingEnabled(getBoolean(e, "wild-card-routing-enabled", config.isWildcardRoutingEnabled()));

      config.setMessageCounterEnabled(getBoolean(e, "message-counter-enabled", config.isMessageCounterEnabled()));

      config.setMessageCounterSamplePeriod(getLong(e, "message-counter-sample-period",
                                                   config.getMessageCounterSamplePeriod(),
                                                   Validators.GT_ZERO));

      config.setMessageCounterMaxDayHistory(getInteger(e, "message-counter-max-day-history",
                                                       config.getMessageCounterMaxDayHistory(),
                                                       Validators.GT_ZERO));

      config.setServerDumpInterval(getLong(e, "server-dump-interval", config.getServerDumpInterval(),
                                           Validators.MINUS_ONE_OR_GT_ZERO)); // in milliseconds

      config.setMemoryWarningThreshold(getInteger(e,
                                                  "memory-warning-threshold",
                                                  config.getMemoryWarningThreshold(),
                                                  Validators.PERCENTAGE));

      config.setMemoryMeasureInterval(getLong(e,
                                              "memory-measure-interval",
                                              config.getMemoryMeasureInterval(),
                                              Validators.MINUS_ONE_OR_GT_ZERO)); // in

      parseAddressSettings(e, config);

      parseQueues(e, config);

      parseSecurity(e, config);

      NodeList connectorServiceConfigs = e.getElementsByTagName("connector-service");

      ArrayList<ConnectorServiceConfiguration> configs = new ArrayList<ConnectorServiceConfiguration>();

      for (int i = 0; i < connectorServiceConfigs.getLength(); i++)
      {
         Element node = (Element) connectorServiceConfigs.item(i);

         configs.add((parseConnectorService(node)));
      }

      config.setConnectorServiceConfigurations(configs);
   }

   /**
    * @param e
    * @param config
    */
   private void parseSecurity(final Element e, final Configuration config)
   {
      NodeList elements = e.getElementsByTagName("security-settings");

      if (elements.getLength() != 0)
      {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName(SECURITY_ELEMENT_NAME);
         for (int i = 0; i < list.getLength(); i++)
         {
            Pair<String, Set<Role>> securityItem = parseSecurityRoles(list.item(i));
            config.getSecurityRoles().put(securityItem.getA(), securityItem.getB());
         }
      }
   }

   /**
    * @param e
    * @param config
    */
   private void parseQueues(final Element e, final Configuration config)
   {
      NodeList elements = e.getElementsByTagName("queues");

      if (elements.getLength() != 0)
      {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName("queue");
         for (int i = 0; i < list.getLength(); i++)
         {
            CoreQueueConfiguration queueConfig = parseQueueConfiguration(list.item(i));
            config.getQueueConfigurations().add(queueConfig);
         }
      }
   }

   /**
    * @param e
    * @param config
    */
   private void parseAddressSettings(final Element e, final Configuration config)
   {
      NodeList elements = e.getElementsByTagName("address-settings");

      if (elements.getLength() != 0)
      {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName("address-setting");
         for (int i = 0; i < list.getLength(); i++)
         {
            Pair<String, AddressSettings> addressSettings = parseAddressSettings(list.item(i));
            config.getAddressesSettings().put(addressSettings.getA(), addressSettings.getB());
         }
      }
   }

   /**
    * @param node
    * @return
    */
   protected Pair<String, Set<Role>> parseSecurityRoles(final Node node)
   {
      final String match = node.getAttributes().getNamedItem("match").getNodeValue();

      HashSet<Role> securityRoles = new HashSet<Role>();

      Pair<String, Set<Role>> securityMatch = new Pair<String, Set<Role>>(match, securityRoles);

      ArrayList<String> send = new ArrayList<String>();
      ArrayList<String> consume = new ArrayList<String>();
      ArrayList<String> createDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteDurableQueue = new ArrayList<String>();
      ArrayList<String> createNonDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
      ArrayList<String> manageRoles = new ArrayList<String>();
      ArrayList<String> allRoles = new ArrayList<String>();
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node child = children.item(i);
         final String name = child.getNodeName();
         if (PERMISSION_ELEMENT_NAME.equalsIgnoreCase(name))
         {
            final String type = getAttributeValue(child, TYPE_ATTR_NAME);
            final String roleString = getAttributeValue(child, ROLES_ATTR_NAME);
            String[] roles = roleString.split(",");
            for (String role : roles)
            {
               if (SEND_NAME.equals(type))
               {
                  send.add(role.trim());
               }
               else if (CONSUME_NAME.equals(type))
               {
                  consume.add(role.trim());
               }
               else if (CREATEDURABLEQUEUE_NAME.equals(type))
               {
                  createDurableQueue.add(role.trim());
               }
               else if (DELETEDURABLEQUEUE_NAME.equals(type))
               {
                  deleteDurableQueue.add(role.trim());
               }
               else if (CREATE_NON_DURABLE_QUEUE_NAME.equals(type))
               {
                  createNonDurableQueue.add(role.trim());
               }
               else if (DELETE_NON_DURABLE_QUEUE_NAME.equals(type))
               {
                  deleteNonDurableQueue.add(role.trim());
               }
               else if (CREATETEMPQUEUE_NAME.equals(type))
               {
                  createNonDurableQueue.add(role.trim());
               }
               else if (DELETETEMPQUEUE_NAME.equals(type))
               {
                  deleteNonDurableQueue.add(role.trim());
               }
               else if (MANAGE_NAME.equals(type))
               {
                  manageRoles.add(role.trim());
               }
               else
               {
                  HornetQServerLogger.LOGGER.rolePermissionConfigurationError(type);
               }
               if (!allRoles.contains(role.trim()))
               {
                  allRoles.add(role.trim());
               }
            }
         }

      }

      for (String role : allRoles)
      {
         securityRoles.add(new Role(role,
                                    send.contains(role),
                                    consume.contains(role),
                                    createDurableQueue.contains(role),
                                    deleteDurableQueue.contains(role),
                                    createNonDurableQueue.contains(role),
                                    deleteNonDurableQueue.contains(role),
                                    manageRoles.contains(role)));
      }

      return securityMatch;
   }

   /**
    * @param node
    * @return
    */
   protected Pair<String, AddressSettings> parseAddressSettings(final Node node)
   {
      String match = getAttributeValue(node, "match");

      NodeList children = node.getChildNodes();

      AddressSettings addressSettings = new AddressSettings();

      Pair<String, AddressSettings> setting = new Pair<String, AddressSettings>(match, addressSettings);

      for (int i = 0; i < children.getLength(); i++)
      {
         final Node child = children.item(i);
         final String name = child.getNodeName();
         if (DEAD_LETTER_ADDRESS_NODE_NAME.equalsIgnoreCase(name))
         {
            SimpleString queueName = new SimpleString(getTrimmedTextContent(child));
            addressSettings.setDeadLetterAddress(queueName);
         }
         else if (EXPIRY_ADDRESS_NODE_NAME.equalsIgnoreCase(name))
         {
            SimpleString queueName = new SimpleString(getTrimmedTextContent(child));
            addressSettings.setExpiryAddress(queueName);
         }
         else if (EXPIRY_DELAY_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setExpiryDelay(XMLUtil.parseLong(child));
         }
         else if (REDELIVERY_DELAY_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setRedeliveryDelay(XMLUtil.parseLong(child));
         }
         else if (REDELIVERY_DELAY_MULTIPLIER_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setRedeliveryMultiplier(XMLUtil.parseDouble(child));
         }
         else if (MAX_REDELIVERY_DELAY_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setMaxRedeliveryDelay(XMLUtil.parseLong(child));
         }
         else if (MAX_SIZE_BYTES_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setMaxSizeBytes(XMLUtil.parseLong(child));
         }
         else if (PAGE_SIZE_BYTES_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setPageSizeBytes(XMLUtil.parseLong(child));
         }
         else if (PAGE_MAX_CACHE_SIZE_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setPageCacheMaxSize(XMLUtil.parseInt(child));
         }
         else if (MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setMessageCounterHistoryDayLimit(XMLUtil.parseInt(child));
         }
         else if (ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME.equalsIgnoreCase(name))
         {
            String value = getTrimmedTextContent(child);
            Validators.ADDRESS_FULL_MESSAGE_POLICY_TYPE.validate(ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME,
                                                                 value);
            AddressFullMessagePolicy policy = Enum.valueOf(AddressFullMessagePolicy.class, value);
            addressSettings.setAddressFullMessagePolicy(policy);
         }
         else if (LVQ_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setLastValueQueue(XMLUtil.parseBoolean(child));
         }
         else if (MAX_DELIVERY_ATTEMPTS.equalsIgnoreCase(name))
         {
            addressSettings.setMaxDeliveryAttempts(XMLUtil.parseInt(child));
         }
         else if (REDISTRIBUTION_DELAY_NODE_NAME.equalsIgnoreCase(name))
         {
            addressSettings.setRedistributionDelay(XMLUtil.parseLong(child));
         }
         else if (SEND_TO_DLA_ON_NO_ROUTE.equalsIgnoreCase(name))
         {
            addressSettings.setSendToDLAOnNoRoute(XMLUtil.parseBoolean(child));
         }
         else if (SLOW_CONSUMER_THRESHOLD_NODE_NAME.equalsIgnoreCase(name))
         {
            long slowConsumerThreshold = XMLUtil.parseLong(child);
            Validators.MINUS_ONE_OR_GT_ZERO.validate(SLOW_CONSUMER_THRESHOLD_NODE_NAME, slowConsumerThreshold);

            addressSettings.setSlowConsumerThreshold(slowConsumerThreshold);
         }
         else if (SLOW_CONSUMER_CHECK_PERIOD_NODE_NAME.equalsIgnoreCase(name))
         {
            long slowConsumerCheckPeriod = XMLUtil.parseLong(child);
            Validators.GT_ZERO.validate(SLOW_CONSUMER_CHECK_PERIOD_NODE_NAME, slowConsumerCheckPeriod);

            addressSettings.setSlowConsumerCheckPeriod(slowConsumerCheckPeriod);
         }
         else if (SLOW_CONSUMER_POLICY_NODE_NAME.equalsIgnoreCase(name))
         {
            String value = getTrimmedTextContent(child);
            Validators.SLOW_CONSUMER_POLICY_TYPE.validate(SLOW_CONSUMER_POLICY_NODE_NAME,
                                                                 value);
            SlowConsumerPolicy policy = Enum.valueOf(SlowConsumerPolicy.class, value);
            addressSettings.setSlowConsumerPolicy(policy);
         }
      }
      return setting;
   }

   protected CoreQueueConfiguration parseQueueConfiguration(final Node node)
   {
      String name = getAttributeValue(node, "name");
      String address = null;
      String filterString = null;
      boolean durable = true;

      NodeList children = node.getChildNodes();

      for (int j = 0; j < children.getLength(); j++)
      {
         Node child = children.item(j);

         if (child.getNodeName().equals("address"))
         {
            address = getTrimmedTextContent(child);
         }
         else if (child.getNodeName().equals("filter"))
         {
            filterString = getAttributeValue(child, "string");
         }
         else if (child.getNodeName().equals("durable"))
         {
            durable = XMLUtil.parseBoolean(child);
         }
      }

      return new CoreQueueConfiguration()
         .setAddress(address)
         .setName(name)
         .setFilterString(filterString)
         .setDurable(durable);
   }

   private TransportConfiguration parseTransportConfiguration(final Element e, final Configuration mainConfig)
   {
      Node nameNode = e.getAttributes().getNamedItem("name");

      String name = nameNode != null ? nameNode.getNodeValue() : null;

      String clazz = getString(e, "factory-class", null, Validators.NOT_NULL_OR_EMPTY);

      Map<String, Object> params = new HashMap<String, Object>();

      if (mainConfig.isMaskPassword())
      {
         params.put(ActiveMQDefaultConfiguration.getPropMaskPassword(), mainConfig.isMaskPassword());

         if (mainConfig.getPasswordCodec() != null)
         {
            params.put(ActiveMQDefaultConfiguration.getPropPasswordCodec(), mainConfig.getPasswordCodec());
         }
      }

      NodeList paramsNodes = e.getElementsByTagName("param");

      for (int i = 0; i < paramsNodes.getLength(); i++)
      {
         Node paramNode = paramsNodes.item(i);

         NamedNodeMap attributes = paramNode.getAttributes();

         Node nkey = attributes.getNamedItem("key");

         String key = nkey.getTextContent();

         Node nValue = attributes.getNamedItem("value");

         params.put(key, nValue.getTextContent());
      }

      return new TransportConfiguration(clazz, params, name);
   }

   private static final ArrayList<String> POLICY_LIST = new ArrayList<>();
   static
   {
      POLICY_LIST.add("colocated");
      POLICY_LIST.add("live-only");
      POLICY_LIST.add("replicated");
      POLICY_LIST.add("replica");
      POLICY_LIST.add("shared-store-master");
      POLICY_LIST.add("shared-store-slave");
   }
   private static final ArrayList<String> HA_LIST = new ArrayList<>();
   static
   {
      HA_LIST.add("live-only");
      HA_LIST.add("shared-store");
      HA_LIST.add("replication");
   }
   private void parseHAPolicyConfiguration(final Element e, final Configuration mainConfig)
   {
      for (String haType : HA_LIST)
      {
         NodeList haNodeList = e.getElementsByTagName(haType);
         if (haNodeList.getLength() > 0)
         {
            Element haNode = (Element) haNodeList.item(0);
            if (haNode.getTagName().equals("replication"))
            {
               NodeList masterNodeList = e.getElementsByTagName("master");
               if (masterNodeList.getLength() > 0)
               {
                  Element masterNode = (Element) masterNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createReplicatedHaPolicy(masterNode));
               }
               NodeList slaveNodeList = e.getElementsByTagName("slave");
               if (slaveNodeList.getLength() > 0)
               {
                  Element slaveNode = (Element) slaveNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createReplicaHaPolicy(slaveNode));
               }
               NodeList colocatedNodeList = e.getElementsByTagName("colocated");
               if (colocatedNodeList.getLength() > 0)
               {
                  Element colocatedNode = (Element) colocatedNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createColocatedHaPolicy(colocatedNode, true));
               }
            }
            else if (haNode.getTagName().equals("shared-store"))
            {
               NodeList masterNodeList = e.getElementsByTagName("master");
               if (masterNodeList.getLength() > 0)
               {
                  Element masterNode = (Element) masterNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createSharedStoreMasterHaPolicy(masterNode));
               }
               NodeList slaveNodeList = e.getElementsByTagName("slave");
               if (slaveNodeList.getLength() > 0)
               {
                  Element slaveNode = (Element) slaveNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createSharedStoreSlaveHaPolicy(slaveNode));
               }
               NodeList colocatedNodeList = e.getElementsByTagName("colocated");
               if (colocatedNodeList.getLength() > 0)
               {
                  Element colocatedNode = (Element) colocatedNodeList.item(0);
                  mainConfig.setHAPolicyConfiguration(createColocatedHaPolicy(colocatedNode, false));
               }
            }
            else if (haNode.getTagName().equals("live-only"))
            {
               NodeList noneNodeList = e.getElementsByTagName("live-only");
               Element noneNode = (Element) noneNodeList.item(0);
               mainConfig.setHAPolicyConfiguration(createLiveOnlyHaPolicy(noneNode));
            }
         }
      }
   }

   private LiveOnlyPolicyConfiguration createLiveOnlyHaPolicy(Element policyNode)
   {
      LiveOnlyPolicyConfiguration configuration = new LiveOnlyPolicyConfiguration();

      configuration.setScaleDownConfiguration(parseScaleDownConfig(policyNode));

      return configuration;
   }

   private ReplicatedPolicyConfiguration createReplicatedHaPolicy(Element policyNode)
   {
      ReplicatedPolicyConfiguration configuration = new ReplicatedPolicyConfiguration();

      configuration.setCheckForLiveServer(getBoolean(policyNode, "check-for-live-server", configuration.isCheckForLiveServer()));

      configuration.setGroupName(getString(policyNode, "group-name", configuration.getGroupName(), Validators.NO_CHECK));

      configuration.setClusterName(getString(policyNode, "cluster-name", configuration.getClusterName(), Validators.NO_CHECK));

      return configuration;
   }

   private ReplicaPolicyConfiguration createReplicaHaPolicy(Element policyNode)
   {
      ReplicaPolicyConfiguration configuration = new ReplicaPolicyConfiguration();

      configuration.setRestartBackup(getBoolean(policyNode, "restart-backup", configuration.isRestartBackup()));

      configuration.setGroupName(getString(policyNode, "group-name", configuration.getGroupName(), Validators.NO_CHECK));

      configuration.setAllowFailBack(getBoolean(policyNode, "allow-failback", configuration.isAllowFailBack()));

      configuration.setFailbackDelay(getLong(policyNode, "failback-delay", configuration.getFailbackDelay(), Validators.GT_ZERO));

      configuration.setClusterName(getString(policyNode, "cluster-name", configuration.getClusterName(), Validators.NO_CHECK));

      configuration.setMaxSavedReplicatedJournalsSize(getInteger(policyNode, "max-saved-replicated-journals-size",
            configuration.getMaxSavedReplicatedJournalsSize(), Validators.MINUS_ONE_OR_GE_ZERO));

      configuration.setScaleDownConfiguration(parseScaleDownConfig(policyNode));

      return configuration;
   }

   private SharedStoreMasterPolicyConfiguration createSharedStoreMasterHaPolicy(Element policyNode)
   {
      SharedStoreMasterPolicyConfiguration configuration = new SharedStoreMasterPolicyConfiguration();

      configuration.setFailoverOnServerShutdown(getBoolean(policyNode, "failover-on-shutdown", configuration.isFailoverOnServerShutdown()));

      configuration.setFailbackDelay(getLong(policyNode, "failback-delay", configuration.getFailbackDelay(), Validators.GT_ZERO));

      return configuration;
   }

   private SharedStoreSlavePolicyConfiguration createSharedStoreSlaveHaPolicy(Element policyNode)
   {
      SharedStoreSlavePolicyConfiguration configuration = new SharedStoreSlavePolicyConfiguration();

      configuration.setAllowFailBack(getBoolean(policyNode, "allow-failback", configuration.isAllowFailBack()));

      configuration.setFailoverOnServerShutdown(getBoolean(policyNode, "failover-on-shutdown", configuration.isFailoverOnServerShutdown()));

      configuration.setFailbackDelay(getLong(policyNode, "failback-delay", configuration.getFailbackDelay(), Validators.GT_ZERO));

      configuration.setRestartBackup(getBoolean(policyNode, "restart-backup", configuration.isRestartBackup()));

      configuration.setScaleDownConfiguration(parseScaleDownConfig(policyNode));

      return configuration;
   }

   private ColocatedPolicyConfiguration createColocatedHaPolicy(Element policyNode, boolean replicated)
   {
      ColocatedPolicyConfiguration configuration = new ColocatedPolicyConfiguration();

      boolean requestBackup = getBoolean(policyNode, "request-backup", configuration.isRequestBackup());

      configuration.setRequestBackup(requestBackup);

      int backupRequestRetries = getInteger(policyNode, "backup-request-retries", configuration.getBackupRequestRetries(), Validators.MINUS_ONE_OR_GE_ZERO);

      configuration.setBackupRequestRetries(backupRequestRetries);

      long backupRequestRetryInterval = getLong(policyNode, "backup-request-retry-interval", configuration.getBackupRequestRetryInterval(), Validators.GT_ZERO);

      configuration.setBackupRequestRetryInterval(backupRequestRetryInterval);

      int maxBackups = getInteger(policyNode, "max-backups", configuration.getMaxBackups(), Validators.GE_ZERO);

      configuration.setMaxBackups(maxBackups);

      int backupPortOffset = getInteger(policyNode, "backup-port-offset", configuration.getBackupPortOffset(), Validators.GT_ZERO);

      configuration.setBackupPortOffset(backupPortOffset);

      NodeList remoteConnectorNode = policyNode.getElementsByTagName("excludes");

      if (remoteConnectorNode != null && remoteConnectorNode.getLength() > 0)
      {
         NodeList remoteConnectors = remoteConnectorNode.item(0).getChildNodes();
         for (int i = 0; i < remoteConnectors.getLength(); i++)
         {
            Node child = remoteConnectors.item(i);
            if (child.getNodeName().equals("connector-ref"))
            {
               String connectorName = getTrimmedTextContent(child);
               configuration.getExcludedConnectors().add(connectorName);
            }
         }
      }

      NodeList masterNodeList = policyNode.getElementsByTagName("master");
      if (masterNodeList.getLength() > 0)
      {
         Element masterNode = (Element) masterNodeList.item(0);
         configuration.setLiveConfig(replicated ? createReplicatedHaPolicy(masterNode) : createSharedStoreMasterHaPolicy(masterNode));
      }
      NodeList slaveNodeList = policyNode.getElementsByTagName("slave");
      if (slaveNodeList.getLength() > 0)
      {
         Element slaveNode = (Element) slaveNodeList.item(0);
         configuration.setBackupConfig(replicated ? createReplicaHaPolicy(slaveNode) : createSharedStoreSlaveHaPolicy(slaveNode));
      }

      return configuration;
   }
   private ScaleDownConfiguration parseScaleDownConfig(Element policyNode)
   {
      NodeList scaleDownNode = policyNode.getElementsByTagName("scale-down");

      if (scaleDownNode.getLength() > 0)
      {
         ScaleDownConfiguration scaleDownConfiguration = new ScaleDownConfiguration();

         Element scaleDownElement = (Element) scaleDownNode.item(0);

         scaleDownConfiguration.setEnabled(getBoolean(scaleDownElement, "enabled", scaleDownConfiguration.isEnabled()));

         String scaleDownDiscoveryGroup = getString(scaleDownElement, "discovery-group", scaleDownConfiguration.getDiscoveryGroup(), Validators.NO_CHECK);

         scaleDownConfiguration.setDiscoveryGroup(scaleDownDiscoveryGroup);

         String scaleDownDiscoveryGroupName = getString(scaleDownElement, "group-name", scaleDownConfiguration.getGroupName(), Validators.NO_CHECK);

         scaleDownConfiguration.setGroupName(scaleDownDiscoveryGroupName);

         NodeList scaleDownConnectorNode = scaleDownElement.getElementsByTagName("connectors");

         if (scaleDownConnectorNode != null && scaleDownConnectorNode.getLength() > 0)
         {
            NodeList scaleDownConnectors = scaleDownConnectorNode.item(0).getChildNodes();
            for (int i = 0; i < scaleDownConnectors.getLength(); i++)
            {
               Node child = scaleDownConnectors.item(i);
               if (child.getNodeName().equals("connector-ref"))
               {
                  String connectorName = getTrimmedTextContent(child);

                  scaleDownConfiguration.getConnectors().add(connectorName);
               }
            }
         }
         return scaleDownConfiguration;
      }
      return null;
   }

   private void parseBroadcastGroupConfiguration(final Element e, final Configuration mainConfig)
   {
      String name = e.getAttribute("name");

      List<String> connectorNames = new ArrayList<String>();

      NodeList children = e.getChildNodes();

      for (int j = 0; j < children.getLength(); j++)
      {
         Node child = children.item(j);

         if (child.getNodeName().equals("connector-ref"))
         {
            String connectorName = getString(e,
                                             "connector-ref",
                                             null,
                                             Validators.NOT_NULL_OR_EMPTY);

            connectorNames.add(connectorName);
         }
      }

      long broadcastPeriod =
         getLong(e, "broadcast-period", ActiveMQDefaultConfiguration.getDefaultBroadcastPeriod(), Validators.GT_ZERO);

      String localAddress = getString(e, "local-bind-address", null, Validators.NO_CHECK);

      int localBindPort = getInteger(e, "local-bind-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      String groupAddress = getString(e, "group-address", null, Validators.NO_CHECK);

      int groupPort = getInteger(e, "group-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      String jgroupsFile = getString(e, "jgroups-file", null, Validators.NO_CHECK);

      String jgroupsChannel = getString(e, "jgroups-channel", null, Validators.NO_CHECK);


      // TODO: validate if either jgroups or UDP is being filled

      BroadcastEndpointFactoryConfiguration endpointFactoryConfiguration;

      if (jgroupsFile != null)
      {
         endpointFactoryConfiguration = new JGroupsBroadcastGroupConfiguration(jgroupsFile, jgroupsChannel);
      }
      else
      {
         endpointFactoryConfiguration = new UDPBroadcastGroupConfiguration()
            .setGroupAddress(groupAddress)
            .setGroupPort(groupPort)
            .setLocalBindAddress(localAddress)
            .setLocalBindPort(localBindPort);
      }

      BroadcastGroupConfiguration config = new BroadcastGroupConfiguration()
         .setName(name)
         .setBroadcastPeriod(broadcastPeriod)
         .setConnectorInfos(connectorNames)
         .setEndpointFactoryConfiguration(endpointFactoryConfiguration);

      mainConfig.getBroadcastGroupConfigurations().add(config);
   }

   private void parseDiscoveryGroupConfiguration(final Element e, final Configuration mainConfig)
   {
      String name = e.getAttribute("name");

      long discoveryInitialWaitTimeout =
         getLong(e, "initial-wait-timeout", HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT,
                 Validators.GT_ZERO);

      long refreshTimeout =
         getLong(e, "refresh-timeout", ActiveMQDefaultConfiguration.getDefaultBroadcastRefreshTimeout(),
                 Validators.GT_ZERO);

      String localBindAddress = getString(e, "local-bind-address", null, Validators.NO_CHECK);

      int localBindPort = getInteger(e, "local-bind-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      String groupAddress = getString(e, "group-address", null, Validators.NO_CHECK);

      int groupPort = getInteger(e, "group-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      String jgroupsFile = getString(e, "jgroups-file", null, Validators.NO_CHECK);

      String jgroupsChannel = getString(e, "jgroups-channel", null, Validators.NO_CHECK);

      // TODO: validate if either jgroups or UDP is being filled
      BroadcastEndpointFactoryConfiguration endpointFactoryConfiguration;
      if (jgroupsFile != null)
      {
         endpointFactoryConfiguration = new JGroupsBroadcastGroupConfiguration(jgroupsFile, jgroupsChannel);
      }
      else
      {
         endpointFactoryConfiguration = new UDPBroadcastGroupConfiguration()
            .setGroupAddress(groupAddress)
            .setGroupPort(groupPort)
            .setLocalBindAddress(localBindAddress)
            .setLocalBindPort(localBindPort);
      }

      DiscoveryGroupConfiguration config = new DiscoveryGroupConfiguration()
         .setName(name)
         .setRefreshTimeout(refreshTimeout)
         .setDiscoveryInitialWaitTimeout(discoveryInitialWaitTimeout)
         .setBroadcastEndpointFactoryConfiguration(endpointFactoryConfiguration);

      if (mainConfig.getDiscoveryGroupConfigurations().containsKey(name))
      {
         HornetQServerLogger.LOGGER.discoveryGroupAlreadyDeployed(name);

         return;
      }
      else
      {
         mainConfig.getDiscoveryGroupConfigurations().put(name, config);
      }
   }

   private void parseClusterConnectionConfiguration(final Element e, final Configuration mainConfig)
   {
      String name = e.getAttribute("name");

      String address = getString(e, "address", null, Validators.NOT_NULL_OR_EMPTY);

      String connectorName = getString(e, "connector-ref", null, Validators.NOT_NULL_OR_EMPTY);

      boolean duplicateDetection =
         getBoolean(e, "use-duplicate-detection", ActiveMQDefaultConfiguration.isDefaultClusterDuplicateDetection());

      boolean forwardWhenNoConsumers =
         getBoolean(e, "forward-when-no-consumers",
                    ActiveMQDefaultConfiguration.isDefaultClusterForwardWhenNoConsumers());

      int maxHops = getInteger(e, "max-hops",
                               ActiveMQDefaultConfiguration.getDefaultClusterMaxHops(),
                               Validators.GE_ZERO);

      long clientFailureCheckPeriod =
         getLong(e, "check-period", ActiveMQDefaultConfiguration.getDefaultClusterFailureCheckPeriod(),
                 Validators.GT_ZERO);

      long connectionTTL =
         getLong(e, "connection-ttl", ActiveMQDefaultConfiguration.getDefaultClusterConnectionTtl(),
                 Validators.GT_ZERO);


      long retryInterval =
         getLong(e, "retry-interval", ActiveMQDefaultConfiguration.getDefaultClusterRetryInterval(),
                 Validators.GT_ZERO);

      long callTimeout = getLong(e, "call-timeout", HornetQClient.DEFAULT_CALL_TIMEOUT, Validators.GT_ZERO);

      long callFailoverTimeout = getLong(e, "call-failover-timeout", HornetQClient.DEFAULT_CALL_FAILOVER_TIMEOUT, Validators.MINUS_ONE_OR_GT_ZERO);

      double retryIntervalMultiplier = getDouble(e, "retry-interval-multiplier",
                                                 ActiveMQDefaultConfiguration.getDefaultClusterRetryIntervalMultiplier(), Validators.GT_ZERO);

      int minLargeMessageSize = getInteger(e, "min-large-message-size", HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE, Validators.GT_ZERO);

      long maxRetryInterval = getLong(e, "max-retry-interval", ActiveMQDefaultConfiguration.getDefaultClusterMaxRetryInterval(), Validators.GT_ZERO);

      int initialConnectAttempts = getInteger(e, "initial-connect-attempts", ActiveMQDefaultConfiguration.getDefaultClusterInitialConnectAttempts(), Validators.MINUS_ONE_OR_GE_ZERO);

      int reconnectAttempts = getInteger(e, "reconnect-attempts", ActiveMQDefaultConfiguration.getDefaultClusterReconnectAttempts(), Validators.MINUS_ONE_OR_GE_ZERO);


      int confirmationWindowSize =
         getInteger(e, "confirmation-window-size", ActiveMQDefaultConfiguration.getDefaultClusterConfirmationWindowSize(),
                    Validators.GT_ZERO);

      long clusterNotificationInterval = getLong(e, "notification-interval", ActiveMQDefaultConfiguration.getDefaultClusterNotificationInterval(), Validators.GT_ZERO);

      int clusterNotificationAttempts = getInteger(e, "notification-attempts", ActiveMQDefaultConfiguration.getDefaultClusterNotificationAttempts(), Validators.GT_ZERO);

      String scaleDownConnector = e.getAttribute("scale-down-connector");

      String discoveryGroupName = null;

      List<String> staticConnectorNames = new ArrayList<String>();

      boolean allowDirectConnectionsOnly = false;

      NodeList children = e.getChildNodes();

      for (int j = 0; j < children.getLength(); j++)
      {
         Node child = children.item(j);

         if (child.getNodeName().equals("discovery-group-ref"))
         {
            discoveryGroupName = child.getAttributes().getNamedItem("discovery-group-name").getNodeValue();
         }
         else if (child.getNodeName().equals("static-connectors"))
         {
            Node attr = child.getAttributes().getNamedItem("allow-direct-connections-only");
            if (attr != null)
            {
               allowDirectConnectionsOnly = "true".equalsIgnoreCase(attr.getNodeValue()) || allowDirectConnectionsOnly;
            }
            getStaticConnectors(staticConnectorNames, child);
         }
      }

      ClusterConnectionConfiguration config = new ClusterConnectionConfiguration()
         .setName(name)
         .setAddress(address)
         .setConnectorName(connectorName)
         .setMinLargeMessageSize(minLargeMessageSize)
         .setClientFailureCheckPeriod(clientFailureCheckPeriod)
         .setConnectionTTL(connectionTTL)
         .setRetryInterval(retryInterval)
         .setRetryIntervalMultiplier(retryIntervalMultiplier)
         .setMaxRetryInterval(maxRetryInterval)
         .setInitialConnectAttempts(initialConnectAttempts)
         .setReconnectAttempts(reconnectAttempts)
         .setCallTimeout(callTimeout)
         .setCallFailoverTimeout(callFailoverTimeout)
         .setDuplicateDetection(duplicateDetection)
         .setForwardWhenNoConsumers(forwardWhenNoConsumers)
         .setMaxHops(maxHops)
         .setConfirmationWindowSize(confirmationWindowSize)
         .setAllowDirectConnectionsOnly(allowDirectConnectionsOnly)
         .setClusterNotificationInterval(clusterNotificationInterval)
         .setClusterNotificationAttempts(clusterNotificationAttempts);

      if (discoveryGroupName == null)
      {
         config.setStaticConnectors(staticConnectorNames);
      }
      else
      {
         config.setDiscoveryGroupName(discoveryGroupName);
      }

      mainConfig.getClusterConfigurations().add(config);
   }

   private void parseGroupingHandlerConfiguration(final Element node, final Configuration mainConfiguration)
   {
      String name = node.getAttribute("name");
      String type = getString(node, "type", null, Validators.NOT_NULL_OR_EMPTY);
      String address = getString(node, "address", null, Validators.NOT_NULL_OR_EMPTY);
      Integer timeout = getInteger(node, "timeout", ActiveMQDefaultConfiguration.getDefaultGroupingHandlerTimeout(), Validators.GT_ZERO);
      Long groupTimeout = getLong(node, "group-timeout", ActiveMQDefaultConfiguration.getDefaultGroupingHandlerGroupTimeout(), Validators.MINUS_ONE_OR_GT_ZERO);
      Long reaperPeriod = getLong(node, "reaper-period", ActiveMQDefaultConfiguration.getDefaultGroupingHandlerReaperPeriod(), Validators.GT_ZERO);
      mainConfiguration.setGroupingHandlerConfiguration(new GroupingHandlerConfiguration()
                                                           .setName(new SimpleString(name))
                                                           .setType(
                                                              type.equals(GroupingHandlerConfiguration.TYPE.LOCAL.getType())
                                                                 ? GroupingHandlerConfiguration.TYPE.LOCAL
                                                                 : GroupingHandlerConfiguration.TYPE.REMOTE)
                                                           .setAddress(new SimpleString(address))
                                                           .setTimeout(timeout)
                                                           .setGroupTimeout(groupTimeout)
                                                           .setReaperPeriod(reaperPeriod));
   }

   private void parseBridgeConfiguration(final Element brNode, final Configuration mainConfig) throws Exception
   {
      String name = brNode.getAttribute("name");

      String queueName = getString(brNode, "queue-name", null, Validators.NOT_NULL_OR_EMPTY);

      String forwardingAddress = getString(brNode, "forwarding-address", null, Validators.NO_CHECK);

      String transformerClassName = getString(brNode, "transformer-class-name", null, Validators.NO_CHECK);

      // Default bridge conf
      int confirmationWindowSize =
         getInteger(brNode, "confirmation-window-size", ActiveMQDefaultConfiguration.getDefaultBridgeConfirmationWindowSize(),
                    Validators.GT_ZERO);

      long retryInterval = getLong(brNode, "retry-interval", HornetQClient.DEFAULT_RETRY_INTERVAL, Validators.GT_ZERO);

      long clientFailureCheckPeriod =
         getLong(brNode, "check-period", HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD, Validators.GT_ZERO);

      long connectionTTL = getLong(brNode, "connection-ttl", HornetQClient.DEFAULT_CONNECTION_TTL, Validators.GT_ZERO);

      int minLargeMessageSize =
         getInteger(brNode, "min-large-message-size", HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
                    Validators.GT_ZERO);

      long maxRetryInterval = getLong(brNode, "max-retry-interval", HornetQClient.DEFAULT_MAX_RETRY_INTERVAL, Validators.GT_ZERO);


      double retryIntervalMultiplier =
         getDouble(brNode, "retry-interval-multiplier", HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
                   Validators.GT_ZERO);

      int initialConnectAttempts =
         getInteger(brNode, "initial-connect-attempts", ActiveMQDefaultConfiguration.getDefaultBridgeInitialConnectAttempts(),
                    Validators.MINUS_ONE_OR_GE_ZERO);

      int reconnectAttempts =
         getInteger(brNode, "reconnect-attempts", ActiveMQDefaultConfiguration.getDefaultBridgeReconnectAttempts(),
                    Validators.MINUS_ONE_OR_GE_ZERO);

      int reconnectAttemptsSameNode =
         getInteger(brNode, "reconnect-attempts-same-node", ActiveMQDefaultConfiguration.getDefaultBridgeConnectSameNode(),
                    Validators.MINUS_ONE_OR_GE_ZERO);

      boolean useDuplicateDetection = getBoolean(brNode,
                                                 "use-duplicate-detection",
                                                 ActiveMQDefaultConfiguration.isDefaultBridgeDuplicateDetection());

      String user = getString(brNode,
                              "user",
                              ActiveMQDefaultConfiguration.getDefaultClusterUser(),
                              Validators.NO_CHECK);

      NodeList clusterPassNodes = brNode.getElementsByTagName("password");
      String password = null;
      boolean maskPassword = mainConfig.isMaskPassword();

      SensitiveDataCodec<String> codec = null;

      if (clusterPassNodes.getLength() > 0)
      {
         Node passNode = clusterPassNodes.item(0);
         password = passNode.getTextContent();
      }

      if (password != null)
      {
         if (maskPassword)
         {
            codec = PasswordMaskingUtil.getCodec(mainConfig.getPasswordCodec());
            password = codec.decode(password);
         }
      }
      else
      {
         password = ActiveMQDefaultConfiguration.getDefaultClusterPassword();
      }

      boolean ha = getBoolean(brNode, "ha", false);

      String filterString = null;

      List<String> staticConnectorNames = new ArrayList<String>();

      String discoveryGroupName = null;

      NodeList children = brNode.getChildNodes();

      for (int j = 0; j < children.getLength(); j++)
      {
         Node child = children.item(j);

         if (child.getNodeName().equals("filter"))
         {
            filterString = child.getAttributes().getNamedItem("string").getNodeValue();
         }
         else if (child.getNodeName().equals("discovery-group-ref"))
         {
            discoveryGroupName = child.getAttributes().getNamedItem("discovery-group-name").getNodeValue();
         }
         else if (child.getNodeName().equals("static-connectors"))
         {
            getStaticConnectors(staticConnectorNames, child);
         }
      }

      BridgeConfiguration config = new BridgeConfiguration()
         .setName(name)
         .setQueueName(queueName)
         .setForwardingAddress(forwardingAddress)
         .setFilterString(filterString)
         .setTransformerClassName(transformerClassName)
         .setMinLargeMessageSize(minLargeMessageSize)
         .setClientFailureCheckPeriod(clientFailureCheckPeriod)
         .setConnectionTTL(connectionTTL)
         .setRetryInterval(retryInterval)
         .setMaxRetryInterval(maxRetryInterval)
         .setRetryIntervalMultiplier(retryIntervalMultiplier)
         .setInitialConnectAttempts(initialConnectAttempts)
         .setReconnectAttempts(reconnectAttempts)
         .setReconnectAttemptsOnSameNode(reconnectAttemptsSameNode)
         .setUseDuplicateDetection(useDuplicateDetection)
         .setConfirmationWindowSize(confirmationWindowSize)
         .setHA(ha)
         .setUser(user)
         .setPassword(password);

      if (!staticConnectorNames.isEmpty())
      {
         config.setStaticConnectors(staticConnectorNames);
      }
      else
      {
         config.setDiscoveryGroupName(discoveryGroupName);
      }

      mainConfig.getBridgeConfigurations().add(config);
   }

   private void getStaticConnectors(List<String> staticConnectorNames, Node child)
   {
      NodeList children2 = ((Element) child).getElementsByTagName("connector-ref");

      for (int k = 0; k < children2.getLength(); k++)
      {
         Element child2 = (Element) children2.item(k);

         String connectorName = child2.getChildNodes().item(0).getNodeValue();

         staticConnectorNames.add(connectorName);
      }
   }

   private void parseDivertConfiguration(final Element e, final Configuration mainConfig)
   {
      String name = e.getAttribute("name");

      String routingName = getString(e, "routing-name", null, Validators.NO_CHECK);

      String address = getString(e, "address", null, Validators.NOT_NULL_OR_EMPTY);

      String forwardingAddress = getString(e, "forwarding-address", null, Validators.NOT_NULL_OR_EMPTY);

      boolean exclusive = getBoolean(e, "exclusive", ActiveMQDefaultConfiguration.isDefaultDivertExclusive());

      String transformerClassName = getString(e, "transformer-class-name", null, Validators.NO_CHECK);

      String filterString = null;

      NodeList children = e.getChildNodes();

      for (int j = 0; j < children.getLength(); j++)
      {
         Node child = children.item(j);

         if (child.getNodeName().equals("filter"))
         {
            filterString = getAttributeValue(child, "string");
         }
      }

      DivertConfiguration config = new DivertConfiguration()
         .setName(name)
         .setRoutingName(routingName)
         .setAddress(address)
         .setForwardingAddress(forwardingAddress)
         .setExclusive(exclusive)
         .setFilterString(filterString)
         .setTransformerClassName(transformerClassName);

      mainConfig.getDivertConfigurations().add(config);
   }

   private ConnectorServiceConfiguration parseConnectorService(final Element e)
   {
      Node nameNode = e.getAttributes().getNamedItem("name");

      String name = nameNode != null ? nameNode.getNodeValue() : null;

      String clazz = getString(e, "factory-class", null, Validators.NOT_NULL_OR_EMPTY);

      Map<String, Object> params = new HashMap<String, Object>();

      NodeList paramsNodes = e.getElementsByTagName("param");

      for (int i = 0; i < paramsNodes.getLength(); i++)
      {
         Node paramNode = paramsNodes.item(i);

         NamedNodeMap attributes = paramNode.getAttributes();

         Node nkey = attributes.getNamedItem("key");

         String key = nkey.getTextContent();

         Node nValue = attributes.getNamedItem("value");

         params.put(key, nValue.getTextContent());
      }

      return new ConnectorServiceConfiguration()
         .setFactoryClassName(clazz)
         .setParams(params)
         .setName(name);
   }
}
