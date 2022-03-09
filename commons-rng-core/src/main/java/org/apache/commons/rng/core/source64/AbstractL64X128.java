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
 * the state save/restore functions.
 *
 * @since 1.5
 */
abstract class AbstractL64X128 extends AbstractL64 {
    /** LCG multiplier. */
    protected static final long M = LXMSupport.M64;
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 4;
    /** Size of the XBG state vector. */
    private static final int XBG_STATE_SIZE = 2;

    /** State 0 of the XBG. */
    protected long x0;
    /** State 1 of the XBG. */
    protected long x1;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     */
    AbstractL64X128(long[] seed) {
        super(seed = extendSeed(seed, SEED_SIZE));
        x0 = seed[2];
        x1 = seed[3];
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>64</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    AbstractL64X128(long seed0, long seed1, long seed2, long seed3) {
        super(seed0, seed1);
        x0 = seed2;
        x1 = seed3;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    AbstractL64X128(AbstractL64X128 source) {
        super(source);
        x0 = source.x0;
        x1 = source.x1;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                                        new long[] {x0, x1}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, XBG_STATE_SIZE * Long.BYTES);
        final long[] tmp = NumberFactory.makeLongArray(c[0]);
        x0 = tmp[0];
        x1 = tmp[1];
        super.setStateInternal(c[1]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of moving the state <em>backwards</em> by
     * (2<sup>128</sup> - 1) positions. It can provide up to 2<sup>64</sup>
     * non-overlapping subsequences.
     */
    @Override
    public UniformRandomProvider jump() {
        return super.jump();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of moving the state <em>backwards</em> by
     * 2<sup>32</sup> (2<sup>128</sup> - 1) positions. It can provide up to
     * 2<sup>32</sup> non-overlapping subsequences of length 2<sup>32</sup>
     * (2<sup>128</sup> - 1); each subsequence can provide up to 2<sup>32</sup>
     * non-overlapping subsequences of length (2<sup>128</sup> - 1) using the
     * {@link #jump()} method.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        return super.longJump();
    }
}
