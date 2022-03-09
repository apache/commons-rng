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

import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.LongJumpableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;

/**
 * A 32-bit all purpose generator.
 *
 * <p>This is a member of the LXM family of generators: L=Linear congruential generator;
 * X=Xor based generator; and M=Mix. This member uses a 32-bit LCG and 64-bit Xor-based
 * generator. It is named as {@code "L32X64MixRandom"} in the {@code java.util.random}
 * package introduced in JDK 17; the LXM family is described in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Memory footprint is 128 bits and the period is 2<sup>32</sup> (2<sup>64</sup> - 1).
 *
 * <p>This generator implements {@link LongJumpableUniformRandomProvider}.
 * In addition instances created with a different additive parameter for the LCG are robust
 * against accidental correlation in a multi-threaded setting. The additive parameters must be
 * different in the most significant 31-bits.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/random/package-summary.html">
 *      JDK 17 java.util.random javadoc</a>
 * @since 1.5
 */
public final class L32X64Mix extends IntProvider implements LongJumpableUniformRandomProvider {
    // Implementation note:
    // This does not extend AbstractXoRoShiRo64 as the XBG function is re-implemented
    // inline to allow parallel pipelining. Inheritance would provide only the XBG state.

    /** LCG multiplier. */
    private static final int M = LXMSupport.M32;
    /** Size of the state vector. */
    private static final int SEED_SIZE = 4;

    /** Per-instance LCG additive parameter (must be odd).
     * Cannot be final to support RestorableUniformRandomProvider. */
    private int la;
    /** State of the LCG generator. */
    private int ls;
    /** State 0 of the XBG generator. */
    private int x0;
    /** State 1 of the XBG generator. */
    private int x1;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 4, only the first 4 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>32</sup>.
     *
     * <p>The 1st element is used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 2nd element is used
     * to set the LCG state.</p>
     */
    public L32X64Mix(int[] seed) {
        setState(extendSeed(seed, SEED_SIZE));
    }

    /**
     * Creates a new instance using a 4 element seed.
     * A seed containing all zeros in the last two elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>32</sup>.
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
    public L32X64Mix(int seed0, int seed1, int seed2, int seed3) {
        // Additive parameter must be odd
        la = seed0 | 1;
        ls = seed1;
        x0 = seed2;
        x1 = seed3;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    private L32X64Mix(L32X64Mix source) {
        super(source);
        la = source.la;
        ls = source.ls;
        x0 = source.x0;
        x1 = source.x1;
    }

    /**
     * Copies the state into the generator state.
     *
     * @param state the new state
     */
    private void setState(int[] state) {
        // Additive parameter must be odd
        la = state[0] | 1;
        ls = state[1];
        x0 = state[2];
        x1 = state[3];
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new int[] {la, ls, x0, x1}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, SEED_SIZE * Integer.BYTES);
        setState(NumberFactory.makeIntArray(c[0]));
        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public int next() {
        // LXM generate.
        // Old state is used for the output allowing parallel pipelining
        // on processors that support multiple concurrent instructions.

        final int s0 = x0;
        final int s = ls;

        // Mix
        final int z = LXMSupport.lea32(s + s0);

        // LCG update
        ls = M * s + la;

        // XBG update
        int s1 = x1;

        s1 ^= s0;
        x0 = Integer.rotateLeft(s0, 26) ^ s1 ^ (s1 << 9); // a, b
        x1 = Integer.rotateLeft(s1, 13); // c

        return z;
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by 1 cycle.
     * The XBG state is unchanged. The jump size is the equivalent of moving the state
     * <em>backwards</em> by (2<sup>64</sup> - 1) positions. It can provide up to 2<sup>32</sup>
     * non-overlapping subsequences.</p>
     */
    @Override
    public UniformRandomProvider jump() {
        final UniformRandomProvider copy = new L32X64Mix(this);
        // Advance the LCG 1 step
        ls = M * ls + la;
        resetCachedState();
        return copy;
    }

    /**
     * Creates a copy of the UniformRandomProvider and then <em>retreats</em> the state of the
     * current instance. The copy is returned.
     *
     * <p>The jump is performed by advancing the state of the LCG sub-generator by
     * 2<sup>16</sup> cycles. The XBG state is unchanged. The jump size is the equivalent
     * of moving the state <em>backwards</em> by 2<sup>16</sup> (2<sup>64</sup> - 1)
     * positions. It can provide up to 2<sup>16</sup> non-overlapping subsequences of
     * length 2<sup>16</sup> (2<sup>64</sup> - 1); each subsequence can provide up to
     * 2<sup>16</sup> non-overlapping subsequences of length (2<sup>64</sup> - 1) using
     * the {@link #jump()} method.</p>
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        final JumpableUniformRandomProvider copy = new L32X64Mix(this);
        // Advance the LCG 2^16 steps
        ls = LXMSupport.M32P * ls + LXMSupport.C32P * la;
        resetCachedState();
        return copy;
    }
}
