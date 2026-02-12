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

package org.apache.commons.rng.core.source32;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

import java.util.Arrays;

/**
 * This class implements the Philox4x32 128-bit counter-based generator with 10 rounds.
 *
 * <p>This is a member of the Philox family of generators. Memory footprint is 192 bits
 * and the period is 4*2<sup>128</sup>.</p>
 *
 * <p>Jumping in the sequence is essentially instantaneous.
 * This generator provides subsequences for easy parallelization.
 *
 * <p>References:
 * <ol>
 * <li>
 * Salmon, J.K. <i>et al</i> (2011)
 * <a href="https://dl.acm.org/doi/epdf/10.1145/2063384.2063405">
 * Parallel Random Numbers: As Easy as 1,2,3</a>.</li>
 * </ol>
 *
 * @since 1.7
 */
public final class Philox4x32 extends IntProvider implements LongJumpableUniformRandomProvider {
    /** Philox 32-bit mixing constant for counter 0. */
    private static final int K_PHILOX_10_A = 0x9E3779B9;
    /** Philox 32-bit mixing constant for counter 1. */
    private static final int K_PHILOX_10_B = 0xBB67AE85;
    /** Philox 32-bit constant for key 0. */
    private static final int K_PHILOX_SA = 0xD2511F53;
    /** Philox 32-bit constant for key 1. */
    private static final int K_PHILOX_SB = 0xCD9E8D57;
    /** Internal buffer size. */
    private static final int PHILOX_BUFFER_SIZE = 4;
    /** Number of state variables. */
    private static final int STATE_SIZE = 7;

    /** Counter 0. */
    private int counter0;
    /** Counter 1. */
    private int counter1;
    /** Counter 2. */
    private int counter2;
    /** Counter 3. */
    private int counter3;
    /** Output buffer. */
    private final int[] buffer = new int[PHILOX_BUFFER_SIZE];
    /** Key low bits. */
    private int key0;
    /** Key high bits. */
    private int key1;
    /** Output buffer index. When at the end of the buffer the counter is
     * incremented and the buffer regenerated. */
    private int bufferPosition;

    /**
     * Creates a new instance based on an array of int containing, key (first two ints) and
     * the counter (next 4 ints, low bits = first int). The counter is not scrambled and may
     * be used to create contiguous blocks with size a multiple of 4 ints. For example,
     * setting seed[2] = 1 is equivalent to start with seed[2]=0 and calling {@link #next()} 4 times.
     *
     * @param seed Array of size 6 defining key0,key1,counter0,counter1,counter2,counter3.
     *             If the size is smaller, zero values are assumed.
     */
    public Philox4x32(int[] seed) {
        final int[] input = seed.length < 6 ? Arrays.copyOf(seed, 6) : seed;
        setState(input);
        bufferPosition = PHILOX_BUFFER_SIZE;
    }

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    private Philox4x32(Philox4x32 source) {
        super(source);
        counter0 = source.counter0;
        counter1 = source.counter1;
        counter2 = source.counter2;
        counter3 = source.counter3;
        key0 = source.key0;
        key1 = source.key1;
        bufferPosition = source.bufferPosition;
        System.arraycopy(source.buffer, 0, buffer, 0, PHILOX_BUFFER_SIZE);
    }

    /**
     * Creates a new instance with default seed. Subsequence and offset (or equivalently, the internal counter)
     * are set to zero.
     */
    public Philox4x32() {
        this(67280421310721L);
    }

    /**
     * Creates a new instance with a given seed. Subsequence and offset (or equivalently, the internal counter)
     * are set to zero.
     *
     * @param key the low 32 bits constitute the first int key of Philox,
     *            and the high 32 bits constitute the second int key of Philox
     */
    private Philox4x32(long key) {
        this(new int[]{(int) key, (int) (key >>> 32)});
    }

    /**
     * Copies the state from the array into the generator state.
     *
     * @param state New state.
     */
    private void setState(int[] state) {
        key0 = state[0];
        key1 = state[1];
        counter0 = state[2];
        counter1 = state[3];
        counter2 = state[4];
        counter3 = state[5];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(
            NumberFactory.makeByteArray(new int[] {key0, key1, counter0, counter1, counter2, counter3, bufferPosition}),
            super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * Integer.BYTES);
        final int[] state = NumberFactory.makeIntArray(c[0]);
        setState(state);
        bufferPosition = state[6];
        super.setStateInternal(c[1]);
        // Regenerate the internal buffer
        rand10();
    }

    /** {@inheritDoc} */
    @Override
    public int next() {
        final int p = bufferPosition;
        if (p < PHILOX_BUFFER_SIZE) {
            bufferPosition = p + 1;
            return buffer[p];
        }
        incrementCounter();
        rand10();
        bufferPosition = 1;
        return buffer[0];
    }

    /**
     * Increment the counter by one.
     */
    private void incrementCounter() {
        counter0++;
        if (counter0 != 0) {
            return;
        }
        counter1++;
        if (counter1 != 0) {
            return;
        }
        counter2++;
        if (counter2 != 0) {
            return;
        }
        counter3++;
    }

    /**
     * Perform 10 rounds, using counter0, counter1, counter2, counter3 as starting point.
     * It updates the buffer member variable, but no others.
     */
    private void rand10() {
        buffer[0] = counter0;
        buffer[1] = counter1;
        buffer[2] = counter2;
        buffer[3] = counter3;

        int k0 = key0;
        int k1 = key1;

        //unrolled loop for performance
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
        k0 += K_PHILOX_10_A;
        k1 += K_PHILOX_10_B;
        singleRound(buffer, k0, k1);
    }

    /**
     * Performs a single round of philox.
     *
     * @param counter Counter, which will be updated after each call.
     * @param key0 Key low bits.
     * @param key1 Key high bits.
     */
    private static void singleRound(int[] counter, int key0, int key1) {
        final long product0 = (K_PHILOX_SA & 0xffff_ffffL) * (counter[0] & 0xffff_ffffL);
        final int hi0 = (int) (product0 >>> 32);
        final int lo0 = (int) product0;
        final long product1 = (K_PHILOX_SB & 0xffff_ffffL) * (counter[2] & 0xffff_ffffL);
        final int hi1 = (int) (product1 >>> 32);
        final int lo1 = (int) product1;

        counter[0] = hi1 ^ counter[1] ^ key0;
        counter[1] = lo1;
        counter[2] = hi0 ^ counter[3] ^ key1;
        counter[3] = lo0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 4*2<sup>64</sup>
     * calls to {@link UniformRandomProvider#nextInt() nextInt()}. It can provide
     * up to 2<sup>64</sup> non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final Philox4x32 copy = copy();
        if (++counter2 == 0) {
            counter3++;
        }
        rand10();
        resetCachedState();
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 4*2<sup>96</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>32</sup> non-overlapping subsequences of length 4*2<sup>96</sup>; each
     * subsequence can provide up to 2<sup>32</sup> non-overlapping subsequences of
     * length 2<sup>64</sup> using the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final Philox4x32 copy = copy();
        counter3++;
        rand10();
        resetCachedState();
        return copy;
    }

    /**
     * Create a copy.
     *
     * @return the copy
     */
    private Philox4x32 copy() {
        return new Philox4x32(this);
    }
}
