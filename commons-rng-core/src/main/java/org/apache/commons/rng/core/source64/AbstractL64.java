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
 * generators with a 64-bit LCG sub-generator. The class implements
 * the jump functions.
 *
 * @since 1.5
 */
abstract class AbstractL64 extends LongProvider implements LongJumpableUniformRandomProvider {
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 2;

    /** Per-instance LCG additive parameter (must be odd).
     * Cannot be final to support RestorableUniformRandomProvider. */
    protected long la;
    /** State of the LCG generator. */
    protected long ls;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     */
    AbstractL64(long[] seed) {
        setState(extendSeed(seed, SEED_SIZE));
    }

    /**
     * Creates a new instance using a 2 element seed.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     */
    AbstractL64(long seed0, long seed1) {
        // Additive parameter must be odd
        la = seed0 | 1;
        ls = seed1;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    AbstractL64(AbstractL64 source) {
        super(source);
        la = source.la;
        ls = source.ls;
    }

    /**
     * Copies the state into the generator state.
     *
     * @param state the new state
     */
    private void setState(long[] state) {
        // Additive parameter must be odd
        la = state[0] | 1;
        ls = state[1];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                                        new long[] {la, ls}),
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
     * 2<sup>32</sup> cycles. The XBG state is unchanged.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = copy();
        // Advance the LCG 2^32 steps
        ls = LXMSupport.M64P * ls + LXMSupport.C64P * la;
        resetCachedState();
        return copy;
    }

    /**
     * Create a copy.
     *
     * @return the copy
     */
    abstract AbstractL64 copy();
}
