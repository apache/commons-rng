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

/**
 * Encodes and decodes bytes as hexadecimal characters.
 *
 * <p>Adapted from commons-codec.</p>
 */
final class Hex {
    /**
     * Used to build 4-bit numbers as Hex.
     */
    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /** No public construction. */
    private Hex() {}

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal
     * values of each byte in order. The returned array will be double the length of the
     * passed array, as it takes two characters to represent any given byte.
     *
     * <p>This can be used to encode byte array seeds into a text representation.</p>
     *
     * @param data A byte[] to convert to Hex characters
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

    /**
     * Converts an array of characters representing hexadecimal values into an array
     * of bytes of those same values. The returned array will be half the length of
     * the passed array, as it takes two characters to represent any given byte. An
     * exception is thrown if the passed char array has an odd number of elements.
     *
     * @param data An array of characters containing hexadecimal digits
     * @return A byte array containing binary data decoded from the supplied char array.
     * @throws IllegalArgumentException Thrown if an odd number or illegal of
     * characters is supplied
     */
    static byte[] decodeHex(final CharSequence data) {
        final int len = data.length();

        if ((len & 0x01) != 0) {
            throw new IllegalArgumentException("Odd number of characters.");
        }

        final byte[] out = new byte[len >> 1];

        // Two characters form the hex value
        for (int j = 0; j < len; j += 2) {
            final int f = (toDigit(data, j) << 4) |
                           toDigit(data, j + 1);
            out[j / 2] = (byte) f;
        }

        return out;
    }

    /**
     * Converts a hexadecimal character to an integer.
     *
     * @param data An array of characters containing hexadecimal digits
     * @param index The index of the character in the source
     * @return An integer
     * @throws IllegalArgumentException Thrown if ch is an illegal hex character
     */
    private static int toDigit(final CharSequence data, final int index) {
        final char ch = data.charAt(index);
        final int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new IllegalArgumentException("Illegal hexadecimal character " + ch +
                                               " at index " + index);
        }
        return digit;
    }
}
