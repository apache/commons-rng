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

import org.junit.Assert;
import org.junit.Test;

/**
 * The tests the caching of calls to {@link IntProvider#nextInt()} are used
 * as the source for {@link IntProvider#nextInt()} and
 * {@link IntProvider#nextBoolean()}.
 */
public class IntProviderTest {

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
        public FlipIntProvider(int value) {
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
    public void testNextBoolean() {
        for (int i = 0; i < Integer.SIZE; i++) {
            // Set only a single bit in the source
            final int value = 1 << i;
            final IntProvider provider = new FlipIntProvider(value);
            // Test the result for a single pass over the long
            for (int j = 0; j < Integer.SIZE; j++) {
                final boolean expected = (i == j);
                Assert.assertEquals("Pass 1, bit " + j, expected, provider.nextBoolean());
            }
            // The second pass should use the opposite bits
            for (int j = 0; j < Integer.SIZE; j++) {
                final boolean expected = (i != j);
                Assert.assertEquals("Pass 2, bit " + j, expected, provider.nextBoolean());
            }
        }
    }
}
