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
package org.apache.commons.rng.core.util;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Utility for creating streams using a source of randomness.
 */
public final class RandomStreams {
    /** The number of bits of each random character in the seed.
     * The generation algorithm will work if this is in the range [2, 30]. */
    private static final int SEED_CHAR_BITS = 4;

    /**
     * A factory for creating objects using a seed and a using a source of randomness.
     *
     * @param <T> the object type
     */
    public interface ObjectFactory<T> {
        /**
         * Creates the object.
         *
         * @param seed Seed used to initialise the instance.
         * @param source Source of randomness used to initialise the instance.
         * @return the object
         */
        T create(long seed, UniformRandomProvider source);
    }

    /**
     * Class contains only static methods.
     */
    private RandomStreams() {}

    /**
     * Returns a stream producing the given {@code streamSize} number of new objects
     * generated using the supplied {@code source} of randomness using the {@code factory}.
     *
     * <p>A {@code long} seed is provided for each object instance using the stream position
     * and random bits created from the supplied {@code source}.
     *
     * <p>The stream supports parallel execution by splitting the provided {@code source}
     * of randomness. Consequently objects in the same position in the stream created from
     * a sequential stream may be created from a different source of randomness than a parallel
     * stream; it is not expected that parallel execution will create the same final
     * collection of objects.
     *
     * @param <T> the object type
     * @param streamSize Number of objects to generate.
     * @param source A source of randomness used to initialise the new instances; this may
     * be split to provide a source of randomness across a parallel stream.
     * @param factory Factory to create new instances.
     * @return a stream of objects; the stream is limited to the given {@code streamSize}.
     * @throws IllegalArgumentException if {@code streamSize} is negative.
     * @throws NullPointerException if {@code source} or {@code factory} is null
     */
    public static <T> Stream<T> generateWithSeed(long streamSize,
                                                 SplittableUniformRandomProvider source,
                                                 ObjectFactory<T> factory) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("Invalid stream size: " + streamSize);
        }
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(factory, "factory");
        final long seed = createSeed(source);
        return StreamSupport
                .stream(new SeededObjectSpliterator<>(0, streamSize, source, factory, seed), false);
    }

    /**
     * Creates a seed to prepend to a counter. The seed is created to satisfy the following
     * requirements:
     * <ul>
     * <li>The least significant bit is set
     * <li>The seed is composed of characters from an n-bit alphabet
     * <li>The character used in the least significant bits is unique
     * <li>The other characters are sampled uniformly from the remaining (n-1) characters
     * </ul>
     *
     * <p>The composed seed is created using {@code ((seed << shift) | count)}
     * where the shift is applied to ensure non-overlap of the shifted seed and
     * the count. This is achieved by ensuring the lowest 1-bit of the seed is
     * above the highest 1-bit of the count. The shift is a multiple of n to ensure
     * the character used in the least significant bits aligns with higher characters
     * after a shift. As higher characters exclude the least significant character
     * no shifted seed can duplicate previously observed composed seeds. This holds
     * until the least significant character itself is shifted out of the composed seed.
     *
     * <p>The seed generation algorithm starts with a random series of bits with the lowest bit
     * set. Any occurrences of the least significant character in the remaining characters are
     * replaced using {@link UniformRandomProvider#nextInt()}.
     *
     * <p>The remaining characters will be rejected at a rate of 2<sup>-n</sup>. The
     * character size is a compromise between a low rejection rate and the highest supported
     * count that may receive a prepended seed.
     *
     * <p>The JDK's {@code java.util.random} package uses 4-bits for the character size when
     * creating a stream of SplittableGenerator. This achieves a rejection rate
     * of {@code 1/16}. Using this size will require 1 call to generate a {@code long} and
     * on average 1 call to {@code nextInt(15)}. The maximum supported stream size with a unique
     * seed per object is 2<sup>60</sup>. The algorithm here also uses a character size of 4-bits;
     * this simplifies the implementation as there are exactly 16 characters. The algorithm is a
     * different implementation to the JDK and creates an output seed with similar properties.
     *
     * @param rng Source of randomness.
     * @return the seed
     */
    static long createSeed(UniformRandomProvider rng) {
        // Initial random bits. Lowest bit must be set.
        long bits = rng.nextLong() | 1;
        // Mask to extract characters.
        // Can be used to sample from (n-1) n-bit characters.
        final long n = (1 << SEED_CHAR_BITS) - 1;

        // Extract the unique character.
        final long unique = bits & n;

        // Check the rest of the characters do not match the unique character.
        // This loop extracts the remaining characters and replaces if required.
        // This will work if the characters do not evenly divide into 64 as we iterate
        // over the count of remaining bits. The original order is maintained so that
        // if the bits already satisfy the requirements they are unchanged.
        for (int i = SEED_CHAR_BITS; i < Long.SIZE; i += SEED_CHAR_BITS) {
            // Next character
            long c = (bits >>> i) & n;
            if (c == unique) {
                // Branch frequency of 2^-bits.
                // This code is deliberately branchless.
                // Avoid nextInt(n) using: c = floor(n * ([0, 2^32) / 2^32))
                // Rejection rate for non-uniformity will be negligible: 2^32 % 15 == 1
                // so any rejection algorithm only has to exclude 1 value from nextInt().
                c = (n * Integer.toUnsignedLong(rng.nextInt())) >>> Integer.SIZE;
                // Ensure the sample is uniform in [0, n] excluding the unique character
                c = (unique + c + 1) & n;
                // Replace by masking out the current character and bitwise add the new one
                bits = (bits & ~(n << i)) | (c << i);
            }
        }
        return bits;
    }

    /**
     * Spliterator for streams of a given object type that can be created from a seed
     * and source of randomness. The source of randomness is splittable allowing parallel
     * stream support.
     *
     * <p>The seed is mixed with the stream position to ensure each object is created using
     * a unique seed value. As the position increases the seed is left shifted until there
     * is no bit overlap between the seed and the position, i.e the right-most 1-bit of the seed
     * is larger than the left-most 1-bit of the position.
     *s
     * @param <T> the object type
     */
    private static final class SeededObjectSpliterator<T>
            implements Spliterator<T> {
        /** Message when the consumer action is null. */
        private static final String NULL_ACTION = "action must not be null";

        /** The current position in the range. */
        private long position;
        /** The upper limit of the range. */
        private final long end;
        /** Seed used to initialise the new instances. The least significant 1-bit of
         * the seed must be above the most significant bit of the position. This is maintained
         * by left shift when the position is updated. */
        private long seed;
        /** Source of randomness used to initialise the new instances. */
        private final SplittableUniformRandomProvider source;
        /** Factory to create new instances. */
        private final ObjectFactory<T> factory;

        /**
         * @param start Start position of the stream (inclusive).
         * @param end Upper limit of the stream (exclusive).
         * @param source Source of randomness used to initialise the new instances.
         * @param factory Factory to create new instances.
         * @param seed Seed used to initialise the instances. The least significant 1-bit of
         * the seed must be above the most significant bit of the {@code start} position.
         */
        SeededObjectSpliterator(long start, long end,
                                SplittableUniformRandomProvider source,
                                ObjectFactory<T> factory,
                                long seed) {
            position = start;
            this.end = end;
            this.seed = seed;
            this.source = source;
            this.factory = factory;
        }

        @Override
        public long estimateSize() {
            return end - position;
        }

        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE;
        }

        @Override
        public Spliterator<T> trySplit() {
            final long start = position;
            final long middle = (start + end) >>> 1;
            if (middle <= start) {
                return null;
            }
            // The child spliterator can use the same seed as the position does not overlap
            final SeededObjectSpliterator<T> s =
                new SeededObjectSpliterator<>(start, middle, source.split(), factory, seed);
            // Since the position has increased ensure the seed does not overlap
            position = middle;
            while (seed != 0 && Long.compareUnsigned(Long.lowestOneBit(seed), middle) <= 0) {
                seed <<= SEED_CHAR_BITS;
            }
            return s;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, NULL_ACTION);
            final long pos = position;
            if (pos < end) {
                // Advance before exceptions from the action are relayed to the caller
                position = pos + 1;
                action.accept(factory.create(seed | pos, source));
                // If the position overlaps the seed, shift it by 1 character
                if ((position & seed) != 0) {
                    seed <<= SEED_CHAR_BITS;
                }
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action, NULL_ACTION);
            long pos = position;
            final long last = end;
            if (pos < last) {
                // Ensure forEachRemaining is called only once
                position = last;
                final SplittableUniformRandomProvider s = source;
                final ObjectFactory<T> f = factory;
                do {
                    action.accept(f.create(seed | pos, s));
                    pos++;
                    // If the position overlaps the seed, shift it by 1 character
                    if ((pos & seed) != 0) {
                        seed <<= SEED_CHAR_BITS;
                    }
                } while (pos < last);
            }
        }
    }
}
