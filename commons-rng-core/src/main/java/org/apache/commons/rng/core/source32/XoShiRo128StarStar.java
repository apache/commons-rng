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

/**
 * A fast all-purpose 32-bit generator. For faster generation of {@code float} values try the
 * {@link XoShiRo128Plus} generator.
 *
 * <p>This is a member of the Xor-Shift-Rotate family of generators. Memory footprint is 128
 * bits.</p>
 *
 * @see <a href="http://xoshiro.di.unimi.it/xoshiro128starstar.c">Original source code</a>
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 * @since 1.3
 */
public class XoShiRo128StarStar extends AbstractXoShiRo128 {
    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    public XoShiRo128StarStar(final int[] seed) {
        super(seed);
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros will create a non-functional generator.
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     */
    public XoShiRo128StarStar(final int seed0, final int seed1, final int seed2, final int seed3) {
        super(seed0, seed1, seed2, seed3);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected XoShiRo128StarStar(final XoShiRo128StarStar source) {
        super(source);
    }

    /** {@inheritDoc} */
    @Override
    protected int nextOutput() {
        return Integer.rotateLeft(state0 * 5, 7) * 9;
    }

    /** {@inheritDoc} */
    @Override
    protected XoShiRo128StarStar copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new XoShiRo128StarStar(this);
    }
}
