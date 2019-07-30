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
 * Generator (LCG) combined with the XSH-RS (xorshift; random shift) output
 * transformation to create 32-bit output.
 *
 * <p>State size is 128 bits and the period is 2<sup>64</sup>.</p>
 *
 * @see <a href="http://www.pcg-random.org/">
 *  PCG, A Family of Better Random Number Generators</a>
 * @since 1.3
 */
public class PcgXshRs32 extends AbstractPcg6432 {
    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 2, only the first 2 elements will
     * be used; if smaller, the remaining elements will be automatically set.
     */
    public PcgXshRs32(long[] seed) {
        super(seed);
    }

    /** {@inheritDoc} */
    @Override
   protected int transform(long x) {
        final int count = (int)(x >>> 61);
        return (int)((x ^ (x >>> 22)) >>> (22 + count));
    }
}
