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
package org.apache.activemq.api.core;

import org.apache.activemq.core.protocol.core.Packet;
import org.apache.activemq.spi.core.protocol.RemotingConnection;

/**
 * This is class is a simple way to intercepting calls on ActiveMQ client and servers.
 * <p>
 * To add an interceptor to ActiveMQ server, you have to modify the server configuration file
 * {@literal activemq-configuration.xml}.<br>
 * To add it to a client, use {@link org.apache.activemq.api.core.client.ServerLocator#addIncomingInterceptor(Interceptor)}
 */
public interface Interceptor
{
   /**
    * Intercepts a packet which is received before it is sent to the channel
    *
    * @param packet     the packet being received
    * @param connection the connection the packet was received on
    * @return {@code true} to process the next interceptor and handle the packet,
    * {@code false} to abort processing of the packet
    * @throws ActiveMQException
    */
   boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException;
}
