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

package org.apache.commons.rng.examples.jmh;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Defines the benchmark state to retrieve the various "RandomSource"s.
 *
 * <p>A baseline implementation for the {@link UniformRandomProvider} must be provided by
 * implementing classes.
 */
@State(Scope.Benchmark)
public abstract class BaselineSources {
    /** The keyword identifying the baseline implementation. */
    private static final String BASELINE = "BASELINE";

    /**
     * RNG providers.
     *
     * <p>List all providers that do not require additional constructor arguments. This list
     * is in the declared order of {@link RandomSource}.</p>
     */
    @Param({BASELINE,
           "JDK",
           "WELL_512_A",
           "WELL_1024_A",
           "WELL_19937_A",
           "WELL_19937_C",
           "WELL_44497_A",
           "WELL_44497_B",
           "MT",
           "ISAAC",
           "SPLIT_MIX_64",
           "XOR_SHIFT_1024_S",
           "TWO_CMRES",
           "MT_64",
           "MWC_256",
           "KISS",
           "XOR_SHIFT_1024_S_PHI",
            })
    private String randomSourceName;

    /** RNG. */
    private UniformRandomProvider provider;

    /**
     * Gets the generator.
     *
     * @return the RNG.
     */
    public UniformRandomProvider getGenerator() {
        return provider;
    }

    /** Instantiates generator. This need only be done once per set of iterations. */
    @Setup(value = Level.Trial)
    public void setup() {
        if (BASELINE.equals(randomSourceName)) {
            provider = createBaseline();
        } else {
            final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
            provider = RandomSource.create(randomSource);
        }
    }

    /**
     * Creates the baseline {@link UniformRandomProvider}.
     *
     * <p>This should implement the method(s) that will be tested. The speed of this RNG is expected
     * to create a baseline against which all other generators will be compared.</p>
     *
     * @return the baseline RNG.
     */
    protected abstract UniformRandomProvider createBaseline();
}
