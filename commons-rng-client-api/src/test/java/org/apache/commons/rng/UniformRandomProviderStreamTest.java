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
package org.apache.commons.rng;

import java.util.Spliterator;
import org.junit.jupiter.api.Assertions;

/**
 * Tests for default stream method implementations in {@link UniformRandomProvider}.
 */
class UniformRandomProviderStreamTest extends BaseRandomProviderStreamTest {

    /**
     * Dummy class for checking the behavior of the UniformRandomProvider.
     */
    private static class DummyGenerator implements UniformRandomProvider {
        /** An instance. */
        static final DummyGenerator INSTANCE = new DummyGenerator();

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("The nextLong method should not be invoked");
        }
    }

    @Override
    UniformRandomProvider create() {
        return DummyGenerator.INSTANCE;
    }

    @Override
    UniformRandomProvider createInts(int[] values) {
        return new DummyGenerator() {
            private int i;
            @Override
            public int nextInt() {
                return values[i++];
            }
        };
    }

    @Override
    UniformRandomProvider createInts(int[] values, int origin, int bound) {
        return new DummyGenerator() {
            private int i;
            @Override
            public int nextInt(int o, int b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[i++];
            }
        };
    }

    @Override
    UniformRandomProvider createLongs(long[] values) {
        return new DummyGenerator() {
            private int i;
            @Override
            public long nextLong() {
                return values[i++];
            }
        };
    }

    @Override
    UniformRandomProvider createLongs(long[] values, long origin, long bound) {
        return new DummyGenerator() {
            private int i;
            @Override
            public long nextLong(long o, long b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[i++];
            }
        };
    }

    @Override
    UniformRandomProvider createDoubles(double[] values) {
        return new DummyGenerator() {
            private int i;
            @Override
            public double nextDouble() {
                return values[i++];
            }
        };
    }

    @Override
    UniformRandomProvider createDoubles(double[] values, double origin, double bound) {
        return new DummyGenerator() {
            private int i;
            @Override
            public double nextDouble(double o, double b) {
                Assertions.assertEquals(origin, o, "origin");
                Assertions.assertEquals(bound, b, "bound");
                return values[i++];
            }
        };
    }

    @Override
    int getCharacteristics() {
        // The current stream produced by the generate method only returns immutable
        return Spliterator.IMMUTABLE;
    }
}
