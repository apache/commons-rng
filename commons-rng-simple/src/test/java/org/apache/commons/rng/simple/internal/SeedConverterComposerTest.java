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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SeedConverterComposer}.
 */
class SeedConverterComposerTest {
    @Test
    void testComposedCoversion() {
        final Int2Long int2Long = new Int2Long();
        final Long2LongArray long2LongArray = new Long2LongArray(3);
        final SeedConverterComposer<Integer, Long, long[]> composer =
                new SeedConverterComposer<Integer, Long, long[]>(int2Long, long2LongArray);
        final Integer in = 123;
        final Object out = composer.convert(in);
        Assertions.assertTrue(out instanceof long[], "Bad type conversion");
        Assertions.assertEquals(3, ((long[])out).length, "Incorrect long[] length");
    }
}
