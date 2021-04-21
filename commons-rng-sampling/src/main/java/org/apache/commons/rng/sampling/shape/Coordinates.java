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

package org.apache.commons.rng.sampling.shape;

/**
 * Utility class for common coordinate operations for shape samplers.
 *
 * @since 1.4
 */
final class Coordinates {

    /** No public construction. */
    private Coordinates() {}

    /**
     * Check that the values are finite. This method is primarily for parameter
     * validation in methods and constructors, for example:
     *
     * <pre>
     * public Line(double[] start, double[] end) {
     *     this.start = Coordinates.requireFinite(start, "start");
     *     this.end = Coordinates.requireFinite(end, "end");
     * }
     * </pre>
     *
     * @param values the values
     * @param message the message detail to prepend to the message in the event an exception is thrown
     * @return the values
     * @throws IllegalArgumentException If a non-finite value is found
     */
    static double[] requireFinite(double[] values, String message) {
        for (final double value : values) {
            if (!isFinite(value)) {
                throw new IllegalArgumentException(message + " contains non-finite value: " + value);
            }
        }
        return values;
    }

    /**
     * Checks if the value is finite.
     * To be replaced by {@code Double.isFinite(double)} when source requires Java 8.
     *
     * @param value the value
     * @return true if finite
     */
    private static boolean isFinite(double value) {
        return Math.abs(value) <= Double.MAX_VALUE;
    }
}
