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
package org.apache.commons.rng.sampling;

import java.util.List;
import org.apache.commons.rng.UniformRandomProvider;

/**
 * Utilities for shuffling an array in-place.
 *
 * <p>Shuffles use the <a
 * href="https://en.wikipedia.org/wiki/Fisher-Yates_shuffle#The_modern_algorithm">
 * Fisher-Yates</a> algorithm.
 *
 * <p>Small ranges use batched random integer generation to increase performance.
 *
 * <ul>
 * <li>Nevin Brackett-Rozinsky, Daniel Lemire,
 * Batched Ranged Random Integer Generation, Software: Practice and Experience (to appear)
 * <a href="https://arxiv.org/abs/2408.06213">arXiv:2408.06213M</a>
 * </ul>
 *
 * @since 1.6
 */
public final class ArraySampler {
    /** Mask the lower 32-bit of a long. */
    private static final long MASK_32 = 0xffffffffL;
    /** 2^32. Used for the bounded random algorithm. This is required as the original
     * method used (-bound % bound) for (2^L % bound) which only works for unsigned integer
     * modulus. */
    private static final long POW_32 = 1L << 32;
    /** Length threshold to sample 2 integers from a random 32-bit value.
     * The threshold provided in the Brackett-Rozinsky and Lemire paper
     * is the power of 2 below 20724. Note that the product 2^15*2^15
     * is representable using signed integers. */
    private static final int BATCH_2 = 1 << 15;

    /** Class contains only static methods. */
    private ArraySampler() {}

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static boolean[] shuffle(UniformRandomProvider rng, boolean[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static byte[] shuffle(UniformRandomProvider rng, byte[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static char[] shuffle(UniformRandomProvider rng, char[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static double[] shuffle(UniformRandomProvider rng, double[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static float[] shuffle(UniformRandomProvider rng, float[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static int[] shuffle(UniformRandomProvider rng, int[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static long[] shuffle(UniformRandomProvider rng, long[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static short[] shuffle(UniformRandomProvider rng, short[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param <T> Type of the items.
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @return a reference to the given array
     */
    public static <T> T[] shuffle(UniformRandomProvider rng, T[] array) {
        int i = array.length;
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given list.
     *
     * <p>Note: This method is intentionally package-private.
     *
     * <p>This method exists to allow the shuffle performed by the {@link ListSampler} to
     * match the {@link PermutationSampler} and {@link ArraySampler}.
     *
     * @param <T> Type of the items.
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    static <T> void shuffle(UniformRandomProvider rng, List<T> array) {
        int i = array.size();
        for (; i > BATCH_2; --i) {
            swap(array, i - 1, rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, i - 1, index1);
            swap(array, i - 2, index2);
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static boolean[] shuffle(UniformRandomProvider rng, boolean[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static byte[] shuffle(UniformRandomProvider rng, byte[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static char[] shuffle(UniformRandomProvider rng, char[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static double[] shuffle(UniformRandomProvider rng, double[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static float[] shuffle(UniformRandomProvider rng, float[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static int[] shuffle(UniformRandomProvider rng, int[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static long[] shuffle(UniformRandomProvider rng, long[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static short[] shuffle(UniformRandomProvider rng, short[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param <T> Type of the items.
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @return a reference to the given array
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static <T> T[] shuffle(UniformRandomProvider rng, T[] array, int from, int to) {
        int i = to - checkFromToIndex(from, to, array.length);
        for (; i > BATCH_2; --i) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
        // Batches of 2
        final int[] productBound = {i * (i - 1)};
        for (; i > 1; i -= 2) {
            final int[] indices = randomBounded2(i, i - 1, productBound, rng);
            final int index1 = indices[0];
            final int index2 = indices[1];
            swap(array, from + i - 1, from + index1);
            swap(array, from + i - 2, from + index2);
        }
        return array;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(boolean[] array, int i, int j) {
        final boolean tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(byte[] array, int i, int j) {
        final byte tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(char[] array, int i, int j) {
        final char tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(double[] array, int i, int j) {
        final double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(float[] array, int i, int j) {
        final float tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(int[] array, int i, int j) {
        final int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(long[] array, int i, int j) {
        final long tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
     *
     * @param array Array.
     * @param i First index.
     * @param j Second index.
     */
    private static void swap(short[] array, int i, int j) {
        final short tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * Swaps the two specified elements in the array.
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
     * Swaps the two specified elements in the list.
     *
     * @param <T> Type of the list items.
     * @param list List.
     * @param i First index.
     * @param j Second index.
     */
    private static <T> void swap(List<T> list, int i, int j) {
        final T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    /**
     * Return two random values in {@code [0, range1)} and {@code [0, range2)}. The
     * product bound is used for the reject algorithm. See Brackett-Rozinsky and Lemire.
     *
     * <p>The product bound can be any positive integer {@code >= range1*range2}.
     * It may be updated to become {@code range1*range2}.
     *
     * @param range1 Range 1.
     * @param range2 Range 2.
     * @param productBound Product bound.
     * @param rng Source of randomness.
     * @return [index1, index2]
     */
    static int[] randomBounded2(int range1, int range2, int[] productBound, UniformRandomProvider rng) {
        long m = (rng.nextInt() & MASK_32) * range1;
        // index1 and index2 are the top 32-bits of the long
        long index1 = m;
        // Leftover bits * range2
        m = (m & MASK_32) * range2;
        long index2 = m;
        // Leftover bits must be unsigned
        long l = m & MASK_32;
        if (l < productBound[0]) {
            final int bound = range1 * range2;
            productBound[0] = bound;
            if (l < bound) {
                // 2^32 % bound
                final long t = POW_32 % bound;
                while (l < t) {
                    m = (rng.nextInt() & MASK_32) * range1;
                    index1 = m;
                    m = (m & MASK_32) * range2;
                    index2 = m;
                    l = m & MASK_32;
                }
            }
        }
        // Convert to [0, range1), [0, range2)
        return new int[] {(int) (index1 >> 32), (int) (index2 >> 32)};
    }

    /**
     * Checks if the sub-range from fromIndex (inclusive) to toIndex (exclusive) is
     * within the bounds of range from 0 (inclusive) to length (exclusive).
     *
     * <p>This function provides the functionality of
     * {@code java.utils.Objects.checkFromToIndex} introduced in JDK 9. The <a
     * href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Objects.html#checkFromToIndex(int,int,int)">Objects</a>
     * javadoc has been reproduced for reference.
     *
     * <p>The sub-range is defined to be out of bounds if any of the following
     * inequalities is true:
     * <ul>
     * <li>{@code fromIndex < 0}
     * <li>{@code fromIndex > toIndex}
     * <li>{@code toIndex > length}
     * <li>{@code length < 0}, which is implied from the former inequalities
     * </ul>
     *
     * @param fromIndex Lower-bound (inclusive) of the sub-range.
     * @param toIndex Upper-bound (exclusive) of the sub-range.
     * @param length Upper-bound (exclusive) of the range
     * @return fromIndex if the sub-range is within the bounds of the range
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    private static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        // Checks as documented above
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException(
                    String.format("Range [%d, %d) out of bounds for length %d", fromIndex, toIndex, length));
        }
        return fromIndex;
    }
}
