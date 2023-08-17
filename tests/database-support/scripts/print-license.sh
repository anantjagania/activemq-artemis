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

source ./container-define.sh

echo ""
echo "*******************************************************************************************************************************"
echo "    Notice:"
echo "    This script is provided just to help you to run a $1 Free Database in a $CONTAINER_COMMAND environment, "
echo "    in a way to facilitate development and testing, with an image provided by $2."
echo "    By running this script you agree with all licensing issued by $2 towards the $1 image being downloaded here."
echo "*******************************************************************************************************************************"
echo ""
