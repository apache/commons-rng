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
package org.apache.commons.rng.examples.sampling;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Specification for the top-level command in the examples sampling application.
 *
 * <p>This command is will print the top-level help message.</p>
 */
@Command(name = "examples-sampling",
         description = "Apache Commons RNG Examples Sampling Utilities.")
class ExamplesSamplingCommand implements Callable<Void> {
    /** The command specification. Used to print the usage built by Picocli. */
    @Spec
    private CommandSpec spec;

    /** The standard options. */
    @Mixin
    private StandardOptions reusableOptions;

    @Override
    public Void call() {
        // All work is done in sub-commands so just print the usage
        spec.commandLine().usage(System.out);
        return null;
    }
}
