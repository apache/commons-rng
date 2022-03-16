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
 * Creates a {@code long[]} from a {@code byte[]}.
 *
 * @since 1.0
 */
public class ByteArray2LongArray implements Seed2ArrayConverter<byte[], long[]> {
    /** {@inheritDoc} */
    @Override
    public long[] convert(byte[] seed) {
        // Full length conversion
        return convertSeed(seed, SeedUtils.longSizeFromByteSize(seed.length));
    }

    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    @Override
    public long[] convert(byte[] seed, int outputSize) {
        return convertSeed(seed, outputSize);
    }

    /**
     * Creates an array of {@code long} values from a sequence of bytes. The longs are
     * filled in little-endian order (least significant byte first).
     *
     * @param input Input bytes
     * @param length Output length
     * @return an array of {@code long}.
     */
    private static long[] convertSeed(byte[] input, int length) {
        final long[] output = new long[length];

        // Overflow-safe minimum using long
        final int n = (int) Math.min(input.length, length * (long) Long.BYTES);
        // Little-endian fill
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 3         = long index
            // i & 0x7        = byte number in the long  [0, 7]
            // (i & 0x7) << 3 = little-endian byte shift to the long {0, 8, 16, 24, 32, 36, 40, 48, 56}
            output[i >> 3] |= (input[i] & 0xffL) << ((i & 0x7) << 3);
        }

        return output;
    }
}
