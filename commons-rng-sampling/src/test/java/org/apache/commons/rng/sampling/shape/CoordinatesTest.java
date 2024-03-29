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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Coordinates} utility class.
 */
class CoordinatesTest {
    /**
     * Test {@link Coordinates#requireFinite(double[], String)} detects infinite and NaN.
     */
    @Test
    void testRequireFiniteWithMessageThrows() {
        final double[] c = {0, 1, 2};
        final String message = "This should be prepended";
        Assertions.assertSame(c, Coordinates.requireFinite(c, message));
        final double[] bad = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (int i = 0; i < c.length; i++) {
            final int ii = i;
            for (final double d : bad) {
                final double value = c[i];
                c[i] = d;
                final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Coordinates.requireFinite(c, message),
                    () -> String.format("Did not detect non-finite coordinate: %d = %s", ii, d));
                Assertions.assertTrue(ex.getMessage().startsWith(message), "Missing message prefix");
                c[i] = value;
            }
        }
    }

    /**
     * Test {@link Coordinates#requireLength(double[], int, String)} detects invalid lengths.
     */
    @Test
    void testRequireLengthWithMessageThrows() {
        final String message = "This should be prepended";
        for (final double[] c : new double[][] {{0, 1}, {0, 1, 2}}) {
            final int length = c.length;
            Assertions.assertSame(c, Coordinates.requireLength(c, length, message));
            IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Coordinates.requireLength(c, length - 1, message),
                () -> "Did not detect length was too long: " + (length - 1));
            Assertions.assertTrue(ex.getMessage().startsWith(message), "Missing message prefix");
            ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Coordinates.requireLength(c, length + 1, message),
                () -> "Did not detect length was too short: " + (length + 1));
            Assertions.assertTrue(ex.getMessage().startsWith(message), "Missing message prefix");
        }
    }
}
