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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for split method implementations in
 * {@link SplittableUniformRandomProvider}.
 *
 * <p>This class verifies all exception conditions for the split methods and the
 * arguments to the methods to stream RNGs. Exception conditions and sequential
 * (default) output from the primitive stream methods are tested in
 * {@link SplittableUniformRandomProviderStreamTest}.
 *
 * <p>Parallel streams (RNGs and primitives) are tested using a splittable
 * generator that outputs a unique sequence using an atomic counter that is
 * thread-safe.
 */
class SplittableUniformRandomProviderTest {
    private static final long STREAM_SIZE_ONE = 1;
    /** The expected characteristics for the spliterator from the splittable stream. */
    private static final int SPLITERATOR_CHARACTERISTICS =
        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE;

    /**
     * Dummy class for checking the behavior of the SplittableUniformRandomProvider.
     * All generation and split methods throw an exception. This can be used to test
     * exception conditions for arguments to default stream functions.
     */
    private static class DummyGenerator implements SplittableUniformRandomProvider {
        /** An instance. */
        static final DummyGenerator INSTANCE = new DummyGenerator();

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("The nextLong method should not be invoked");
        }

        @Override
        public SplittableUniformRandomProvider split(UniformRandomProvider source) {
            throw new UnsupportedOperationException("The split(source) method should not be invoked");
        }
    }

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
     * Class for outputting a fixed value from the nextLong() method even under
     * recursive splitting. Splitting creates a new instance seeded with the nextLong value
     * from the source of randomness. This can be used to distinguish self-seeding from
     * seeding with an alternative source.
     */
    private class FixedGenerator implements SplittableUniformRandomProvider {
        /** The value for nextLong. */
        private final long value;

        /**
         * @param value Fixed value.
         */
        FixedGenerator(long value) {
            this.value = value;
        }

        @Override
        public long nextLong() {
            return value;
        }

        @Override
        public SplittableUniformRandomProvider split(UniformRandomProvider source) {
            return new FixedGenerator(source.nextLong());
        }
    }

    /**
     * Class to track recursive splitting and iterating over a fixed set of values.
     * Splitting without a source of randomness returns the same instance; with a
     * source of randomness will throw an exception. All generation methods throw an
     * exception.
     *
     * <p>An atomic counter is maintained to allow concurrent return of unique
     * values from a fixed array. The values are expected to be maintained in child
     * classes. Any generation methods that are overridden for tests should
     * be thread-safe, e.g. returning {@code values[count.getAndIncrement()]}.
     *
     * <p>A count of the number of splits is maintained. This is not used for assertions
     * to avoid test failures that may occur when streams are split differently, or not
     * at all, by the current JVM. The count can be used to debug splitting behavior
     * on JVM implementations.
     */
    private static class CountingGenerator extends DummyGenerator {
        /** The split count. Incrementded when the generator is split. */
        protected final AtomicInteger splitCount = new AtomicInteger();
        /** The count of returned values. */
        protected final AtomicInteger count = new AtomicInteger();

        @Override
        public SplittableUniformRandomProvider split() {
            splitCount.getAndIncrement();
            return this;
        }
    }

    /**
     * Class to return the same instance when splitting without a source of randomness;
     * with a source of randomness will throw an exception. All generation methods
     * throw an exception. Any generation methods that are overridden for tests should
     * be thread-safe.
     */
    private abstract static class SingleInstanceGenerator extends DummyGenerator {
        @Override
        public SplittableUniformRandomProvider split() {
            return this;
        }
    }

    /**
     * Thread and stream sizes used to test parallel streams.
     *
     * @return the arguments
     */
    static Stream<Arguments> threadAndStreamSizes() {
        return Stream.of(
            Arguments.of(1, 16),
            Arguments.of(2, 16),
            Arguments.of(4, 16),
            Arguments.of(8, 16),
            Arguments.of(4, 2),
            Arguments.of(8, 4)
        );
    }

    /**
     * Execute the task in a ForkJoinPool with the specified level of parallelism. Any
     * parallel stream executing in the task should be limited to the specified level of
     * parallelism.
     *
     * <p><b>Note</b>
     *
     * <p>This is a JDK undocumented feature of streams to use the enclosing ForkJoinPool
     * in-place of {@link ForkJoinPool#commonPool()}; this behaviour may be subject to
     * change.
     *
     * <p>Here the intention is to force the parallel stream to execute with a varying
     * number of threads. Note that debugging using the {@link CountingGenerator}
     * indicates that the number of splits is not influenced by the enclosing pool
     * parallelism but rather the number of stream elements and possibly the
     * <em>standard</em> number of available processors. Further testing on Linux using
     * {@code numactl -C 1} to limit the number of processors returns 1 for
     * {@link ForkJoinPool#getCommonPoolParallelism()} and
     * {@link Runtime#availableProcessors()} with no change in the number of splits
     * performed by parallel streams. This indicates the splitting of parallel streams may
     * not respect the limits imposed on the executing JVM. However this does mean that
     * tests using this method do test the splitting of the stream, irrespective of
     * configured parallelism when executed on a machine that has multiple CPU cores, i.e.
     * the <em>potential</em> for parallelism.
     *
     * <p>It is unknown if the parallel streams will split when executed on a true single-core
     * JVM such as that provided by a continuous integration build environment running for
     * example in a virtual machine.
     *
     * @param <T> Return type of the task.
     * @param parallelism Level of parallelism.
     * @param task Task.
     * @return the task result
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    private static <T> T execute(int parallelism, Callable<T> task) throws InterruptedException, ExecutionException {
        final ForkJoinPool threadPool = new ForkJoinPool(parallelism);
        try {
            return threadPool.submit(task).get();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Helper method to raise an assertion error inside an action passed to a Spliterator
     * when the action should not be invoked.
     *
     * @see Spliterator#tryAdvance(Consumer)
     * @see Spliterator#forEachRemaining(Consumer)
     */
    private static void failSpliteratorShouldBeEmpty() {
        Assertions.fail("Spliterator should not have any remaining elements");
    }

    @Test
    void testDefaultSplit() {
        // Create the split result so we can check the return value
        final SplittableUniformRandomProvider expected = new DummyGenerator();
        // Implement split(UniformRandomProvider)
        final SplittableUniformRandomProvider rng = new DummyGenerator() {
            @Override
            public SplittableUniformRandomProvider split(UniformRandomProvider source) {
                Assertions.assertSame(this, source, "default split should use itself as the source");
                return expected;
            }
        };
        // Test the default split()
        Assertions.assertSame(expected, rng.split());
    }

    // Tests for splitting the stream of splittable RNGs

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void testSplitsInvalidStreamSizeThrows(long size) {
        final SplittableUniformRandomProvider rng = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.splits(size), "splits(size)");
        final SplittableUniformRandomProvider source = new SequenceGenerator(42);
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.splits(size, source), "splits(size, source)");
    }

    @Test
    void testSplitsUnlimitedStreamSize() {
        final SplittableUniformRandomProvider rng = DummyGenerator.INSTANCE;
        assertUnlimitedSpliterator(rng.splits().spliterator(), "splits()");
        final SplittableUniformRandomProvider source = new SequenceGenerator(42);
        assertUnlimitedSpliterator(rng.splits(source).spliterator(), "splits(source)");
    }

    /**
     * Assert the spliterator has an unlimited expected size and the characteristics for a sized
     * non-null immutable stream.
     *
     * @param spliterator Spliterator.
     * @param msg Error message.
     */
    private static void assertUnlimitedSpliterator(Spliterator<?> spliterator, String msg) {
        BaseRandomProviderStreamTest.assertSpliterator(spliterator, Long.MAX_VALUE, SPLITERATOR_CHARACTERISTICS, msg);
    }

    @Test
    void testSplitsNullSourceThrows() {
        final SplittableUniformRandomProvider rng = DummyGenerator.INSTANCE;
        final SplittableUniformRandomProvider source = null;
        Assertions.assertThrows(NullPointerException.class, () -> rng.splits(source));
        Assertions.assertThrows(NullPointerException.class, () -> rng.splits(STREAM_SIZE_ONE, source));
    }

    /**
     * Test the splits method. The test asserts that a parallel stream of RNGs output a
     * sequence using a specialised sequence generator that maintains the sequence output
     * under recursive splitting.
     */
    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testSplitsParallel(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final long start = Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt());
        final long[] actual = execute(threads, (Callable<long[]>) () -> {
            // The splits method will use itself as the source and the output should be the sequence
            final SplittableUniformRandomProvider rng = new SequenceGenerator(start);
            final SplittableUniformRandomProvider[] rngs =
                    rng.splits(streamSize).parallel().toArray(SplittableUniformRandomProvider[]::new);
            // Check the instance is a new object of the same type.
            // These will be hashed using the system identity hash code.
            final HashSet<SplittableUniformRandomProvider> observed = new HashSet<>();
            observed.add(rng);
            Arrays.stream(rngs).forEach(r -> {
                Assertions.assertTrue(observed.add(r), "Instance should be unique");
                Assertions.assertEquals(SequenceGenerator.class, r.getClass());
            });
            // Get output from the unique RNGs: these return from the same atomic sequence
            return Arrays.stream(rngs).mapToLong(UniformRandomProvider::nextLong).toArray();
        });
        // Required to reorder the sequence to ascending
        Arrays.sort(actual);
        final long[] expected = LongStream.range(start, start + streamSize).toArray();
        Assertions.assertArrayEquals(expected, actual);
    }

    /**
     * Test the splits method. The test asserts that a parallel stream of RNGs output a
     * sequence using a specialised sequence generator that maintains the sequence output
     * under recursive splitting. The sequence is used to seed a fixed generator. The stream
     * instances are verified to be the correct class type.
     */
    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testSplitsParallelWithSource(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final long start = Integer.toUnsignedLong(ThreadLocalRandom.current().nextInt());
        final long[] actual = execute(threads, (Callable<long[]>) () -> {
            // This generator defines the instances created.
            // It should not be split without a source.
            // Seed with something not the start value.
            final SplittableUniformRandomProvider rng = new FixedGenerator(~start) {
                @Override
                public SplittableUniformRandomProvider split() {
                    throw new UnsupportedOperationException("The split method should not be invoked");
                }
            };
            // The splits method will use this to seed each instance.
            // This generator is split within the spliterator.
            final SplittableUniformRandomProvider source = new SequenceGenerator(start);
            final SplittableUniformRandomProvider[] rngs =
                rng.splits(streamSize, source).parallel().toArray(SplittableUniformRandomProvider[]::new);
            // Check the instance is a new object of the same type.
            // These will be hashed using the system identity hash code.
            final HashSet<SplittableUniformRandomProvider> observed = new HashSet<>();
            observed.add(rng);
            Arrays.stream(rngs).forEach(r -> {
                Assertions.assertTrue(observed.add(r), "Instance should be unique");
                Assertions.assertEquals(FixedGenerator.class, r.getClass());
            });
            // Get output from the unique RNGs: these return from the same atomic sequence
            return Arrays.stream(rngs).mapToLong(UniformRandomProvider::nextLong).toArray();
        });
        // Required to reorder the sequence to ascending
        Arrays.sort(actual);
        final long[] expected = LongStream.range(start, start + streamSize).toArray();
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void testSplitsSpliterator() {
        final int start = 42;
        final SplittableUniformRandomProvider rng = new SequenceGenerator(start);

        // Split a large spliterator into four smaller ones;
        // each is used to test different functionality
        final long size = 41;
        Spliterator<SplittableUniformRandomProvider> s1 = rng.splits(size).spliterator();
        Assertions.assertEquals(size, s1.estimateSize());
        final Spliterator<SplittableUniformRandomProvider> s2 = s1.trySplit();
        final Spliterator<SplittableUniformRandomProvider> s3 = s1.trySplit();
        final Spliterator<SplittableUniformRandomProvider> s4 = s2.trySplit();
        Assertions.assertEquals(size, s1.estimateSize() + s2.estimateSize() + s3.estimateSize() + s4.estimateSize());

        // s1. Test cannot split indefinitely
        while (s1.estimateSize() > 1) {
            final long currentSize = s1.estimateSize();
            final Spliterator<SplittableUniformRandomProvider> other = s1.trySplit();
            Assertions.assertEquals(currentSize, s1.estimateSize() + other.estimateSize());
            s1 = other;
        }
        Assertions.assertNull(s1.trySplit(), "Cannot split when size <= 1");

        // The expected value is incremented for each generation call
        final long[] expected = {start};

        // s2. Test advance
        for (long newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance(r -> Assertions.assertEquals(expected[0]++, r.nextLong())));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        Assertions.assertFalse(s2.tryAdvance(r -> failSpliteratorShouldBeEmpty()));
        s2.forEachRemaining(r -> failSpliteratorShouldBeEmpty());

        // s3. Test forEachRemaining
        s3.forEachRemaining(r -> Assertions.assertEquals(expected[0]++, r.nextLong()));
        Assertions.assertEquals(0, s3.estimateSize());
        s3.forEachRemaining(r -> failSpliteratorShouldBeEmpty());

        // s4. Test tryAdvance and forEachRemaining when the action throws an exception
        final IllegalStateException ex = new IllegalStateException();
        final Consumer<SplittableUniformRandomProvider> badAction = r -> {
            throw ex;
        };
        final long currentSize = s4.estimateSize();
        Assertions.assertTrue(currentSize > 1, "Spliterator requires more elements to test advance");
        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.tryAdvance(badAction)));
        Assertions.assertEquals(currentSize - 1, s4.estimateSize(), "Spliterator should be advanced even when action throws");

        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.forEachRemaining(badAction)));
        Assertions.assertEquals(0, s4.estimateSize(), "Spliterator should be finished even when action throws");
        s4.forEachRemaining(r -> failSpliteratorShouldBeEmpty());
    }

    // Tests for splitting the primitive streams to test support for parallel execution

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testIntsParallelWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final int[] values = ThreadLocalRandom.current().ints(streamSize).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public int nextInt() {
                return values[count.getAndIncrement()];
            }
        };
        final int[] actual = execute(threads, (Callable<int[]>) () ->
            rng.ints(streamSize).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testIntsParallelOriginBoundWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final int origin = 13;
        final int bound = 42;
        final int[] values = ThreadLocalRandom.current().ints(streamSize, origin, bound).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public int nextInt(int o, int b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[count.getAndIncrement()];
            }
        };
        final int[] actual = execute(threads, (Callable<int[]>) () ->
            rng.ints(streamSize, origin, bound).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @Test
    void testIntsSpliterator() {
        final int start = 42;
        final SplittableUniformRandomProvider rng = new SingleInstanceGenerator() {
            private final AtomicInteger value = new AtomicInteger(start);

            @Override
            public int nextInt() {
                return value.getAndIncrement();
            }
        };

        // Split a large spliterator into four smaller ones;
        // each is used to test different functionality
        final long size = 41;
        Spliterator.OfInt s1 = rng.ints(size).spliterator();
        Assertions.assertEquals(size, s1.estimateSize());
        final Spliterator.OfInt s2 = s1.trySplit();
        final Spliterator.OfInt s3 = s1.trySplit();
        final Spliterator.OfInt s4 = s2.trySplit();
        Assertions.assertEquals(size, s1.estimateSize() + s2.estimateSize() + s3.estimateSize() + s4.estimateSize());

        // s1. Test cannot split indefinitely
        while (s1.estimateSize() > 1) {
            final long currentSize = s1.estimateSize();
            final Spliterator.OfInt other = s1.trySplit();
            Assertions.assertEquals(currentSize, s1.estimateSize() + other.estimateSize());
            s1 = other;
        }
        Assertions.assertNull(s1.trySplit(), "Cannot split when size <= 1");

        // The expected value is incremented for each generation call
        final int[] expected = {start};

        // s2. Test advance
        for (long newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance((IntConsumer) i -> Assertions.assertEquals(expected[0]++, i)));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        Assertions.assertFalse(s2.tryAdvance((IntConsumer) i -> failSpliteratorShouldBeEmpty()));
        s2.forEachRemaining((IntConsumer) i -> failSpliteratorShouldBeEmpty());

        // s3. Test forEachRemaining
        s3.forEachRemaining((IntConsumer) i -> Assertions.assertEquals(expected[0]++, i));
        Assertions.assertEquals(0, s3.estimateSize());
        s3.forEachRemaining((IntConsumer) i -> failSpliteratorShouldBeEmpty());

        // s4. Test tryAdvance and forEachRemaining when the action throws an exception
        final IllegalStateException ex = new IllegalStateException();
        final IntConsumer badAction = i -> {
            throw ex;
        };
        final long currentSize = s4.estimateSize();
        Assertions.assertTrue(currentSize > 1, "Spliterator requires more elements to test advance");
        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.tryAdvance(badAction)));
        Assertions.assertEquals(currentSize - 1, s4.estimateSize(), "Spliterator should be advanced even when action throws");

        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.forEachRemaining(badAction)));
        Assertions.assertEquals(0, s4.estimateSize(), "Spliterator should be finished even when action throws");
        s4.forEachRemaining((IntConsumer) i -> failSpliteratorShouldBeEmpty());
    }

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testLongsParallelWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final long[] values = ThreadLocalRandom.current().longs(streamSize).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public long nextLong() {
                return values[count.getAndIncrement()];
            }
        };
        final long[] actual = execute(threads, (Callable<long[]>) () ->
            rng.longs(streamSize).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testLongsParallelOriginBoundWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final long origin = 195267376168313L;
        final long bound = 421268681268318L;
        final long[] values = ThreadLocalRandom.current().longs(streamSize, origin, bound).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public long nextLong(long o, long b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[count.getAndIncrement()];
            }
        };
        final long[] actual = execute(threads, (Callable<long[]>) () ->
            rng.longs(streamSize, origin, bound).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @Test
    void testLongsSpliterator() {
        final long start = 42;
        final SplittableUniformRandomProvider rng = new SingleInstanceGenerator() {
            private final AtomicLong value = new AtomicLong(start);

            @Override
            public long nextLong() {
                return value.getAndIncrement();
            }
        };

        // Split a large spliterator into four smaller ones;
        // each is used to test different functionality
        final long size = 41;
        Spliterator.OfLong s1 = rng.longs(size).spliterator();
        Assertions.assertEquals(size, s1.estimateSize());
        final Spliterator.OfLong s2 = s1.trySplit();
        final Spliterator.OfLong s3 = s1.trySplit();
        final Spliterator.OfLong s4 = s2.trySplit();
        Assertions.assertEquals(size, s1.estimateSize() + s2.estimateSize() + s3.estimateSize() + s4.estimateSize());

        // s1. Test cannot split indefinitely
        while (s1.estimateSize() > 1) {
            final long currentSize = s1.estimateSize();
            final Spliterator.OfLong other = s1.trySplit();
            Assertions.assertEquals(currentSize, s1.estimateSize() + other.estimateSize());
            s1 = other;
        }
        Assertions.assertNull(s1.trySplit(), "Cannot split when size <= 1");

        // The expected value is incremented for each generation call
        final long[] expected = {start};

        // s2. Test advance
        for (long newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance((LongConsumer) i -> Assertions.assertEquals(expected[0]++, i)));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        Assertions.assertFalse(s2.tryAdvance((LongConsumer) i -> failSpliteratorShouldBeEmpty()));
        s2.forEachRemaining((LongConsumer) i -> failSpliteratorShouldBeEmpty());

        // s3. Test forEachRemaining
        s3.forEachRemaining((LongConsumer) i -> Assertions.assertEquals(expected[0]++, i));
        Assertions.assertEquals(0, s3.estimateSize());
        s3.forEachRemaining((LongConsumer) i -> failSpliteratorShouldBeEmpty());

        // s4. Test tryAdvance and forEachRemaining when the action throws an exception
        final IllegalStateException ex = new IllegalStateException();
        final LongConsumer badAction = i -> {
            throw ex;
        };
        final long currentSize = s4.estimateSize();
        Assertions.assertTrue(currentSize > 1, "Spliterator requires more elements to test advance");
        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.tryAdvance(badAction)));
        Assertions.assertEquals(currentSize - 1, s4.estimateSize(), "Spliterator should be advanced even when action throws");

        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.forEachRemaining(badAction)));
        Assertions.assertEquals(0, s4.estimateSize(), "Spliterator should be finished even when action throws");
        s4.forEachRemaining((LongConsumer) i -> failSpliteratorShouldBeEmpty());
    }

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testDoublesParallelWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final double[] values = ThreadLocalRandom.current().doubles(streamSize).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public double nextDouble() {
                return values[count.getAndIncrement()];
            }
        };
        final double[] actual = execute(threads, (Callable<double[]>) () ->
            rng.doubles(streamSize).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @ParameterizedTest
    @MethodSource(value = {"threadAndStreamSizes"})
    void testDoublesParallelOriginBoundWithSize(int threads, long streamSize) throws InterruptedException, ExecutionException {
        final double origin = 0.123;
        final double bound = 0.789;
        final double[] values = ThreadLocalRandom.current().doubles(streamSize, origin, bound).toArray();
        final CountingGenerator rng = new CountingGenerator() {
            @Override
            public double nextDouble(double o, double b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[count.getAndIncrement()];
            }
        };
        final double[] actual = execute(threads, (Callable<double[]>) () ->
            rng.doubles(streamSize, origin, bound).parallel().toArray()
        );
        Arrays.sort(values);
        Arrays.sort(actual);
        Assertions.assertArrayEquals(values, actual);
    }

    @Test
    void testDoublesSpliterator() {
        // Due to lack of an AtomicDouble this uses an AtomicInteger. Any int value can be
        // represented as a double and the increment operator functions without loss of
        // precision (the same is not true if using an AtomicLong with >53 bits of precision).
        final int start = 42;
        final SplittableUniformRandomProvider rng = new SingleInstanceGenerator() {
            private final AtomicInteger value = new AtomicInteger(start);

            @Override
            public double nextDouble() {
                return value.getAndIncrement();
            }
        };

        // Split a large spliterator into four smaller ones;
        // each is used to test different functionality
        final long size = 41;
        Spliterator.OfDouble s1 = rng.doubles(size).spliterator();
        Assertions.assertEquals(size, s1.estimateSize());
        final Spliterator.OfDouble s2 = s1.trySplit();
        final Spliterator.OfDouble s3 = s1.trySplit();
        final Spliterator.OfDouble s4 = s2.trySplit();
        Assertions.assertEquals(size, s1.estimateSize() + s2.estimateSize() + s3.estimateSize() + s4.estimateSize());

        // s1. Test cannot split indefinitely
        while (s1.estimateSize() > 1) {
            final double currentSize = s1.estimateSize();
            final Spliterator.OfDouble other = s1.trySplit();
            Assertions.assertEquals(currentSize, s1.estimateSize() + other.estimateSize());
            s1 = other;
        }
        Assertions.assertNull(s1.trySplit(), "Cannot split when size <= 1");

        // The expected value is incremented for each generation call
        final double[] expected = {start};

        // s2. Test advance
        for (double newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance((DoubleConsumer) i -> Assertions.assertEquals(expected[0]++, i)));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        Assertions.assertFalse(s2.tryAdvance((DoubleConsumer) i -> failSpliteratorShouldBeEmpty()));
        s2.forEachRemaining((DoubleConsumer) i -> failSpliteratorShouldBeEmpty());

        // s3. Test forEachRemaining
        s3.forEachRemaining((DoubleConsumer) i -> Assertions.assertEquals(expected[0]++, i));
        Assertions.assertEquals(0, s3.estimateSize());
        s3.forEachRemaining((DoubleConsumer) i -> failSpliteratorShouldBeEmpty());

        // s4. Test tryAdvance and forEachRemaining when the action throws an exception
        final IllegalStateException ex = new IllegalStateException();
        final DoubleConsumer badAction = i -> {
            throw ex;
        };
        final double currentSize = s4.estimateSize();
        Assertions.assertTrue(currentSize > 1, "Spliterator requires more elements to test advance");
        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.tryAdvance(badAction)));
        Assertions.assertEquals(currentSize - 1, s4.estimateSize(), "Spliterator should be advanced even when action throws");

        Assertions.assertSame(ex, Assertions.assertThrows(IllegalStateException.class, () -> s4.forEachRemaining(badAction)));
        Assertions.assertEquals(0, s4.estimateSize(), "Spliterator should be finished even when action throws");
        s4.forEachRemaining((DoubleConsumer) i -> failSpliteratorShouldBeEmpty());
    }
}
