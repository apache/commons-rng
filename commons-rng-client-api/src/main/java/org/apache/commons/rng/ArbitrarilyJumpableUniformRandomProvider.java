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

import java.util.stream.Stream;

/**
 * Applies to generators that can be advanced an arbitrary number of steps of the output
 * sequence in a single operation.
 *
 * <p>Implementations must ensure that a jump of a specified {@code distance} will advance
 * the state cycle sufficiently that an equivalent number of sequential calls to the
 * original provider will <strong>not overlap</strong> output from the advanced
 * provider.</p>
 *
 * <p>For many applications, it suffices to jump forward by a power of two or some small
 * multiple of a power of two, but this power of two may not be representable as a
 * {@code long} value. To avoid the use of {@link java.math.BigInteger BigInteger} values
 * as jump distances, double values are used instead.</p>
 *
 * <p>Typical usage in a multithreaded application is to create a single
 * {@link ArbitrarilyJumpableUniformRandomProvider} and {@link #jump(double) jump} the
 * generator forward while passing each copy generator to a worker thread. The jump
 * {@code distance} should be sufficient to cover all expected output by each worker.
 * Since each copy generator is also a {@link ArbitrarilyJumpableUniformRandomProvider}
 * with care it is possible to further distribute generators within the original jump
 * {@code distance} and use the entire state cycle in different ways.</p>
 *
 * @since 1.7
 */
public interface ArbitrarilyJumpableUniformRandomProvider extends UniformRandomProvider {
    /**
     * Creates a copy of the {@link ArbitrarilyJumpableUniformRandomProvider} and then advances
     * the state cycle of the current instance by the specified {@code distance}.
     * The copy is returned.
     *
     * <p>The current state will be advanced in a single operation by the equivalent of a
     * number of sequential calls to a method that updates the state cycle of the provider.</p>
     *
     * <p>Repeat invocations of this method will create a series of generators
     * that are uniformly spaced at intervals of the output sequence. Each generator provides
     * non-overlapping output for the length specified by {@code distance} for use in parallel
     * computations.</p>
     *
     * @param distance Distance to jump forward with the state cycle.
     * @return A copy of the current state.
     * @throws IllegalArgumentException if {@code distance} is negative,
     * or is greater than the period of this generator.
     */
    ArbitrarilyJumpableUniformRandomProvider jump(double distance);

    /**
     * Creates a copy of the {@link ArbitrarilyJumpableUniformRandomProvider} and then advances
     * the state cycle of the current instance by a distance equal to 2<sup>{@code logDistance}</sup>.
     * The copy is returned.
     *
     * <p>The current state will be advanced in a single operation by the equivalent of a
     * number of sequential calls to a method that updates the state cycle of the provider.</p>
     *
     * <p>Repeat invocations of this method will create a series of generators
     * that are uniformly spaced at intervals of the output sequence. Each generator provides
     * non-overlapping output for the length specified by 2<sup>{@code logDistance}</sup> for use
     * in parallel computations.</p>
     *
     * @param logDistance Base-2 logarithm of the distance to jump forward with the state cycle.
     * @return A copy of the current state.
     * @throws IllegalArgumentException if 2<sup>{@code logDistance}</sup>
     * is greater than the period of this generator.
     */
    ArbitrarilyJumpableUniformRandomProvider jumpPowerOfTwo(int logDistance);

    /**
     * Returns an effectively unlimited stream of new random generators, each of which
     * implements the {@link ArbitrarilyJumpableUniformRandomProvider} interface. The
     * generators are output at integer multiples of the specified jump {@code distance}
     * in the generator's state cycle.
     *
     * @param distance Distance to jump forward with the state cycle.
     * @return a stream of random generators.
     * @throws IllegalArgumentException if {@code distance} is negative,
     * or is greater than the period of this generator.
     */
    default Stream<ArbitrarilyJumpableUniformRandomProvider> jumps(double distance) {
        UniformRandomProviderSupport.validateJumpDistance(distance);
        return Stream.generate(() -> jump(distance)).sequential();
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of new random
     * generators, each of which implements the {@link ArbitrarilyJumpableUniformRandomProvider}
     * interface. The generators are output at integer multiples of the specified jump
     * {@code distance} in the generator's state cycle.
     *
     * @param streamSize Number of objects to generate.
     * @param distance Distance to jump forward with the state cycle.
     * @return a stream of random generators; the stream is limited to the given
     * {@code streamSize}.
     * @throws IllegalArgumentException if {@code streamSize} is negative;
     * or if {@code distance} is negative, or is greater than the period of this generator.
     */
    default Stream<ArbitrarilyJumpableUniformRandomProvider> jumps(long streamSize,
                                                                   double distance) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        return jumps(distance).limit(streamSize);
    }
}
