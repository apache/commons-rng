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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link Long2IntArray} converter.
 */
public class Long2IntArrayTest {
    @Test
    public void testFixedLengthConversion() {
        for (int length = 0; length < 10; length++) {
            testFixedLengthConversion(length);
        }
    }

    private static void testFixedLengthConversion(int length) {
        final Long seed = 567L;
        final int[] out = new Long2IntArray(length).convert(seed);
        Assert.assertEquals(length, out.length);
        // This very seed dependent but the algorithm
        // should only produce 0 about 1 in 2^32 times.
        for (int i = 0; i < length; i++) {
            Assert.assertNotEquals(0, out[i]);
        }
    }
}
