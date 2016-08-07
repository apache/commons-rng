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
package org.apache.commons.math4.userguide.rng;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math4.random.RandomGenerator;
import org.apache.commons.rng.RandomSource;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.math4.PerfTestUtils;

/**
 * Benchmark of the RNGs.
 */
public class RandomNumberGeneratorBenchmark {
    /** Number of loops over the operations to be benchmarked. */
    private static final int NUM_CALLS = 20_000_000;
    /** Number of runs for computing the statistics. */
    private static final int NUM_STATS = 500;
    /** Report formatting. */
    private static final int MAX_NAME_WIDTH = 45;

    /**
     * Program's entry point.
     *
     * @param args Application's arguments.  Not used.
     */
    public static void main(String[] args) throws Exception {
        final RandomNumberGeneratorBenchmark app = new RandomNumberGeneratorBenchmark();

        printEnv();

        // app.all();
        app.nextIntComparison();
        app.nextDoubleComparison();
    }

    /**
     * Test all generators.
     * The reference is JDK's "Random".
     */
    public void all() {
        // List of candidates.
        final List<UniformRandomProvider> rngList = new ArrayList<>();

        for (UniformRandomProvider rng : new GeneratorsList()) {
            rngList.add(rng);
        }

        // Reference is JDK's "Random".
        final Random jdk = new Random();

        // List of benchmarked codes.
        final PerfTestUtils.RunTest[] candidates = new PerfTestUtils.RunTest[rngList.size() + 1];

        // "nextInt()" benchmark.

        candidates[0] = new PerfTestUtils.RunTest(jdk.toString()) {
                @Override
                    public Double call() throws Exception {
                    return (double) jdk.nextInt();
                }
            };
        for (int i = 0; i < rngList.size(); i++) {
            final UniformRandomProvider rng = rngList.get(i);

            candidates[i + 1] = new PerfTestUtils.RunTest(rng.toString()) {
                    @Override
                    public Double call() throws Exception {
                        return (double) rng.nextInt();
                    }
                };
        }

        PerfTestUtils.timeAndReport("nextInt()",
                                    MAX_NAME_WIDTH,
                                    NUM_CALLS,
                                    NUM_STATS,
                                    false,
                                    candidates);

        // "nextDouble()" benchmark.

        candidates[0] = new PerfTestUtils.RunTest(jdk.toString()) {
                @Override
                    public Double call() throws Exception {
                    return (double) jdk.nextDouble();
                }
            };
        for (int i = 0; i < rngList.size(); i++) {
            final UniformRandomProvider rng = rngList.get(i);

            candidates[i + 1] = new PerfTestUtils.RunTest(rng.toString()) {
                    @Override
                    public Double call() throws Exception {
                       return rng.nextDouble();
                    }
                };
        }

        PerfTestUtils.timeAndReport("nextDouble()",
                                    MAX_NAME_WIDTH,
                                    NUM_CALLS,
                                    NUM_STATS,
                                    false,
                                    candidates);

        // "nextLong()" benchmark.

        candidates[0] = new PerfTestUtils.RunTest(jdk.toString()) {
                @Override
                    public Double call() throws Exception {
                    return (double) jdk.nextLong();
                }
            };
        for (int i = 0; i < rngList.size(); i++) {
            final UniformRandomProvider rng = rngList.get(i);

            candidates[i + 1] = new PerfTestUtils.RunTest(rng.toString()) {
                    @Override
                    public Double call() throws Exception {
                        return (double) rng.nextLong();
                    }
                };
        }

        PerfTestUtils.timeAndReport("nextLong()",
                                    MAX_NAME_WIDTH,
                                    NUM_CALLS,
                                    NUM_STATS,
                                    false,
                                    candidates);
    }

    /**
     * Compare old and new implementations.
     */
    public void nextIntComparison() {
        final List<PerfTestUtils.RunTest> candidates = new ArrayList<PerfTestUtils.RunTest>();

        candidates.add(createNextInt(new org.apache.commons.math4.random.JDKRandomGenerator()));
        candidates.add(createNextInt(RandomSource.JDK));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well512a()));
        candidates.add(createNextInt(RandomSource.WELL_512_A));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well1024a()));
        candidates.add(createNextInt(RandomSource.WELL_1024_A));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well19937a()));
        candidates.add(createNextInt(RandomSource.WELL_19937_A));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well19937c()));
        candidates.add(createNextInt(RandomSource.WELL_19937_C));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well44497a()));
        candidates.add(createNextInt(RandomSource.WELL_44497_A));
        candidates.add(createNextInt(new org.apache.commons.math4.random.Well44497b()));
        candidates.add(createNextInt(RandomSource.WELL_44497_B));
        candidates.add(createNextInt(new org.apache.commons.math4.random.ISAACRandom()));
        candidates.add(createNextInt(RandomSource.ISAAC));
        candidates.add(createNextInt(new org.apache.commons.math4.random.MersenneTwister()));
        candidates.add(createNextInt(RandomSource.MT));

        // "nextInt()" benchmark.
        PerfTestUtils.timeAndReport("nextInt()",
                                    MAX_NAME_WIDTH,
                                    NUM_CALLS,
                                    NUM_STATS,
                                    false,
                                    candidates.toArray(new PerfTestUtils.RunTest[0]));
    }

    /**
     * Compare old and new implementations.
     */
    public void nextDoubleComparison() {
        final List<PerfTestUtils.RunTest> candidates = new ArrayList<PerfTestUtils.RunTest>();

        candidates.add(createNextDouble(new org.apache.commons.math4.random.JDKRandomGenerator()));
        candidates.add(createNextDouble(RandomSource.JDK));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well512a()));
        candidates.add(createNextDouble(RandomSource.WELL_512_A));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well1024a()));
        candidates.add(createNextDouble(RandomSource.WELL_1024_A));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well19937a()));
        candidates.add(createNextDouble(RandomSource.WELL_19937_A));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well19937c()));
        candidates.add(createNextDouble(RandomSource.WELL_19937_C));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well44497a()));
        candidates.add(createNextDouble(RandomSource.WELL_44497_A));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.Well44497b()));
        candidates.add(createNextDouble(RandomSource.WELL_44497_B));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.ISAACRandom()));
        candidates.add(createNextDouble(RandomSource.ISAAC));
        candidates.add(createNextDouble(new org.apache.commons.math4.random.MersenneTwister()));
        candidates.add(createNextDouble(RandomSource.MT));

        // "nextDouble()" benchmark.
        PerfTestUtils.timeAndReport("nextDouble()",
                                    MAX_NAME_WIDTH,
                                    NUM_CALLS,
                                    NUM_STATS,
                                    false,
                                    candidates.toArray(new PerfTestUtils.RunTest[0]));
    }

    /**
     * @param prefix Prefix for the benchmark item.
     * @param rng Generator.
     * @return a benchmark item.
     */
    private PerfTestUtils.RunTest createNextInt(final RandomSource source) {
        final UniformRandomProvider rng = RandomSource.create(source);
        return new PerfTestUtils.RunTest("[rng] " + rng) {
            @Override
            public Double call() throws Exception {
                return (double) rng.nextInt();
            }
        };
    }

    /**
     * @param prefix Prefix for the benchmark item.
     * @param rng Generator.
     * @return a benchmark item.
     */
    private PerfTestUtils.RunTest createNextInt(final RandomGenerator rng) {
        return new PerfTestUtils.RunTest("[cm] " + rng) {
            @Override
            public Double call() throws Exception {
                return (double) rng.nextInt();
            }
        };
    }

    /**
     * @param prefix Prefix for the benchmark item.
     * @param rng Generator.
     * @return a benchmark item.
     */
    private PerfTestUtils.RunTest createNextDouble(final RandomSource source) {
        final UniformRandomProvider rng = RandomSource.create(source);
        return new PerfTestUtils.RunTest("[rng] " + rng) {
            @Override
            public Double call() throws Exception {
                return (double) rng.nextDouble();
            }
        };
    }

    /**
     * @param prefix Prefix for the benchmark item.
     * @param rng Generator.
     * @return a benchmark item.
     */
    private PerfTestUtils.RunTest createNextDouble(final RandomGenerator rng) {
        return new PerfTestUtils.RunTest("[cm] " + rng) {
            @Override
            public Double call() throws Exception {
                return (double) rng.nextDouble();
            }
        };
    }

    /**
     * Print environment.
     */
    private static void printEnv() {
        final String sep = " ";
        System.out.println("Java" + sep + System.getProperty("java.version"));
        System.out.println("Runtime" + sep + System.getProperty("java.runtime.version", "?"));
        System.out.println("JVM" + sep + System.getProperty("java.vm.name") +
                           sep + System.getProperty("java.vm.version"));
    }
}
