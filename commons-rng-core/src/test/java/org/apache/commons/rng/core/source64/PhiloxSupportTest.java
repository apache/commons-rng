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
package org.apache.commons.rng.core.source64;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PhiloxSupport}.
 */
class PhiloxSupportTest {
    @Test
    void testGetMathMethod() throws NoSuchMethodException, IllegalAccessException {
        // Java 8 method: Math.addExact(long, long)
        Assertions.assertNotNull(PhiloxSupport.getMathMethod("addExact"));
        Assertions.assertThrows(NoSuchMethodException.class, () -> PhiloxSupport.getMathMethod("foo"));
    }

    @Test
    void testTestUnsignedMultiplyHigh() {
        Assertions.assertTrue(PhiloxSupport.testUnsignedMultiplyHigh(LXMSupport::unsignedMultiplyHigh));
        // Test all code paths
        Assertions.assertFalse(PhiloxSupport.testUnsignedMultiplyHigh(null), "Null operator");
        Assertions.assertFalse(PhiloxSupport.testUnsignedMultiplyHigh((a, b) -> 0), "Invalid multiply operator");
        Assertions.assertFalse(PhiloxSupport.testUnsignedMultiplyHigh((a, b) -> {
            throw new IllegalStateException();
        }), "Illegal call to operator");
    }

    @Test
    void testUnsignedMultiplyHighEdgeCases() {
        final long[] values = {
            -1, 0, 1, Long.MAX_VALUE, Long.MIN_VALUE,
            0xffL, 0xff00L, 0xff0000L, 0xff000000L,
            0xff00000000L, 0xff0000000000L, 0xff000000000000L, 0xff000000000000L,
            0xffffL, 0xffff0000L, 0xffff00000000L, 0xffff000000000000L,
            0xffffffffL, 0xffffffff00000000L,
            // Philox 4x64 multiplication constants
            0xD2E7470EE14C6C93L, 0xCA5A826395121157L,
        };

        for (final long v1 : values) {
            // Must be odd
            if (v1 >= 0) {
                continue;
            }
            for (final long v2 : values) {
                LXMSupportTest.assertMultiplyHigh(v1, v2, PhiloxSupport.unsignedMultiplyHigh(v1, v2));
            }
        }
    }

    @Test
    void testUnsignedMultiplyHigh() {
        final long[] values = new SplittableRandom().longs(100).toArray();
        for (long v1 : values) {
            // Must be odd
            v1 |= Long.MIN_VALUE;
            for (final long v2 : values) {
                LXMSupportTest.assertMultiplyHigh(v1, v2, PhiloxSupport.unsignedMultiplyHigh(v1, v2));
            }
        }
    }
}
