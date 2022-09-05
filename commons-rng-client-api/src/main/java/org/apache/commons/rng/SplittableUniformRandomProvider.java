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

import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Applies to generators that can be split into two objects (the original and a new instance)
 * each of which implements the same interface (and can be recursively split indefinitely).
 * It is assumed that the two generators resulting from a split can be used concurrently on
 * different threads.
 *
 * <p>Ideally all generators produced by recursive splitting from the original object are
 * statistically independent and individually uniform. In this case it would be expected that
 * the set of values collectively generated from a group of split generators would have the
 * same statistical properties as the same number of values produced from a single generator
 * object.
 *
 * @since 1.5
 */
public interface SplittableUniformRandomProvider extends UniformRandomProvider {
    /**
     * Creates a new random generator, split off from this one, that implements
     * the {@link SplittableUniformRandomProvider} interface.
     *
     * <p>The current generator may be used a source of randomness to initialise the new instance.
     * In this case repeat invocations of this method will return objects with a different
     * initial state that are expected to be statistically independent.
     *
     * @return A new instance.
     */
    default SplittableUniformRandomProvider split() {
        return split(this);
    }

    /**
     * Creates a new random generator, split off from this one, that implements
     * the {@link SplittableUniformRandomProvider} interface.
     *
     * @param source A source of randomness used to initialise the new instance.
     * @return A new instance.
     * @throws NullPointerException if {@code source} is null
     */
    SplittableUniformRandomProvider split(UniformRandomProvider source);

    /**
     * Returns an effectively unlimited stream of new random generators, each of which
     * implements the {@link SplittableUniformRandomProvider} interface.
     *
     * <p>The current generator may be used a source of randomness to initialise the new instances.
     *
     * @return a stream of random generators.
     */
    default Stream<SplittableUniformRandomProvider> splits() {
        return splits(Long.MAX_VALUE, this);
    }

    /**
     * Returns an effectively unlimited stream of new random generators, each of which
     * implements the {@link SplittableUniformRandomProvider} interface.
     *
     * @param source A source of randomness used to initialise the new instances; this may
     * be split to provide a source of randomness across a parallel stream.
     * @return a stream of random generators.
     * @throws NullPointerException if {@code source} is null
     */
    default Stream<SplittableUniformRandomProvider> splits(SplittableUniformRandomProvider source) {
        return this.splits(Long.MAX_VALUE, source);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of new random
     * generators, each of which implements the {@link SplittableUniformRandomProvider}
     * interface.
     *
     * <p>The current generator may be used a source of randomness to initialise the new instances.
     *
     * @param streamSize Number of objects to generate.
     * @return a stream of random generators; the stream is limited to the given
     * {@code streamSize}.
     * @throws IllegalArgumentException if {@code streamSize} is negative.
     */
    default Stream<SplittableUniformRandomProvider> splits(long streamSize) {
        return splits(streamSize, this);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of new random
     * generators, each of which implements the {@link SplittableUniformRandomProvider}
     * interface.
     *
     * @param streamSize Number of objects to generate.
     * @param source A source of randomness used to initialise the new instances; this may
     * be split to provide a source of randomness across a parallel stream.
     * @return a stream of random generators; the stream is limited to the given
     * {@code streamSize}.
     * @throws IllegalArgumentException if {@code streamSize} is negative.
     * @throws NullPointerException if {@code source} is null
     */
    default Stream<SplittableUniformRandomProvider> splits(long streamSize,
                                                           SplittableUniformRandomProvider source) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        Objects.requireNonNull(source, "source");
        return StreamSupport.stream(
            new UniformRandomProviderSupport.ProviderSplitsSpliterator(0, streamSize, source, this), false);
    }

    @Override
    default IntStream ints() {
        return ints(Long.MAX_VALUE);
    }

    @Override
    default IntStream ints(int origin, int bound) {
        return ints(Long.MAX_VALUE, origin, bound);
    }

    @Override
    default IntStream ints(long streamSize) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        return StreamSupport.intStream(
            new UniformRandomProviderSupport.ProviderIntsSpliterator(
                0, streamSize, this, UniformRandomProvider::nextInt), false);
    }

    @Override
    default IntStream ints(long streamSize, int origin, int bound) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        UniformRandomProviderSupport.validateRange(origin, bound);
        return StreamSupport.intStream(
            new UniformRandomProviderSupport.ProviderIntsSpliterator(
                0, streamSize, this, rng -> rng.nextInt(origin, bound)), false);
    }

    @Override
    default LongStream longs() {
        return longs(Long.MAX_VALUE);
    }

    @Override
    default LongStream longs(long origin, long bound) {
        return longs(Long.MAX_VALUE, origin, bound);
    }

    @Override
    default LongStream longs(long streamSize) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        return StreamSupport.longStream(
            new UniformRandomProviderSupport.ProviderLongsSpliterator(
                0, streamSize, this, UniformRandomProvider::nextLong), false);
    }

    @Override
    default LongStream longs(long streamSize, long origin, long bound) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        UniformRandomProviderSupport.validateRange(origin, bound);
        return StreamSupport.longStream(
            new UniformRandomProviderSupport.ProviderLongsSpliterator(
                0, streamSize, this, rng -> rng.nextLong(origin, bound)), false);
    }

    @Override
    default DoubleStream doubles() {
        return doubles(Long.MAX_VALUE);
    }

    @Override
    default DoubleStream doubles(double origin, double bound) {
        return doubles(Long.MAX_VALUE, origin, bound);
    }

    @Override
    default DoubleStream doubles(long streamSize) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        return StreamSupport.doubleStream(
            new UniformRandomProviderSupport.ProviderDoublesSpliterator(
                0, streamSize, this, UniformRandomProvider::nextDouble), false);
    }

    @Override
    default DoubleStream doubles(long streamSize, double origin, double bound) {
        UniformRandomProviderSupport.validateStreamSize(streamSize);
        UniformRandomProviderSupport.validateRange(origin, bound);
        return StreamSupport.doubleStream(
            new UniformRandomProviderSupport.ProviderDoublesSpliterator(
                0, streamSize, this, rng -> rng.nextDouble(origin, bound)), false);
    }
}
