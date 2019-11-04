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
 * A large-state all-purpose 64-bit generator.
 *
 * <p>This is a member of the Xor-Shift-Rotate family of generators. Memory footprint is 1024 bits
 * and the period is 2<sup>1024</sup>-1.</p>
 *
 * <p>Note: This can be used as a replacement for {@link XorShift1024Star}.</p>
 *
 * @see <a href="http://xorshift.di.unimi.it/xoroshiro1024plusplus.c">Original source code</a>
 * @see <a href="http://xoshiro.di.unimi.it/">xorshiro / xoroshiro generators</a>
 * @since 1.3
 */
public class XoRoShiRo1024PlusPlus extends AbstractXoRoShiRo1024 {
    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 16, only the first 16 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros will create a non-functional generator.
     */
    public XoRoShiRo1024PlusPlus(long[] seed) {
        super(seed);
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected XoRoShiRo1024PlusPlus(XoRoShiRo1024PlusPlus source) {
        super(source);
    }

    /** {@inheritDoc} */
    @Override
    protected long transform(long s0, long s15) {
        return Long.rotateLeft(s0 + s15, 23) + s15;
    }

    /** {@inheritDoc} */
    @Override
    protected XoRoShiRo1024PlusPlus copy() {
        // This exists to ensure the jump function returns
        // the correct class type. It should not be public.
        return new XoRoShiRo1024PlusPlus(this);
    }
}
