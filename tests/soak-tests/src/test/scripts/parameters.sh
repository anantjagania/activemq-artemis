#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# this script contains a suggest set of variables to run the soak tests.

## Generic variable:
# Some tests will support saving the producer's state before consumption. If you set this variable these tests will hold a zip file and recover it approprieatedly.
#export TEST_ZIP_LOCATION=~/zipTest/

#HorizontalPagingTest

export TEST_HORIZONTAL_TEST_ENABLED=true
export TEST_HORIZONTAL_SERVER_START_TIMEOUT=300000
export TEST_HORIZONTAL_TIMEOUT_MINUTES=120
export TEST_HORIZONTAL_PROTOCOL_LIST=OPENWIRE,CORE,AMQP

export TEST_HORIZONTAL_CORE_DESTINATIONS=20
export TEST_HORIZONTAL_CORE_MESSAGES=5000
export TEST_HORIZONTAL_CORE_COMMIT_INTERVAL=1000
export TEST_HORIZONTAL_CORE_RECEIVE_COMMIT_INTERVAL=0
export TEST_HORIZONTAL_CORE_MESSAGE_SIZE=20000
export TEST_HORIZONTAL_CORE_PARALLEL_SENDS=20

export TEST_HORIZONTAL_AMQP_DESTINATIONS=20
export TEST_HORIZONTAL_AMQP_MESSAGES=1000
export TEST_HORIZONTAL_AMQP_COMMIT_INTERVAL=100
export TEST_HORIZONTAL_AMQP_RECEIVE_COMMIT_INTERVAL=0
export TEST_HORIZONTAL_AMQP_MESSAGE_SIZE=20000
export TEST_HORIZONTAL_AMQP_PARALLEL_SENDS=10

export TEST_HORIZONTAL_OPENWIRE_DESTINATIONS=20
export TEST_HORIZONTAL_OPENWIRE_MESSAGES=1000
export TEST_HORIZONTAL_OPENWIRE_COMMIT_INTERVAL=100
export TEST_HORIZONTAL_OPENWIRE_RECEIVE_COMMIT_INTERVAL=0
export TEST_HORIZONTAL_OPENWIRE_MESSAGE_SIZE=20000
export TEST_HORIZONTAL_OPENWIRE_PARALLEL_SENDS=10

export TEST_FLOW_SERVER_START_TIMEOUT=300000
export TEST_FLOW_TIMEOUT_MINUTES=120


# FlowControlPagingTest
export TEST_FLOW_PROTOCOL_LIST=CORE,AMQP,OPENWIRE
export TEST_FLOW_PRINT_INTERVAL=100

export TEST_FLOW_OPENWIRE_MESSAGES=10000
export TEST_FLOW_OPENWIRE_COMMIT_INTERVAL=1000
export TEST_FLOW_OPENWIRE_RECEIVE_COMMIT_INTERVAL=10
export TEST_FLOW_OPENWIRE_MESSAGE_SIZE=60000

export TEST_FLOW_CORE_MESSAGES=10000
export TEST_FLOW_CORE_COMMIT_INTERVAL=1000
export TEST_FLOW_CORE_RECEIVE_COMMIT_INTERVAL=10
export TEST_FLOW_CORE_MESSAGE_SIZE=30000

export TEST_FLOW_AMQP_MESSAGES=10000
export TEST_FLOW_AMQP_COMMIT_INTERVAL=1000
export TEST_FLOW_AMQP_RECEIVE_COMMIT_INTERVAL=10
export TEST_FLOW_AMQP_MESSAGE_SIZE=30000


# SubscriptionPagingTest
export TEST_SUBSCRIPTION_PROTOCOL_LIST=CORE

export TEST_SUBSCRIPTION_SERVER_START_TIMEOUT=300000
export TEST_SUBSCRIPTION_TIMEOUT_MINUTES=120
export TEST_SUBSCRIPTION_PRINT_INTERVAL=100
export TEST_SUBSCRIPTION_SLOW_SUBSCRIPTIONS=5


export TEST_SUBSCRIPTION_CORE_MESSAGES=10000
export TEST_SUBSCRIPTION_CORE_COMMIT_INTERVAL=1000
export TEST_SUBSCRIPTION_CORE_RECEIVE_COMMIT_INTERVAL=0
export TEST_SUBSCRIPTION_CORE_MESSAGE_SIZE=30000
export TEST_SUBSCRIPTION_SLEEP_SLOW=1000


#OWLeakTest
export TEST_OW_LEAK_TEST_ENABLED=true
export TEST_OW_LEAK_PROTOCOL_LIST=OPENWIRE
export TEST_OW_LEAK_OPENWIRE_NUMBER_OF_MESSAGES=15
export TEST_OW_LEAK_OPENWIRE_PRODUCERS=1
export TEST_OW_LEAK_OPENWIRE_MESSAGE_SIZE=2000000
export TEST_OW_LEAK_PRINT_INTERVAL=1

#DatabasePagingTest
export TEST_PGDB_DB_LIST=derby
# use this to allow all the databases
#export TEST_PGDB_DB_LIST=derby,postgres,mysql
export TEST_PGDB_MAX_MESSAGES=500
export TEST_PGDB_MESSAGE_SIZE=100
export TEST_PGDB_COMMIT_INTERVAL=50

#ClientFailureSoakTest
export TEST_CLIENT_FAILURE_TEST_ENABLED=true
export TEST_CLIENT_FAILURE_PROTOCOL_LIST=AMQP,CORE,OPENWIRE

export TEST_CLIENT_FAILURE_AMQP_USE_LARGE_MESSAGE=TRUE
export TEST_CLIENT_FAILURE_AMQP_THREADS_PER_VM=20
export TEST_CLIENT_FAILURE_AMQP_CLIENT_CONSUMERS_PER_THREAD=20
export TEST_CLIENT_FAILURE_AMQP_TEST_REPEATS=1
export TEST_CLIENT_FAILURE_AMQP_TOTAL_ITERATION=2
export TEST_CLIENT_FAILURE_AMQP_NUMBER_OF_VMS=5
export TEST_CLIENT_FAILURE_AMQP_NUMBER_OF_MESSAGES=20000
export TEST_CLIENT_FAILURE_AMQP_MEMORY_CLIENT=-Xmx256m

export TEST_CLIENT_FAILURE_CORE_USE_LARGE_MESSAGE=TRUE
export TEST_CLIENT_FAILURE_CORE_THREADS_PER_VM=20
export TEST_CLIENT_FAILURE_CORE_CLIENT_CONSUMERS_PER_THREAD=20
export TEST_CLIENT_FAILURE_CORE_TEST_REPEATS=1
export TEST_CLIENT_FAILURE_CORE_TOTAL_ITERATION=2
export TEST_CLIENT_FAILURE_CORE_NUMBER_OF_VMS=5
export TEST_CLIENT_FAILURE_CORE_NUMBER_OF_MESSAGES=20000
export TEST_CLIENT_FAILURE_CORE_MEMORY_CLIENT=-Xmx256m

export TEST_CLIENT_FAILURE_OPENWIRE_USE_LARGE_MESSAGE=TRUE
export TEST_CLIENT_FAILURE_OPENWIRE_THREADS_PER_VM=20
export TEST_CLIENT_FAILURE_OPENWIRE_CLIENT_CONSUMERS_PER_THREAD=20
export TEST_CLIENT_FAILURE_OPENWIRE_TEST_REPEATS=1
export TEST_CLIENT_FAILURE_OPENWIRE_TOTAL_ITERATION=2
export TEST_CLIENT_FAILURE_OPENWIRE_NUMBER_OF_VMS=5
export TEST_CLIENT_FAILURE_OPENWIRE_NUMBER_OF_MESSAGES=20000
export TEST_CLIENT_FAILURE_OPENWIRE_MEMORY_CLIENT=-Xmx256m

#clusterNotificationsContinuityTest
export TEST_CLUSTER_NOTIFICATIONS_CONTINUITY_TEST_ENABLED=true
export TEST_CLUSTER_NOTIFICATIONS_CONTINUITY_NUMBER_OF_SERVERS=3
export TEST_CLUSTER_NOTIFICATIONS_CONTINUITY_NUMBER_OF_QUEUES=200
export TEST_CLUSTER_NOTIFICATIONS_CONTINUITY_NUMBER_OF_WORKERS=10

export TEST_SINGLE_MIRROR_SOAK_TRACE_LOGS=true
export TEST_SINGLE_MIRROR_SOAK_NUMBER_MESSAGES=33125
export TEST_SINGLE_MIRROR_SOAK_NUMBER_MESSAGES_RECEIVE=27500
export TEST_SINGLE_MIRROR_SOAK_RECEIVE_COMMIT=50
export TEST_SINGLE_MIRROR_SOAK_SEND_COMMIT=1000
export TEST_SINGLE_MIRROR_SOAK_KILL_INTERVAL=7500
export TEST_SINGLE_MIRROR_SOAK_SNF_TIMEOUT=300000
export TEST_SINGLE_MIRROR_SOAK_GENERAL_TIMEOUT=60000
export TEST_SINGLE_MIRROR_SOAK_CONSUMER_PROCESSING_TIME=100