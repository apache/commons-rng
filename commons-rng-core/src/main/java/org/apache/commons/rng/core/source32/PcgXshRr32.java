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
 * A Permuted Congruential Generator (PCG) that is composed of a 64-bit Linear Congruential
 * Generator (LCG) combined with the XSH-RR (xorshift; random rotate) output
 * transformation to create 32-bit output.
 *
 * <p>State size is 128 bits and the period is 2<sup>64</sup>.</p>
 *
 * <p><strong>Note:</strong> Although the seed size is 128 bits, only the first 64 are
 * effective: in effect, two seeds that only differ by the last 64 bits may produce
 * highly correlated sequences.
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
public class PcgXshRr32 extends AbstractPcg6432 {
    /**
     * Creates a new instance using a default increment.
     *
     * @param seed Initial state.
     * @since 1.4
     */
    public PcgXshRr32(final Long seed) {
        super(seed);
    }

    /**
     * Creates a new instance.
     *
     * <p><strong>Note:</strong> Although the seed size is 128 bits, only the first 64 are
     * effective: in effect, two seeds that only differ by the last 64 bits may produce
     * highly correlated sequences.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     *
     * <p>The 1st element is used to set the LCG state. The 2nd element is used
     * to set the LCG increment; the most significant bit
     * is discarded by left shift and the increment is set to odd.</p>
     */
    public PcgXshRr32(final long[] seed) {
        super(seed);
    }

    /** {@inheritDoc} */
    @Override
    protected int transform(final long x) {
        final int count = (int)(x >>> 59);
        return Integer.rotateRight((int)((x ^ (x >>> 18)) >>> 27), count);
    }
}
