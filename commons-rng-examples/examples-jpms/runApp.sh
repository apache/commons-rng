#!/bin/bash -e

# Script assumes that the JAR files have all been generated (e.g. by calling
# "mvn package" from the top level project directory).

TOPDIR=../..
RNGVERSION=1.1-SNAPSHOT
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
