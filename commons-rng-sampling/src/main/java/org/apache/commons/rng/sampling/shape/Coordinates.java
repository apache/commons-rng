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
     * @throws IllegalArgumentException if a non-finite value is found
     */
    static double[] requireFinite(double[] values, String message) {
        for (final double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(message + " contains non-finite value: " + value);
            }
        }
        return values;
    }

    /**
     * Check that the values is the specified length. This method is primarily for
     * parameter validation in methods and constructors, for example:
     *
     * <pre>
     * public Square(double[] topLeft, double[] bottomRight) {
     *     this.topLeft = Coordinates.requireLength(topLeft, 2, "topLeft");
     *     this.bottomRight = Coordinates.requireLength(bottomRight, 2, "bottomRight");
     * }
     * </pre>
     *
     * @param values the values
     * @param length the length
     * @param message the message detail to prepend to the message in the event an
     * exception is thrown
     * @return the values
     * @throws IllegalArgumentException if the array length is not the specified length
     */
    static double[] requireLength(double[] values, int length, String message) {
        if (values.length != length) {
            throw new IllegalArgumentException(String.format("%s length mismatch: %d != %d",
                    message, values.length, length));
        }
        return values;
    }
}
