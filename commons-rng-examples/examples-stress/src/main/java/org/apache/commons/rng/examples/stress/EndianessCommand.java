/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.examples.stress;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.ByteOrder;
import java.util.concurrent.Callable;

/**
 * Specification for the "endian" command.
 *
 * <p>This command prints the native byte order of the platform.</p>
 *
 * @see ByteOrder#nativeOrder()
 */
@Command(name = "endian",
         description = "Show the platform native byte order.")
class EndianessCommand implements Callable<Void> {
    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    /**
     * Prints a template generators list to stdout.
     */
    @Override
    public Void call() {
        // Log level will be relevant for any exception logging
        LogUtils.setLogLevel(reusableOptions.logLevel);
        LogUtils.info(ByteOrder.nativeOrder().toString());
        return null;
    }
}
