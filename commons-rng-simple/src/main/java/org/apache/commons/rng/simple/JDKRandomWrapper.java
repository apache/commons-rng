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
package org.apache.commons.rng.simple;

import org.apache.commons.rng.UniformRandomProvider;

import java.util.Random;

/**
 * Wraps a {@link Random} instance to implement {@link UniformRandomProvider}. All methods from
 * the {@code Random} that match those in {@code UniformRandomProvider} are used directly.
 *
 * <p>This class can be used to wrap an instance of
 * {@link java.security.SecureRandom SecureRandom}. The {@code SecureRandom} class provides
 * cryptographic random number generation. The features available depend on the Java version
 * and platform. Consult the Java documentation for more details.</p>
 *
 * <p>Note: Use of {@code java.util.Random} is <em>not</em> recommended for applications.
 * There are many other pseudo-random number generators that are statistically superior and often
 * faster (see {@link RandomSource}).</p>
 *
 * @since 1.3
 * @see java.security.SecureRandom
 * @see RandomSource
 */
public final class JDKRandomWrapper implements UniformRandomProvider {
    /** The JDK Random instance. */
    private final Random rng;

    /**
     * Create a wrapper around a Random instance.
     *
     * @param rng JDK {@link Random} instance to which the random number
     * generation is delegated.
     */
    public JDKRandomWrapper(Random rng) {
        this.rng = rng;
    }

    /** {@inheritDoc} */
    @Override
    public void nextBytes(byte[] bytes) {
        rng.nextBytes(bytes);
    }

    /** {@inheritDoc} */
    @Override
    public void nextBytes(byte[] bytes,
                          int start,
                          int len) {
        final byte[] reduced = new byte[len];
        rng.nextBytes(reduced);
        System.arraycopy(reduced, 0, bytes, start, len);
    }

    /** {@inheritDoc} */
    @Override
    public int nextInt() {
        return rng.nextInt();
    }

    /** {@inheritDoc} */
    @Override
    public int nextInt(int n) {
        return rng.nextInt(n);
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong() {
        return rng.nextLong();
    }

    /** {@inheritDoc} */
    @Override
    public long nextLong(long n) {
        // Code copied from "o.a.c.rng.core.BaseProvider".
        if (n <= 0) {
            throw new IllegalArgumentException("Must be strictly positive: " + n);
        }

        long bits;
        long val;
        do {
            bits = nextLong() >>> 1;
            val  = bits % n;
        } while (bits - val + (n - 1) < 0);

        return val;
    }

    /** {@inheritDoc} */
    @Override
    public boolean nextBoolean() {
        return rng.nextBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public float nextFloat() {
        return rng.nextFloat();
    }

    /** {@inheritDoc} */
    @Override
    public double nextDouble() {
        return rng.nextDouble();
    }
}
