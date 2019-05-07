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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Sampler for a discrete distribution using an optimised look-up table.
 *
 * <ul>
 *  <li>
 *   The method requires 30-bit integer probabilities that sum to 2<sup>30</sup> as described
 *   in George Marsaglia, Wai Wan Tsang, Jingbo Wang (2004) Fast Generation of Discrete
 *   Random Variables. Journal of Statistical Software. Vol. 11, Issue. 3, pp. 1-11.
 *  </li>
 * </ul>
 *
 * <p>Sampling uses 1 call to {@link UniformRandomProvider#nextInt()}.</p>
 *
 * <p>Memory requirements depend on the maximum number of possible sample values, {@code n},
 * and the values for the probabilities. Storage is optimised for {@code n}. The worst case
 * scenario is a uniform distribution of the maximum sample size. This is capped at 0.06MB for
 * {@code n <= } 2<sup>8</sup>, 17.0MB for {@code n <= } 2<sup>16</sup>, and 4.3GB for
 * {@code n <=} 2<sup>30</sup>. Realistic requirements will be in the kB range.</p>
 *
 * @since 1.3
 * @see <a href="http://dx.doi.org/10.18637/jss.v011.i03">Margsglia, et al (2004) JSS Vol.
 * 11, Issue 3</a>
 */
public class MarsagliaTsangWangDiscreteSampler implements DiscreteSampler {
    /** The exclusive upper bound for an unsigned 8-bit integer. */
    private static final int UNSIGNED_INT_8 = 1 << 8;
    /** The exclusive upper bound for an unsigned 16-bit integer. */
    private static final int UNSIGNED_INT_16 = 1 << 16;

    /** Limit for look-up table 1. */
    private final int t1;
    /** Limit for look-up table 2. */
    private final int t2;
    /** Limit for look-up table 3. */
    private final int t3;
    /** Limit for look-up table 4. */
    private final int t4;

    /** Index look-up table. */
    private final IndexTable indexTable;

    /** Underlying source of randomness. */
    private final UniformRandomProvider rng;

    /**
     * An index table contains the sample values. This is efficiently accessed for any index in the
     * range {@code [0,2^30)} by using an algorithm based on the decomposition of the index into
     * 5 base-64 digits.
     *
     * <p>This interface defines the methods for the filling and accessing values from 5 tables.
     * It allows a concrete implementation to allocate appropriate tables to optimise memory
     * requirements.</p>
     */
    private interface IndexTable {
        /**
         * @param from Lower bound index (inclusive).
         * @param to Upper bound index (exclusive).
         * @param value Value.
         */
        void fillTable1(int from, int to, int value);
        /**
         * @param from Lower bound index (inclusive).
         * @param to Upper bound index (exclusive).
         * @param value Value.
         */
        void fillTable2(int from, int to, int value);
        /**
         * @param from Lower bound index (inclusive).
         * @param to Upper bound index (exclusive).
         * @param value Value.
         */
        void fillTable3(int from, int to, int value);
        /**
         * @param from Lower bound index (inclusive).
         * @param to Upper bound index (exclusive).
         * @param value Value.
         */
        void fillTable4(int from, int to, int value);
        /**
         * @param from Lower bound index (inclusive).
         * @param to Upper bound index (exclusive).
         * @param value Value.
         */
        void fillTable5(int from, int to, int value);

        /**
         * @param index Index.
         * @return Value.
         */
        int getTable1(int index);
        /**
         * @param index Index.
         * @return Value.
         */
        int getTable2(int index);
        /**
         * @param index Index.
         * @return Value.
         */
        int getTable3(int index);
        /**
         * @param index Index.
         * @return Value.
         */
        int getTable4(int index);
        /**
         * @param index Index.
         * @return Value.
         */
        int getTable5(int index);
    }

    /**
     * Index table for an 8-bit index.
     */
    private static class IndexTable8 implements IndexTable {
        /** The mask to convert a {@code byte} to an unsigned 8-bit integer. */
        private static final int MASK = 0xff;

        /** Look-up table table1. */
        private final byte[] table1;
        /** Look-up table table2. */
        private final byte[] table2;
        /** Look-up table table3. */
        private final byte[] table3;
        /** Look-up table table4. */
        private final byte[] table4;
        /** Look-up table table5. */
        private final byte[] table5;

        /**
         * @param n1 Size of table 1.
         * @param n2 Size of table 2.
         * @param n3 Size of table 3.
         * @param n4 Size of table 4.
         * @param n5 Size of table 5.
         */
        IndexTable8(int n1, int n2, int n3, int n4, int n5) {
            table1 = new byte[n1];
            table2 = new byte[n2];
            table3 = new byte[n3];
            table4 = new byte[n4];
            table5 = new byte[n5];
        }

        @Override
        public void fillTable1(int from, int to, int value) { fill(table1, from, to, value); }
        @Override
        public void fillTable2(int from, int to, int value) { fill(table2, from, to, value); }
        @Override
        public void fillTable3(int from, int to, int value) { fill(table3, from, to, value); }
        @Override
        public void fillTable4(int from, int to, int value) { fill(table4, from, to, value); }
        @Override
        public void fillTable5(int from, int to, int value) { fill(table5, from, to, value); }

        /**
         * Fill the table with the value.
         *
         * @param table Table.
         * @param from Lower bound index (inclusive)
         * @param to Upper bound index (exclusive)
         * @param value Value.
         */
        private static void fill(byte[] table, int from, int to, int value) {
            while (from < to) {
                // Primitive type conversion will extract lower 8 bits
                table[from++] = (byte) value;
            }
        }

        @Override
        public int getTable1(int index) { return table1[index] & MASK; }
        @Override
        public int getTable2(int index) { return table2[index] & MASK; }
        @Override
        public int getTable3(int index) { return table3[index] & MASK; }
        @Override
        public int getTable4(int index) { return table4[index] & MASK; }
        @Override
        public int getTable5(int index) { return table5[index] & MASK; }
    }

    /**
     * Index table for a 16-bit index.
     */
    private static class IndexTable16 implements IndexTable {
        /** The mask to convert a {@code short} to an unsigned 16-bit integer. */
        private static final int MASK = 0xffff;

        /** Look-up table table1. */
        private final short[] table1;
        /** Look-up table table2. */
        private final short[] table2;
        /** Look-up table table3. */
        private final short[] table3;
        /** Look-up table table4. */
        private final short[] table4;
        /** Look-up table table5. */
        private final short[] table5;

        /**
         * @param n1 Size of table 1.
         * @param n2 Size of table 2.
         * @param n3 Size of table 3.
         * @param n4 Size of table 4.
         * @param n5 Size of table 5.
         */
        IndexTable16(int n1, int n2, int n3, int n4, int n5) {
            table1 = new short[n1];
            table2 = new short[n2];
            table3 = new short[n3];
            table4 = new short[n4];
            table5 = new short[n5];
        }

        @Override
        public void fillTable1(int from, int to, int value) { fill(table1, from, to, value); }
        @Override
        public void fillTable2(int from, int to, int value) { fill(table2, from, to, value); }
        @Override
        public void fillTable3(int from, int to, int value) { fill(table3, from, to, value); }
        @Override
        public void fillTable4(int from, int to, int value) { fill(table4, from, to, value); }
        @Override
        public void fillTable5(int from, int to, int value) { fill(table5, from, to, value); }

        /**
         * Fill the table with the value.
         *
         * @param table Table.
         * @param from Lower bound index (inclusive)
         * @param to Upper bound index (exclusive)
         * @param value Value.
         */
        private static void fill(short[] table, int from, int to, int value) {
            while (from < to) {
                // Primitive type conversion will extract lower 16 bits
                table[from++] = (short) value;
            }
        }

        @Override
        public int getTable1(int index) { return table1[index] & MASK; }
        @Override
        public int getTable2(int index) { return table2[index] & MASK; }
        @Override
        public int getTable3(int index) { return table3[index] & MASK; }
        @Override
        public int getTable4(int index) { return table4[index] & MASK; }
        @Override
        public int getTable5(int index) { return table5[index] & MASK; }
    }

    /**
     * Index table for a 32-bit index.
     */
    private static class IndexTable32 implements IndexTable {
        /** Look-up table table1. */
        private final int[] table1;
        /** Look-up table table2. */
        private final int[] table2;
        /** Look-up table table3. */
        private final int[] table3;
        /** Look-up table table4. */
        private final int[] table4;
        /** Look-up table table5. */
        private final int[] table5;

        /**
         * @param n1 Size of table 1.
         * @param n2 Size of table 2.
         * @param n3 Size of table 3.
         * @param n4 Size of table 4.
         * @param n5 Size of table 5.
         */
        IndexTable32(int n1, int n2, int n3, int n4, int n5) {
            table1 = new int[n1];
            table2 = new int[n2];
            table3 = new int[n3];
            table4 = new int[n4];
            table5 = new int[n5];
        }

        @Override
        public void fillTable1(int from, int to, int value) { fill(table1, from, to, value); }
        @Override
        public void fillTable2(int from, int to, int value) { fill(table2, from, to, value); }
        @Override
        public void fillTable3(int from, int to, int value) { fill(table3, from, to, value); }
        @Override
        public void fillTable4(int from, int to, int value) { fill(table4, from, to, value); }
        @Override
        public void fillTable5(int from, int to, int value) { fill(table5, from, to, value); }

        /**
         * Fill the table with the value.
         *
         * @param table Table.
         * @param from Lower bound index (inclusive)
         * @param to Upper bound index (exclusive)
         * @param value Value.
         */
        private static void fill(int[] table, int from, int to, int value) {
            while (from < to) {
                table[from++] = value;
            }
        }

        @Override
        public int getTable1(int index) { return table1[index]; }
        @Override
        public int getTable2(int index) { return table2[index]; }
        @Override
        public int getTable3(int index) { return table3[index]; }
        @Override
        public int getTable4(int index) { return table4[index]; }
        @Override
        public int getTable5(int index) { return table5[index]; }
    }

    /**
     * Create a new instance for probabilities {@code p(i)} where the sample value {@code x} is
     * {@code i + offset}.
     *
     * <p>The sum of the probabilities must be >= 2<sup>30</sup>. Only the
     * values for cumulative probability up to 2<sup>30</sup> will be sampled.</p>
     *
     * <p>Note: This is package-private for use by discrete distribution samplers that can
     * compute their probability distribution.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param prob The probabilities.
     * @param offset The offset (must be positive).
     * @throws IllegalArgumentException if the offset is negative or the maximum sample index
     * exceeds the maximum positive {@code int} value (2<sup>31</sup> - 1).
     */
    MarsagliaTsangWangDiscreteSampler(UniformRandomProvider rng,
                                      int[] prob,
                                      int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Unsupported offset: " + offset);
        }
        if ((long) prob.length + offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unsupported sample index: " + (prob.length + offset));
        }

        this.rng = rng;

        // Get table sizes for each base-64 digit
        int n1 = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        for (final int m : prob) {
            n1 += getBase64Digit(m, 1);
            n2 += getBase64Digit(m, 2);
            n3 += getBase64Digit(m, 3);
            n4 += getBase64Digit(m, 4);
            n5 += getBase64Digit(m, 5);
        }

        // Allocate tables based on the maximum index
        final int maxIndex = prob.length + offset - 1;
        if (maxIndex < UNSIGNED_INT_8) {
            indexTable = new IndexTable8(n1, n2, n3, n4, n5);
        } else if (maxIndex < UNSIGNED_INT_16) {
            indexTable = new IndexTable16(n1, n2, n3, n4, n5);
        } else {
            indexTable = new IndexTable32(n1, n2, n3, n4, n5);
        }

        // Compute offsets
        t1 = n1 << 24;
        t2 = t1 + (n2 << 18);
        t3 = t2 + (n3 << 12);
        t4 = t3 + (n4 << 6);
        n1 = n2 = n3 = n4 = n5 = 0;

        // Fill tables
        for (int i = 0; i < prob.length; i++) {
            final int m = prob[i];
            final int k = i + offset;
            indexTable.fillTable1(n1, n1 += getBase64Digit(m, 1), k);
            indexTable.fillTable2(n2, n2 += getBase64Digit(m, 2), k);
            indexTable.fillTable3(n3, n3 += getBase64Digit(m, 3), k);
            indexTable.fillTable4(n4, n4 += getBase64Digit(m, 4), k);
            indexTable.fillTable5(n5, n5 += getBase64Digit(m, 5), k);
        }
    }

    /**
     * Creates a sampler.
     *
     * <p>The probabilities will be normalised using their sum. The only requirement is the sum
     * is positive.</p>
     *
     * <p>The sum of the probabilities is normalised to 2<sup>30</sup>. Any probability less
     * than 2<sup>-30</sup> will not be observed in samples. An adjustment is made to the maximum
     * probability to compensate for round-off during conversion.</p>
     *
     * @param rng Generator of uniformly distributed random numbers.
     * @param probabilities The list of probabilities.
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    public MarsagliaTsangWangDiscreteSampler(UniformRandomProvider rng,
                                             double[] probabilities) {
        this(rng, normaliseProbabilities(probabilities), 0);
    }

    /**
     * Normalise the probabilities to integers that sum to 2<sup>30</sup>.
     *
     * @param probabilities The list of probabilities.
     * @return the normalised probabilities.
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    private static int[] normaliseProbabilities(double[] probabilities) {
        final double sumProb = validateProbabilities(probabilities);

        // Compute the normalisation: 2^30 / sum
        final double normalisation = (1 << 30) / sumProb;
        final int[] prob = new int[probabilities.length];
        int sum = 0;
        int max = 0;
        int mode = 0;
        for (int i = 0; i < prob.length; i++) {
            // Add 0.5 for rounding
            final int p = (int) (probabilities[i] * normalisation + 0.5);
            sum += p;
            // Find the mode (maximum probability)
            if (max < p) {
                max = p;
                mode = i;
            }
            prob[i] = p;
        }

        // The sum must be >= 2^30.
        // Here just compensate the difference onto the highest probability.
        prob[mode] += (1 << 30) - sum;

        return prob;
    }

    /**
     * Validate the probabilities sum to a finite positive number.
     *
     * @param probabilities the probabilities
     * @return the sum
     * @throws IllegalArgumentException if {@code probabilities} is null or empty, a
     * probability is negative, infinite or {@code NaN}, or the sum of all
     * probabilities is not strictly positive.
     */
    private static double validateProbabilities(double[] probabilities) {
        if (probabilities == null || probabilities.length == 0) {
            throw new IllegalArgumentException("Probabilities must not be empty.");
        }

        double sumProb = 0;
        for (final double prob : probabilities) {
            if (prob < 0 ||
                Double.isInfinite(prob) ||
                Double.isNaN(prob)) {
                throw new IllegalArgumentException("Invalid probability: " +
                                                   prob);
            }
            sumProb += prob;
        }

        if (Double.isInfinite(sumProb) || sumProb <= 0) {
            throw new IllegalArgumentException("Invalid sum of probabilities: " + sumProb);
        }
        return sumProb;
    }

    /**
     * Gets the k<sup>th</sup> base 64 digit of {@code m}.
     *
     * @param m the value m.
     * @param k the digit.
     * @return the base 64 digit
     */
    private static int getBase64Digit(int m, int k) {
        return (m >>> (30 - 6 * k)) & 63;
    }

    /** {@inheritDoc} */
    @Override
    public int sample() {
        final int j = rng.nextInt() >>> 2;
        if (j < t1) {
            return indexTable.getTable1(j >>> 24);
        }
        if (j < t2) {
            return indexTable.getTable2((j - t1) >>> 18);
        }
        if (j < t3) {
            return indexTable.getTable3((j - t2) >>> 12);
        }
        if (j < t4) {
            return indexTable.getTable4((j - t3) >>> 6);
        }
        // Note the tables are filled on the assumption that the sum of the probabilities.
        // is >=2^30. If this is not true then the final table table5 will be smaller by the
        // difference. So the tables *must* be constructed correctly.
        return indexTable.getTable5(j - t4);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Marsaglia Tsang Wang discrete deviate [" + rng.toString() + "]";
    }
}
