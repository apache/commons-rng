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

import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the {@link LongArray2IntArray} converter.
 */
class LongArray2IntArrayTest {
    /**
     * Gets the lengths for the output int[] seeds.
     *
     * @return the lengths
     */
    static IntStream getLengths() {
        return IntStream.rangeClosed(0, 5);
    }

    /**
     * Gets the expected input length to produce the specified number of ints.
     *
     * @param ints Number of ints
     * @return the input length
     */
    private static int getInputLength(int ints) {
        return (int) Math.ceil((double) ints / Integer.BYTES);
    }

    @ParameterizedTest
    @MethodSource(value = {"getLengths"})
    void testIntSizeIsMultipleOfSeedSize(int ints) {
        final long[] seed = new long[getInputLength(ints)];

        final int[] out = new LongArray2IntArray().convert(seed);
        Assertions.assertEquals(seed.length * 2, out.length);
    }
}
