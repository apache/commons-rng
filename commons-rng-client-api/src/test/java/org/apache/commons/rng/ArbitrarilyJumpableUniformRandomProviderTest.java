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
package org.apache.commons.rng;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for default method implementations in
 * {@link ArbitrarilyJumpableUniformRandomProvider}.
 *
 * <p>Streams methods are asserted to call the corresponding jump method in the
 * interface.
 */
class ArbitrarilyJumpableUniformRandomProviderTest {
    /**
     * Class for checking the behavior of the ArbitrarilyJumpableUniformRandomProvider.
     * This generator returns a fixed value. The value is incremented by jumping.
     */
    private static class JumpableGenerator implements ArbitrarilyJumpableUniformRandomProvider {
        /** The value for nextLong(). */
        private long value;

        JumpableGenerator(long seed) {
            this.value = seed;
        }

        @Override
        public long nextLong() {
            return value;
        }

        @Override
        public ArbitrarilyJumpableUniformRandomProvider jump(double distance) {
            final JumpableGenerator copy = new JumpableGenerator(value);
            // Support small distances as a long
            value += (long) distance;
            return copy;
        }

        @Override
        public ArbitrarilyJumpableUniformRandomProvider jumpPowerOfTwo(int logDistance) {
            throw new IllegalStateException("Not required by default methods");
        }
    }

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
     * Return a stream of jump arguments, each of the arguments consisting of the size of
     * the stream, the seed value for the jumpable generator, and the jump distance.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> jumpArguments() {
        return Stream.of(
            // size, seed
            Arguments.of(0, 0, 0.0),
            Arguments.of(1, 62317845757L, 2.0),
            Arguments.of(5, -12683127894356L, 42.0)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void testInvalidStreamSizeThrows(long size) {
        final ArbitrarilyJumpableUniformRandomProvider rng = new JumpableGenerator(0);
        final double distance = 10;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(size, distance));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN})
    void testInvalidDistanceThrows(double distance) {
        final ArbitrarilyJumpableUniformRandomProvider rng = new JumpableGenerator(0);
        final long size = 1;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(size, distance));
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumps(int size, long seed, double distance) {
        assertJumps(size, seed, distance,
            (rng, n, d) -> rng.jumps(d).limit(n));
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumpsWithSize(int size, long seed, double distance) {
        assertJumps(size, seed, distance,
            ArbitrarilyJumpableUniformRandomProvider::jumps);
    }

    /**
     * Assert that successive calls to the generator jump function will create a series of
     * generators that matches the stream function.
     *
     * @param size Number of jumps.
     * @param seed Seed for the generator.
     * @param distance Jump distance.
     * @param streamFunction Stream function to create a series of generators spaced
     * using the jump function.
     */
    private static void assertJumps(int size, long seed, double distance,
            JumpsFunction streamFunction) {
        // Manually jump
        final JumpableGenerator jumpingRNG = new JumpableGenerator(seed);
        final long[] expected = new long[size];
        for (int i = 0; i < size; i++) {
            final UniformRandomProvider copy = jumpingRNG.jump(distance);
            Assertions.assertNotSame(jumpingRNG, copy, "Jump function should return a copy");
            expected[i] = copy.nextLong();
        }

        // Stream (must be sequential)
        final JumpableGenerator streamingRNG = new JumpableGenerator(seed);
        final Stream<? extends UniformRandomProvider> stream =
            streamFunction.apply(streamingRNG, size, distance);
        Assertions.assertFalse(stream.isParallel(), "Jumping requires a non-parallel stream");

        // Stream should create unique generators
        final Set<UniformRandomProvider> set = new HashSet<>();
        final long[] actual = stream.map(x -> addAndReturn(set, x))
                                    .mapToLong(UniformRandomProvider::nextLong)
                                    .toArray();
        Assertions.assertEquals(size, set.size(), "Stream should have unique generators");
        Assertions.assertFalse(set.contains(streamingRNG), "Stream contains the source of the stream as a generator");

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
}
