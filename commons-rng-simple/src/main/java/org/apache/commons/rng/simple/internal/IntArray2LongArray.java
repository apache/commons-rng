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

import org.apache.commons.rng.core.util.NumberFactory;

/**
 * Creates a {@code long[]} from an {@code int[]}.
 *
 * @since 1.0
 */
public class IntArray2LongArray implements SeedConverter<int[], long[]> {
    /** {@inheritDoc} */
    @Override
    public long[] convert(final int[] seed) {
        final int outSize = (seed.length + 1) / 2;
        final long[] out = new long[outSize];
        for (int i = 0; i < outSize; i++) {
            final int lo = seed[i];
            final int hi = outSize + i < seed.length ? seed[outSize + i] : 0;
            out[i] = NumberFactory.makeLong(hi, lo);
        }

        return out;
    }
}
