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
 * This abstract class is a base for algorithms from the LXM family of
 * generators with a 128-bit LCG sub-generator. The class implements
 * the jump functions.
 *
 * @since 1.5
 */
abstract class AbstractL128 extends LongProvider implements LongJumpableUniformRandomProvider {
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 4;
    /** Low half of 128-bit LCG multiplier. */
    private static final long ML = LXMSupport.M128L;

    /** High half of the 128-bit per-instance LCG additive parameter.
     * Cannot be final to support RestorableUniformRandomProvider. */
    protected long lah;
    /** Low half of the 128-bit per-instance LCG additive parameter (must be odd).
     * Cannot be final to support RestorableUniformRandomProvider. */
    protected long lal;
    /** High half of the 128-bit state of the LCG generator. */
    protected long lsh;
    /** Low half of the 128-bit state of the LCG generator. */
    protected long lsl;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set.
     *
     * <p>The 1st and 2nd elements are used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 3rd and 4th elements are used
     * to set the LCG state.</p>
     */
    AbstractL128(long[] seed) {
        setState(extendSeed(seed, SEED_SIZE));
    }

    /**
     * Creates a new instance using a 4 element seed.
     *
     * <p>The 1st and 2nd elements are used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 3rd and 4th elements are used
     * to set the LCG state.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    AbstractL128(long seed0, long seed1, long seed2, long seed3) {
        lah = seed0;
        // Additive parameter must be odd
        lal = seed1 | 1;
        lsh = seed2;
        lsl = seed3;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    AbstractL128(AbstractL128 source) {
        super(source);
        lah = source.lah;
        lal = source.lal;
        lsh = source.lsh;
        lsl = source.lsl;
    }

    /**
     * Copies the state into the generator state.
     *
     * @param state the new state
     */
    private void setState(long[] state) {
        lah = state[0];
        // Additive parameter must be odd
        lal = state[1] | 1;
        lsh = state[2];
        lsl = state[3];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                                        new long[] {lah, lal, lsh, lsl}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, SEED_SIZE * Long.BYTES);
        setState(NumberFactory.makeLongArray(c[0]));
        super.setStateInternal(c[1]);
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by 1 cycle.
     * The XBG state is unchanged.
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = copy();
        // Advance the LCG 1 step.
        // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
        final long sh = lsh;
        final long sl = lsl;
        final long u = ML * sl;
        // High half
        lsh = ML * sh + LXMSupport.unsignedMultiplyHigh(ML, sl) + sl + lah +
              // Carry propagation
              LXMSupport.unsignedAddHigh(u, lal);
        // Low half
        lsl = u + lal;
        resetCachedState();
        return copy;
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by
     * 2<sup>64</sup> cycles. The XBG state is unchanged.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = copy();
        // Advance the LCG 2^64 steps
        // s = m' * s + c' * c
        // Specialised routine given M128PL=1, C128PL=0 and many terms
        // can be dropped as the low half is unchanged and there is no carry
        // sh = m'l * sh         // sh
        //    + high(m'l * sl)   // dropped as m'l=1 and there is no high part
        //    + m'h * sl
        //    + c'l * ah         // dropped as c'l=0
        //    + high(c'l * ah)   // dropped as c'l=0
        //    + c'h * al
        // sl = m'l * sl + c'l * al
        //    = sl
        lsh = lsh + LXMSupport.M128PH * lsl + LXMSupport.C128PH * lal;
        resetCachedState();
        return copy;
    }

    /**
     * Create a copy.
     *
     * @return the copy
     */
    protected abstract AbstractL128 copy();
}
