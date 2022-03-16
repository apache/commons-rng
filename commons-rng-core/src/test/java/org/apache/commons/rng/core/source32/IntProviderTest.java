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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The tests the caching of calls to {@link IntProvider#nextInt()} are used as
 * the source for {@link IntProvider#nextInt()} and
 * {@link IntProvider#nextBoolean()}.
 */
class IntProviderTest {
    /**
     * A simple class to flip the bits in a number as the source for
     * {@link IntProvider#next()}.
     */
    static final class FlipIntProvider extends IntProvider {
        /** The value. */
        private int value;

        /**
         * @param value the value
         */
        FlipIntProvider(int value) {
            // Flip the bits so the first call to next() returns to the same state
            this.value = ~value;
        }

        @Override
        public int next() {
            // Flip the bits
            value = ~value;
            return value;
        }
    }

    /**
     * This test ensures that the call to {@link IntProvider#nextBoolean()} returns
     * each of the bits from a call to {@link IntProvider#nextInt()}.
     *
     * <p>The order should be from the least-significant bit.
     */
    @Test
    void testNextBoolean() {
        for (int i = 0; i < Integer.SIZE; i++) {
            // Set only a single bit in the source
            final int value = 1 << i;
            final IntProvider provider = new FlipIntProvider(value);
            // Test the result for a single pass over the long
            for (int j = 0; j < Integer.SIZE; j++) {
                final boolean expected = i == j;
                final int index = j;
                Assertions.assertEquals(expected, provider.nextBoolean(), () -> "Pass 1, bit " + index);
            }
            // The second pass should use the opposite bits
            for (int j = 0; j < Integer.SIZE; j++) {
                final boolean expected = i != j;
                final int index = j;
                Assertions.assertEquals(expected, provider.nextBoolean(), () -> "Pass 2, bit " + index);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
        // OK
        "10, 0, 10",
        "10, 5, 5",
        "10, 9, 1",
        // Allowed edge cases
        "0, 0, 0",
        "10, 10, 0",
        // Bad
        "10, 0, 11",
        "10, 10, 1",
        "10, 10, 2147483647",
        "10, 0, -1",
        "10, 5, -1",
    })
    void testNextBytesIndices(int size, int start, int len) {
        final FlipIntProvider rng = new FlipIntProvider(999);
        final byte[] bytes = new byte[size];
        // Be consistent with System.arraycopy
        try {
            System.arraycopy(bytes, start, bytes, start, len);
        } catch (IndexOutOfBoundsException ex) {
            // nextBytes should throw under the same conditions.
            // Note: This is not ArrayIndexOutOfBoundException to be
            // future compatible with Objects.checkFromIndexSize.
            Assertions.assertThrows(IndexOutOfBoundsException.class, () ->
                rng.nextBytes(bytes, start, len));
            return;
        }
        rng.nextBytes(bytes, start, len);
    }
}
