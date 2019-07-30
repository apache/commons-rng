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
 * A benchmark state that can retrieve the various generators defined by {@link RandomSource}
 * values.
 *
 * <p>The state will include only those that do not require additional constructor arguments.</p>
 *
 * <p>This class is abstract since it adds a special {@code RandomSource} named
 * {@code BASELINE}. A baseline implementation for the {@link UniformRandomProvider}
 * interface must be provided by implementing classes. For example to baseline methods
 * using {@link UniformRandomProvider#nextInt()} use the following code:</p>
 *
 * <pre>
 * &#64;State(Scope.Benchmark)
 * public static class Sources extends BaselineSources {
 *     &#64;Override
 *     protected UniformRandomProvider createBaseline() {
 *         return BaselineUtils.getNextInt();
 *     }
 * }
 * </pre>
 *
 * <p>Note: It is left to the implementation to ensure the baseline is suitable for the method
 * being tested.</p>
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
            "XO_RO_SHI_RO_64_S",
            "XO_RO_SHI_RO_64_SS",
            "XO_SHI_RO_128_PLUS",
            "XO_SHI_RO_128_SS",
            "XO_RO_SHI_RO_128_PLUS",
            "XO_RO_SHI_RO_128_SS",
            "XO_SHI_RO_256_PLUS",
            "XO_SHI_RO_256_SS",
            "XO_SHI_RO_512_PLUS",
            "XO_SHI_RO_512_SS",
            "PCG_XSH_RR_32",
            "PCG_XSH_RS_32",
            "PCG_RXS_M_XS_64",
            "PCG_MCG_XSH_RR_32",
            "PCG_MCG_XSH_RS_32",
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
