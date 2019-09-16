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

import org.apache.commons.rng.core.util.NumberFactory;

/**
 * Implement Bob Jenkins's small fast (JSF) 32-bit generator.
 *
 * <p>The state size is 128-bits; the shortest period is expected to be about 2<sup>94</sup>
 * and it expected that about one seed will run into another seed within 2<sup>64</sup> values.</p>
 *
 * @see <a href="https://burtleburtle.net/bob/rand/smallprng.html">A small noncryptographic PRNG</a>
 *
 * @since 1.3
 */
public class JSF32 extends IntProvider {
    /** State a. */
    private int a;
    /** State b. */
    private int b;
    /** State c. */
    private int c;
    /** Statd d. */
    private int d;

    /**
     * Creates an instance with the given seed.
     *
     * @param seed Initial seed.
     */
    public JSF32(Integer seed) {
        setSeedInternal(seed);
    }

    /**
     * Seeds the RNG.
     *
     * @param seed Seed.
     */
    private void setSeedInternal(int seed) {
        a = 0xf1ea5eed;
        b = c = d = seed;
        for (int i = 0; i < 20; i++) {
            next();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int next() {
        final int e = a - Integer.rotateLeft(b, 27);
        a = b ^ Integer.rotateLeft(c, 17);
        b = c + d;
        c = d + e;
        d = e + a;
        return d;
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        return composeStateInternal(NumberFactory.makeByteArray(new int[] {a, b, c, d}),
                                    super.getStateInternal());
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(byte[] s) {
        final byte[][] parts = splitStateInternal(s, 4 * 4);

        final int[] tmp = NumberFactory.makeIntArray(parts[0]);
        a = tmp[0];
        b = tmp[1];
        c = tmp[2];
        d = tmp[3];

        super.setStateInternal(parts[1]);
    }
}
