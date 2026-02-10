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
 * Jumping in the sequence is essentially instantaneous. This generator provides subsequences for easy parallelization.
 *
 * @see <a href="https://www.thesalmons.org/john/random123/papers/random123sc11.pdf">Parallel Random Numbers: As Easy as 1,2,3</a>
 * for details regarding the engine.
 * @since 1.7
 */
public final class Philox4x32 extends IntProvider implements LongJumpableUniformRandomProvider {
    /**
     * Philox 32-bit mixing constant for counter 0.
     */
    private static final int K_PHILOX_10_A = 0x9E3779B9;
    /**
     * Philox 32-bit mixing constant for counter 1.
     */
    private static final int K_PHILOX_10_B = 0xBB67AE85;
    /**
     * Philox 32-bit constant for key 0.
     */
    private static final int K_PHILOX_SA = 0xD2511F53;
    /**
     * Philox 32-bit constant for key 1.
     */
    private static final int K_PHILOX_SB = 0xCD9E8D57;
    /**
     * Internal buffer size.
     */
    private static final int PHILOX_BUFFER_SIZE = 4;
    /**
     * number of int variables.
     */
    private static final int STATE_SIZE = 7;

    /**
     * Counter 0.
     */
    private int counter0;
    /**
     * Counter 1.
     */
    private int counter1;
    /**
     * Counter 2.
     */
    private int counter2;
    /**
     * Counter 3.
     */
    private int counter3;
    /**
     * Output point.
     */
    private int[] buffer = new int[PHILOX_BUFFER_SIZE]; // UINT4
    /**
     * Key low bits.
     */
    private int key0;
    /**
     * Key high bits.
     */
    private int key1;
    /**
     * State index:  which output word is next (0..3).
     */
    private int bufferPosition;


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
        buffer = source.buffer.clone();
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
    public Philox4x32(long key) {
        this(new int[]{(int) key, (int) (key >>> 32)});
    }

    /**
     * Creates a new instance based on an array of int containing, key (first two ints) and
     * the counter (next 4 ints, starts at first int). The counter is not scrambled and may
     * be used to create contiguous blocks with size a multiple of 4 ints.
     *
     * @param seed an array of size 6 defining key0,key1,counter0,counter1,counter2,counter3.
     *             If the size is smaller, zero values are assumed.
     */
    public Philox4x32(int[] seed) {
        final int[] input = seed.length < 6 ? Arrays.copyOf(seed, 6) : seed;
        key0 = input[0];
        key1 = input[1];
        counter0 = input[2];
        counter1 = input[3];
        counter2 = input[4];
        counter3 = input[5];
        bufferPosition = PHILOX_BUFFER_SIZE;
    }

    /**
     * Fetch next integer from the buffer, or regenerate the buffer using 10 rounds.
     *
     * @return random integer
     */
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
     * Increment by one.
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
     * Performs a single round of philox.
     *
     * @param ctr  local counter, which will be updated after each call.
     * @param key0 key low bits
     * @param key1 key high bits
     */
    private static void singleRound(int[] ctr, int key0, int key1) {
        long product = (K_PHILOX_SA & 0xFFFFFFFFL) * (ctr[0] & 0xFFFFFFFFL);
        final int hi0 = (int) (product >>> 32);
        final int lo0 = (int) product;
        product = (K_PHILOX_SB & 0xFFFFFFFFL) * (ctr[2] & 0xFFFFFFFFL);
        final int hi1 = (int) (product >>> 32);
        final int lo1 = (int) product;

        ctr[0] = hi1 ^ ctr[1] ^ key0;
        ctr[1] = lo1;
        ctr[2] = hi0 ^ ctr[3] ^ key1;
        ctr[3] = lo0;
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
     * {@inheritDoc}
     *
     * <p>Increments the subsequence by 1.</p>
     * <p>The jump size is the equivalent of 4*2<sup>96</sup> calls to
     * {@link UniformRandomProvider#nextInt() nextInt()}.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final Philox4x32 copy = copy();
        counter3++;
        rand10();
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 4*2<sup>64</sup>
     * calls to {@link UniformRandomProvider#nextInt() nextInt()}.
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
     */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                new int[]{key0, key1, counter0, counter1, counter2, counter3, bufferPosition}),
            super.getStateInternal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * 4);
        final int[] state = NumberFactory.makeIntArray(c[0]);
        key0 = state[0];
        key1 = state[1];
        counter0 = state[2];
        counter1 = state[3];
        counter2 = state[4];
        counter3 = state[5];
        bufferPosition = state[6];
        super.setStateInternal(c[1]);
        rand10(); //to regenerate the internal buffer
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
