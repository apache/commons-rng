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

import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.RandomStreamsTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests which all {@link SplittableUniformRandomProvider} generators must pass.
 */
class SplittableProvidersParametricTest {
    /** The expected characteristics for the spliterator from the splittable stream. */
    private static final int SPLITERATOR_CHARACTERISTICS =
        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE;

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
     * Thread-safe class for checking the behavior of the SplittableUniformRandomProvider.
     * Generation methods default to ThreadLocalRandom. Split methods return the same instance.
     * This is a functioning generator that can be used as a source to seed splitting.
     */
    private static class ThreadLocalGenerator implements SplittableUniformRandomProvider {
        /** An instance. */
        static final ThreadLocalGenerator INSTANCE = new ThreadLocalGenerator();

        @Override
        public long nextLong() {
            return ThreadLocalRandom.current().nextLong();
        }

        @Override
        public SplittableUniformRandomProvider split(UniformRandomProvider source) {
            return this;
        }
    }

    /**
     * Gets the list of splittable generators.
     *
     * @return the list
     */
    private static Iterable<SplittableUniformRandomProvider> getSplittableProviders() {
        return ProvidersList.listSplittable();
    }

    /**
     * Test that the split methods throw when the source of randomness is null.
     */
    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitThrowsWithNullSource(SplittableUniformRandomProvider generator) {
        Assertions.assertThrows(NullPointerException.class, () -> generator.split(null));
    }

    /**
     * Test that the random generator returned from the split is a new instance of the same class.
     */
    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitReturnsANewInstance(SplittableUniformRandomProvider generator) {
        assertSplitReturnsANewInstance(SplittableUniformRandomProvider::split, generator);
    }

    /**
     * Test that the random generator returned from the split(source) is a new instance of the same class.
     */
    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitWithSourceReturnsANewInstance(SplittableUniformRandomProvider generator) {
        assertSplitReturnsANewInstance(s -> s.split(ThreadLocalGenerator.INSTANCE), generator);
    }

    /**
     * Assert that the random generator returned from the split function is a new instance of the same class.
     *
     * @param splitFunction Split function to test.
     * @param generator RNG under test.
     */
    private static void assertSplitReturnsANewInstance(UnaryOperator<SplittableUniformRandomProvider> splitFunction,
                                                       SplittableUniformRandomProvider generator) {
        final UniformRandomProvider child = splitFunction.apply(generator);
        Assertions.assertNotSame(generator, child, "The child instance should be a different object");
        Assertions.assertEquals(generator.getClass(), child.getClass(), "The child instance should be the same class");
        RandomAssert.assertNextLongNotEquals(10, generator, child);
    }

    /**
     * Test that the split method is reproducible when used with the same generator source in the
     * same state.
     */
    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitWithSourceIsReproducible(SplittableUniformRandomProvider generator) {
        final long seed = ThreadLocalRandom.current().nextLong();
        final UniformRandomProvider rng1 = generator.split(new SplittableRandom(seed)::nextLong);
        final UniformRandomProvider rng2 = generator.split(new SplittableRandom(seed)::nextLong);
        RandomAssert.assertNextLongEquals(10, rng1, rng2);
    }

    /**
     * Test that the other stream splits methods all call the
     * {@link SplittableUniformRandomProvider#splits(long, SplittableUniformRandomProvider)} method.
     * This is tested by checking the spliterator is the same.
     *
     * <p>This test serves to ensure the default implementations in SplittableUniformRandomProvider
     * eventually call the same method. The RNG implementation thus only has to override one method.
     */
    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsMethodsUseSameSpliterator(SplittableUniformRandomProvider generator) {
        final long size = 10;
        final Spliterator<SplittableUniformRandomProvider> s = generator.splits(size, generator).spliterator();
        Assertions.assertEquals(s.getClass(), generator.splits().spliterator().getClass());
        Assertions.assertEquals(s.getClass(), generator.splits(size).spliterator().getClass());
        Assertions.assertEquals(s.getClass(), generator.splits(ThreadLocalGenerator.INSTANCE).spliterator().getClass());
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsSize(SplittableUniformRandomProvider generator) {
        for (final long size : new long[] {0, 1, 7, 13}) {
            Assertions.assertEquals(size, generator.splits(size).count(), "splits");
            Assertions.assertEquals(size, generator.splits(size, ThreadLocalGenerator.INSTANCE).count(), "splits with source");
        }
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplits(SplittableUniformRandomProvider generator) {
        assertSplits(generator, false);
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsParallel(SplittableUniformRandomProvider generator) {
        assertSplits(generator, true);
    }

    /**
     * Test the splits method returns a stream of unique instances. The test uses a
     * fixed source of randomness such that the only randomness is from the stream
     * position.
     *
     * @param generator Generator
     * @param parallel true to use a parallel stream
     */
    private static void assertSplits(SplittableUniformRandomProvider generator, boolean parallel) {
        final long size = 13;
        for (final long seed : new long[] {0, RandomStreamsTestHelper.createSeed(ThreadLocalGenerator.INSTANCE)}) {
            final SplittableUniformRandomProvider source = new SplittableUniformRandomProvider() {
                @Override
                public long nextLong() {
                    return seed;
                }

                @Override
                public SplittableUniformRandomProvider split(UniformRandomProvider source) {
                    return this;
                }
            };
            // Test the assumption that the seed will be passed through (lowest bit is set)
            Assertions.assertEquals(seed | 1, RandomStreamsTestHelper.createSeed(source));

            Stream<SplittableUniformRandomProvider> stream = generator.splits(size, source);
            Assertions.assertFalse(stream.isParallel(), "Initial stream should be sequential");
            if (parallel) {
                stream = stream.parallel();
                Assertions.assertTrue(stream.isParallel(), "Stream should be parallel");
            }

            // Check the instance is a new object of the same type.
            // These will be hashed using the system identity hash code.
            final Set<SplittableUniformRandomProvider> observed = ConcurrentHashMap.newKeySet();
            observed.add(generator);
            stream.forEach(r -> {
                Assertions.assertTrue(observed.add(r), "Instance should be unique");
                Assertions.assertEquals(generator.getClass(), r.getClass());
            });
            // Note: observed contains the original generator so subtract 1
            Assertions.assertEquals(size, observed.size() - 1);

            // Test instances generate different values.
            // The only randomness is from the stream position.
            final long[] values = observed.stream().mapToLong(r -> {
                // Warm up generator with some cycles.
                // E.g. LXM generators return the first value from the initial state.
                for (int i = 0; i < 10; i++) {
                    r.nextLong();
                }
                return r.nextLong();
            }).distinct().toArray();
            // This test is looking for different values.
            // To avoid the rare case of not all distinct we relax the threshold to
            // half the generators. This will spot errors where all generators are
            // the same.
            Assertions.assertTrue(values.length > size / 2,
                () -> "splits did not seed randomness from the stream position. Initial seed = " + seed);
        }
    }

    // Test adapted from stream tests in commons-rng-client-api module

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

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsInvalidStreamSizeThrows(SplittableUniformRandomProvider rng) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.splits(-1), "splits(size)");
        final SplittableUniformRandomProvider source = DummyGenerator.INSTANCE;
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.splits(-1, source), "splits(size, source)");
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsUnlimitedStreamSize(SplittableUniformRandomProvider rng) {
        assertUnlimitedSpliterator(rng.splits().spliterator(), "splits()");
        final SplittableUniformRandomProvider source = ThreadLocalGenerator.INSTANCE;
        assertUnlimitedSpliterator(rng.splits(source).spliterator(), "splits(source)");
    }

    /**
     * Assert the spliterator has an unlimited expected size and the characteristics for a sized
     * immutable stream.
     *
     * @param spliterator Spliterator.
     * @param msg Error message.
     */
    private static void assertUnlimitedSpliterator(Spliterator<?> spliterator, String msg) {
        Assertions.assertEquals(Long.MAX_VALUE, spliterator.estimateSize(), msg);
        Assertions.assertTrue(spliterator.hasCharacteristics(SPLITERATOR_CHARACTERISTICS),
            () -> String.format("%s: characteristics = %s, expected %s", msg,
                Integer.toBinaryString(spliterator.characteristics()),
                Integer.toBinaryString(SPLITERATOR_CHARACTERISTICS)
            ));
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsNullSourceThrows(SplittableUniformRandomProvider rng) {
        final SplittableUniformRandomProvider source = null;
        Assertions.assertThrows(NullPointerException.class, () -> rng.splits(source));
        Assertions.assertThrows(NullPointerException.class, () -> rng.splits(1, source));
    }

    @ParameterizedTest
    @MethodSource("getSplittableProviders")
    void testSplitsSpliterator(SplittableUniformRandomProvider rng) {
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

        // Check the instance is a new object of the same type.
        // These will be hashed using the system identity hash code.
        final HashSet<SplittableUniformRandomProvider> observed = new HashSet<>();
        observed.add(rng);

        final Consumer<SplittableUniformRandomProvider> action = r -> {
            Assertions.assertTrue(observed.add(r), "Instance should be unique");
            Assertions.assertEquals(rng.getClass(), r.getClass());
        };

        // s2. Test advance
        for (long newSize = s2.estimateSize(); newSize-- > 0;) {
            Assertions.assertTrue(s2.tryAdvance(action));
            Assertions.assertEquals(newSize, s2.estimateSize(), "s2 size estimate");
        }
        Assertions.assertFalse(s2.tryAdvance(r -> failSpliteratorShouldBeEmpty()));
        s2.forEachRemaining(r -> failSpliteratorShouldBeEmpty());

        // s3. Test forEachRemaining
        s3.forEachRemaining(action);
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
}
