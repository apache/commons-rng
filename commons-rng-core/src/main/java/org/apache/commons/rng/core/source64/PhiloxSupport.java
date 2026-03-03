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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.LongBinaryOperator;
import java.util.stream.Stream;

/**
 * Utility support for the Philox family of generators.
 *
 * <p>Contains methods that use the {@code java.lang.invoke} package to call
 * {@code java.lang.Math} functions for computing the high part of the 128-bit
 * result of a multiply of two 64-bit longs. These methods may be supported
 * by intrinsic calls to native operations if supported on the platform for
 * a significant performance gain.
 *
 * <p>Note
 *
 * <p>This class is used specifically in the {@link Philox4x64} generator which
 * has a state update cycle which is performance dependent on the multiply
 * of two unsigned long values. Other classes which use unsigned multiply
 * and are not performance dependent on the method do not use this implementation
 * (for example the LXM family of generators). This allows the multiply method
 * to be adapted to the usage of {@link Philox4x64} which always has the first
 * argument as a negative constant.
 *
 * @since 1.7
 */
final class PhiloxSupport {
    /**
     * Method to compute unsigned multiply high. Uses:
     * <ul>
     * <li>{@code java.lang.Math.unsignedMultiplyHigh} if Java 18
     * <li>{@code java.lang.Math.multiplyHigh} if Java 9
     * <li>otherwise a default implementation.
     * </ul>
     */
    private static final LongBinaryOperator UNSIGNED_MULTIPLY_HIGH;

    static {
        // Note:
        // This uses the public lookup mechanism for static methods to find methods
        // added to java.lang.Math since java 8 to make them available in java 8.
        // For simplicity the lookup is always attempted rather than checking the
        // the java version from System.getProperty("java.version").
        final LongBinaryOperator op1 = getMathUnsignedMultiplyHigh();
        final LongBinaryOperator op2 = getMathMultiplyHigh();
        UNSIGNED_MULTIPLY_HIGH = Stream.of(op1, op2)
            .filter(PhiloxSupport::testUnsignedMultiplyHigh)
            .findFirst()
            .orElse(LXMSupport::unsignedMultiplyHigh);
    }

    /** No instances. */
    private PhiloxSupport() {}

    /**
     * Gets a method to compute the high 64-bits of an unsigned 64-bit multiplication
     * using the Math unsignedMultiplyHigh method from JDK 18.
     *
     * @return the method, or null
     */
    private static LongBinaryOperator getMathUnsignedMultiplyHigh() {
        try {
            // JDK 18 method
            final MethodHandle mh = getMathMethod("unsignedMultiplyHigh");
            return (a, b) -> {
                try {
                    return (long) mh.invokeExact(a, b);
                } catch (Throwable ignored) {
                    throw new IllegalStateException("Cannot invoke Math.unsignedMultiplyHigh");
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    /**
     * Gets a method to compute the high 64-bits of an unsigned 64-bit multiplication
     * using the Math multiplyHigh method from JDK 9.
     *
     * @return the method, or null
     */
    private static LongBinaryOperator getMathMultiplyHigh() {
        try {
            // JDK 9 method
            final MethodHandle mh = getMathMethod("multiplyHigh");
            return (a, b) -> {
                try {
                    // Correct signed result to unsigned.
                    // Assume a is negative, but use sign bit to check b is negative.
                    return (long) mh.invokeExact(a, b) + b + ((b >> 63) & a);
                } catch (Throwable ignored) {
                    throw new IllegalStateException("Cannot invoke Math.multiplyHigh");
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    /**
     * Gets the named method from the {@link Math} class.
     *
     * <p>The look-up assumes the named method accepts two long arguments and returns
     * a long.
     *
     * @param methodName Method name.
     * @return the method
     * @throws NoSuchMethodException if the method does not exist
     * @throws IllegalAccessException if the method cannot be accessed
     */
    static MethodHandle getMathMethod(String methodName) throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.publicLookup()
            .findStatic(Math.class,
                        methodName,
                        MethodType.methodType(long.class, long.class, long.class));
    }

    /**
     * Test the implementation of unsigned multiply high.
     * It is assumed the invocation of the method may raise an {@link IllegalStateException}
     * if it cannot be invoked.
     *
     * @param op Method implementation.
     * @return True if the method can be called to generate the expected result
     */
    static boolean testUnsignedMultiplyHigh(LongBinaryOperator op) {
        try {
            // Test with a signed input to the multiplication.
            // The result is: (1L << 63) * 2 == 1LL << 64
            return op != null && op.applyAsLong(Long.MIN_VALUE, 2L) == 1;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /**
     * Multiply the two values as if unsigned 64-bit longs to produce the high 64-bits
     * of the 128-bit unsigned result. The first argument is assumed to be negative.
     *
     * <p>This method uses a {@link MethodHandle} to call Java functions added since
     * Java 8 to the {@link Math} class:
     * <ul>
     * <li>{@code java.lang.Math.unsignedMultiplyHigh} if Java 18
     * <li>{@code java.lang.Math.multiplyHigh} if Java 9
     * <li>otherwise a default implementation.
     * </ul>
     *
     * <p><strong>Warning</strong>
     *
     * <p>For performance reasons this method assumes the first argument is negative.
     * This allows some operations to be dropped if running on Java 9 to 17.
     *
     * @param value1 the first value (must be negative)
     * @param value2 the second value
     * @return the high 64-bits of the 128-bit result
     */
    static long unsignedMultiplyHigh(long value1, long value2) {
        return UNSIGNED_MULTIPLY_HIGH.applyAsLong(value1, value2);
    }
}
