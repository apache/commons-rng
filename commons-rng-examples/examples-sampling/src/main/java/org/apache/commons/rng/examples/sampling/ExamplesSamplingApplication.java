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

import picocli.CommandLine;

/**
 * Executes testing utilities for the samplers in the Commons RNG library.
 *
 * <p>Functionality includes:</p>
 *
 * <ul>
 *   <li>Creating a PDF approximation using sample data from a distribution
 *   <li>Sampling from a small range from a distribution to visually inspect sampling density
 * </ul>
 */
public final class ExamplesSamplingApplication {
    /** No public constructor. */
    private ExamplesSamplingApplication() {}

    /**
     * Run the RNG examples stress command line application.
     *
     * @param args Application's arguments.
     */
    public static void main(String[] args) {
        // Build the command line manually so we can configure options.
        final CommandLine cmd = new CommandLine(new ExamplesSamplingCommand())
                .addSubcommand("density", new ProbabilityDensityApproximationCommand())
                .addSubcommand("visual", new UniformSamplingVisualCheckCommand())
                // Call last to apply to all sub-commands
                .setCaseInsensitiveEnumValuesAllowed(true);

        // Parse the command line and invokes the Callable program
        cmd.execute(args);
    }
}
