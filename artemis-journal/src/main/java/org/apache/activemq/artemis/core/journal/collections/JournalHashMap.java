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

package org.apache.activemq.artemis.core.journal.collections;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.apache.activemq.artemis.core.io.IOCriticalErrorListener;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.persistence.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalHashMap<K, V, C> implements Map<K, V> {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   public static class MapRecord<K, V> implements Entry<K, V> {
      final long collectionID;
      long id;
      K key;
      V value;

      MapRecord(long collectionID, long id, K key, V value) {
         this.collectionID = collectionID;
         this.id = id;
         this.key = key;
         this.value = value;
      }

      @Override
      public K getKey() {
         return key;
      }

      @Override
      public V getValue() {
         return value;
      }

      @Override
      public V setValue(V value) {
         V oldValue = this.value;
         this.value = value;
         return oldValue;
      }

      @Override
      public String toString() {
         return "MapRecord{" + "collectionID=" + collectionID + ", id=" + id + ", key=" + key + ", value=" + value + '}';
      }
   }

   public JournalHashMap(long collectionId, Journal journal, LongSupplier idGenerator, Persister<MapRecord<K, V>> persister, byte recordType, Supplier<IOCompletion> completionSupplier, LongFunction<C> contextProvider, IOCriticalErrorListener ioExceptionListener) {
      this.collectionId = collectionId;
      this.journal = journal;
      this.idGenerator = idGenerator;
      this.persister = persister;
      this.recordType = recordType;
      this.exceptionListener = ioExceptionListener;
      this.completionSupplier = completionSupplier;
      this.contextProvider = contextProvider;
   }

   C context;

   LongFunction<C> contextProvider;

   private final Persister<MapRecord<K, V>> persister;

   private final Journal journal;

   private final long collectionId;

   private final byte recordType;

   private final LongSupplier idGenerator;

   private final Supplier<IOCompletion> completionSupplier;

   private final IOCriticalErrorListener exceptionListener;

   private final Map<K, MapRecord<K, V>> map = new HashMap<>();

   public long getCollectionId() {
      return collectionId;
   }

   @Override
   public synchronized int size() {
      return map.size();
   }

   public C getContext() {
      if (context == null && contextProvider != null) {
         context = contextProvider.apply(this.collectionId);
      }
      return context;
   }

   public JournalHashMap<K, V, C> setContext(C context) {
      this.context = context;
      return this;
   }

   @Override
   public synchronized boolean isEmpty() {
      return map.isEmpty();
   }

   @Override
   public synchronized boolean containsKey(Object key) {
      return map.containsKey(key);
   }

   @Override
   public synchronized boolean containsValue(Object value) {
      for (Entry<K, MapRecord<K, V>> entry : map.entrySet()) {
         if (value.equals(entry.getValue().value)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public synchronized V get(Object key) {
      MapRecord<K, V> reccord = map.get(key);
      if (reccord == null) {
         return null;
      } else {
         return reccord.value;
      }
   }

   /** This is to be called from a single thread during reload, no need to be synchronized */
   public void reload(MapRecord<K, V> reloadValue) {
      map.put(reloadValue.getKey(), reloadValue);
   }

   @Override
   public synchronized V put(K key, V value) {
      logger.debug("adding {} = {}", key, value);
      long id = idGenerator.getAsLong();
      MapRecord<K, V> record = new MapRecord(collectionId, id, key, value);
      store(key, record);
      MapRecord<K, V> oldRecord = map.put(key, record);

      if (oldRecord != null) {
         removed(oldRecord);
         return oldRecord.value;
      } else {
         return null;
      }

   }

   private synchronized void store(K key, MapRecord<K, V> record) {
      try {
         IOCompletion callback = null;
         if (completionSupplier != null) {
            callback = completionSupplier.get();
         }

         if (callback == null) {
            journal.appendAddRecord(record.id, recordType, persister, record, false);
         } else {
            journal.appendAddRecord(record.id, recordType, persister, record, true, callback);
         }
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
         exceptionListener.onIOException(e, e.getMessage(), null);
      }
   }

   // callers must be synchronized
   private void removed(MapRecord reccord) {
      try {
         journal.appendDeleteRecord(reccord.id, false);
      } catch (Exception e) {
         exceptionListener.onIOException(e, e.getMessage(), null);
      }
   }

   // callers must be synchronized
   private void removed(MapRecord reccord, long txid) {
      try {
         journal.appendDeleteRecordTransactional(txid, reccord.id);
      } catch (Exception e) {
         exceptionListener.onIOException(e, e.getMessage(), null);
      }
   }

   @Override
   public synchronized V remove(Object key) {
      MapRecord<K, V> record = map.remove(key);
      this.removed(record);
      return record.value;
   }

   /** This method will remove the element from the HashMap immediately however the record is still part of a transaction.
    *  This is not playing with rollbacks. So a rollback on the transaction wouldn't place the elements back.
    *  This is intended to make sure the operation would be atomic in case of a failure, while an appendRollback is not expected. */
   public synchronized V remove(Object key, long transactionID) {
      MapRecord<K, V> record = map.remove(key);
      this.removed(record, transactionID);
      return record.value;
   }

   @Override
   public synchronized void putAll(Map<? extends K, ? extends V> m) {
      m.forEach(this::put);
   }

   @Override
   public synchronized void clear() {
      map.values().forEach(v -> remove(v));
      map.clear();
   }

   @Override
   public synchronized Set<K> keySet() {
      HashSet<K> keys = new HashSet(map.size());
      map.values().forEach(v -> keys.add(v.key));
      return keys;
   }

   @Override
   public synchronized Collection<V> values() {
      ArrayList values = new ArrayList(map.size());
      map.values().forEach(v -> values.add(v.value));
      return values;
   }

   @Override
   public synchronized Set<Entry<K, V>> entrySet() {
      HashSet<Entry<K, V>> values = new HashSet<>();
      map.values().forEach(values::add);
      return values;
   }

   @Override
   public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
      Objects.requireNonNull(action);
      map.forEach((a, b) -> action.accept(b.key, b.value));
   }

   public HashMap<K, V> copy() {
      HashMap<K, V> theCopy = new HashMap();
      forEach(theCopy::put);
      return theCopy;
   }
}
