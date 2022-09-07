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

import java.util.stream.Stream;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.SplittableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.util.NumberFactory;
import org.apache.commons.rng.core.util.RandomStreams;

/**
 * A 64-bit all purpose generator.
 *
 * <p>This is a member of the LXM family of generators: L=Linear congruential generator;
 * X=Xor based generator; and M=Mix. This member uses a 128-bit LCG and 256-bit Xor-based
 * generator. It is named as {@code "L128X256MixRandom"} in the {@code java.util.random}
 * package introduced in JDK 17; the LXM family is described in further detail in:
 *
 * <blockquote>Steele and Vigna (2021) LXM: better splittable pseudorandom number generators
 * (and almost as fast). Proceedings of the ACM on Programming Languages, Volume 5,
 * Article 148, pp 1–31.</blockquote>
 *
 * <p>Memory footprint is 512 bits and the period is 2<sup>128</sup> (2<sup>256</sup> - 1).
 *
 * <p>This generator implements
 * {@link org.apache.commons.rng.LongJumpableUniformRandomProvider LongJumpableUniformRandomProvider}.
 * In addition instances created with a different additive parameter for the LCG are robust
 * against accidental correlation in a multi-threaded setting. The additive parameters must be
 * different in the most significant 127-bits.
 *
 * <p>This generator implements
 * {@link org.apache.commons.rng.SplittableUniformRandomProvider SplittableUniformRandomProvider}.
 * The stream of generators created using the {@code splits} methods support parallelisation
 * and are robust against accidental correlation by using unique values for the additive parameter
 * for each instance in the same stream. The primitive streaming methods support parallelisation
 * but with no assurances of accidental correlation; each thread uses a new instance with a
 * randomly initialised state.
 *
 * @see <a href="https://doi.org/10.1145/3485525">Steele &amp; Vigna (2021) Proc. ACM Programming
 *      Languages 5, 1-31</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/random/package-summary.html">
 *      JDK 17 java.util.random javadoc</a>
 * @since 1.5
 */
public class L128X256Mix extends AbstractL128 implements SplittableUniformRandomProvider {
    /** Size of the seed vector. */
    private static final int SEED_SIZE = 8;
    /** Size of the XBG state vector. */
    private static final int XBG_STATE_SIZE = 4;
    /** Low half of 128-bit LCG multiplier. */
    private static final long ML = LXMSupport.M128L;

    /** State 0 of the XBG. */
    private long x0;
    /** State 1 of the XBG. */
    private long x1;
    /** State 2 of the XBG. */
    private long x2;
    /** State 3 of the XBG. */
    private long x3;

    /**
     * Creates a new instance.
     *
     * @param seed Initial seed.
     * If the length is larger than 8, only the first 8 elements will
     * be used; if smaller, the remaining elements will be automatically
     * set. A seed containing all zeros in the last four elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>128</sup>.
     *
     * <p>The 1st and 2nd elements are used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 3rd and 4th elements are used
     * to set the LCG state.</p>
     */
    public L128X256Mix(long[] seed) {
        super(seed = extendSeed(seed, SEED_SIZE));
        x0 = seed[4];
        x1 = seed[5];
        x2 = seed[6];
        x3 = seed[7];
    }

    /**
     * Creates a new instance using an 8 element seed.
     * A seed containing all zeros in the last four elements
     * will create a non-functional XBG sub-generator and a low
     * quality output with a period of 2<sup>128</sup>.
     *
     * <p>The 1st and 2nd elements are used to set the LCG increment; the least significant bit
     * is set to odd to ensure a full period LCG. The 3rd and 4th elements are used
     * to set the LCG state.</p>
     *
     * @param seed0 Initial seed element 0.
     * @param seed1 Initial seed element 1.
     * @param seed2 Initial seed element 2.
     * @param seed3 Initial seed element 3.
     * @param seed4 Initial seed element 4.
     * @param seed5 Initial seed element 5.
     * @param seed6 Initial seed element 6.
     * @param seed7 Initial seed element 7.
     */
    public L128X256Mix(long seed0, long seed1, long seed2, long seed3,
                       long seed4, long seed5, long seed6, long seed7) {
        super(seed0, seed1, seed2, seed3);
        x0 = seed4;
        x1 = seed5;
        x2 = seed6;
        x3 = seed7;
    }

    /**
     * Creates a copy instance.
     *
     * @param source Source to copy.
     */
    protected L128X256Mix(L128X256Mix source) {
        super(source);
        x0 = source.x0;
        x1 = source.x1;
        x2 = source.x2;
        x3 = source.x3;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(
                                        new long[] {x0, x1, x2, x3}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] c = splitStateInternal(s, XBG_STATE_SIZE * Long.BYTES);
        final long[] tmp = NumberFactory.makeLongArray(c[0]);
        x0 = tmp[0];
        x1 = tmp[1];
        x2 = tmp[2];
        x3 = tmp[3];
        super.setStateInternal(c[1]);
    }

    /** {@inheritDoc} */
    @Override
    public long next() {
        // LXM generate.
        // Old state is used for the output allowing parallel pipelining
        // on processors that support multiple concurrent instructions.

        long s0 = x0;
        final long sh = lsh;

        // Mix
        final long z = LXMSupport.lea64(sh + s0);

        // LCG update
        // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
        final long sl = lsl;
        final long al = lal;
        final long u = ML * sl;
        // High half
        lsh = ML * sh + LXMSupport.unsignedMultiplyHigh(ML, sl) + sl + lah +
              // Carry propagation
              LXMSupport.unsignedAddHigh(u, al);
        // Low half
        lsl = u + al;

        // XBG update
        long s1 = x1;
        long s2 = x2;
        long s3 = x3;

        final long t = s1 << 17;

        s2 ^= s0;
        s3 ^= s1;
        s1 ^= s2;
        s0 ^= s3;

        s2 ^= t;

        s3 = Long.rotateLeft(s3, 45);

        x0 = s0;
        x1 = s1;
        x2 = s2;
        x3 = s3;

        return z;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The jump size is the equivalent of moving the state <em>backwards</em> by
     * (2<sup>256</sup> - 1) positions. It can provide up to 2<sup>128</sup>
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
     * 2<sup>64</sup> (2<sup>256</sup> - 1) positions. It can provide up to
     * 2<sup>64</sup> non-overlapping subsequences of length 2<sup>64</sup>
     * (2<sup>256</sup> - 1); each subsequence can provide up to 2<sup>64</sup>
     * non-overlapping subsequences of length (2<sup>256</sup> - 1) using the
     * {@link #jump()} method.
     */
    @Override
    public JumpableUniformRandomProvider longJump() {
        return super.longJump();
    }

    /** {@inheritDoc} */
    @Override
    AbstractL128 copy() {
        // This exists to ensure the jump function performed in the super class returns
        // the correct class type. It should not be public.
        return new L128X256Mix(this);
    }

    /** {@inheritDoc} */
    @Override
    public SplittableUniformRandomProvider split(UniformRandomProvider source) {
        return create(source.nextLong(), source);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<SplittableUniformRandomProvider> splits(long streamSize, SplittableUniformRandomProvider source) {
        return RandomStreams.generateWithSeed(streamSize, source, L128X256Mix::create);
    }

    /**
     * Create a new instance using the given {@code seed} and {@code source} of randomness
     * to initialise the instance.
     *
     * @param seed Seed used to initialise the instance.
     * @param source Source of randomness used to initialise the instance.
     * @return A new instance.
     */
    private static SplittableUniformRandomProvider create(long seed, UniformRandomProvider source) {
        // LCG state. The addition lower-half uses the input seed.
        // The LCG addition parameter is set to odd so left-shift the seed.
        final long s0 = source.nextLong();
        final long s1 = seed << 1;
        final long s2 = source.nextLong();
        final long s3 = source.nextLong();
        // XBG state must not be all zero
        long x0 = source.nextLong();
        long x1 = source.nextLong();
        long x2 = source.nextLong();
        long x3 = source.nextLong();
        if ((x0 | x1 | x2 | x3) == 0) {
            // SplitMix style seed ensures at least one non-zero value
            long z = s3;
            x0 = LXMSupport.lea64(z);
            x1 = LXMSupport.lea64(z += LXMSupport.GOLDEN_RATIO_64);
            x2 = LXMSupport.lea64(z += LXMSupport.GOLDEN_RATIO_64);
            x3 = LXMSupport.lea64(z + LXMSupport.GOLDEN_RATIO_64);
        }
        // The LCG addition parameter is set to odd so left-shift the seed
        return new L128X256Mix(s0, s1, s2, s3, x0, x1, x2, x3);
    }
}
