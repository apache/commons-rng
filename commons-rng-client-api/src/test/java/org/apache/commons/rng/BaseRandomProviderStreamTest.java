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

import java.util.Spliterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for default stream method implementations in {@link UniformRandomProvider} and derived
 * interfaces.
 *
 * <p>This class exists to test that {@link UniformRandomProvider} and any derived interface that
 * overloads the base implementation function identically for the stream based methods. Stream
 * methods are asserted to call the corresponding single value generation method in the interface.
 */
abstract class BaseRandomProviderStreamTest {
    private static final long STREAM_SIZE_ONE = 1;

    static Stream<Arguments> invalidNextIntOriginBound() {
        return UniformRandomProviderTest.invalidNextIntOriginBound();
    }

    static Stream<Arguments> invalidNextLongOriginBound() {
        return UniformRandomProviderTest.invalidNextLongOriginBound();
    }

    static Stream<Arguments> invalidNextDoubleOriginBound() {
        return UniformRandomProviderTest.invalidNextDoubleOriginBound();
    }

    static long[] streamSizes() {
        return new long[] {0, 1, 13};
    }

    /**
     * Creates the provider used to test the stream methods.
     * The instance will be used to verify the following conditions:
     * <ul>
     * <li>Invalid stream sizes
     * <li>Unspecified stream size has an iterator that initially reports Long.MAX_VALUE
     * <li>Invalid bounds for the bounded stream methods
     * </ul>
     *
     * @return the uniform random provider
     */
    abstract UniformRandomProvider create();

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextInt()} method. All other primitive
     * generation methods should raise an exception to ensure the
     * {@link UniformRandomProvider#ints()} method calls the correct generation
     * method.
     *
     * @param values Values to return from the generation method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createInts(int[] values);

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextInt(int, int)} method. All other primitive
     * generation methods should raise an exception to ensure the
     * {@link UniformRandomProvider#ints(int, int)} method calls the correct
     * generation method.
     *
     * @param values Values to return from the generation method.
     * @param origin Origin for the generation method. Can be asserted to match the argument passed to the method.
     * @param bound Bound for the generation method. Can be asserted to match the argument passed to the method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createInts(int[] values, int origin, int bound);

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextLong()} method.
     * All other primitive generation methods should raise an exception to
     * ensure the {@link UniformRandomProvider#longs()} method calls the correct
     * generation method.
     *
     * @param values Values to return from the generation method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createLongs(long[] values);

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextLong(long, long)} method.
     * All other primitive generation methods should raise an exception to
     * ensure the {@link UniformRandomProvider#longs(long, long)} method calls the correct
     * generation method.
     *
     * @param values Values to return from the generation method.
     * @param origin Origin for the generation method. Can be asserted to match the argument passed to the method.
     * @param bound Bound for the generation method. Can be asserted to match the argument passed to the method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createLongs(long[] values, long origin, long bound);

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextDouble()} method.
     * All other primitive generation methods should raise an exception to
     * ensure the {@link UniformRandomProvider#doubles()} method calls the correct
     * generation method.
     *
     * @param values Values to return from the generation method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createDoubles(double[] values);

    /**
     * Creates the provider using the specified {@code values} for the
     * {@link UniformRandomProvider#nextDouble(double, double)} method.
     * All other primitive generation methods should raise an exception to
     * ensure the {@link UniformRandomProvider#doubles(double, double)} method calls the correct
     * generation method.
     *
     * @param values Values to return from the generation method.
     * @param origin Origin for the generation method. Can be asserted to match the argument passed to the method.
     * @param bound Bound for the generation method. Can be asserted to match the argument passed to the method.
     * @return the uniform random provider
     */
    abstract UniformRandomProvider createDoubles(double[] values, double origin, double bound);

    /**
     * Gets the expected stream characteristics for the initial stream created with unlimited size.
     *
     * @return the characteristics
     */
    abstract int getCharacteristics();

    @ParameterizedTest
    @ValueSource(longs = {-1, -2, Long.MIN_VALUE})
    void testInvalidStreamSizeThrows(long size) {
        final UniformRandomProvider rng = create();
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.ints(size), "ints()");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.ints(size, 1, 42), "ints(lower, upper)");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.longs(size), "longs()");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.longs(size, 3L, 33L), "longs(lower, upper)");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.doubles(size), "doubles()");
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.doubles(size, 1.5, 2.75), "doubles(lower, upper)");
    }

    @Test
    void testUnlimitedStreamSize() {
        final UniformRandomProvider rng = create();
        assertUnlimitedSpliterator(rng.ints().spliterator(), "ints()");
        assertUnlimitedSpliterator(rng.ints(1, 42).spliterator(), "ints(lower, upper)");
        assertUnlimitedSpliterator(rng.longs().spliterator(), "longs()");
        assertUnlimitedSpliterator(rng.longs(1627384682623L, 32676823622343L).spliterator(), "longs(lower, upper)");
        assertUnlimitedSpliterator(rng.doubles().spliterator(), "doubles()");
        assertUnlimitedSpliterator(rng.doubles(1.5, 2.75).spliterator(), "doubles(lower, upper)");
    }

    /**
     * Assert the spliterator has an unlimited expected size and the characteristics specified
     * by {@link #getCharacteristics()}.
     *
     * @param spliterator Spliterator.
     * @param msg Error message.
     */
    private void assertUnlimitedSpliterator(Spliterator<?> spliterator, String msg) {
        assertSpliterator(spliterator, Long.MAX_VALUE, getCharacteristics(), msg);
    }

    /**
     * Assert the spliterator has the expected size and characteristics.
     *
     * @param spliterator Spliterator.
     * @param expectedSize Expected size.
     * @param characteristics Expected characteristics.
     * @param msg Error message.
     * @see Spliterator#hasCharacteristics(int)
     */
    static void assertSpliterator(Spliterator<?> spliterator, long expectedSize, int characteristics, String msg) {
        Assertions.assertEquals(expectedSize, spliterator.estimateSize(), msg);
        Assertions.assertTrue(spliterator.hasCharacteristics(characteristics),
            () -> String.format("%s: characteristics = %s, expected %s", msg,
                Integer.toBinaryString(spliterator.characteristics()),
                Integer.toBinaryString(characteristics)
            ));
    }

    // Test stream methods throw immediately for invalid range arguments.

    @ParameterizedTest
    @MethodSource(value = {"invalidNextIntOriginBound"})
    void testIntsOriginBoundThrows(int origin, int bound) {
        final UniformRandomProvider rng = create();
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.ints(origin, bound));
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.ints(STREAM_SIZE_ONE, origin, bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextLongOriginBound"})
    void testLongsOriginBoundThrows(long origin, long bound) {
        final UniformRandomProvider rng = create();
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.longs(origin, bound));
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.longs(STREAM_SIZE_ONE, origin, bound));
    }

    @ParameterizedTest
    @MethodSource(value = {"invalidNextDoubleOriginBound"})
    void testDoublesOriginBoundThrows(double origin, double bound) {
        final UniformRandomProvider rng = create();
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.doubles(origin, bound));
        Assertions.assertThrows(IllegalArgumentException.class, () -> rng.doubles(STREAM_SIZE_ONE, origin, bound));
    }

    // Test stream methods call the correct generation method in the UniformRandomProvider.
    // If range arguments are supplied they are asserted to be passed through.
    // Streams are asserted to be sequential.

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testInts(long streamSize) {
        final int[] values = ThreadLocalRandom.current().ints(streamSize).toArray();
        final UniformRandomProvider rng = createInts(values);
        final IntStream stream = rng.ints();
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testIntsOriginBound(long streamSize) {
        final int origin = 13;
        final int bound = 42;
        final int[] values = ThreadLocalRandom.current().ints(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createInts(values, origin, bound);
        final IntStream stream = rng.ints(origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testIntsWithSize(long streamSize) {
        final int[] values = ThreadLocalRandom.current().ints(streamSize).toArray();
        final UniformRandomProvider rng = createInts(values);
        final IntStream stream = rng.ints(streamSize);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testIntsOriginBoundWithSize(long streamSize) {
        final int origin = 13;
        final int bound = 42;
        final int[] values = ThreadLocalRandom.current().ints(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createInts(values, origin, bound);
        final IntStream stream = rng.ints(streamSize, origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testLongs(long streamSize) {
        final long[] values = ThreadLocalRandom.current().longs(streamSize).toArray();
        final UniformRandomProvider rng = createLongs(values);
        final LongStream stream = rng.longs();
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testLongsOriginBound(long streamSize) {
        final long origin = 26278368423L;
        final long bound = 422637723236L;
        final long[] values = ThreadLocalRandom.current().longs(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createLongs(values, origin, bound);
        final LongStream stream = rng.longs(origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testLongsWithSize(long streamSize) {
        final long[] values = ThreadLocalRandom.current().longs(streamSize).toArray();
        final UniformRandomProvider rng = createLongs(values);
        final LongStream stream = rng.longs(streamSize);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testLongsOriginBoundWithSize(long streamSize) {
        final long origin = 26278368423L;
        final long bound = 422637723236L;
        final long[] values = ThreadLocalRandom.current().longs(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createLongs(values, origin, bound);
        final LongStream stream = rng.longs(streamSize, origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testDoubles(long streamSize) {
        final double[] values = ThreadLocalRandom.current().doubles(streamSize).toArray();
        final UniformRandomProvider rng = createDoubles(values);
        final DoubleStream stream = rng.doubles();
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testDoublesOriginBound(long streamSize) {
        final double origin = 1.23;
        final double bound = 4.56;
        final double[] values = ThreadLocalRandom.current().doubles(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createDoubles(values, origin, bound);
        final DoubleStream stream = rng.doubles(origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.limit(streamSize).toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testDoublesWithSize(long streamSize) {
        final double[] values = ThreadLocalRandom.current().doubles(streamSize).toArray();
        final UniformRandomProvider rng = createDoubles(values);
        final DoubleStream stream = rng.doubles(streamSize);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }

    @ParameterizedTest
    @MethodSource(value = {"streamSizes"})
    void testDoublesOriginBoundWithSize(long streamSize) {
        final double origin = 1.23;
        final double bound = 4.56;
        final double[] values = ThreadLocalRandom.current().doubles(streamSize, origin, bound).toArray();
        final UniformRandomProvider rng = createDoubles(values, origin, bound);
        final DoubleStream stream = rng.doubles(streamSize, origin, bound);
        Assertions.assertFalse(stream.isParallel());
        Assertions.assertArrayEquals(values, stream.toArray());
    }
}
