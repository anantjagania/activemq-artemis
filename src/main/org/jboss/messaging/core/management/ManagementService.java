/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.messaging.core.management;

import java.util.Set;

import javax.management.ObjectName;

import org.jboss.messaging.core.client.management.impl.ManagementHelper;
import org.jboss.messaging.core.config.Configuration;
import org.jboss.messaging.core.messagecounter.MessageCounterManager;
import org.jboss.messaging.core.persistence.StorageManager;
import org.jboss.messaging.core.postoffice.PostOffice;
import org.jboss.messaging.core.remoting.RemotingService;
import org.jboss.messaging.core.security.Role;
import org.jboss.messaging.core.server.MessagingServer;
import org.jboss.messaging.core.server.Queue;
import org.jboss.messaging.core.server.ServerMessage;
import org.jboss.messaging.core.settings.HierarchicalRepository;
import org.jboss.messaging.core.settings.impl.QueueSettings;
import org.jboss.messaging.core.transaction.ResourceManager;
import org.jboss.messaging.util.SimpleString;
import org.jboss.messaging.util.TypedProperties;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 * 
 */
public interface ManagementService
{
   MessageCounterManager getMessageCounterManager();

   MessagingServerControlMBean registerServer(PostOffice postOffice,
                                              StorageManager storageManager,
                                              Configuration configuration,                                            
                                              HierarchicalRepository<QueueSettings> queueSettingsRepository,
                                              HierarchicalRepository<Set<Role>> securityRepository,
                                              ResourceManager resourceManager,
                                              RemotingService remotingService,
                                              MessagingServer messagingServer) throws Exception;

   void unregisterServer() throws Exception;

   void registerAddress(SimpleString address) throws Exception;

   void unregisterAddress(SimpleString address) throws Exception;

   void registerQueue(Queue queue, SimpleString address, StorageManager storageManager) throws Exception;

   void unregisterQueue(SimpleString name, SimpleString address) throws Exception;

   void registerResource(ObjectName objectName, Object resource) throws Exception;

   void unregisterResource(ObjectName objectName) throws Exception;

   public Object getResource(ObjectName objectName);

   void handleMessage(ServerMessage message);

   void sendNotification(NotificationType type, String message) throws Exception;

   /** 
    * the message corresponding to a notification will always contain the properties:
    * <ul>
    *   <li><code>ManagementHelper.HDR_NOTIFICATION_TYPE</code> - the type of notification (SimpleString)</li>
    *   <li><code>ManagementHelper.HDR_NOTIFICATION_MESSAGE</code> - a message contextual to the notification (SimpleString)</li>
    *   <li><code>ManagementHelper.HDR_NOTIFICATION_TIMESTAMP</code> - the timestamp when the notification occured (long)</li>
    * </ul>
    * 
    * in addition to the properties defined in <code>props</code>
    * 
    * @see ManagementHelper
    */
   void sendNotification(NotificationType type, String message, TypedProperties props) throws Exception;
}
