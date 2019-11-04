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

/**
 * A fast all-purpose 64-bit generator.
 *
 * <p>This is a member of the Xor-Shift-Rotate family of generators. Memory footprint is 128 bits
 * and the period is 2<sup>128</sup>-1. Speed is expected to be similar to
 * {@link XoShiRo256StarStar}.</p>
 *
 * @see <a href="http://xoshiro.di.unimi.it/xoroshiro128plusplus.c">Original source code</a>
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 * @since 1.3
 */
public class XoRoShiRo128PlusPlus extends AbstractXoRoShiRo128 {
    /** The coefficients for the jump function. */
    private static final long[] JUMP_COEFFICIENTS = {
        0x2bd7a6a6e99c2ddcL, 0x0992ccaf6a6fca05L
    };
    /** The coefficients for the long jump function. */
    private static final long[] LONG_JUMP_COEFFICIENTS = {
        0x360fd5f2cf8d5d99L, 0x9c6e6877736c46e3L
    };

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    public XoRoShiRo128PlusPlus(long[] seed) {
        super(seed);
    }

    /**
     * Creates a new instance using a 2 element seed.
     * A seed containing all zeros will create a non-functional generator.
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     */
    public XoRoShiRo128PlusPlus(long seed0, long seed1) {
        super(seed0, seed1);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected XoRoShiRo128PlusPlus(XoRoShiRo128PlusPlus source) {
        super(source);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        // Override the abstract class to use a different state update step.
        // Note: This requires different jump coefficients.

        final long s0 = state0;
        long s1 = state1;
        final long result = Long.rotateLeft(s0 + s1, 17) + s0;

        s1 ^= s0;
        state0 = Long.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21); // a, b
        state1 = Long.rotateLeft(s1, 28); // c

        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected long nextOutput() {
        throw new UnsupportedOperationException("The PlusPlus algorithm redefines the next() method");
    }

    /** {@inheritDoc} */
    @Override
    public UniformRandomProvider jump() {
        // Duplicated from the abstract class to change the jump coefficients
        final UniformRandomProvider copy = copy();
        performJump(JUMP_COEFFICIENTS);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public JumpableUniformRandomProvider longJump() {
        // Duplicated from the abstract class to change the jump coefficients
        final JumpableUniformRandomProvider copy = copy();
        performJump(LONG_JUMP_COEFFICIENTS);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    protected XoRoShiRo128PlusPlus copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new XoRoShiRo128PlusPlus(this);
    }
}
