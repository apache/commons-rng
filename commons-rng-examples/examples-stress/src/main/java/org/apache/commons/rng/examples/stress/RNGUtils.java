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

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for a {@link UniformRandomProvider}.
 */
final class RNGUtils {
    /**
     * Used to build 4-bit numbers as Hex.
     */
    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

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
     * Wrap the random generator with an {@link IntProvider} that will use the upper 32-bits
     * of the {@code long} from {@link UniformRandomProvider#nextLong()}.
     *
     * @param rng The random generator.
     * @return the upper bits random generator.
     */
    static UniformRandomProvider createLongUpperBitsIntProvider(final UniformRandomProvider rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return (int) (rng.nextLong() >>> 32);
            }

            @Override
            public String toString() {
                return "Long upper-bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use the lower 32-bits
     * of the {@code long} from {@link UniformRandomProvider#nextLong()}.
     *
     * @param rng The random generator.
     * @return the lower bits random generator.
     */
    static UniformRandomProvider createLongLowerBitsIntProvider(final UniformRandomProvider rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return (int) rng.nextLong();
            }

            @Override
            public String toString() {
                return "Long lower-bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will combine the bits
     * using a {@code xor} operation with a generated hash code.
     *
     * <pre>{@code
     * System.identityHashCode(new Object()) ^ rng.nextInt()
     * }</pre>
     *
     * Note: This generator will be slow.
     *
     * @param rng The random generator.
     * @return the combined random generator.
     * @see System#identityHashCode(Object)
     */
    static UniformRandomProvider createHashCodeIntProvider(final UniformRandomProvider rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return System.identityHashCode(new Object()) ^ rng.nextInt();
            }

            @Override
            public String toString() {
                return "HashCode ^ " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will combine the bits
     * using a {@code xor} operation with the output from {@link ThreadLocalRandom}.
     *
     * <pre>{@code
     * ThreadLocalRandom.current().nextInt() ^ rng.nextInt()
     * }</pre>
     *
     * @param rng The random generator.
     * @return the combined random generator.
     */
    static UniformRandomProvider createThreadLocalRandomIntProvider(final UniformRandomProvider rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return ThreadLocalRandom.current().nextInt() ^ rng.nextInt();
            }

            @Override
            public String toString() {
                return "ThreadLocalRandom ^ " + rng.toString();
            }
        };
    }

    /**
     * Combine the two random generators using a {@code xor} operations.
     *
     * <pre>{@code
     * rng1.nextInt() ^ rng2.nextInt()
     * }</pre>
     *
     * @param rng1 The first random generator.
     * @param rng2 The second random generator.
     * @return the combined random generator.
     */
    static UniformRandomProvider createXorIntProvider(final UniformRandomProvider rng1,
                                                      final UniformRandomProvider rng2) {
        return new IntProvider() {
            @Override
            public int next() {
                return rng1.nextInt() ^ rng2.nextInt();
            }

            @Override
            public String toString() {
                return rng1.toString() + " ^ " + rng2.toString();
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

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal
     * values of each byte in order. The returned array will be double the length of the
     * passed array, as it takes two characters to represent any given byte.
     *
     * <p>This can be used to encode byte array seeds into a text representation.</p>
     *
     * <p>Adapted from commons-codec.</p>
     *
     * @param data a byte[] to convert to Hex characters
     * @return A char[] containing the lower-case Hex representation
     */
    static char[] encodeHex(final byte[] data) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // Two characters form the hex value
        for (int i = 0; i < l; i++) {
            // Upper 4-bits
            out[2 * i]     = HEX_DIGITS[(0xf0 & data[i]) >>> 4];
            // Lower 4-bits
            out[2 * i + 1] = HEX_DIGITS[ 0x0f & data[i]];
        }
        return out;
    }
}
