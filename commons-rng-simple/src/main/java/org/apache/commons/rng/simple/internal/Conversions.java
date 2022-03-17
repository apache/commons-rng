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
 * Performs seed conversions.
 *
 * <p>Note: Legacy converters from version 1.0 use instances of
 * the {@link SeedConverter} interface. Instances are no longer
 * required as no state is used during conversion and converters
 * can use static methods.
 *
 * @since 1.5
 */
final class Conversions {
    /** No instances. */
    private Conversions() {}

    /**
     * Creates an {@code int} value from a sequence of bytes. The conversion
     * is made as if converting to a {@code int[]} array by filling the ints
     * in little-endian order (least significant byte first), then combining
     * all the ints with a xor operation.
     *
     * @param input Input bytes
     * @return an {@code int}.
     */
    static int byteArray2Int(byte[] input) {
        int output = 0;

        final int n = input.length;
        // xor in the bits to an int in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 2         = integer index
            // i & 0x3        = byte number in the integer  [0, 3]
            // (i & 0x3) << 3 = little-endian byte shift to the integer {0, 8, 16, 24}
            output ^= (input[i] & 0xff) << ((i & 0x3) << 3);
        }

        return output;
    }

    /**
     * Creates a {@code long} value from a sequence of bytes. The conversion
     * is made as if converting to a {@code long[]} array by filling the longs
     * in little-endian order (least significant byte first), then combining
     * all the longs with a xor operation.
     *
     * @param input Input bytes
     * @return a {@code long}.
     */
    static long byteArray2Long(byte[] input) {
        long output = 0;

        final int n = input.length;
        // xor in the bits to a long in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = byte index
            // i >> 3         = long index
            // i & 0x7        = byte number in the long  [0, 7]
            // (i & 0x7) << 3 = little-endian byte shift to the long {0, 8, 16, 24, 32, 36, 40, 48, 56}
            output ^= (input[i] & 0xffL) << ((i & 0x7) << 3);
        }

        return output;
    }

    /**
     * Creates a {@code long} value from a sequence of ints. The conversion
     * is made as if converting to a {@code long[]} array by filling the longs
     * in little-endian order (least significant byte first), then combining
     * all the longs with a xor operation.
     *
     * @param input Input bytes
     * @return a {@code long}.
     */
    static long intArray2Long(int[] input) {
        long output = 0;

        final int n = input.length;
        // xor in the bits to a long in little-endian order
        for (int i = 0; i < n; i++) {
            // i              = int index
            // i >> 1         = long index
            // i & 0x1        = int number in the long  [0, 1]
            // (i & 0x1) << 5 = little-endian byte shift to the long {0, 32}
            output ^= (input[i] & 0xffffffffL) << ((i & 0x1) << 5);
        }

        return output;
    }
}
