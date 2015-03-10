/**
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
package org.apache.activemq.core.transaction;

import java.util.List;
import java.util.Map;

import javax.transaction.xa.Xid;

import org.apache.activemq.core.server.ActiveMQComponent;

/**
 * A ResourceManager
 */
public interface ResourceManager extends ActiveMQComponent
{
   boolean putTransaction(Xid xid, Transaction tx);

   Transaction getTransaction(Xid xid);

   Transaction removeTransaction(Xid xid);

   int getTimeoutSeconds();

   List<Xid> getPreparedTransactions();

   Map<Xid, Long> getPreparedTransactionsWithCreationTime();

   void putHeuristicCompletion(long txid, Xid xid, boolean b);

   long removeHeuristicCompletion(Xid xid);

   List<Xid> getHeuristicCommittedTransactions();

   List<Xid> getHeuristicRolledbackTransactions();

}
