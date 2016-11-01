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
 * Tests for the {@link ByteArray2LongArray} converter.
 */
public class ByteArray2LongArrayTest {
    @Test
    public void testSeedSizeIsMultipleOfLongSize() {
        final byte[] seed = new byte[128];
        final long[] out = new ByteArray2LongArray().convert(seed);
        Assert.assertEquals(16, out.length);
    }

    @Test
    public void testSeedSizeIsNotMultipleOfLongSize() {
        final int len = 16;
        final ByteArray2LongArray conv = new ByteArray2LongArray();
        for (int i = 1; i < 8; i++) {
            final byte[] seed = new byte[len + i];
            final long[] out = conv.convert(seed);
            Assert.assertEquals(3, out.length);
        }
    }
}
