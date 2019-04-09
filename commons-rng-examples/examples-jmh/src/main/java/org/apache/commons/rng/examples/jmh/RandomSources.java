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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * A benchmark state that can retrieve the various {@link RandomSource} generators.
 *
 * <p>The state will include only those that do not require additional constructor arguments.</p>
 */
@State(Scope.Benchmark)
public class RandomSources {
    /**
     * RNG providers. This list is maintained in the order of the {@link RandomSource} enum.
     *
     * <p>Include only those that do not require additional constructor arguments.</p>
     *
     * <p>Note: JMH does support using an Enum for the {@code @Param} annotation. However
     * the default set will encompass all the enum values, including those that require
     * additional constructor arguments. So this list is maintained manually.</p>
     */
    @Param({"JDK",
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
    private UniformRandomProvider generator;

    /**
     * Gets the generator.
     *
     * @return the RNG.
     */
    public UniformRandomProvider getGenerator() {
        return generator;
    }

    /** Instantiates generator. */
    @Setup
    public void setup() {
        final RandomSource randomSource = RandomSource.valueOf(randomSourceName);
        generator = RandomSource.create(randomSource);
    }
}
