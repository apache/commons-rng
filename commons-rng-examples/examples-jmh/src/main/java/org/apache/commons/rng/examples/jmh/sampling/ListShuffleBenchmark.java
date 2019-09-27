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

package org.apache.commons.rng.examples.jmh.sampling;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.sampling.ListSampler;
import org.apache.commons.rng.sampling.PermutationSampler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.RandomAccess;
import java.util.concurrent.TimeUnit;

/**
 * Executes benchmark to compare the speed of shuffling a {@link List}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class ListShuffleBenchmark {
    /** 2^32. Used for the nextInt(int) algorithm. */
    private static final long POW_32 = 1L << 32;

    /**
     * The size threshold for using the random access algorithm
     * when the list does not implement RandomAccess.
     */
    private static final int RANDOM_ACCESS_SIZE_THRESHOLD = 5;

    /**
     * The data for the shuffle. Contains the data size and the random generators.
     */
    @State(Scope.Benchmark)
    public static class ShuffleData {
        /**
         * The list size.
         */
        @Param({"10", "100", "1000", "10000"})
        private int size;

        /** The UniformRandomProvider instance. */
        private UniformRandomProvider rng;

        /** The Random instance. */
        private Random random;

        /**
         * Gets the size.
         *
         * @return the size
         */
        public int getSize() {
            return size;
        }

        /**
         * Gets the uniform random provider.
         *
         * @return the uniform random provider
         */
        public UniformRandomProvider getRNG() {
            return rng;
        }

        /**
         * Gets the random.
         *
         * @return the random
         */
        public Random getRandom() {
            return random;
        }

        /**
         * Create the random generators.
         */
        @Setup
        public void setupGenerators() {
            final long seed = System.currentTimeMillis();
            rng = new SplitMix32RNG(seed);
            random = new SplitMix32Random(seed);
        }
    }

    /**
     * The list to shuffle. Either an ArrayList or a LinkedList.
     */
    @State(Scope.Benchmark)
    public static class ListData extends ShuffleData {
        /**
         * The list type.
         */
        @Param({"Array", "Linked"})
        private String type;

        /** The list. */
        private List<Integer> list;

        /**
         * Gets the list.
         *
         * @return the list
         */
        public List<Integer> getList() {
            return list;
        }

        /**
         * Create the list.
         */
        @Setup
        public void setupList() {
            if ("Array".equals(type)) {
                list = new ArrayList<>();
            } else if ("Linked".equals(type)) {
                list = new LinkedList<>();
            }
            for (int i = 0; i < getSize(); i++) {
                list.add(i);
            }
        }
    }

    /**
     * The LinkedList to shuffle.
     *
     * <p>This is used to determine the threshold to switch from the direct shuffle of a list
     * without RandomAccess to the iterator based version.</p>
     */
    @State(Scope.Benchmark)
    public static class LinkedListData {
        /**
         * The list size. The java.utils.Collections.shuffle method switches at size 5.
         */
        @Param({"3", "4", "5", "6", "7", "8"})
        private int size;

        /** The UniformRandomProvider instance. */
        private UniformRandomProvider rng;

        /** The list. */
        private List<Integer> list;

        /**
         * Gets the uniform random provider.
         *
         * @return the uniform random provider
         */
        public UniformRandomProvider getRNG() {
            return rng;
        }

        /**
         * Gets the list.
         *
         * @return the list
         */
        public List<Integer> getList() {
            return list;
        }

        /**
         * Create the list.
         */
        @Setup
        public void setup() {
            rng = new SplitMix32RNG(System.currentTimeMillis());
            list = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                list.add(i);
            }
        }
    }

    /**
     * Implement the SplitMix algorithm from {@link java.util.SplittableRandom
     * SplittableRandom} to output 32-bit int values. Expose this through the
     * {@link UniformRandomProvider} API.
     */
    static final class SplitMix32RNG extends IntProvider {
        /** The state. */
        private long state;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        SplitMix32RNG(long seed) {
            state = seed;
        }

        @Override
        public int next() {
            long key = state += 0x9e3779b97f4a7c15L;
            // 32 high bits of Stafford variant 4 mix64 function as int:
            // http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
            key = (key ^ (key >>> 33)) * 0x62a9d9ed799705f5L;
            return (int) (((key ^ (key >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
        }

        @Override
        public int nextInt(int n) {
            // No check for positive n.
            // Implement the Lemire method to create an integer in a range.

            long m = (next() & 0xffffffffL) * n;
            long l = m & 0xffffffffL;
            if (l < n) {
                // 2^32 % n
                final long t = POW_32 % n;
                while (l < t) {
                    m = (next() & 0xffffffffL) * n;
                    l = m & 0xffffffffL;
                }
            }
            return (int) (m >>> 32);
        }
    }


    /**
     * Implement the SplitMix algorithm from {@link java.util.SplittableRandom
     * SplittableRandom} to output 32-bit int values. Expose this through the
     * {@link java.util.Random} API.
     */
    static final class SplitMix32Random extends Random {
        private static final long serialVersionUID = 1L;

        /** The state. */
        private long state;

        /**
         * Create a new instance.
         *
         * @param seed the seed
         */
        SplitMix32Random(long seed) {
            state = seed;
        }

        /**
         * Return the next value.
         *
         * @return the value
         */
        private int next() {
            long key = state += 0x9e3779b97f4a7c15L;
            // 32 high bits of Stafford variant 4 mix64 function as int:
            // http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
            key = (key ^ (key >>> 33)) * 0x62a9d9ed799705f5L;
            return (int) (((key ^ (key >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
        }

        @Override
        public int nextInt(int n) {
            // No check for positive n.
            // Implement the Lemire method to create an integer in a range.

            long m = (next() & 0xffffffffL) * n;
            long l = m & 0xffffffffL;
            if (l < n) {
                // 2^32 % n
                final long t = POW_32 % n;
                while (l < t) {
                    m = (next() & 0xffffffffL) * n;
                    l = m & 0xffffffffL;
                }
            }
            return (int) (m >>> 32);
        }

        @Override
        protected int next(int n) {
            // For the Random implementation. This is unused.
            return next() >>> (32 - n);
        }
    }

    /**
     * ListSampler shuffle from version 1.2 delegates to the PermutationSampler.
     *
     * @param <T> Type of the list items.
     * @param rng Random number generator.
     * @param list List.
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed to the beginning or end of the list.
     */
    private static <T> void shuffleUsingPermutationSampler(UniformRandomProvider rng, List<T> list,
            int start, boolean towardHead) {
        final int len = list.size();
        final int[] indices = PermutationSampler.natural(len);
        PermutationSampler.shuffle(rng, indices, start, towardHead);

        final ArrayList<T> items = new ArrayList<>(list);
        for (int i = 0; i < len; i++) {
            list.set(i, items.get(indices[i]));
        }
    }

    /**
     * ListSampler shuffle from version 1.2 delegates to the PermutationSampler.
     * Modified for RandomAccess lists.
     *
     * @param <T> Type of the list items.
     * @param rng Random number generator.
     * @param list List.
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed to the beginning or end of the list.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> void shuffleUsingPermutationSamplerRandomAccess(UniformRandomProvider rng, List<T> list,
            int start, boolean towardHead) {
        final int len = list.size();
        final int[] indices = PermutationSampler.natural(len);
        PermutationSampler.shuffle(rng, indices, start, towardHead);

        // Copy back.
        final ArrayList<T> items = new ArrayList<>(list);
        final int low = towardHead ? 0 : start;
        final int high = towardHead ? start + 1 : len;
        if (list instanceof RandomAccess) {
            for (int i = low; i < high; i++) {
                list.set(i, items.get(indices[i]));
            }
        } else {
            // Copy back. Use raw types.
            final ListIterator it = list.listIterator(low);
            for (int i = low; i < high; i++) {
                it.next();
                it.set(items.get(indices[i]));
            }
        }
    }

    /**
     * Direct shuffle on the list adapted from JDK java.util.Collections.
     * This handles RandomAccess lists.
     *
     * @param rng Random number generator.
     * @param list List.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void shuffleDirectRandomAccess(UniformRandomProvider rng, List<?> list) {
        if (list instanceof RandomAccess || list.size() < RANDOM_ACCESS_SIZE_THRESHOLD) {
            // Shuffle list in-place
            for (int i = list.size(); i > 1; i--) {
                swap(list, i - 1, rng.nextInt(i));
            }
        } else {
            // Shuffle as an array
            final Object[] array = list.toArray();
            for (int i = array.length; i > 1; i--) {
                swap(array, i - 1, rng.nextInt(i));
            }

            // Copy back. Use raw types.
            final ListIterator it = list.listIterator();
            for (final Object value : array) {
                it.next();
                it.set(value);
            }
        }
    }

    /**
     * A direct list shuffle.
     *
     * @param rng Random number generator.
     * @param list List.
     */
    private static void shuffleDirect(UniformRandomProvider rng, List<?> list) {
        for (int i = list.size(); i > 1; i--) {
            swap(list, i - 1, rng.nextInt(i));
        }
    }

    /**
     * A list shuffle using an iterator.
     *
     * @param rng Random number generator.
     * @param list List.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void shuffleIterator(UniformRandomProvider rng, List<?> list) {
        final Object[] array = list.toArray();

        // Shuffle array
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }

        // Copy back. Use raw types.
        final ListIterator it = list.listIterator();
        for (final Object value : array) {
            it.next();
            it.set(value);
        }
    }

    /**
     * Direct shuffle on the list adapted from JDK java.util.Collections.
     * This has been modified to handle the directional shuffle from a start index.
     *
     * @param rng Random number generator.
     * @param list List.
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed to the beginning or end of the list.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void shuffleDirectRandomAccessDirectional(UniformRandomProvider rng, List<?> list,
            int start, boolean towardHead) {
        final int size = list.size();
        if (list instanceof RandomAccess || size < RANDOM_ACCESS_SIZE_THRESHOLD) {
            if (towardHead) {
                for (int i = start; i > 0; i--) {
                    swap(list, i, rng.nextInt(i + 1));
                }
            } else {
                for (int i = size - 1; i > start; i--) {
                    swap(list, i, start + rng.nextInt(i + 1 - start));
                }
            }
        } else {
            final Object[] array = list.toArray();

            // Shuffle array
            if (towardHead) {
                for (int i = start; i > 0; i--) {
                    swap(array, i, rng.nextInt(i + 1));
                }
                // Copy back. Use raw types.
                final ListIterator it = list.listIterator();
                for (int i = 0; i <= start; i++) {
                    it.next();
                    it.set(array[i]);
                }
            } else {
                for (int i = size - 1; i > start; i--) {
                    swap(array, i, start + rng.nextInt(i + 1 - start));
                }
                // Copy back. Use raw types.
                final ListIterator it = list.listIterator(start);
                for (int i = start; i < array.length; i++) {
                    it.next();
                    it.set(array[i]);
                }
            }
        }
    }

    /**
     * Direct shuffle on the list using the JDK java.util.Collections method with a sub-list
     * to handle the directional shuffle from a start index.
     *
     * @param rng Random number generator.
     * @param list List.
     * @param start Index at which shuffling begins.
     * @param towardHead Shuffling is performed to the beginning or end of the list.
     */
    private static void shuffleDirectRandomAccessSubList(UniformRandomProvider rng, List<?> list,
            int start, boolean towardHead) {
        if (towardHead) {
            shuffleDirectRandomAccess(rng, list.subList(0, start + 1));
        } else {
            shuffleDirectRandomAccess(rng, list.subList(start, list.size()));
        }
    }

    /**
     * Swaps the two specified elements in the specified list.
     *
     * @param list List.
     * @param i First index.
     * @param j Second index.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void swap(List<?> list, int i, int j) {
        // Use raw type
        final List l = list;
        l.set(i, l.set(j, l.get(i)));
    }

    /**
     * Swaps the two specified elements in the specified array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(Object[] array, int i, int j) {
        final Object tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Baseline a shuffle using the Random.
     * This is the in java.util.Collections that decrements to above one.
     * This should be the same speed as the benchmark using UniformRandomProvider.
     *
     * @param data Shuffle data.
     * @return the sum
     */
    @Benchmark
    public int baselineRandom(ShuffleData data) {
        int sum = 0;
        for (int i = data.getSize(); i > 1; i--) {
            // A shuffle would swap (i-1) and j=nextInt(i)
            sum += (i - 1) * data.getRandom().nextInt(i);
        }
        return sum;
    }

    /**
     * Baseline a shuffle using the UniformRandomProvider.
     * This should be the same speed as the benchmark using Random.
     *
     * @param data Shuffle data.
     * @return the sum
     */
    @Benchmark
    public int baselineRNG(ShuffleData data) {
        int sum = 0;
        for (int i = data.getSize(); i > 1; i--) {
            // A shuffle would swap (i-1) and j=nextInt(i)
            sum += (i - 1) * data.getRNG().nextInt(i);
        }
        return sum;
    }

    /**
     * Baseline a shuffle using the UniformRandomProvider.
     * This should be the same speed as the benchmark using Random.
     *
     * @param data Shuffle data.
     * @return the sum
     */
    @Benchmark
    public int baselineRNG2(ShuffleData data) {
        int sum = 0;
        for (int i = data.getSize(); i > 1; i--) {
            // A shuffle would swap j=nextInt(i) and (i-1)
            sum += data.getRNG().nextInt(i) * (i - 1);
        }
        return sum;
    }

    /**
     * Baseline a shuffle using the UniformRandomProvider.
     * This should be the same speed as the benchmark using Random.
     * This uses a variant that decrements to above zero so that the index i is one
     * of the indices to swap. This is included to determine if there is a difference.
     *
     * @param data Shuffle data.
     * @return the sum
     */
    @Benchmark
    public int baselineRNG3(ShuffleData data) {
        int sum = 0;
        for (int i = data.getSize() - 1; i > 0; i--) {
            // A shuffle would swap i and j=nextInt(i+1)
            sum += i * data.getRNG().nextInt(i + 1);
        }
        return sum;
    }

    /**
     * Performs a shuffle using java.utils.Collections.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingCollections(ListData data) {
        Collections.shuffle(data.getList(), data.getRandom());
        return data.getList();
    }

    /**
     * Performs a shuffle using ListSampler shuffle method from version 1.2 which delegates
     * to the PermuationSampler.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingPermutationSampler(ListData data) {
        shuffleUsingPermutationSampler(data.getRNG(), data.getList(), data.getSize() - 1, true);
        return data.getList();
    }

    /**
     * Performs a shuffle using ListSampler shuffle method from version 1.2 which delegates
     * to the PermuationSampler.
     * This performs two part shuffles from the middle
     * towards the head and then towards the end.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingPermutationSamplerBidirectional(ListData data) {
        final int start = data.getSize() / 2;
        shuffleUsingPermutationSampler(data.getRNG(), data.getList(), start, true);
        shuffleUsingPermutationSampler(data.getRNG(), data.getList(), start + 1, false);
        return data.getList();
    }

    /**
     * Performs a shuffle using ListSampler shuffle method from version 1.2 which delegates
     * to the PermuationSampler. The method has been modified to detect RandomAccess lists.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingPermutationSamplerRandomAccess(ListData data) {
        shuffleUsingPermutationSamplerRandomAccess(data.getRNG(), data.getList(), data.getSize() - 1, true);
        return data.getList();
    }

    /**
     * Performs a shuffle using ListSampler shuffle method from version 1.2 which delegates
     * to the PermuationSampler. The method has been modified to detect RandomAccess lists.
     * This performs two part shuffles from the middle
     * towards the head and then towards the end.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingPermutationSamplerRandomAccessBidirectional(ListData data) {
        final int start = data.getSize() / 2;
        shuffleUsingPermutationSamplerRandomAccess(data.getRNG(), data.getList(), start, true);
        shuffleUsingPermutationSamplerRandomAccess(data.getRNG(), data.getList(), start + 1, false);
        return data.getList();
    }

    /**
     * Performs a direct shuffle on the list using JDK Collections method.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingDirectRandomAccess(ListData data) {
        shuffleDirectRandomAccess(data.getRNG(), data.getList());
        return data.getList();
    }

    /**
     * Performs a direct shuffle on the list using JDK Collections method modified to handle
     * a directional shuffle from a start index.
     * This performs two part shuffles from the middle
     * towards the head and then towards the end.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingDirectRandomAccessDirectionalBidirectional(ListData data) {
        final int start = data.getSize() / 2;
        shuffleDirectRandomAccessDirectional(data.getRNG(), data.getList(), start, true);
        shuffleDirectRandomAccessDirectional(data.getRNG(), data.getList(), start + 1, false);
        return data.getList();
    }

    /**
     * Performs a direct shuffle on the list using JDK Collections method modified to handle
     * a directional shuffle from a start index by extracting a sub-list.
     * This performs two part shuffles from the middle
     * towards the head and then towards the end.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingDirectRandomAccessSublistBidirectional(ListData data) {
        final int start = data.getSize() / 2;
        shuffleDirectRandomAccessSubList(data.getRNG(), data.getList(), start, true);
        shuffleDirectRandomAccessSubList(data.getRNG(), data.getList(), start + 1, false);
        return data.getList();
    }

    /**
     * Performs a shuffle using the current ListSampler shuffle.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingListSampler(ListData data) {
        ListSampler.shuffle(data.getRNG(), data.getList());
        return data.getList();
    }

    /**
     * Performs a shuffle using the current ListSampler shuffle.
     * This performs two part shuffles from the middle
     * towards the head and then towards the end.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object usingListSamplerBidirectional(ListData data) {
        final int start = data.getSize() / 2;
        ListSampler.shuffle(data.getRNG(), data.getList(), start, true);
        ListSampler.shuffle(data.getRNG(), data.getList(), start + 1, false);
        return data.getList();
    }

    /**
     * Performs a direct shuffle on a LinkedList.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object shuffleDirect(LinkedListData data) {
        shuffleDirect(data.getRNG(), data.getList());
        return data.getList();
    }

    /**
     * Performs a shuffle on a LinkedList using an iterator.
     *
     * @param data Shuffle data.
     * @return the list
     */
    @Benchmark
    public Object shuffleIterator(LinkedListData data) {
        shuffleIterator(data.getRNG(), data.getList());
        return data.getList();
    }
}
