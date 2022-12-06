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

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Utilities for shuffling an array in-place.
 *
 * <p>Shuffles use the <a
 * href="https://en.wikipedia.org/wiki/Fisher-Yates_shuffle#The_modern_algorithm">
 * Fisher-Yates</a> algorithm.
 *
 * @since 1.6
 */
public final class ArraySampler {
    /** Class contains only static methods. */
    private ArraySampler() {}

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, boolean[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, byte[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, char[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, double[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, float[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, int[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, long[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static void shuffle(UniformRandomProvider rng, short[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array.
     *
     * @param <T> Type of the items.
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     */
    public static <T> void shuffle(UniformRandomProvider rng, T[] array) {
        for (int i = array.length; i > 1; i--) {
            swap(array, i - 1, rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, boolean[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, byte[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, char[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, double[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, float[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, int[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, long[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static void shuffle(UniformRandomProvider rng, short[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
    }

    /**
     * Shuffles the entries of the given array in the range {@code [from, to)}.
     *
     * @param <T> Type of the items.
     * @param rng Source of randomness.
     * @param array Array whose entries will be shuffled (in-place).
     * @param from Lower-bound (inclusive) of the sub-range.
     * @param to Upper-bound (exclusive) of the sub-range.
     * @throws IndexOutOfBoundsException if the sub-range is out of bounds
     */
    public static <T> void shuffle(UniformRandomProvider rng, T[] array, int from, int to) {
        final int length = to - checkFromToIndex(from, to, array.length);
        for (int i = length; i > 1; i--) {
            swap(array, from + i - 1, from + rng.nextInt(i));
        }
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
