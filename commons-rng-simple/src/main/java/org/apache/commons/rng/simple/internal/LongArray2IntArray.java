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

/**
 * Creates an {@code int[]} from a {@code long[]}.
 *
 * <p>Note: From version 1.5 this conversion uses the long bytes in little-endian order.
 * The output {@code int[]} can be converted back using {@link IntArray2LongArray}.
 *
 * @since 1.0
 */
public class LongArray2IntArray implements SeedConverter<long[], int[]> {
    /** Create an instance. */
    public LongArray2IntArray() {}

    /** {@inheritDoc} */
    @Override
    public int[] convert(long[] seed) {
        // Full length conversion
        return Conversions.longArray2IntArray(seed, Conversions.intSizeFromLongSize(seed.length));
    }
}
