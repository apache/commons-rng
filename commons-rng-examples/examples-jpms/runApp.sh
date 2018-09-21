#!/bin/bash -e

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Script assumes that the JAR files have all been generated (e.g. by calling
# "mvn package" from the top level project directory).

TOPDIR=../..
RNGVERSION=1.2-SNAPSHOT
TARGETDIR=target

# Module path.
MODPATH=\
$TOPDIR/commons-rng-core/$TARGETDIR/commons-rng-core-$RNGVERSION.jar:\
$TOPDIR/commons-rng-simple/$TARGETDIR/commons-rng-simple-$RNGVERSION.jar:\
$TOPDIR/commons-rng-examples/examples-jpms/jpms-app/$TARGETDIR/commons-rng-examples-jpms-app-$RNGVERSION.jar:\
$TOPDIR/commons-rng-examples/examples-jpms/jpms-lib/$TARGETDIR/commons-rng-examples-jpms-lib-$RNGVERSION.jar:\
$TOPDIR/commons-rng-sampling/$TARGETDIR/commons-rng-sampling-$RNGVERSION.jar:\
$TOPDIR/commons-rng-client-api/$TARGETDIR/commons-rng-client-api-$RNGVERSION.jar

# Application.
MOD=org.apache.commons.rng.examples.jpms.app/org.apache.commons.rng.examples.jpms.app.DiceGameApplication

# Arguments of the application (see source).
ARGS="3 4 19 MT"

# Assuming Java 9 (or later).
java --module-path $MODPATH --module $MOD $ARGS
