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
package org.apache.commons.rng.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.apache.commons.rng.ArbitrarilyJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests which all {@link ArbitrarilyJumpableUniformRandomProvider} generators must pass.
 * Note: This supplements basic jump functionality tested in {@link JumpableProvidersParametricTest}.
 */
class ArbitrarilyJumpableProvidersParametricTest {
    /** Negative and non-finite distances. */
    private static final double[] INVALID_DISTANCES = {Double.NaN, -1, -0.5, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY};

    interface JumpsFunction {
        /**
         * Create a stream of generators separated by the jump distance.
         *
         * @param rng Jumpable generator.
         * @param streamSize Number of objects to generate.
         * @param distance Distance to jump forward with the state cycle.
         * @return the stream
         */
        Stream<ArbitrarilyJumpableUniformRandomProvider> apply(ArbitrarilyJumpableUniformRandomProvider rng, long size, double distance);
    }

    /**
     * Gets the list of arbitrarily jumpable generators.
     *
     * @return the list
     */
    private static Iterable<ArbitrarilyJumpableUniformRandomProvider> getArbitrarilyJumpableProviders() {
        return ProvidersList.listArbitrarilyJumpable();
    }

    /**
     * Test that the jump methods throw when the distance is invalid.
     */
    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testJumpThrowsWithInvalidDistance(ArbitrarilyJumpableUniformRandomProvider generator) {
        for (final double distance : INVALID_DISTANCES) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> generator.jump(distance));
        }
    }

    /**
     * Test that the random generator returned from no jump is a copy with the same output.
     */
    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testZeroJump(ArbitrarilyJumpableUniformRandomProvider generator) {
        assertZeroJump(s -> s.jump(0.0), generator);
    }

    /**
     * Test that the random generator returned from no jump (power of 2) is a copy with the same output.
     */
    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testZeroJumpPowerOfTwo(ArbitrarilyJumpableUniformRandomProvider generator) {
        assertZeroJump(s -> s.jumpPowerOfTwo(-1), generator);
        assertZeroJump(s -> s.jumpPowerOfTwo(Integer.MIN_VALUE), generator);
    }

    /**
     * Assert that the random generator returned from the jump function for no distance outputs
     * the same sequence.
     *
     * @param jumpFunction Jump function to test.
     * @param generator RNG under test.
     */
    private static void assertZeroJump(UnaryOperator<ArbitrarilyJumpableUniformRandomProvider> jumpFunction,
                                       ArbitrarilyJumpableUniformRandomProvider generator) {
        final UniformRandomProvider child = jumpFunction.apply(generator);
        RandomAssert.assertNextLongEquals(10, generator, child);
    }

    static Stream<Arguments> testSmallJump() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final ArbitrarilyJumpableUniformRandomProvider rng : ProvidersList.listArbitrarilyJumpable()) {
            // Distance forward must be strictly positive for the test
            // to generate an expected sequence
            for (final int size : new int[] {1, 2, 3}) {
                builder.add(Arguments.of(rng, size, size));
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource
    void testSmallJump(ArbitrarilyJumpableUniformRandomProvider generator, int distance) {
        assertSmallJump(s -> s.jump(distance), distance, generator);
    }

    @ParameterizedTest
    @MethodSource("testSmallJump")
    void testSmallJumpPowerOfTwo(ArbitrarilyJumpableUniformRandomProvider generator, int distance) {
        assertSmallJump(s -> s.jumpPowerOfTwo(distance), 1 << distance, generator);
    }

    /**
     * Assert that the random generator returned from the jump function for the specified
     * distance outputs the same sequence. The distance jumped should be small so it can be
     * verified by skipping sequential output from the generator.
     *
     * @param jumpFunction Jump function to test.
     * @param distance Distance advanced by the jump function.
     * @param generator RNG under test.
     */
    private static void assertSmallJump(UnaryOperator<ArbitrarilyJumpableUniformRandomProvider> jumpFunction,
                                        int distance,
                                        ArbitrarilyJumpableUniformRandomProvider generator) {
        // Manually jump.
        final ArbitrarilyJumpableUniformRandomProvider reference = copy(generator);

        // Get the primary output of the generator
        final ToLongFunction<UniformRandomProvider> fun = generator instanceof IntProvider ?
            UniformRandomProvider::nextInt :
            UniformRandomProvider::nextLong;

        final int size = 10;
        final long[] expected = new long[size];
        for (int i = 0; i < size; i++) {
            expected[i] = fun.applyAsLong(reference);
            // Skip forward
            for (int j = 1; j < distance; j++) {
                fun.applyAsLong(reference);
            }
        }

        final long[] actual = new long[size];
        for (int i = 0; i < size; i++) {
            final UniformRandomProvider copy = jumpFunction.apply(generator);
            Assertions.assertNotSame(generator, copy, "Jump function should return a copy");
            actual[i] = fun.applyAsLong(copy);
        }

        Assertions.assertArrayEquals(expected, actual, "Small jump function did not match the modulo sequence");
    }

    static Stream<Arguments> testPowerOfTwoJumps() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final ArbitrarilyJumpableUniformRandomProvider rng : ProvidersList.listArbitrarilyJumpable()) {
            // Small jumps
            builder.add(Arguments.of(rng, new int[] {0})); // 1
            builder.add(Arguments.of(rng, new int[] {1})); // 2
            builder.add(Arguments.of(rng, new int[] {0, 1})); // 3
            builder.add(Arguments.of(rng, new int[] {1, 3, 5})); // 42
            // Bigger jumps. Use values within a period of 2^128
            builder.add(Arguments.of(rng, new int[] {99}));
            builder.add(Arguments.of(rng, new int[] {42, 67, 63}));
            builder.add(Arguments.of(rng, new int[] {113, 115, 110}));
            // Limit of a 53-bit mantissa
            builder.add(Arguments.of(rng, new int[] {100, 100 - 52}));
        }
        return builder.build();
    }

    /**
     * Test power of two jumps can be combined in any order and should match an equivalent
     * single jump of a double distance.
     */
    @ParameterizedTest
    @MethodSource
    void testPowerOfTwoJumps(ArbitrarilyJumpableUniformRandomProvider generator,
            int[] logDistances) {
        final int min = Arrays.stream(logDistances).min().getAsInt();
        final int max = Arrays.stream(logDistances).max().getAsInt();
        Assertions.assertTrue(max - min <= 52, "log-distances are too far apart for a double");

        final ArbitrarilyJumpableUniformRandomProvider reference = copy(generator);
        double distance = 0;
        for (final int logDistance : logDistances) {
            distance += Math.scalb(1.0, logDistance);
            generator.jumpPowerOfTwo(logDistance);
        }
        reference.jump(distance);
        RandomAssert.assertNextLongEquals(10, reference, generator);
    }

    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testJumpsStreamSize(ArbitrarilyJumpableUniformRandomProvider generator) {
        final double distance = 1.0;
        for (final long size : new long[] {0, 1, 7, 13}) {
            Assertions.assertEquals(size, generator.jumps(size, distance).count(), "jumps");
        }
    }

    // Test adapted from stream tests in commons-rng-client-api module

    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testJumpsInvalidStreamSizeThrows(ArbitrarilyJumpableUniformRandomProvider rng) {
        final double distance = 1.0;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(-1, distance));
    }

    @ParameterizedTest
    @MethodSource("getArbitrarilyJumpableProviders")
    void testJumpsInvalidDistanceThrows(ArbitrarilyJumpableUniformRandomProvider rng) {
        final long streamSize = 10;
        for (final double distance : INVALID_DISTANCES) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(distance), "jumps");
            Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(streamSize, distance), "jumps(distance)");
        }
    }

    /**
     * Return a stream of jump arguments, each of the arguments consisting of: the
     * generator; the size of the stream; and the jump distance.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> jumpArguments() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final ArbitrarilyJumpableUniformRandomProvider rng : ProvidersList.listArbitrarilyJumpable()) {
            for (final int size : new int[] {0, 1, 5}) {
                for (final double distance : new double[] {0, 1, 42}) {
                    builder.add(Arguments.of(rng, size, distance));
                }
            }
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumps(ArbitrarilyJumpableUniformRandomProvider generator, int size, double distance) {
        assertJumps(generator, size, distance,
            (rng, n, d) -> rng.jumps(d).limit(n));
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumpsWithSize(ArbitrarilyJumpableUniformRandomProvider generator, int size, double distance) {
        assertJumps(generator, size, distance,
            ArbitrarilyJumpableUniformRandomProvider::jumps);
    }

    /**
     * Assert that successive calls to the generator jump function will create a series of
     * generators that matches the stream function.
     *
     * @param size Number of jumps.
     * @param distance Jump distance.
     * @param streamFunction Stream function to create a series of generators spaced
     * using the jump function.
     */
    private static void assertJumps(ArbitrarilyJumpableUniformRandomProvider generator, int size, double distance,
            JumpsFunction streamFunction) {
        // Manually jump.
        final ArbitrarilyJumpableUniformRandomProvider jumpingRNG = copy(generator);
        final long[] expected = new long[size];
        for (int i = 0; i < size; i++) {
            final UniformRandomProvider copy = jumpingRNG.jump(distance);
            Assertions.assertNotSame(jumpingRNG, copy, "Jump function should return a copy");
            expected[i] = copy.nextLong();
        }

        // Stream (must be sequential)
        final Stream<? extends UniformRandomProvider> stream =
            streamFunction.apply(generator, size, distance);
        Assertions.assertFalse(stream.isParallel(), "Jumping requires a non-parallel stream");

        // Stream should create unique generators
        final Set<UniformRandomProvider> set = new HashSet<>();
        final long[] actual = stream.map(x -> addAndReturn(set, x))
                                    .mapToLong(UniformRandomProvider::nextLong)
                                    .toArray();
        Assertions.assertEquals(size, set.size(), "Stream should have unique generators");
        Assertions.assertFalse(set.contains(generator), "Stream contains the source of the stream as a generator");

        Assertions.assertArrayEquals(expected, actual, "Stream function did not match the jump function");
    }

    /**
     * Add the generator to the set and return the generator. This is a convenience
     * method used to check generator objects in a stream are unique and not the same as
     * the generator that created the stream.
     *
     * @param set Set
     * @param x Generator
     * @return the generator
     */
    private static UniformRandomProvider addAndReturn(Set<UniformRandomProvider> set, UniformRandomProvider x) {
        set.add(x);
        return x;
    }

    /**
     * Copy the generator.
     *
     * @param generator Generator
     * @return the copy
     */
    private static ArbitrarilyJumpableUniformRandomProvider copy(ArbitrarilyJumpableUniformRandomProvider generator) {
        // This exploits a jump of zero to create a copy.
        // This assumption is tested in the zero jump test.
        return generator.jump(0);
    }
}
