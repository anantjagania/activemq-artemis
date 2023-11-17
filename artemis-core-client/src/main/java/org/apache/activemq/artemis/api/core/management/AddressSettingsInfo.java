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
package org.apache.activemq.artemis.api.core.management;

import org.apache.activemq.artemis.json.JsonObject;

import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.json.dynamic.MetaJSON;

// XXX no javadocs
public final class AddressSettingsInfo {

   static final MetaJSON<AddressSettingsInfo> metaJson = new MetaJSON<>();


   {
      metaJson.add(String.class, "addressFullMessagePolicy", (o, p) -> o.addressFullMessagePolicy = p, o -> o.addressFullMessagePolicy);
   }
   private String addressFullMessagePolicy;

   {
      metaJson.add(Long.class, "maxSizeBytes", (o, p) -> o.maxSizeBytes = p, o -> o.maxSizeBytes);
   }
   private long maxSizeBytes;

   {
      metaJson.add(Integer.class, "pageSizeBytes", (o, p) -> o.pageSizeBytes = p, o -> o.pageSizeBytes);
   }
   private int pageSizeBytes;

   {
      metaJson.add(Integer.class, "pageCacheMaxSize", (o, p) -> o.pageCacheMaxSize = p, o -> o.pageCacheMaxSize);
   }
   private int pageCacheMaxSize;

   {
      metaJson.add(Integer.class, "maxDeliveryAttempts", (o, p) -> o.maxDeliveryAttempts = p, o -> o.maxDeliveryAttempts);
   }
   private int maxDeliveryAttempts;

   {
      metaJson.add(Double.class, "redeliveryMultiplier", (o, p) -> o.redeliveryMultiplier = p, o -> o.redeliveryMultiplier);
   }
   private double redeliveryMultiplier;

   {
      metaJson.add(Long.class, "maxRedeliveryDelay", (o, p) -> o.maxRedeliveryDelay = p, o -> o.maxRedeliveryDelay);
   }
   private long maxRedeliveryDelay;

   {
      metaJson.add(Long.class, "redeliveryDelay", (o, p) -> o.redeliveryDelay = p, o -> o.redeliveryDelay);
   }
   private long redeliveryDelay;

   {
      metaJson.add(String.class, "deadLetterAddress", (o, p) -> o.deadLetterAddress = p, o -> o.deadLetterAddress);
   }
   private String deadLetterAddress;

   {
      metaJson.add(String.class, "expiryAddress", (o, p) -> o.expiryAddress = p, o -> o.expiryAddress);
   }
   private String expiryAddress;

   {
      metaJson.add(Boolean.class, "lastValueQueue", (o, p) -> o.lastValueQueue = p, o -> o.lastValueQueue);
   }
   private boolean lastValueQueue;

   {
      metaJson.add(Long.class, "redistributionDelay", (o, p) -> o.redistributionDelay = p, o -> o.redistributionDelay);
   }
   private long redistributionDelay;

   {
      metaJson.add(Boolean.class, "sendToDLAOnNoRoute", (o, p) -> o.sendToDLAOnNoRoute = p, o -> o.sendToDLAOnNoRoute);
   }
   private boolean sendToDLAOnNoRoute;

   {
      metaJson.add(Long.class, "slowConsumerThreshold", (o, p) -> o.slowConsumerThreshold = p, o -> o.slowConsumerThreshold);
   }
   private long slowConsumerThreshold;

   {
      metaJson.add(Long.class, "slowConsumerCheckPeriod", (o, p) -> o.slowConsumerCheckPeriod = p, o -> o.slowConsumerCheckPeriod);
   }
   private long slowConsumerCheckPeriod;

   {
      metaJson.add(String.class, "slowConsumerPolicy", (o, p) -> o.slowConsumerPolicy = p, o -> o.slowConsumerPolicy);
   }
   private String slowConsumerPolicy;

   {
      metaJson.add(Boolean.class, "autoCreateJmsQueues", (o, p) -> o.autoCreateJmsQueues = p, o -> o.autoCreateJmsQueues);
   }
   private boolean autoCreateJmsQueues;

   {
      metaJson.add(Boolean.class, "autoDeleteJmsQueues", (o, p) -> o.autoDeleteJmsQueues = p, o -> o.autoDeleteJmsQueues);
   }
   private boolean autoDeleteJmsQueues;

   {
      metaJson.add(Boolean.class, "autoCreateJmsTopics", (o, p) -> o.autoCreateJmsTopics = p, o -> o.autoCreateJmsTopics);
   }
   private boolean autoCreateJmsTopics;

   {
      metaJson.add(Boolean.class, "autoDeleteJmsTopics", (o, p) -> o.autoDeleteJmsTopics = p, o -> o.autoDeleteJmsTopics);
   }
   private boolean autoDeleteJmsTopics;

   {
      metaJson.add(Boolean.class, "autoCreateQueues", (o, p) -> o.autoCreateQueues = p, o -> o.autoCreateQueues);
   }
   private boolean autoCreateQueues;

   {
      metaJson.add(Boolean.class, "autoDeleteQueues", (o, p) -> o.autoDeleteQueues = p, o -> o.autoDeleteQueues);
   }
   private boolean autoDeleteQueues;

   {
      metaJson.add(Boolean.class, "autoCreateAddresses", (o, p) -> o.autoCreateAddresses = p, o -> o.autoCreateAddresses);
   }
   private boolean autoCreateAddresses;

   {
      metaJson.add(Boolean.class, "autoDeleteAddresses", (o, p) -> o.autoDeleteAddresses = p, o -> o.autoDeleteAddresses);
   }
   private boolean autoDeleteAddresses;

   {
      metaJson.add(String.class, "configDeleteQueues", (o, p) -> o.configDeleteQueues = p, o -> o.configDeleteQueues);
   }
   private String configDeleteQueues;

   {
      metaJson.add(String.class, "configDeleteAddresses", (o, p) -> o.configDeleteAddresses = p, o -> o.configDeleteAddresses);
   }
   private String configDeleteAddresses;

   {
      metaJson.add(Long.class, "maxSizeBytesRejectThreshold", (o, p) -> o.maxSizeBytesRejectThreshold = p, o -> o.maxSizeBytesRejectThreshold);
   }
   private long maxSizeBytesRejectThreshold;

   {
      metaJson.add(String.class, "defaultLastValueKey", (o, p) -> o.defaultLastValueKey = p, o -> o.defaultLastValueKey);
   }
   private String defaultLastValueKey;

   {
      metaJson.add(Boolean.class, "defaultNonDestructive", (o, p) -> o.defaultNonDestructive = p, o -> o.defaultNonDestructive);
   }
   private boolean defaultNonDestructive;

   {
      metaJson.add(Boolean.class, "defaultExclusiveQueue", (o, p) -> o.defaultExclusiveQueue = p, o -> o.defaultExclusiveQueue);
   }
   private boolean defaultExclusiveQueue;

   {
      metaJson.add(Boolean.class, "defaultGroupRebalance", (o, p) -> o.defaultGroupRebalance = p, o -> o.defaultGroupRebalance);
   }
   private boolean defaultGroupRebalance;

   {
      metaJson.add(Integer.class, "defaultGroupBuckets", (o, p) -> o.defaultGroupBuckets = p, o -> o.defaultGroupBuckets);
   }
   private int defaultGroupBuckets;

   {
      metaJson.add(String.class, "defaultGroupFirstKey", (o, p) -> o.defaultGroupFirstKey = p, o -> o.defaultGroupFirstKey);
   }
   private String defaultGroupFirstKey;

   {
      metaJson.add(Integer.class, "defaultMaxConsumers", (o, p) -> o.defaultMaxConsumers = p, o -> o.defaultMaxConsumers);
   }
   private int defaultMaxConsumers;

   {
      metaJson.add(Boolean.class, "defaultPurgeOnNoConsumers", (o, p) -> o.defaultPurgeOnNoConsumers = p, o -> o.defaultPurgeOnNoConsumers);
   }
   private boolean defaultPurgeOnNoConsumers;

   {
      metaJson.add(Integer.class, "defaultConsumersBeforeDispatch", (o, p) -> o.defaultConsumersBeforeDispatch = p, o -> o.defaultConsumersBeforeDispatch);
   }
   private int defaultConsumersBeforeDispatch;

   {
      metaJson.add(Long.class, "defaultDelayBeforeDispatch", (o, p) -> o.defaultDelayBeforeDispatch = p, o -> o.defaultDelayBeforeDispatch);
   }
   private long defaultDelayBeforeDispatch;

   {
      metaJson.add(String.class, "defaultQueueRoutingType", (o, p) -> o.defaultQueueRoutingType = p, o -> o.defaultQueueRoutingType);
   }
   private String defaultQueueRoutingType;

   {
      metaJson.add(String.class, "defaultAddressRoutingType", (o, p) -> o.defaultAddressRoutingType = p, o -> o.defaultAddressRoutingType);
   }
   private String defaultAddressRoutingType;

   {
      metaJson.add(Integer.class, "defaultConsumerWindowSize", (o, p) -> o.defaultConsumerWindowSize = p, o -> o.defaultConsumerWindowSize);
   }
   private int defaultConsumerWindowSize;

   {
      metaJson.add(Long.class, "defaultRingSize", (o, p) -> o.defaultRingSize = p, o -> o.defaultRingSize);
   }
   private long defaultRingSize;

   {
      metaJson.add(Boolean.class, "autoDeleteCreatedQueues", (o, p) -> o.autoDeleteCreatedQueues = p, o -> o.autoDeleteCreatedQueues);
   }
   private boolean autoDeleteCreatedQueues;

   {
      metaJson.add(Long.class, "autoDeleteQueuesDelay", (o, p) -> o.autoDeleteQueuesDelay = p, o -> o.autoDeleteQueuesDelay);
   }
   private long autoDeleteQueuesDelay;

   {
      metaJson.add(Long.class, "autoDeleteQueuesMessageCount", (o, p) -> o.autoDeleteQueuesMessageCount = p, o -> o.autoDeleteQueuesMessageCount);
   }
   private long autoDeleteQueuesMessageCount;

   {
      metaJson.add(Long.class, "autoDeleteAddressesDelay", (o, p) -> o.autoDeleteAddressesDelay = p, o -> o.autoDeleteAddressesDelay);
   }
   private long autoDeleteAddressesDelay;

   {
      metaJson.add(Double.class, "redeliveryCollisionAvoidanceFactor", (o, p) -> o.redeliveryCollisionAvoidanceFactor = p, o -> o.redeliveryCollisionAvoidanceFactor);
   }
   private double redeliveryCollisionAvoidanceFactor;

   {
      metaJson.add(Long.class, "retroactiveMessageCount", (o, p) -> o.retroactiveMessageCount = p, o -> o.retroactiveMessageCount);
   }
   private long retroactiveMessageCount;

   {
      metaJson.add(Boolean.class, "autoCreateDeadLetterResources", (o, p) -> o.autoCreateDeadLetterResources = p, o -> o.autoCreateDeadLetterResources);
   }
   private boolean autoCreateDeadLetterResources;

   {
      metaJson.add(String.class, "deadLetterQueuePrefix", (o, p) -> o.deadLetterQueuePrefix = p, o -> o.deadLetterQueuePrefix);
   }
   private String deadLetterQueuePrefix;

   {
      metaJson.add(String.class, "deadLetterQueueSuffix", (o, p) -> o.deadLetterQueueSuffix = p, o -> o.deadLetterQueueSuffix);
   }
   private String deadLetterQueueSuffix;

   {
      metaJson.add(Boolean.class, "autoCreateExpiryResources", (o, p) -> o.autoCreateExpiryResources = p, o -> o.autoCreateExpiryResources);
   }
   private boolean autoCreateExpiryResources;


   {
      metaJson.add(String.class, "expiryQueuePrefix", (o, p) -> o.expiryQueuePrefix = p, o -> o.expiryQueuePrefix);
   }
   private String expiryQueuePrefix;

   {
      metaJson.add(String.class, "expiryQueueSuffix", (o, p) -> o.expiryQueueSuffix = p, o -> o.expiryQueueSuffix);
   }
   private String expiryQueueSuffix;

   {
      metaJson.add(Long.class, "expiryDelay", (o, p) -> o.expiryDelay = p, o -> o.expiryDelay);
   }
   private long expiryDelay;

   {
      metaJson.add(Long.class, "minExpiryDelay", (o, p) -> o.minExpiryDelay = p, o -> o.minExpiryDelay);
   }
   private long minExpiryDelay;

   {
      metaJson.add(Long.class, "maxExpiryDelay", (o, p) -> o.maxExpiryDelay = p, o -> o.maxExpiryDelay);
   }
   private long maxExpiryDelay;

   {
      metaJson.add(Boolean.class, "enableMetrics", (o, p) -> o.enableMetrics = p, o -> o.enableMetrics);
   }
   private boolean enableMetrics;


   public static AddressSettingsInfo fromJSON(final String jsonString) {
      AddressSettingsInfo newInfo = new AddressSettingsInfo();
      metaJson.fromJSON(newInfo, jsonString);
      return newInfo;
   }

   public AddressSettingsInfo() {
   }

   public AddressSettingsInfo(String addressFullMessagePolicy,
                              long maxSizeBytes,
                              int pageSizeBytes,
                              int pageCacheMaxSize,
                              int maxDeliveryAttempts,
                              long redeliveryDelay,
                              double redeliveryMultiplier,
                              long maxRedeliveryDelay,
                              String deadLetterAddress,
                              String expiryAddress,
                              boolean lastValueQueue,
                              long redistributionDelay,
                              boolean sendToDLAOnNoRoute,
                              long slowConsumerThreshold,
                              long slowConsumerCheckPeriod,
                              String slowConsumerPolicy,
                              boolean autoCreateJmsQueues,
                              boolean autoCreateJmsTopics,
                              boolean autoDeleteJmsQueues,
                              boolean autoDeleteJmsTopics,
                              boolean autoCreateQueues,
                              boolean autoDeleteQueues,
                              boolean autoCreateAddresses,
                              boolean autoDeleteAddresses,
                              String configDeleteQueues,
                              String configDeleteAddresses,
                              long maxSizeBytesRejectThreshold,
                              String defaultLastValueKey,
                              boolean defaultNonDestructive,
                              boolean defaultExclusiveQueue,
                              boolean defaultGroupRebalance,
                              int defaultGroupBuckets,
                              String defaultGroupFirstKey,
                              int defaultMaxConsumers,
                              boolean defaultPurgeOnNoConsumers,
                              int defaultConsumersBeforeDispatch,
                              long defaultDelayBeforeDispatch,
                              String defaultQueueRoutingType,
                              String defaultAddressRoutingType,
                              int defaultConsumerWindowSize,
                              long defaultRingSize,
                              boolean autoDeleteCreatedQueues,
                              long autoDeleteQueuesDelay,
                              long autoDeleteQueuesMessageCount,
                              long autoDeleteAddressesDelay,
                              double redeliveryCollisionAvoidanceFactor,
                              long retroactiveMessageCount,
                              boolean autoCreateDeadLetterResources,
                              String deadLetterQueuePrefix,
                              String deadLetterQueueSuffix,
                              boolean autoCreateExpiryResources,
                              String expiryQueuePrefix,
                              String expiryQueueSuffix,
                              long expiryDelay,
                              long minExpiryDelay,
                              long maxExpiryDelay,
                              boolean enableMetrics) {
      this.addressFullMessagePolicy = addressFullMessagePolicy;
      this.maxSizeBytes = maxSizeBytes;
      this.pageSizeBytes = pageSizeBytes;
      this.pageCacheMaxSize = pageCacheMaxSize;
      this.maxDeliveryAttempts = maxDeliveryAttempts;
      this.redeliveryDelay = redeliveryDelay;
      this.redeliveryMultiplier = redeliveryMultiplier;
      this.maxRedeliveryDelay = maxRedeliveryDelay;
      this.deadLetterAddress = deadLetterAddress;
      this.expiryAddress = expiryAddress;
      this.lastValueQueue = lastValueQueue;
      this.redistributionDelay = redistributionDelay;
      this.sendToDLAOnNoRoute = sendToDLAOnNoRoute;
      this.slowConsumerThreshold = slowConsumerThreshold;
      this.slowConsumerCheckPeriod = slowConsumerCheckPeriod;
      this.slowConsumerPolicy = slowConsumerPolicy;
      this.autoCreateJmsQueues = autoCreateJmsQueues;
      this.autoDeleteJmsQueues = autoDeleteJmsQueues;
      this.autoCreateJmsTopics = autoCreateJmsTopics;
      this.autoDeleteJmsTopics = autoDeleteJmsTopics;
      this.autoCreateQueues = autoCreateQueues;
      this.autoDeleteQueues = autoDeleteQueues;
      this.autoCreateAddresses = autoCreateAddresses;
      this.autoDeleteAddresses = autoDeleteAddresses;
      this.configDeleteQueues = configDeleteQueues;
      this.configDeleteAddresses = configDeleteAddresses;
      this.maxSizeBytesRejectThreshold = maxSizeBytesRejectThreshold;
      this.defaultLastValueKey = defaultLastValueKey;
      this.defaultNonDestructive = defaultNonDestructive;
      this.defaultExclusiveQueue = defaultExclusiveQueue;
      this.defaultGroupRebalance = defaultGroupRebalance;
      this.defaultGroupBuckets = defaultGroupBuckets;
      this.defaultGroupFirstKey = defaultGroupFirstKey;
      this.defaultMaxConsumers = defaultMaxConsumers;
      this.defaultPurgeOnNoConsumers = defaultPurgeOnNoConsumers;
      this.defaultConsumersBeforeDispatch = defaultConsumersBeforeDispatch;
      this.defaultDelayBeforeDispatch = defaultDelayBeforeDispatch;
      this.defaultQueueRoutingType = defaultQueueRoutingType;
      this.defaultAddressRoutingType = defaultAddressRoutingType;
      this.defaultConsumerWindowSize = defaultConsumerWindowSize;
      this.defaultRingSize = defaultRingSize;
      this.autoDeleteCreatedQueues = autoDeleteCreatedQueues;
      this.autoDeleteQueuesDelay = autoDeleteQueuesDelay;
      this.autoDeleteQueuesMessageCount = autoDeleteQueuesMessageCount;
      this.autoDeleteAddressesDelay = autoDeleteAddressesDelay;
      this.redeliveryCollisionAvoidanceFactor = redeliveryCollisionAvoidanceFactor;
      this.retroactiveMessageCount = retroactiveMessageCount;
      this.autoCreateDeadLetterResources = autoCreateDeadLetterResources;
      this.deadLetterQueuePrefix = deadLetterQueuePrefix;
      this.deadLetterQueueSuffix = deadLetterQueueSuffix;
      this.autoCreateExpiryResources = autoCreateExpiryResources;
      this.expiryQueuePrefix = expiryQueuePrefix;
      this.expiryQueueSuffix = expiryQueueSuffix;
      this.expiryDelay = expiryDelay;
      this.minExpiryDelay = minExpiryDelay;
      this.maxExpiryDelay = maxExpiryDelay;
      this.enableMetrics = enableMetrics;
   }

   public int getPageCacheMaxSize() {
      return pageCacheMaxSize;
   }

   public void setPageCacheMaxSize(int pageCacheMaxSize) {
      this.pageCacheMaxSize = pageCacheMaxSize;
   }

   public String getAddressFullMessagePolicy() {
      return addressFullMessagePolicy;
   }

   public long getMaxSizeBytes() {
      return maxSizeBytes;
   }

   public int getPageSizeBytes() {
      return pageSizeBytes;
   }

   public int getMaxDeliveryAttempts() {
      return maxDeliveryAttempts;
   }

   public long getRedeliveryDelay() {
      return redeliveryDelay;
   }

   public String getDeadLetterAddress() {
      return deadLetterAddress;
   }

   public String getExpiryAddress() {
      return expiryAddress;
   }

   public boolean isLastValueQueue() {
      return lastValueQueue;
   }

   public long getRedistributionDelay() {
      return redistributionDelay;
   }

   public boolean isSendToDLAOnNoRoute() {
      return sendToDLAOnNoRoute;
   }

   public double getRedeliveryMultiplier() {
      return redeliveryMultiplier;
   }

   public long getMaxRedeliveryDelay() {
      return maxRedeliveryDelay;
   }

   public long getSlowConsumerThreshold() {
      return slowConsumerThreshold;
   }

   public long getSlowConsumerCheckPeriod() {
      return slowConsumerCheckPeriod;
   }

   public String getSlowConsumerPolicy() {
      return slowConsumerPolicy;
   }

   @Deprecated
   public boolean isAutoCreateJmsQueues() {
      return autoCreateJmsQueues;
   }

   @Deprecated
   public boolean isAutoDeleteJmsQueues() {
      return autoDeleteJmsQueues;
   }

   @Deprecated
   public boolean isAutoCreateJmsTopics() {
      return autoCreateJmsTopics;
   }

   @Deprecated
   public boolean isAutoDeleteJmsTopics() {
      return autoDeleteJmsTopics;
   }

   public boolean isAutoCreateQueues() {
      return autoCreateQueues;
   }

   public boolean isAutoDeleteQueues() {
      return autoDeleteQueues;
   }

   public boolean isAutoCreateAddresses() {
      return autoCreateAddresses;
   }

   public boolean isAutoDeleteAddresses() {
      return autoDeleteAddresses;
   }

   public String getConfigDeleteQueues() {
      return configDeleteQueues;
   }

   public String getConfigDeleteAddresses() {
      return configDeleteAddresses;
   }

   public long getMaxSizeBytesRejectThreshold() {
      return maxSizeBytesRejectThreshold;
   }

   public String getDefaultLastValueKey() {
      return defaultLastValueKey;
   }

   public boolean isDefaultNonDestructive() {
      return defaultNonDestructive;
   }

   public boolean isDefaultExclusiveQueue() {
      return defaultExclusiveQueue;
   }

   public boolean isDefaultGroupRebalance() {
      return defaultGroupRebalance;
   }

   public int getDefaultGroupBuckets() {
      return defaultGroupBuckets;
   }

   public String getDefaultGroupFirstKey() {
      return defaultGroupFirstKey;
   }

   public int getDefaultMaxConsumers() {
      return defaultMaxConsumers;
   }

   public boolean isDefaultPurgeOnNoConsumers() {
      return defaultPurgeOnNoConsumers;
   }

   public int getDefaultConsumersBeforeDispatch() {
      return defaultConsumersBeforeDispatch;
   }

   public long getDefaultDelayBeforeDispatch() {
      return defaultDelayBeforeDispatch;
   }

   public String getDefaultQueueRoutingType() {
      return defaultQueueRoutingType;
   }

   public String getDefaultAddressRoutingType() {
      return defaultAddressRoutingType;
   }

   public int getDefaultConsumerWindowSize() {
      return defaultConsumerWindowSize;
   }

   public long getDefaultRingSize() {
      return defaultRingSize;
   }

   public boolean isAutoDeleteCreatedQueues() {
      return autoDeleteCreatedQueues;
   }

   public long getAutoDeleteQueuesDelay() {
      return autoDeleteQueuesDelay;
   }

   public long getAutoDeleteQueuesMessageCount() {
      return autoDeleteQueuesMessageCount;
   }

   public long getAutoDeleteAddressesDelay() {
      return autoDeleteAddressesDelay;
   }

   public double getRedeliveryCollisionAvoidanceFactor() {
      return redeliveryCollisionAvoidanceFactor;
   }

   public long getRetroactiveMessageCount() {
      return retroactiveMessageCount;
   }

   public boolean isAutoCreateDeadLetterResources() {
      return autoCreateDeadLetterResources;
   }

   public String getDeadLetterQueuePrefix() {
      return deadLetterQueuePrefix;
   }

   public String getDeadLetterQueueSuffix() {
      return deadLetterQueueSuffix;
   }

   public boolean isAutoCreateExpiryResources() {
      return autoCreateExpiryResources;
   }

   public String getExpiryQueuePrefix() {
      return expiryQueuePrefix;
   }

   public String getExpiryQueueSuffix() {
      return expiryQueueSuffix;
   }

   public long getExpiryDelay() {
      return expiryDelay;
   }

   public long getMinExpiryDelay() {
      return minExpiryDelay;
   }

   public long getMaxExpiryDelay() {
      return maxExpiryDelay;
   }

   public boolean isEnableMetrics() {
      return enableMetrics;
   }
}

