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
package org.apache.commons.rng.simple.internal;

/**
 * Performs mixing of bits.
 *
 * @since 1.5
 */
final class MixFunctions {
    /**
     * The fractional part of the the golden ratio, phi, scaled to 64-bits and rounded to odd.
     * This can be used as an increment for a Weyl sequence.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;
    /**
     * The fractional part of the the golden ratio, phi, scaled to 32-bits and rounded to odd.
     * This can be used as an increment for a Weyl sequence.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Golden_ratio">Golden ratio</a>
     */
    static final int GOLDEN_RATIO_32 = 0x9e3779b9;

    /** No instances. */
    private MixFunctions() {}

    /**
     * Perform variant 13 of David Stafford's 64-bit mix function.
     * This is the mix function used in the
     * {@link org.apache.commons.rng.core.source64.SplitMix64 SplitMix64} RNG.
     *
     * <p>This is ranked first of the top 14 Stafford mixers.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better
     *      Bit Mixing - Improving on MurmurHash3&#39;s 64-bit Finalizer.</a>
     */
    static long stafford13(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    /**
     * Perform the finalising 32-bit mix function of Austin Appleby's MurmurHash3.
     *
     * @param x the input value
     * @return the output value
     * @see <a href="https://github.com/aappleby/smhasher">SMHasher</a>
     */
    static int murmur3(int x) {
        x = (x ^ (x >>> 16)) * 0x85ebca6b;
        x = (x ^ (x >>> 13)) * 0xc2b2ae35;
        return x ^ (x >>> 16);
    }
}
