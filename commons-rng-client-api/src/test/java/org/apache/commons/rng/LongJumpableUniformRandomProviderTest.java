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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for default method implementations in
 * {@link LongJumpableUniformRandomProvider}.
 *
 * <p>Streams methods are asserted to call the corresponding jump method in the
 * interface.
 */
class LongJumpableUniformRandomProviderTest {
    /**
     * Class for checking the behavior of the LongJumpableUniformRandomProvider.
     * This generator returns a fixed value. The value is incremented by jumping.
     */
    private static class JumpableGenerator implements LongJumpableUniformRandomProvider {
        /** The increment to the value after a jump. */
        private static final long JUMP_INCREMENT = 1;
        /** The increment to the value after a long jump. */
        private static final long LONG_JUMP_INCREMENT = 1000;
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
        public UniformRandomProvider jump() {
            final UniformRandomProvider copy = new JumpableGenerator(value);
            value += JUMP_INCREMENT;
            return copy;
        }

        @Override
        public JumpableUniformRandomProvider longJump() {
            final JumpableUniformRandomProvider copy = new JumpableGenerator(value);
            value += LONG_JUMP_INCREMENT;
            return copy;
        }
    }

    /**
     * Return a stream of jump arguments, each of the arguments consisting of the size of
     * the stream and the seed value for the jumpable generator.
     *
     * @return the stream of arguments
     */
    static Stream<Arguments> jumpArguments() {
        return Stream.of(
            // size, seed
            Arguments.of(0, 0L),
            Arguments.of(1, 62317845757L),
            Arguments.of(5, -12683127894356L)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void testInvalidStreamSizeThrows(long size) {
        final LongJumpableUniformRandomProvider rng = new JumpableGenerator(0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.jumps(size), "jumps");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.longJumps(size), "longJumps");
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumps(int size, long seed) {
        assertJumps(size, seed,
            LongJumpableUniformRandomProvider::jump,
            (rng, n) -> rng.jumps().limit(n));
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testJumpsWithSize(int size, long seed) {
        assertJumps(size, seed,
            JumpableUniformRandomProvider::jump,
            JumpableUniformRandomProvider::jumps);
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testLongJumps(int size, long seed) {
        assertJumps(size, seed,
            LongJumpableUniformRandomProvider::longJump,
            (rng, n) -> rng.longJumps().limit(n));
    }

    @ParameterizedTest
    @MethodSource(value = {"jumpArguments"})
    void testLongJumpsWithSize(int size, long seed) {
        assertJumps(size, seed,
            LongJumpableUniformRandomProvider::longJump,
            LongJumpableUniformRandomProvider::longJumps);
    }

    /**
     * Assert that successive calls to the generator jump function will create a series of
     * generators that matches the stream function.
     *
     * @param size Number of jumps.
     * @param seed Seed for the generator.
     * @param jumpFunction Jump function to create a copy and advance the generator.
     * @param streamFunction Stream function to create a series of generators spaced
     * using the jump function.
     */
    private static void assertJumps(int size, long seed,
            Function<? super LongJumpableUniformRandomProvider, ? extends UniformRandomProvider> jumpFunction,
            BiFunction<? super LongJumpableUniformRandomProvider, Long, Stream<? extends UniformRandomProvider>> streamFunction) {
        // Manually jump
        final JumpableGenerator jumpingRNG = new JumpableGenerator(seed);
        final long[] expected = new long[size];
        for (int i = 0; i < size; i++) {
            final UniformRandomProvider copy = jumpFunction.apply(jumpingRNG);
            Assertions.assertNotSame(jumpingRNG, copy, "Jump function should return a copy");
            expected[i] = copy.nextLong();
        }

        // Stream (must be sequential)
        final JumpableGenerator streamingRNG = new JumpableGenerator(seed);
        final Stream<? extends UniformRandomProvider> stream =
            streamFunction.apply(streamingRNG, Long.valueOf(size));
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
