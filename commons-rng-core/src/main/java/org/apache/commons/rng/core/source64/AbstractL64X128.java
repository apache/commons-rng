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
 * This abstract class is a base for algorithms from the LXM family of
 * generators with a 64-bit LCG and 128-bit XBG sub-generator. The class implements
 * the jump and state save/restore functions.
 *
 * @since 1.5
 */
abstract class AbstractL64X128 extends AbstractXoRoShiRo128 {
    /** LCG multiplier. */
    protected static final long M = LXMSupport.M64;
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 4;
    /** Size of the state vector. */
    private static final int STATE_SIZE = 2;

    /** State of the LCG generator. */
    protected long ls;
    /** Per-instance LCG additive parameter (must be odd).
     * Cannot be final to support RestorableUniformRandomProvider. */
    protected long la;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the first two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 3rd element is used to set the LCG state. The 4th element is used
     * to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG.</p>
     */
    AbstractL64X128(long[] seed) {
        // Ensure seed is expanded only once
        super(seed = LXMSupport.ensureSeedLength(seed, SEED_SIZE));
        setState(seed[SEED_SIZE - 2], seed[SEED_SIZE - 1]);
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros in the first two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 3rd element is used to set the LCG state. The 4th element is used
     * to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    AbstractL64X128(long seed0, long seed1, long seed2, long seed3) {
        super(seed0, seed1);
        setState(seed2, seed3);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    AbstractL64X128(AbstractL64X128 source) {
        super(source);
        ls = source.ls;
        la = source.la;
    }

    /**
     * Copies the state into the generator state.
     *
     * @param s State of the LCG
     * @param increment Increment of the LCG
     */
    private void setState(long s, long increment) {
        this.ls = s;
        // Additive parameter must be odd
        this.la = increment | 1;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new long[] {ls, la}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, STATE_SIZE * Long.BYTES);

        final long[] state = NumberFactory.makeLongArray(c[0]);
        setState(state[0], state[1]);

        super.setStateInternal(c[1]);
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
     * <em>backwards</em> by (2<sup>128</sup> - 1) positions. It can provide up to 2<sup>64</sup>
     * non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = copy();
        // Advance the LCG 1 step
        ls = LXMSupport.M64 * ls + la;
        resetCachedState();
        return copy;
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by
     * 2<sup>32</sup> cycles. The XBG state is unchanged. The jump size is the equivalent
     * of moving the state <em>backwards</em> by 2<sup>32</sup> (2<sup>128</sup> - 1)
     * positions. It can provide up to 2<sup>32</sup> non-overlapping subsequences of
     * length 2<sup>32</sup> (2<sup>128</sup> - 1); each subsequence can provide up to
     * 2<sup>32</sup> non-overlapping subsequences of length (2<sup>128</sup> - 1) using
     * the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = copy();
        // Advance the LCG 2^32 steps
        ls = LXMSupport.M64P * ls + LXMSupport.C64P * la;
        resetCachedState();
        return copy;
    }
}
