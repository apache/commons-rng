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

import java.util.Random;

import org.apache.commons.rng.UniformRandomProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JDKRandomWrapper} class.
 */
class JDKRandomWrapperTest {
    /**
     * Test all the methods shared by Random and UniformRandomProvider are equivalent.
     */
    @Test
    void testJDKRandomEquivalence() {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng1 = new Random(seed);
        final UniformRandomProvider rng2 = new JDKRandomWrapper(new Random(seed));
        checkSameSequence(rng1, rng2);
    }

    /**
     * Ensure that both generators produce the same sequences.
     *
     * @param rng1 RNG.
     * @param rng2 RNG.
     */
    private static void checkSameSequence(Random rng1,
                                          UniformRandomProvider rng2) {
        for (int i = 0; i < 4; i++) {
            Assertions.assertEquals(rng1.nextInt(),
                                    rng2.nextInt());
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(rng1.nextLong(),
                                    rng2.nextLong());
        }
        for (int i = 0; i < 9; i++) {
            Assertions.assertEquals(rng1.nextFloat(),
                                    rng2.nextFloat());
        }
        for (int i = 0; i < 12; i++) {
            Assertions.assertEquals(rng1.nextDouble(),
                                    rng2.nextDouble());
        }
        for (int i = 0; i < 18; i++) {
            Assertions.assertEquals(rng1.nextBoolean(),
                                    rng2.nextBoolean());
        }
        for (int i = 0; i < 19; i++) {
            final int max = i + 123456;
            Assertions.assertEquals(rng1.nextInt(max),
                                    rng2.nextInt(max));
        }

        final int len = 233;
        final byte[] store1 = new byte[len];
        final byte[] store2 = new byte[len];
        rng1.nextBytes(store1);
        rng2.nextBytes(store2);
        for (int i = 0; i < len; i++) {
            Assertions.assertEquals(store1[i],
                                    store2[i]);
        }
    }
}
