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

package org.apache.commons.rng.core.source64;

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * This class implements the Philox4x64 128-bit counter-based generator with 10 rounds.
 * Jumping in the sequence is essentially instantaneous. This generator provides subsequences for easy parallelization.
 *
 * @see <a href="https://www.thesalmons.org/john/random123/papers/random123sc11.pdf">Parallel Random Numbers: As Easy as 1,2,3</a>
 * for details regarding the engine.
 * @since 1.7
 */
public final class Philox4x64 extends LongProvider implements LongJumpableUniformRandomProvider {
    /**
     * Philox 32-bit mixing constant for counter 0.
     */
    private static final long PHILOX_M0 = 0xD2E7470EE14C6C93L;
    /**
     * Philox 32-bit mixing constant for counter 1.
     */
    private static final long PHILOX_M1 = 0xCA5A826395121157L;
    /**
     * Philox 32-bit constant for key 0.
     */
    private static final long PHILOX_W0 = 0x9E3779B97F4A7C15L;
    /**
     * Philox 32-bit constant for key 1.
     */
    private static final long PHILOX_W1 = 0xBB67AE8584CAA73BL;
    /**
     * Internal buffer size.
     */
    private static final int PHILOX_BUFFER_SIZE = 4;
    /**
     * number of long variables.
     */
    private static final int STATE_SIZE = 7;

    /**
     * Counter 0.
     */
    private long counter0;
    /**
     * Counter 1.
     */
    private long counter1;
    /**
     * Counter 2.
     */
    private long counter2;
    /**
     * Counter 3.
     */
    private long counter3;

    /**
     * Output point.
     */
    private long[] buffer = new long[PHILOX_BUFFER_SIZE]; // UINT4
    /**
     * Key low bits.
     */
    private long key0;
    /**
     * Key high bits.
     */
    private long key1;
    /**
     * State index:  which output word is next (0..3).
     */
    private int bufferPosition;

    /**
     * Copy constructor.
     *
     * @param source Source to copy.
     */
    private Philox4x64(Philox4x64 source) {
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
     * Creates a new instance with default seed. Subsequence and offset are set to zero.
     */
    public Philox4x64() {
        this(67280421310721L, 0x9E3779B97F4A7C15L, 0L, 0L, 0L, 0L);
    }

    /**
     * Creates a new instance. Offset and subsequence determine the internal counter of Philox.
     *
     * @param seed        Initial seed.
     * @param subsequence a subsequence index
     * @param offset      an offset, zero for the first number.
     */
    public Philox4x64(long[] seed, long[] subsequence, long[] offset) {
        resetState(seed, subsequence);
        incrementCounter(offset);
    }

    /**
     * Creates a new instance given 6 long numbers.
     *
     * @param keyAndCounter the first two number are the key and the next 4 number are the counter.
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public Philox4x64(long... keyAndCounter) {
        key0 = keyAndCounter[0];
        if (keyAndCounter.length > 1) {
            key1 = keyAndCounter[1];
        } else {
            key1 = 0;
        }
        if (keyAndCounter.length > 2) {
            counter0 = keyAndCounter[2];
        } else {
            counter0 = 0;
        }
        if (keyAndCounter.length > 3) {
            counter1 = keyAndCounter[3];
        } else {
            counter1 = 0;
        }
        if (keyAndCounter.length > 4) {
            counter2 = keyAndCounter[4];
        } else {
            counter2 = 0;
        }
        if (keyAndCounter.length > 5) {
            counter3 = keyAndCounter[5];
        } else {
            counter3 = 0;
        }
        bufferPosition = PHILOX_BUFFER_SIZE;
    }

    /**
     * Resets the key, counter and state index.
     *
     * @param seed        key for Philox
     * @param subsequence counter third and fourth long.
     */
    public void resetState(long[] seed, long[] subsequence) {
        key0 = seed[0];
        key1 = seed[1];

        counter0 = 0;
        counter1 = 0;
        counter2 = subsequence[0];
        counter3 = subsequence[1];

        bufferPosition = PHILOX_BUFFER_SIZE;
    }

    /**
     * Set the first two ints of the internal counter of Philox.
     *
     * @param offset an offset as an array of size 2. An offet of 1 corresponds to the 4th number of the sequence.
     */
    public void setOffset(long... offset) {
        counter0 = offset[0];
        if (offset.length > 1) {
            counter1 = offset[1];
        } else {
            counter1 = 0;
        }
        bufferPosition = PHILOX_BUFFER_SIZE;
    }

    /**
     * Returns the offset, that is the first two ints of the internal counter as a long.
     *
     * @return the first two ints of the internal counter of Philox as long.
     */
    public long[] getOffset() {
        return new long[]{counter0, counter1};
    }

    /**
     * Fetch next long from the buffer, or regenerate the buffer using 10 rounds.
     *
     * @return random 64-bit integer
     */
    private long next64() {
        if (bufferPosition < PHILOX_BUFFER_SIZE) {
            return buffer[bufferPosition++];
        }
        incrementCounter();
        rand10();
        bufferPosition = 1;
        return buffer[0];
    }

    /**
     * Counter increment.
     *
     * @param step a long array of size 4 representing how many steps to increment
     */
    private void incrementCounter(long... step) {
        int carry = 0;

        long orig = counter0;
        counter0 += step[0];
        if (Long.compareUnsigned(counter0, orig) < 0) {
            carry = 1;
        }

        if (carry == 1) {
            counter1++;
            carry = (counter1 == 0) ? 1 : 0;
        }
        orig = counter1;
        if (step.length > 1) {
            counter1 += step[1];
        }
        if (Long.compareUnsigned(counter1, orig) < 0) {
            carry = 1;
        }

        if (carry == 1) {
            counter2++;
            carry = (counter2 == 0) ? 1 : 0;
        }

        if (carry == 1) {
            counter3++;
        }
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
     * @param counter local counter, which will be updated after each call.
     * @param key0    key low bits
     * @param key1    key high bits
     */
    private static void singleRound(long[] counter, long key0, long key1) {
        final long lo0 = PHILOX_M0 * counter[0];
        final long hi0 = LXMSupport.unsignedMultiplyHigh(PHILOX_M0, counter[0]);
        final long lo1 = PHILOX_M1 * counter[2];
        final long hi1 = LXMSupport.unsignedMultiplyHigh(PHILOX_M1, counter[2]);

        counter[0] = hi1 ^ counter[1] ^ key0;
        counter[1] = lo1;
        counter[2] = hi0 ^ counter[3] ^ key1;
        counter[3] = lo0;
    }

    /**
     * Perform 10 rounds, using counter0, counter1, counter2, counter3 as starting point.
     *
     */
    private void rand10() {
        buffer[0] = counter0;
        buffer[1] = counter1;
        buffer[2] = counter2;
        buffer[3] = counter3;

        long k0 = key0;
        long k1 = key1;

        //unrolled loop for performance
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
        k0 += PHILOX_W0;
        k1 += PHILOX_W1;
        singleRound(buffer, k0, k1);
    }


    /**
     * {@inheritDoc}
     *
     * <p>Increments the subsequence by 1.</p>
     * <p>The jump size is the equivalent of 4*2<sup>128</sup> calls to
     * {@link UniformRandomProvider#nextInt() nextInt()}.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final Philox4x64 copy = copy();
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
     * <p>The jump size is the equivalent of 4*2<sup>64</sup>
     * calls to {@link UniformRandomProvider#nextInt() nextInt()}.
     */
    @Override
    public UniformRandomProvider jump() {
        final Philox4x64 copy = copy();
        incrementCounter(0, 1);
        rand10();
        resetCachedState();
        return copy;
    }

    /**
     * jump by n numbers in the sequence. This is equivalent to
     * calling nextInt() n times.
     *
     * @param n the length to jump.
     * @return a copy of the original random number generator.
     */
    public UniformRandomProvider jump(long n) {
        final Philox4x64 copy = copy();
        final long ndiv4 = (n + bufferPosition) / 4;
        if (ndiv4 > 0) {
            incrementCounter(ndiv4);
            rand10();
            bufferPosition = (int) ((bufferPosition + n) & 3);
        } else {
            for (int i = 0; i < n; i++) {
                nextLong();
            }
        }
        resetCachedState();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long next() {
        return next64();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                new long[]{key0, key1, counter0, counter1, counter2, counter3, bufferPosition}),
            super.getStateInternal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * 8);

        final long[] state = NumberFactory.makeLongArray(c[0]);
        key0 = state[0];
        key1 = state[1];
        counter0 = state[2];
        counter1 = state[3];
        counter2 = state[4];
        counter3 = state[5];
        bufferPosition = (int) state[6];
        super.setStateInternal(c[1]);
        rand10();
    }


    /**
     * Create a copy.
     *
     * @return the copy
     */
    private Philox4x64 copy() {
        return new Philox4x64(this);
    }
}
