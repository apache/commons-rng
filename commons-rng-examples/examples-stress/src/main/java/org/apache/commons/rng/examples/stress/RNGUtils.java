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
package org.apache.commons.rng.examples.stress;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;

/**
 * Utility methods for a {@link UniformRandomProvider}.
 */
final class RNGUtils {
    /** No public construction. */
    private RNGUtils() {}

    /**
     * Wrap the random generator with an {@link IntProvider} that will reverse the byte order
     * of the {@code int}.
     *
     * @param rng The random generator.
     * @return the byte reversed random generator.
     * @see Integer#reverseBytes(int)
     */
    static UniformRandomProvider createReverseBytesIntProvider(final UniformRandomProvider rng) {
        // Note:
        // This always uses an IntProvider even if the underlying RNG is a LongProvider.
        // A LongProvider will produce 2 ints from 8 bytes of a long: 76543210 -> 7654 3210.
        // This will be reversed to output 2 ints as: 4567 0123.
        // This is a different output order than if reversing the entire long: 0123 4567.
        // The effect is to output the most significant bits from the long first, and
        // the least significant bits second. Thus the output of ints will be the same
        // on big-endian and little-endian platforms.
        return new IntProvider() {
            @Override
            public int next() {
                return Integer.reverseBytes(rng.nextInt());
            }

            @Override
            public String toString() {
                return "Byte-reversed " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will reverse the bits
     * of the {@code int}.
     *
     * @param rng The random generator.
     * @return the bit reversed random generator.
     * @see Integer#reverse(int)
     */
    static UniformRandomProvider createReverseBitsIntProvider(final UniformRandomProvider rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return Integer.reverse(rng.nextInt());
            }

            @Override
            public String toString() {
                return "Bit-reversed " + rng.toString();
            }
        };
    }

    /**
     * Parses the argument into an object suitable for the RandomSource constructor. Supports:
     *
     * <ul>
     *   <li>Integer
     * </ul>
     *
     * @param argument the argument
     * @return the object
     * @throws ApplicationException If the argument is not recognised
     */
    static Object parseArgument(String argument) {
        try {
            // Currently just support TWO_CMRES_SELECT which uses integers.
            // Future RandomSource implementations may require other parsing, for example
            // recognising a long by the suffix 'L'. This functionality
            // could use Commons Lang NumberUtils.createNumber(String).
            return Integer.parseInt(argument);
        } catch (final NumberFormatException ex) {
            throw new ApplicationException("Failed to parse RandomSource argument: " + argument, ex);
        }
    }
}
