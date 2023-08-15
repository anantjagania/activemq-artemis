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

echo "Notice: This script is provided as a facility/tool to let you run an Microsoft Free Database. You agree with any license issues imposed by Oracle by running this script"

./stop-mssql-podman.sh

podman run -d --name mssql-artemis-test -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=ActiveMQ*Artemis" -p 1433:1433 mcr.microsoft.com/mssql/server:2019-latest
