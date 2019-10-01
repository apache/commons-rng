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

package org.apache.commons.rng.examples.jmh.core;

import org.apache.commons.rng.UniformRandomProvider;

/**
 * Defines baseline implementations for the {@link UniformRandomProvider}.
 */
public final class BaselineUtils {
    /** No public construction. */
    private BaselineUtils() {}

    /**
     * Default implementation of {@link UniformRandomProvider} that does nothing.
     *
     * <p>Note: This is not a good baseline as the JVM can optimise the predictable result
     * of the method calls. This is here for convenience when implementing
     * UniformRandomProvider.</p>
     */
    private abstract static class DefaultProvider implements UniformRandomProvider {
        @Override
        public void nextBytes(byte[] bytes) {
            // Do nothing
        }

        @Override
        public void nextBytes(byte[] bytes, int start, int len) {
            // Do nothing
        }

        @Override
        public int nextInt() {
            return 0;
        }

        @Override
        public int nextInt(int n) {
            return 0;
        }

        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public long nextLong(long n) {
            return 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public float nextFloat() {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0;
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextBytes(byte[])} and
     * {@link UniformRandomProvider#nextBytes(byte[], int, int)}.
     */
    private static final class BaselineNextBytes extends DefaultProvider {
        /**
         * The fixed value to fill the byte array.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private byte value;

        @Override
        public void nextBytes(byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = value;
            }
        }

        @Override
        public void nextBytes(byte[] bytes, int start, int len) {
            for (int i = start; i < len; i++) {
                bytes[i] = value;
            }
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextInt()} and
     * {@link UniformRandomProvider#nextInt(int)}.
     */
    private static final class BaselineNextInt extends DefaultProvider {
        /**
         * The fixed value to return.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private int value;

        @Override
        public int nextInt() {
            return value;
        }

        @Override
        public int nextInt(int n) {
            return value;
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextLong()} and
     * {@link UniformRandomProvider#nextLong(long)}.
     */
    private static final class BaselineNextLong extends DefaultProvider {
        /**
         * The fixed value to return.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private long value;

        @Override
        public long nextLong() {
            return value;
        }

        @Override
        public long nextLong(long n) {
            return value;
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextBoolean()}.
     */
    private static final class BaselineNextBoolean extends DefaultProvider {
        /**
         * The fixed value to return.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private boolean value;

        @Override
        public boolean nextBoolean() {
            return value;
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextFloat()}.
     */
    private static final class BaselineNextFloat extends DefaultProvider {
        /**
         * The fixed value to return.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private float value;

        @Override
        public float nextFloat() {
            return value;
        }
    }

    /**
     * Baseline implementation for {@link UniformRandomProvider#nextDouble()}.
     */
    private static final class BaselineNextDouble extends DefaultProvider {
        /**
         * The fixed value to return.
         *
         * <p><strong>DON'T</strong> make this final!
         * This must be a viewed by the JVM as something that cannot be optimised away.</p>
         */
        private double value;

        @Override
        public double nextDouble() {
            return value;
        }
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextBytes(byte[])} and
     * {@link UniformRandomProvider#nextBytes(byte[], int, int)}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextBytes() {
        return new BaselineNextBytes();
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextInt()} and
     * {@link UniformRandomProvider#nextInt(int)}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextInt() {
        return new BaselineNextInt();
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextLong()} and
     * {@link UniformRandomProvider#nextLong(long)}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextLong() {
        return new BaselineNextLong();
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextBoolean()}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextBoolean() {
        return new BaselineNextBoolean();
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextFloat()}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextFloat() {
        return new BaselineNextFloat();
    }

    /**
     * Gets a baseline provider for {@link UniformRandomProvider#nextDouble()}.
     *
     * @return The baseline provider.
     */
    public static UniformRandomProvider getNextDouble() {
        return new BaselineNextDouble();
    }
}
