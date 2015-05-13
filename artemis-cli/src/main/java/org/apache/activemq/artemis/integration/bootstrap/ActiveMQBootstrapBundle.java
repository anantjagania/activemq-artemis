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
package org.apache.activemq.artemis.integration.bootstrap;


import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Logger Code 10
 *
 * each message id must be 6 digits long starting with 10, the 3rd digit should be 9
 *
 * so 109000 to 109999
 */
@MessageBundle(projectCode = "AMQ")
public interface ActiveMQBootstrapBundle
{
   ActiveMQBootstrapBundle BUNDLE = Messages.getBundle(ActiveMQBootstrapBundle.class);

   @Message(id = 109000, value =  "Directory does not exist: {0}", format = Message.Format.MESSAGE_FORMAT)
   IllegalStateException directoryDoesNotExist(String directory);
}
