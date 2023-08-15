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


# As documented on https://www.ibm.com/docs/en/db2/11.5?topic=system-linux

if [ $# -ne 1 ]; then
    echo "Usage: $0 <folder_data>"
    echo "       setting folder_data as ./oradb by default"
    folder_data="./db2db"
else
    folder_data="$1"
fi

./stop-db2-podman.sh

if [ ! -d "$folder_data" ]; then
    mkdir "$folder_data"
    chmod 777 $folder_data
    echo "Folder '$folder_data' created."
else
    echo "Folder '$folder_data' already exists."
fi

echo "Notice: This script is provided as a facility/tool to let you run an DB2 Free Database. You agree with any license issues imposed by IBM by running this script"

podman run -d -h db2-artemis-test --name db2-artemis-test --privileged=true -p 50000:50000 --env-file db2.env -v $folder_data:/database:Z icr.io/db2_community/db2