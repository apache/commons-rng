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

/**
 * A fast 64-bit generator suitable for {@code double} generation. This is slightly faster than the
 * all-purpose generator {@link XoRoShiRo128StarStar}.
 *
 * <p>This is a member of the Xor-Shift-Rotate family of generators. Memory footprint is 128 bits
 * and the period is 2<sup>128</sup>-1. Speed is expected to be similar to
 * {@link XoShiRo256Plus}.</p>
 *
 * @see <a href="http://xoshiro.di.unimi.it/xoroshiro128plus.c">Original source code</a>
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 * @since 1.3
 */
public class XoRoShiRo128Plus extends AbstractXoRoShiRo128 {
    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    public XoRoShiRo128Plus(final long[] seed) {
        super(seed);
    }

    /**
     * Creates a new instance using a 2 element seed.
     * A seed containing all zeros will create a non-functional generator.
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     */
    public XoRoShiRo128Plus(final long seed0, final long seed1) {
        super(seed0, seed1);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected XoRoShiRo128Plus(final XoRoShiRo128Plus source) {
        super(source);
    }

    /** {@inheritDoc} */
    @Override
    protected long nextOutput() {
        return state0 + state1;
    }

    /** {@inheritDoc} */
    @Override
    protected XoRoShiRo128Plus copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new XoRoShiRo128Plus(this);
    }
}
