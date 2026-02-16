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

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.rng.ArbitrarilyJumpableUniformRandomProvider;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * This class implements the Philox4x64 256-bit counter-based generator with 10 rounds.
 *
 * <p>This is a member of the Philox family of generators. Memory footprint is 384 bits
 * and the period is 2<sup>258</sup>.</p>
 *
 * <p>Jumping in the sequence is essentially instantaneous.
 * This generator provides both subsequences and arbitrary jumps for easy parallelization.
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
public final class Philox4x64 extends LongProvider implements LongJumpableUniformRandomProvider,
        ArbitrarilyJumpableUniformRandomProvider {
    /** Philox 32-bit mixing constant for counter 0. */
    private static final long PHILOX_M0 = 0xD2E7470EE14C6C93L;
    /** Philox 32-bit mixing constant for counter 1. */
    private static final long PHILOX_M1 = 0xCA5A826395121157L;
    /** Philox 32-bit constant for key 0. */
    private static final long PHILOX_W0 = 0x9E3779B97F4A7C15L;
    /** Philox 32-bit constant for key 1. */
    private static final long PHILOX_W1 = 0xBB67AE8584CAA73BL;
    /** Internal buffer size. */
    private static final int PHILOX_BUFFER_SIZE = 4;
    /** Number of state variables. */
    private static final int STATE_SIZE = 7;
    /** The base-2 logarithm of the period. */
    private static final int LOG_PERIOD = 258;
    /** The period of 2^258 as a double. */
    private static final double PERIOD = 0x1.0p258;
    /** 2^54. Threshold for a double that cannot have the 2 least
     * significant bits set when converted to a long. */
    private static final double TWO_POW_54 = 0x1.0p54;

    /** Counter 0. */
    private long counter0;
    /** Counter 1. */
    private long counter1;
    /** Counter 2. */
    private long counter2;
    /** Counter 3. */
    private long counter3;
    /** Output buffer. */
    private final long[] buffer = new long[PHILOX_BUFFER_SIZE];
    /** Key low bits. */
    private long key0;
    /** Key high bits. */
    private long key1;
    /** Output buffer index. When at the end of the buffer the counter is
     * incremented and the buffer regenerated. */
    private int bufferPosition;

    /**
     * Creates a new instance given 6 long numbers containing, key (first two longs) and
     * the counter (next 4 longs, low bits = first long). The counter is not scrambled and may
     * be used to create contiguous blocks with size a multiple of 4 longs. For example,
     * setting seed[2] = 1 is equivalent to start with seed[2]=0 and calling {@link #next()} 4 times.
     *
     * @param seed Array of size 6 defining key0,key1,counter0,counter1,counter2,counter3.
     *             If the size is smaller, zero values are assumed.
     */
    public Philox4x64(long[] seed) {
        final long[] input = seed.length < 6 ? Arrays.copyOf(seed, 6) : seed;
        setState(input);
        bufferPosition = PHILOX_BUFFER_SIZE;
    }

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
        System.arraycopy(source.buffer, 0, buffer, 0, PHILOX_BUFFER_SIZE);
    }

    /**
     * Copies the state from the array into the generator state.
     *
     * @param state New state.
     */
    private void setState(long[] state) {
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
            NumberFactory.makeByteArray(new long[] {
                key0, key1,
                counter0, counter1, counter2, counter3,
                bufferPosition}),
            super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * Long.BYTES);
        final long[] state = NumberFactory.makeLongArray(c[0]);
        setState(state);
        bufferPosition = (int) state[6];
        super.setStateInternal(c[1]);
        // Regenerate the internal buffer
        rand10();
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        final int p = bufferPosition;
        if (bufferPosition < PHILOX_BUFFER_SIZE) {
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
     * Performs a single round of philox.
     *
     * @param counter Counter, which will be updated after each call.
     * @param key0 Key low bits.
     * @param key1 Key high bits.
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
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>130</sup>
     * calls to {@link UniformRandomProvider#nextLong() nextLong()}. It can provide
     * up to 2<sup>128</sup> non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final Philox4x64 copy = new Philox4x64(this);
        if (++counter2 == 0) {
            counter3++;
        }
        finishJump();
        return copy;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of 2<sup>194</sup> calls to
     * {@link UniformRandomProvider#nextLong() nextLong()}. It can provide up to
     * 2<sup>64</sup> non-overlapping subsequences of length 2<sup>194</sup>; each
     * subsequence can provide up to 2<sup>64</sup> non-overlapping subsequences of
     * length 2<sup>130</sup> using the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final Philox4x64 copy = new Philox4x64(this);
        counter3++;
        finishJump();
        return copy;
    }

    @Override
    public ArbitrarilyJumpableUniformRandomProvider jump(double distance) {
        LongJumpDistances.validateJump(distance, PERIOD);
        // Decompose into an increment for the buffer position and counter
        final int skip = getBufferPositionIncrement(distance);
        final long[] increment = getCounterIncrement(distance);
        return copyAndJump(skip, increment);
    }

    @Override
    public ArbitrarilyJumpableUniformRandomProvider jumpPowerOfTwo(int logDistance) {
        LongJumpDistances.validateJumpPowerOfTwo(logDistance, LOG_PERIOD);
        // For simplicity this re-uses code to increment the buffer position and counter
        // when only one or the other is required for a power of 2.
        // In practice the jump should be much larger than 1 and the necessary regeneration
        // of the buffer is the most time consuming step.
        int skip = 0;
        final long[] increment = new long[PHILOX_BUFFER_SIZE];
        if (logDistance >= 0) {
            if (logDistance <= 1) {
                // The first 2 powers update the buffer position.
                skip = 1 << logDistance;
            } else {
                // Remaining powers update the 256-bit counter
                final int n = logDistance - 2;
                // Create the increment.
                // Start at n / 64 with a 1-bit shifted n % 64
                increment[n >> 6] = 1L << (n & 0x3f);
            }
        }
        return copyAndJump(skip, increment);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<ArbitrarilyJumpableUniformRandomProvider> jumps(double distance) {
        LongJumpDistances.validateJump(distance, PERIOD);
        // Decompose into an increment for the buffer position and counter
        final int skip = getBufferPositionIncrement(distance);
        final long[] increment = getCounterIncrement(distance);
        return Stream.generate(() -> copyAndJump(skip, increment)).sequential();
    }

    /**
     * Gets the buffer position increment from the jump distance.
     *
     * @param distance Jump distance.
     * @return the buffer position increment
     */
    private static int getBufferPositionIncrement(double distance) {
        return distance < TWO_POW_54 ?
            // 2 least significant digits from the integer representation
            (int)((long) distance) & 0x3 :
            0;
    }

    /**
     * Gets the counter increment from the jump distance.
     *
     * @param distance Jump distance.
     * @return the counter increment
     */
    private static long[] getCounterIncrement(double distance) {
        final long[] increment = new long[PHILOX_BUFFER_SIZE];
        // The counter is incremented if the distance is above the buffer size
        // (increment = distance / 4).
        if (distance >= PHILOX_BUFFER_SIZE) {
            LongJumpDistances.writeUnsignedInteger(distance * 0.25, increment);
        }
        return increment;
    }

    /**
     * Copy the generator and advance the internal state. The copy is returned.
     *
     * <p>This method: (1) assumes that the arguments have been validated;
     * and (2) regenerates the output buffer if required.
     *
     * @param skip Amount to skip the buffer position in [0, 3].
     * @param increment Unsigned 256-bit increment, least significant bits first.
     * @return the copy
     */
    private ArbitrarilyJumpableUniformRandomProvider copyAndJump(int skip, long[] increment) {
        final Philox4x64 copy = new Philox4x64(this);

        // Skip the buffer position forward.
        // Assumes position is in [0, 4] and skip is less than 4.
        // Handle rollover but allow position=4 to regenerate buffer on next output call.
        bufferPosition += skip;
        if (bufferPosition > PHILOX_BUFFER_SIZE) {
            bufferPosition -= PHILOX_BUFFER_SIZE;
            incrementCounter();
        }

        // Increment the 256-bit counter.
        // Addition using unsigned int as longs.
        // Any overflow bit is carried to the next counter.
        // Unrolled branchless loop for performance.
        long r;
        long s;
        r = (counter0 & 0xffff_ffffL) + (increment[0] & 0xffff_ffffL);
        s = (counter0 >>> 32) + (increment[0] >>> 32) + (r >>> 32);
        counter0 = (r & 0xffff_ffffL) | (s << 32);

        r = (counter1 & 0xffff_ffffL) + (increment[1] & 0xffff_ffffL) + (s >>> 32);
        s = (counter1 >>> 32) + (increment[1] >>> 32) + (r >>> 32);
        counter1 = (r & 0xffff_ffffL) | (s << 32);

        r = (counter2 & 0xffff_ffffL) + (increment[2] & 0xffff_ffffL) + (s >>> 32);
        s = (counter2 >>> 32) + (increment[2] >>> 32) + (r >>> 32);
        counter2 = (r & 0xffff_ffffL) | (s << 32);

        r = (counter3 & 0xffff_ffffL) + (increment[3] & 0xffff_ffffL) + (s >>> 32);
        s = (counter3 >>> 32) + (increment[3] >>> 32) + (r >>> 32);
        counter3 = (r & 0xffff_ffffL) | (s << 32);

        finishJump();
        return copy;
    }

    /**
     * Finish the jump of this generator. Resets the cached state and regenerates
     * the output buffer if required.
     */
    private void finishJump() {
        resetCachedState();
        // Regenerate the internal buffer only if the buffer position is
        // within the output buffer. Otherwise regeneration is delayed until
        // next output. This allows more efficient consecutive jumping when
        // the buffer is due to be regenerated.
        if (bufferPosition < PHILOX_BUFFER_SIZE) {
            rand10();
        }
    }
}
