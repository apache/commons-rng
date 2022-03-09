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
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A 64-bit all purpose generator.
 *
 * <p>This is a member of the LXM family of generators: L=Linear congruential generator;
 * X=Xor based generator; and M=Mix. This member uses a 128-bit LCG and 128-bit Xor-based
 * generator. It is named as {@code "L128X128Mix"} in the {@code java.util.random}
 * package introduced in JDK 17; the LXM family is described in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Memory footprint is 384 bits and the period is (2<sup>128</sup> - 1) 2<sup>128</sup>.
 *
 * <p>This generator implements
 * {@link org.apache.commons.rng.LongJumpableUniformRandomProvider LongJumpableUniformRandomProvider}.
 * In addition instances created with a different additive parameter for the LCG are robust
 * against accidental correlation in a multi-threaded setting. The additive parameters must be
 * different in the most significant 127-bits.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/random/package-summary.html">
 *      JDK 17 java.util.random javadoc</a>
 * @since 1.5
 */
public class L128X128Mix extends AbstractXoRoShiRo128 {
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 6;
    /** Size of the state vector. */
    private static final int STATE_SIZE = 4;
    /** Low half of 128-bit LCG multiplier. */
    private static final long ML = LXMSupport.M128L;

    /** High half of the 128-bit state of the LCG generator. */
    private long lsh;
    /** Low half of the 128-bit state of the LCG generator. */
    private long lsl;
    /** High half of the 128-bit per-instance LCG additive parameter.
     * Cannot be final to support RestorableUniformRandomProvider. */
    private long lah;
    /** Low half of the 128-bit per-instance LCG additive parameter (must be odd).
     * Cannot be final to support RestorableUniformRandomProvider. */
    private long lal;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 6, only the first 6 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the first four elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>128</sup>.
     *
     * <p>The 3rd and 4th elements are used to set the LCG state.
     * The 5th and 6th elements are used
     * to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG.</p>
     */
    public L128X128Mix(long[] seed) {
        // Ensure seed is expanded only once
        super(seed = LXMSupport.ensureSeedLength(seed, SEED_SIZE));
        setState(seed[SEED_SIZE - 4], seed[SEED_SIZE - 3], seed[SEED_SIZE - 2], seed[SEED_SIZE - 1]);
    }

    /**
     * Creates a new instance using an 6 element seed.
     * A seed containing all zeros in the first four elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>128</sup>.
     *
     * <p>The 3rd and 4th elements are used to set the LCG state.
     * The 5th and 6th elements are used
     * to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     * @param seed4 Initial seed element 4.
     * @param seed5 Initial seed element 5.
     */
    public L128X128Mix(long seed0, long seed1, long seed2, long seed3,
                       long seed4, long seed5) {
        super(seed0, seed1);
        setState(seed2, seed3, seed4, seed5);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected L128X128Mix(L128X128Mix source) {
        super(source);
        lsh = source.lsh;
        lsl = source.lsl;
        lah = source.lah;
        lal = source.lal;
    }

    /**
     * Copies the state into the generator state.
     *
     * @param sh High half of the 128-bit state of the LCG
     * @param sl Low half of the 128-bit state of the LCG
     * @param ah High half of the 128-bit increment of the LCG
     * @param al Low half of the 128-bit increment of the LCG
     */
    private void setState(long sh, long sl, long ah, long al) {
        this.lsh = sh;
        this.lsl = sl;
        this.lah = ah;
        // Additive parameter must be odd
        this.lal = al | 1;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new long[] {lsh, lsl, lah, lal}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * Long.BYTES);

        final long[] state = NumberFactory.makeLongArray(c[0]);
        setState(state[0], state[1], state[2], state[3]);

        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        // LXM generate.
        // Old state is used for the output allowing parallel pipelining
        // on processors that support multiple concurrent instructions.

        final long s0 = state0;
        final long sh = lsh;

        // Mix
        final long z = LXMSupport.lea64(sh + s0);

        // LCG update
        // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
        final long sl = lsl;
        final long u = ML * sl;
        // High half
        lsh = ML * sh + LXMSupport.unsignedMultiplyHigh(ML, sl) + sl + lah;
        // Low half
        lsl = u + lal;
        // Carry propagation
        if (Long.compareUnsigned(lsl, u) < 0) {
            ++lsh;
        }

        // XBG update
        long s1 = state1;

        s1 ^= s0;
        state0 = Long.rotateLeft(s0, 24) ^ s1 ^ (s1 << 16); // a, b
        state1 = Long.rotateLeft(s1, 37); // c

        return z;
    }

    /** Unsupported. The LXM algorithm redefines the next() method. */
    @Override
    protected long nextOutput() {
        throw new UnsupportedOperationException("The LXM algorithm redefines the next() method");
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by 1 cycle.
     * The XBG state is unchanged. The jump size is the equivalent of moving the state
     * <em>backwards</em> by (2<sup>128</sup> - 1) positions. It can provide up to 2<sup>128</sup>
     * non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = copy();
        // Advance the LCG 1 step.
        final long sh = lsh;
        final long sl = lsl;
        final long u = ML * sl;
        // High half
        lsh = ML * sh + LXMSupport.unsignedMultiplyHigh(ML, sl) + sl + lah;
        // Low half
        lsl = u + lal;
        // Carry propagation
        if (Long.compareUnsigned(lsl, u) < 0) {
            ++lsh;
        }
        resetCachedState();
        return copy;
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by
     * 2<sup>64</sup> cycles. The XBG state is unchanged. The jump size is the equivalent
     * of moving the state <em>backwards</em> by 2<sup>64</sup> (2<sup>128</sup> - 1)
     * positions. It can provide up to 2<sup>64</sup> non-overlapping subsequences of
     * length 2<sup>64</sup> (2<sup>128</sup> - 1); each subsequence can provide up to
     * 2<sup>64</sup> non-overlapping subsequences of length (2<sup>128</sup> - 1) using
     * the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = copy();
        // Advance the LCG 2^64 steps
        // Specialised routine given M128PL=1, C128PL=0 and many terms
        // can be dropped as the low half is unchanged
        lsh = lsh + LXMSupport.M128PH * lsl + LXMSupport.C128PH * lal;
        resetCachedState();
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    protected L128X128Mix copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new L128X128Mix(this);
    }
}
