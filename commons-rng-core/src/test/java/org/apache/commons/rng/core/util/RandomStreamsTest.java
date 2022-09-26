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

import java.util.Arrays;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.RandomStreams.SeededObjectFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link RandomStreams}.
 */
class RandomStreamsTest {
    /** The size in bits of the seed characters. */
    private static final int CHAR_BITS = 4;

    /**
     * Class for outputting a unique sequence from the nextLong() method even under
     * recursive splitting. Splitting creates a new instance.
     */
    private static class SequenceGenerator implements SplittableUniformRandomProvider {
        /** The value for nextLong. */
        private final AtomicLong value;

        /**
         * @param seed Sequence seed value.
         */
        SequenceGenerator(long seed) {
            value = new AtomicLong(seed);
        }

        /**
         * @param value The value for nextLong.
         */
        SequenceGenerator(AtomicLong value) {
            this.value = value;
        }

        @Override
        public long nextLong() {
            return value.getAndIncrement();
        }

        @Override
        public SplittableUniformRandomProvider split(UniformRandomProvider source) {
            // Ignore the source (use of the source is optional)
            return new SequenceGenerator(value);
        }
    }

    /**
     * Class for decoding the combined seed ((seed << shift) | position).
     * Requires the unshifted seed. The shift is assumed to be a multiple of 4.
     * The first call to the consumer will extract the current position.
     * Further calls will compare the value with the predicted value using
     * the last known position.
     */
    private static class SeedDecoder implements Consumer<Long>, LongConsumer {
        /** The initial (unshifted) seed. */
        private final long initial;
        /** The current shifted seed. */
        private long seed;
        /** The last known position. */
        private long position = -1;

        /**
         * @param initial Unshifted seed value.
         */
        SeedDecoder(long initial) {
            this.initial = initial;
        }

        @Override
        public void accept(long value) {
            if (position < 0) {
                // Search for the initial seed value
                seed = initial;
                long mask = -1;
                while (seed != 0 && (value & mask) != seed) {
                    seed <<= CHAR_BITS;
                    mask <<= CHAR_BITS;
                }
                if (seed == 0) {
                    Assertions.fail(() -> String.format("Failed to decode position from %s using seed %s",
                        Long.toBinaryString(value), Long.toBinaryString(initial)));
                }
                // Remove the seed contribution leaving the position
                position = value & ~seed;
            } else {
                // Predict
                final long expected = position + 1;
                //seed = initial;
                while (seed != 0 && Long.compareUnsigned(Long.lowestOneBit(seed), expected) <= 0) {
                    seed <<= CHAR_BITS;
                }
                Assertions.assertEquals(expected | seed, value);
                position = expected;
            }
        }

        @Override
        public void accept(Long t) {
            accept(t.longValue());
        }

        /**
         * Reset the decoder.
         */
        void reset() {
            position = -1;
        }
    }

    /**
     * Test the seed has the required properties:
     * <ul>
     * <li>Test the seed has an odd character in the least significant position
     * <li>Test the remaining characters in the seed do not match this character
     * <li>Test the distribution of characters is uniform
     * <ul>
     *
     * <p>The test assumes the character size is 4-bits.
     *
     * @param seed the seed
     */
    @ParameterizedTest
    @ValueSource(longs = {1628346812812L})
    void testCreateSeed(long seed) {
        final UniformRandomProvider rng = new SplittableRandom(seed)::nextLong;

        // Histogram the distribution for each unique 4-bit character
        final int m = (1 << CHAR_BITS) - 1;
        // Number of remaining characters
        final int n = (int) Math.ceil((Long.SIZE - CHAR_BITS) / CHAR_BITS);
        final int[][] h = new int[m + 1][m + 1];
        final int samples = 1 << 16;
        for (int i = 0; i < samples; i++) {
            long s = RandomStreams.createSeed(rng);
            final int unique = (int) (s & m);
            for (int j = 0; j < n; j++) {
                s >>>= CHAR_BITS;
                h[unique][(int) (s & m)]++;
            }
        }

        // Test unique characters are always odd.
        final int[] empty = new int[m + 1];
        for (int i = 0; i <= m; i += 2) {
            Assertions.assertArrayEquals(empty, h[i], "Even histograms should be empty");
        }

        // Test unique characters are not repeated
        for (int i = 1; i <= m; i += 2) {
            Assertions.assertEquals(0, h[i][i]);
        }

        // Chi-square test the distribution of unique characters
        final long[] sum = new long[(m + 1) / 2];
        for (int i = 1; i <= m; i += 2) {
            final long total = Arrays.stream(h[i]).sum();
            Assertions.assertEquals(0, total % n, "Samples should be a multiple of the number of characters");
            sum[i / 2] = total / n;
        }

        assertChiSquare(sum, () -> "Unique character distribution");

        // Chi-square test the distribution for each unique character.
        // Note: This will fail if the characters do not evenly divide into 64.
        // In that case the expected values are not uniform as the final
        // character will be truncated and skew the expected values to lower characters.
        // For simplicity this has not been accounted for as 4-bits evenly divides 64.
        Assertions.assertEquals(0, Long.SIZE % CHAR_BITS, "Character distribution cannot be tested as uniform");
        for (int i = 1; i <= m; i += 2) {
            final long[] obs = Arrays.stream(h[i]).filter(c -> c != 0).asLongStream().toArray();
            final int c = i;
            assertChiSquare(obs, () -> "Other character distribution for unique character " + c);
        }
    }

    /**
     * Assert the observations are uniform using a chi-square test.
     *
     * @param obs Observations.
     * @param msg Failure message prefix.
     */
    private static void assertChiSquare(long[] obs, Supplier<String> msg) {
        final ChiSquareTest t = new ChiSquareTest();
        final double alpha = 0.001;
        final double[] expected = new double[obs.length];
        Arrays.fill(expected, 1.0 / obs.length);
        final double p = t.chiSquareTest(expected, obs);
        Assertions.assertFalse(p < alpha, () -> String.format("%s: chi2 p-value: %s < %s", msg.get(), p, alpha));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void testGenerateWithSeedInvalidStreamSizeThrows(long size) {
        final SplittableUniformRandomProvider source = new SequenceGenerator(0);
        final SeededObjectFactory<Long> factory = (s, r) -> Long.valueOf(s);
        final IllegalArgumentException ex1 = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RandomStreams.generateWithSeed(size, source, factory));
        // Check the exception method is consistent with UniformRandomProvider stream methods
        final IllegalArgumentException ex2 = Assertions.assertThrows(IllegalArgumentException.class,
            () -> source.ints(size));
        Assertions.assertEquals(ex2.getMessage(), ex1.getMessage(), "Inconsistent exception message");
    }

    @Test
    void testGenerateWithSeedNullArgumentThrows() {
        final long size = 10;
        final SplittableUniformRandomProvider source = new SequenceGenerator(0);
        final SeededObjectFactory<Long> factory = (s, r) -> Long.valueOf(s);
        Assertions.assertThrows(NullPointerException.class,
            () -> RandomStreams.generateWithSeed(size, null, factory));
        Assertions.assertThrows(NullPointerException.class,
            () -> RandomStreams.generateWithSeed(size, source, null));
    }

    /**
     * Test that the seed passed to the factory is ((seed << shift) | position).
     * This is done by creating an initial seed value of 1. When removed the
     * remaining values should be a sequence.
     *
     * @param threads Number of threads.
     * @param streamSize Stream size.
     */
    @ParameterizedTest
    @CsvSource({
        "1, 23",
        "4, 31",
        "4, 3",
        "8, 127",
    })
    void testGenerateWithSeed(int threads, long streamSize) throws InterruptedException, ExecutionException {
        // Provide a generator that results in the seed being set as 1.
        final SplittableUniformRandomProvider rng = new SplittableUniformRandomProvider() {
            @Override
            public long nextLong() {
                return 1;
            }

            @Override
            public SplittableUniformRandomProvider split(UniformRandomProvider source) {
                return this;
            }
        };
        Assertions.assertEquals(1, RandomStreams.createSeed(rng), "Unexpected seed value");

        // Create a factory that will return the seed passed to the factory
        final SeededObjectFactory<Long> factory = (s, r) -> {
            Assertions.assertSame(rng, r, "The source RNG is not used");
            return Long.valueOf(s);
        };

        // Stream in a custom pool
        final ForkJoinPool threadPool = new ForkJoinPool(threads);
        Long[] values;
        try {
            values = threadPool.submit(() ->
                RandomStreams.generateWithSeed(streamSize, rng, factory).parallel().toArray(Long[]::new)).get();
        } finally {
            threadPool.shutdown();
        }

        // Remove the highest 1 bit from each long. The rest should be a sequence.
        final long[] actual = Arrays.stream(values).mapToLong(Long::longValue)
                .map(l -> l - Long.highestOneBit(l)).sorted().toArray();
        final long[] expected = LongStream.range(0, streamSize).toArray();
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void testGenerateWithSeedSpliteratorThrows() {
        final long size = 10;
        final SplittableUniformRandomProvider source = new SequenceGenerator(0);
        final SeededObjectFactory<Long> factory = (s, r) -> Long.valueOf(s);
        final Spliterator<Long> s1 = RandomStreams.generateWithSeed(size, source, factory).spliterator();
        final Consumer<Long> badAction = null;
        final NullPointerException ex1 = Assertions.assertThrows(NullPointerException.class, () -> s1.tryAdvance(badAction), "tryAdvance");
        final NullPointerException ex2 = Assertions.assertThrows(NullPointerException.class, () -> s1.forEachRemaining(badAction), "forEachRemaining");
        // Check the exception method is consistent with UniformRandomProvider stream methods
        final Spliterator.OfInt s2 = source.ints().spliterator();
        final NullPointerException ex3 = Assertions.assertThrows(NullPointerException.class, () -> s2.tryAdvance((IntConsumer) null), "tryAdvance");
        Assertions.assertEquals(ex3.getMessage(), ex1.getMessage(), "Inconsistent tryAdvance exception message");
        Assertions.assertEquals(ex3.getMessage(), ex2.getMessage(), "Inconsistent forEachRemaining exception message");
    }

    @Test
    void testGenerateWithSeedSpliterator() {
        // Create an initial seed value. This should not be modified by the algorithm
        // when generating a 'new' seed from the RNG.
        final long initial = RandomStreams.createSeed(new SplittableRandom()::nextLong);
        final SplittableUniformRandomProvider rng = new SplittableUniformRandomProvider() {
            @Override
            public long nextLong() {
                return initial;
            }

            @Override
            public SplittableUniformRandomProvider split(UniformRandomProvider source) {
                return this;
            }
        };
        Assertions.assertEquals(initial, RandomStreams.createSeed(rng), "Unexpected seed value");

        // Create a factory that will return the seed passed to the factory
        final SeededObjectFactory<Long> factory = (s, r) -> {
            Assertions.assertSame(rng, r, "The source RNG is not used");
            return Long.valueOf(s);
        };

        // Split a large spliterator into four smaller ones;
        // each is used to test different functionality
        final long size = 41;
        Spliterator<Long> s1 = RandomStreams.generateWithSeed(size, rng, factory).spliterator();
        Assertions.assertEquals(size, s1.estimateSize());
        Assertions.assertTrue(s1.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE),
            "Invalid characteristics");
        final Spliterator<Long> s2 = s1.trySplit();
        final Spliterator<Long> s3 = s1.trySplit();
        final Spliterator<Long> s4 = s2.trySplit();
        Assertions.assertEquals(size, s1.estimateSize() + s2.estimateSize() + s3.estimateSize() + s4.estimateSize());

        // s1. Test cannot split indefinitely
        while (s1.estimateSize() > 1) {
            final long currentSize = s1.estimateSize();
            final Spliterator<Long> other = s1.trySplit();
            Assertions.assertEquals(currentSize, s1.estimateSize() + other.estimateSize());
            s1 = other;
        }
        Assertions.assertNull(s1.trySplit(), "Cannot split when size <= 1");

        // Create an action that will decode the shift and position using the
        // known initial seed. This can be used to predict and assert the next value.
        final SeedDecoder action = new SeedDecoder(initial);

        // s2. Test advance
        for (long newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance(action));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        final Consumer<Long> throwIfCalled = r -> Assertions.fail("spliterator should be empty");
        Assertions.assertFalse(s2.tryAdvance(throwIfCalled));
        s2.forEachRemaining(throwIfCalled);

        // s3. Test forEachRemaining
        action.reset();
        s3.forEachRemaining(action);
        Assertions.assertEquals(0, s3.estimateSize());
        s3.forEachRemaining(throwIfCalled);

        // s4. Test tryAdvance and forEachRemaining when the action throws an exception
        final IllegalStateException ex = new IllegalStateException();
        final Consumer<Long> badAction = r -> {
            throw ex;
        };
        final long currentSize = s4.estimateSize();
        Assertions.assertTrue(currentSize > 1, "Spliterator requires more elements to test advance");
        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.tryAdvance(badAction)));
        Assertions.assertEquals(currentSize - 1, s4.estimateSize(), "Spliterator should be advanced even when action throws");

        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.forEachRemaining(badAction)));
        Assertions.assertEquals(0, s4.estimateSize(), "Spliterator should be finished even when action throws");
        s4.forEachRemaining(throwIfCalled);
    }

    /**
     * Test a very large stream size above 2<sup>60</sup>.
     * In this case it is not possible to prepend a 4-bit character
     * to the stream position. The seed passed to the factory will be the stream position.
     */
    @Test
    void testLargeStreamSize() {
        // Create an initial seed value. This should not be modified by the algorithm
        // when generating a 'new' seed from the RNG.
        final long initial = RandomStreams.createSeed(new SplittableRandom()::nextLong);
        final SplittableUniformRandomProvider rng = new SplittableUniformRandomProvider() {
            @Override
            public long nextLong() {
                return initial;
            }

            @Override
            public SplittableUniformRandomProvider split(UniformRandomProvider source) {
                return this;
            }
        };
        Assertions.assertEquals(initial, RandomStreams.createSeed(rng), "Unexpected seed value");

        // Create a factory that will return the seed passed to the factory
        final SeededObjectFactory<Long> factory = (s, r) -> {
            Assertions.assertSame(rng, r, "The source RNG is not used");
            return Long.valueOf(s);
        };

        final Spliterator<Long> s = RandomStreams.generateWithSeed(1L << 62, rng, factory).spliterator();

        // Split uses a divide-by-two approach. The child uses the smaller half.
        final Spliterator<Long> s1 = s.trySplit();

        // Lower half. The next position can be predicted using the decoder.
        final SeedDecoder action = new SeedDecoder(initial);
        long size = s1.estimateSize();
        for (int i = 1; i <= 5; i++) {
            Assertions.assertTrue(s1.tryAdvance(action));
            Assertions.assertEquals(size - i, s1.estimateSize(), "s1 size estimate");
        }

        // Upper half. This should be just the stream position which we can
        // collect with a call to advance.
        final long[] expected = {0};
        s.tryAdvance(seed -> expected[0] = seed);
        size = s.estimateSize();
        for (int i = 1; i <= 5; i++) {
            Assertions.assertTrue(s.tryAdvance(seed -> Assertions.assertEquals(++expected[0], seed)));
            Assertions.assertEquals(size - i, s.estimateSize(), "s size estimate");
        }
    }
}
