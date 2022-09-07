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
package org.apache.commons.rng.simple;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.SplittableRandom;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source64.LongProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RandomSource}.
 */
class RandomSourceTest {
    @Test
    void testCreateInt() {
        final int n = 4;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(RandomSource.createInt(),
                                       RandomSource.createInt());
        }
    }

    @Test
    void testCreateLong() {
        final int n = 6;
        for (int i = 0; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(RandomSource.createLong(),
                                       RandomSource.createLong());
        }
    }

    @Test
    void testCreateIntArray() {
        final int n = 13;
        final int[] seed = RandomSource.createIntArray(n);
        Assertions.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(seed[i - 1], seed[i]);
        }
    }

    @Test
    void testCreateLongArray() {
        final int n = 9;
        final long[] seed = RandomSource.createLongArray(n);
        Assertions.assertEquals(n, seed.length);

        for (int i = 1; i < n; i++) {
            // Can fail, but unlikely given the range.
            Assertions.assertNotEquals(seed[i - 1], seed[i]);
        }
    }

    @Test
    void testIsJumpable() {
        Assertions.assertFalse(RandomSource.JDK.isJumpable(), "JDK is not Jumpable");
        Assertions.assertTrue(RandomSource.XOR_SHIFT_1024_S_PHI.isJumpable(), "XOR_SHIFT_1024_S_PHI is Jumpable");
        Assertions.assertTrue(RandomSource.XO_SHI_RO_256_SS.isJumpable(), "XO_SHI_RO_256_SS is Jumpable");
    }

    @Test
    void testIsLongJumpable() {
        Assertions.assertFalse(RandomSource.JDK.isLongJumpable(), "JDK is not LongJumpable");
        Assertions.assertFalse(RandomSource.XOR_SHIFT_1024_S_PHI.isLongJumpable(), "XOR_SHIFT_1024_S_PHI is not LongJumpable");
        Assertions.assertTrue(RandomSource.XO_SHI_RO_256_SS.isLongJumpable(), "XO_SHI_RO_256_SS is LongJumpable");
    }

    @Test
    void testIsSplittable() {
        Assertions.assertFalse(RandomSource.JDK.isSplittable(), "JDK is not Splittable");
        Assertions.assertTrue(RandomSource.L32_X64_MIX.isSplittable(), "L32_X64_MIX is Splittable");
        Assertions.assertTrue(RandomSource.L64_X128_MIX.isSplittable(), "L64_X128_MIX is Splittable");
    }

    /**
     * MSWS should not infinite loop if the input RNG fails to provide randomness to create a seed.
     * See RNG-175.
     */
    @Test
    void testMSWSCreateSeed() {
        final LongProvider broken = new LongProvider() {
            @Override
            public long next() {
                return 0;
            }
        };
        Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            RandomSource.MSWS.createSeed(broken);
        });
    }

    /**
     * Test the unrestorable method correctly delegates all methods.
     * This includes the methods with default implementations in UniformRandomProvider, i.e.
     * the default methods should not be used.
     */
    @Test
    void testUnrestorable() {
        // The test class should override all default methods
        assertNoDefaultMethods(RestorableRNG.class);

        final UniformRandomProvider rng1 = new RestorableRNG();
        final RestorableRNG r = new RestorableRNG();
        final UniformRandomProvider rng2 = RandomSource.unrestorable(r);
        Assertions.assertNotSame(rng2, r);

        // The unrestorable instance should override all default methods
        assertNoDefaultMethods(rng2.getClass());

        // Despite the newly created RNG not being a RestorableUniformRandomProvider,
        // the method still wraps it in a new object. This is the behaviour since version 1.0.
        // It allows the method to wrap objects that implement UniformRandomProvider (such
        // as sub-interfaces or concrete classes) to prevent access to any additional methods.
        Assertions.assertNotSame(rng2, RandomSource.unrestorable(rng2));

        // Ensure that they generate the same values.
        RandomAssert.assertProduceSameSequence(rng1, rng2);

        // Cast must work.
        @SuppressWarnings("unused")
        final RestorableUniformRandomProvider restorable = (RestorableUniformRandomProvider) rng1;
        // Cast must fail.
        Assertions.assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unused")
            RestorableUniformRandomProvider dummy = (RestorableUniformRandomProvider) rng2;
        });
    }

    /**
     * Assert the class has overridden all default public interface methods.
     *
     * @param cls the class
     */
    private static void assertNoDefaultMethods(Class<?> cls) {
        for (final Method method : cls.getMethods()) {
            if ((method.getModifiers() & Modifier.PUBLIC) != 0) {
                Assertions.assertTrue(!method.isDefault(),
                    () -> cls.getName() + " should override method: " + method.toGenericString());
            }
        }
    }

    /**
     * Class to provide a complete implementation of the {@link UniformRandomProvider} interface.
     * This must return a different result than the default implementations in the interface.
     */
    private static class RestorableRNG implements RestorableUniformRandomProvider {
        /** The source of randomness. */
        private final SplittableRandom rng = new SplittableRandom(123);

        @Override
        public void nextBytes(byte[] bytes) {
            nextBytes(bytes, 0, bytes.length);
        }

        @Override
        public void nextBytes(byte[] bytes, int start, int len) {
            RestorableUniformRandomProvider.super.nextBytes(bytes, start, len);
            // Rotate
            for (int i = start + len; i-- > start;) {
                bytes[i] += 1;
            }
        }

        @Override
        public int nextInt() {
            return RestorableUniformRandomProvider.super.nextInt() + 1;
        }

        @Override
        public int nextInt(int n) {
            final int v = RestorableUniformRandomProvider.super.nextInt(n) + 1;
            return v == n ? 0 : v;
        }

        @Override
        public int nextInt(int origin, int bound) {
            final int v = RestorableUniformRandomProvider.super.nextInt(origin, bound) + 1;
            return v == bound ? origin : v;
        }

        @Override
        public long nextLong() {
            // Source of randomness for all derived methods
            return rng.nextLong();
        }

        @Override
        public long nextLong(long n) {
            final long v = RestorableUniformRandomProvider.super.nextLong(n) + 1;
            return v == n ? 0 : v;
        }

        @Override
        public long nextLong(long origin, long bound) {
            final long v = RestorableUniformRandomProvider.super.nextLong(origin, bound) + 1;
            return v == bound ? origin : v;
        }

        @Override
        public boolean nextBoolean() {
            return !RestorableUniformRandomProvider.super.nextBoolean();
        }

        @Override
        public float nextFloat() {
            final float v = 1 - RestorableUniformRandomProvider.super.nextFloat();
            return v == 1 ? 0 : v;
        }

        @Override
        public float nextFloat(float bound) {
            final float v = Math.nextUp(RestorableUniformRandomProvider.super.nextFloat(bound));
            return v == bound ? 0 : v;
        }

        @Override
        public float nextFloat(float origin, float bound) {
            final float v = Math.nextUp(RestorableUniformRandomProvider.super.nextFloat(origin, bound));
            return v == bound ? 0 : v;
        }

        @Override
        public double nextDouble() {
            final double v = 1 - RestorableUniformRandomProvider.super.nextDouble();
            return v == 1 ? 0 : v;
        }

        @Override
        public double nextDouble(double bound) {
            final double v = Math.nextUp(RestorableUniformRandomProvider.super.nextDouble(bound));
            return v == bound ? 0 : v;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            final double v = Math.nextUp(RestorableUniformRandomProvider.super.nextDouble(origin, bound));
            return v == bound ? 0 : v;
        }

        // Stream methods must return different values than the default so we reimplement them

        @Override
        public IntStream ints() {
            return IntStream.generate(() -> nextInt() + 1).sequential();
        }

        @Override
        public IntStream ints(int origin, int bound) {
            return IntStream.generate(() -> {
                final int v = nextInt(origin, bound) + 1;
                return v == bound ? origin : v;
            }).sequential();
        }

        @Override
        public IntStream ints(long streamSize) {
            return ints().limit(streamSize);
        }

        @Override
        public IntStream ints(long streamSize, int origin, int bound) {
            return ints(origin, bound).limit(streamSize);
        }

        @Override
        public LongStream longs() {
            return LongStream.generate(() -> nextLong() + 1).sequential();
        }

        @Override
        public LongStream longs(long origin, long bound) {
            return LongStream.generate(() -> {
                final long v = nextLong(origin, bound) + 1;
                return v == bound ? origin : v;
            }).sequential();
        }

        @Override
        public LongStream longs(long streamSize) {
            return longs().limit(streamSize);
        }

        @Override
        public LongStream longs(long streamSize, long origin, long bound) {
            return longs(origin, bound).limit(streamSize);
        }

        @Override
        public DoubleStream doubles() {
            return DoubleStream.generate(() -> {
                final double v = Math.nextUp(nextDouble());
                return v == 1 ? 0 : v;
            }).sequential();
        }

        @Override
        public DoubleStream doubles(double origin, double bound) {
            return DoubleStream.generate(() -> {
                final double v = Math.nextUp(nextDouble(origin, bound));
                return v == bound ? origin : v;
            }).sequential();
        }

        @Override
        public DoubleStream doubles(long streamSize) {
            return doubles().limit(streamSize);
        }

        @Override
        public DoubleStream doubles(long streamSize, double origin, double bound) {
            return doubles(origin, bound).limit(streamSize);
        }

        @Override
        public RandomProviderState saveState() {
            // Do nothing
            return null;
        }

        @Override
        public void restoreState(RandomProviderState state) {
            // Do nothing
        }
    }
}
