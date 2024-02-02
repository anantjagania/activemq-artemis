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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import io.netty.util.collection.LongObjectHashMap;
import org.apache.activemq.artemis.core.io.IOCriticalErrorListener;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.persistence.Persister;

public class JournalHashMapProvider<K, V, C> {

   final Journal journal;
   final Persister<JournalHashMap.MapRecord<K, V>> persister;
   final LongObjectHashMap<JournalHashMap<K, V, C>> journalMaps = new LongObjectHashMap<>();
   final LongSupplier idSupplier;
   final byte recordType;
   final IOCriticalErrorListener ioExceptionListener;
   final Supplier<IOCompletion> ioCompletionSupplier;
   final LongFunction<C> contextProvider;

   public JournalHashMapProvider(LongSupplier idSupplier, Journal journal, AbstractHashMapPersister persister, byte recordType, Supplier<IOCompletion> ioCompletionSupplier, LongFunction<C> contextProvider, IOCriticalErrorListener ioExceptionListener) {
      this.idSupplier = idSupplier;
      this.persister = persister;
      this.journal = journal;
      this.recordType = recordType;
      this.ioExceptionListener = ioExceptionListener;
      this.contextProvider = contextProvider;
      this.ioCompletionSupplier = ioCompletionSupplier;
   }

   public List<JournalHashMap<K, V, C>> getMaps() {
      ArrayList<JournalHashMap<K, V, C>> maps = new ArrayList<>();
      journalMaps.values().forEach(maps::add);
      return maps;
   }

   public void clear() {
      journalMaps.clear();
   }

   public void reload(RecordInfo recordInfo) {
      JournalHashMap.MapRecord<K, V> mapRecord = persister.decode(recordInfo.wrapData(), null, null);
      getMap(mapRecord.collectionID, null).reload(mapRecord);
   }

   public Iterator<JournalHashMap<K, V, C>> iterMaps() {
      return journalMaps.values().iterator();
   }

   public JournalHashMap<K, V, C> getMap(long collectionID, C context) {
      JournalHashMap<K, V, C> journalHashMap = journalMaps.get(collectionID);
      if (journalHashMap == null) {
         journalHashMap = new JournalHashMap<>(collectionID, journal, idSupplier, persister, recordType, ioCompletionSupplier, contextProvider, ioExceptionListener).setContext(context);
         journalMaps.put(collectionID, journalHashMap);
      }
      return journalHashMap;
   }

   public JournalHashMap<K, V, C> getMap(long collectionID) {
      return getMap(collectionID, null);
   }
}
