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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.LongStream;
import org.apache.commons.rng.JumpableUniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Executes a benchmark for operations used in the LXM family of RNGs.
 *
 * <h2>Note</h2>
 *
 * <p>Some code in this benchmark is commented out. It requires a higher
 * version of Java than the current target. Bumping the JMH module to a higher
 * minimum java version prevents running benchmarks on the target JVM for
 * the Commons RNG artifacts. Thus these benchmarks must be manually reinstated by
 * uncommenting the code and updating the Java version in the module pom.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M" })
public class LXMBenchmark {
    /** Baseline for generation of 1 long value. */
    private static final String BASELINE1 = "baseline1";
    /** Baseline for generation of 2 long values. */
    private static final String BASELINE2 = "baseline2";
    /** Baseline for generation of 4 long values. */
    private static final String BASELINE4 = "baseline4";

    /**
     * Encapsulates a method to compute an unsigned multiply of 64-bit values to create
     * the upper and optionally low 64-bits of the 128-bit result.
     *
     * <p>This tests method used to compute an update of a 128-bit linear congruential
     * generator (LCG).
     */
    @State(Scope.Benchmark)
    public static class UnsignedMultiplyHighSource {
        /** Low half of 128-bit LCG multiplier. The upper half is {@code 1L}. */
        static final long M128L = 0xd605bbb58c8abbfdL;
        /** A mask to convert an {@code int} to an unsigned integer stored as a {@code long}. */
        private static final long INT_TO_UNSIGNED_BYTE_MASK = 0xffffffffL;
        /** Precomputed upper split (32-bits) of the low half of the 128-bit multiplier constant. */
        private static final long X = M128L >>> 32;
        /** Precomputed lower split (32-bits) of the low half of the 128-bit multiplier constant. */
        private static final long Y = M128L & INT_TO_UNSIGNED_BYTE_MASK;

        /**
         * The method to compute the value.
         */
        @Param({BASELINE1,
            // Require JDK 9+
            //"mathMultiplyHigh", "mathMultiplyHighWithML",
            // Require JDK 18+
            //"mathUnsignedMultiplyHigh", "mathUnsignedMultiplyHighWithML",
            "unsignedMultiplyHigh", "unsignedMultiplyHighWithML",
            "unsignedMultiplyHighML",
            "unsignedMultiplyHighPlusMultiplyLow", "unsignedMultiplyHighAndLow",
            })
        private String method;

        /** Flag to indicate numbers should be precomputed.
         * Note: The multiply method is extremely fast and number generation can take
         * significant part of the overall generation time. */
        @Param({"true", "false"})
        private boolean precompute;

        /** The generator of the next value. */
        private LongSupplier gen;

        /**
         * Compute the next value.
         *
         * @return the value
         */
        long next() {
            return gen.getAsLong();
        }

        /**
         * Create the generator of output values.
         */
        @Setup
        public void setup() {
            final JumpableUniformRandomProvider rng =
                (JumpableUniformRandomProvider) RandomSource.XO_RO_SHI_RO_128_PP.create();

            LongSupplier ga;
            LongSupplier gb;

            // Optionally precompute numbers.
            if (precompute) {
                final long[] values = LongStream.generate(rng::nextLong).limit(1024).toArray();
                class A implements LongSupplier {
                    private int i;
                    @Override
                    public long getAsLong() {
                        return values[i++ & 1023];
                    }
                }
                ga = new A();
                class B implements LongSupplier {
                    private int i;
                    @Override
                    public long getAsLong() {
                        // Using any odd increment will sample all values (after wrapping)
                        return values[(i += 3) & 1023];
                    }
                }
                gb = new B();
            } else {
                ga = rng::nextLong;
                gb = rng.jump()::nextLong;
            }

            if (BASELINE1.equals(method)) {
                gen = () -> ga.getAsLong();
            } else if (BASELINE2.equals(method)) {
                gen = () -> ga.getAsLong() ^ gb.getAsLong();
            } else if ("mathMultiplyHigh".equals(method)) {
                gen = () -> mathMultiplyHigh(ga.getAsLong(), gb.getAsLong());
            } else if ("mathMultiplyHighWithML".equals(method)) {
                gen = () -> mathMultiplyHigh(ga.getAsLong(), M128L);
            } else if ("mathUnsignedMultiplyHigh".equals(method)) {
                gen = () -> mathUnsignedMultiplyHigh(ga.getAsLong(), gb.getAsLong());
            } else if ("mathUnsignedMultiplyHighWithML".equals(method)) {
                gen = () -> mathUnsignedMultiplyHigh(ga.getAsLong(), M128L);
            } else if ("unsignedMultiplyHigh".equals(method)) {
                gen = () -> unsignedMultiplyHigh(ga.getAsLong(), gb.getAsLong());
            } else if ("unsignedMultiplyHighWithML".equals(method)) {
                gen = () -> unsignedMultiplyHigh(ga.getAsLong(), M128L);
            } else if ("unsignedMultiplyHighML".equals(method)) {
                // Note:
                // Running this benchmark should show the explicit precomputation
                // of the ML parts does not increase performance. The JVM can
                // optimise the call to unsignedMultiplyHigh(a, b) when b is constant.
                gen = () -> unsignedMultiplyHighML(ga.getAsLong());
            } else if ("unsignedMultiplyHighPlusMultiplyLow".equals(method)) {
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    return unsignedMultiplyHigh(a, b) ^ (a * b);
                };
            } else if ("unsignedMultiplyHighAndLow".equals(method)) {
                // Note:
                // Running this benchmark should show this method is slower.
                // A CPU supporting parallel instructions can multiply (a*b)
                // in parallel to calling unsignedMultiplyHigh.
                final long[] lo = {0};
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long hi = unsignedMultiplyHigh(a, b, lo);
                    return hi ^ lo[0];
                };
            } else {
                throw new IllegalStateException("Unknown method: " + method);
            }
        }

        /**
         * Compute the unsigned multiply of two values using Math.multiplyHigh.
         * <p>Requires JDK 9.
         * From JDK 10 onwards this method is an intrinsic candidate.
         *
         * @param a First value
         * @param b Second value
         * @return the upper 64-bits of the 128-bit result
         */
        static long mathMultiplyHigh(long a, long b) {
            // Requires JDK 9
            // Note: Using runtime reflection to create a method handle
            // has not been done to avoid any possible artifact during timing
            // of this very fast method. To benchmark with this method
            // requires uncommenting this code and compiling with an
            // appropriate source level.
            //return Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a);
            throw new NoSuchMethodError();
        }

        /**
         * Compute the unsigned multiply of two values using Math.unsignedMultiplyHigh.
         * <p>Requires JDK 18.
         * This method is an intrinsic candidate.
         *
         * @param a First value
         * @param b Second value
         * @return the upper 64-bits of the 128-bit result
         */
        static long mathUnsignedMultiplyHigh(long a, long b) {
            // Requires JDK 18
            //return Math.unsignedMultiplyHigh(a, b);
            throw new NoSuchMethodError();
        }

        /**
         * Multiply the two values as if unsigned 64-bit longs to produce the high 64-bits
         * of the 128-bit unsigned result.
         *
         * <p>This method computes the equivalent of:
         * <pre>
         * Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a)
         * </pre>
         *
         * <p>Note: The method {@code Math.multiplyHigh} was added in JDK 9
         * and should be used as above when the source code targets Java 11
         * to exploit the intrinsic method.
         *
         * <p>Note: The method {@code Math.unsignedMultiplyHigh} was added in JDK 18
         * and should be used when the source code target allows.
         *
         * @param value1 the first value
         * @param value2 the second value
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedMultiplyHigh(long value1, long value2) {
            // Computation is based on the following observation about the upper (a and x)
            // and lower (b and y) bits of unsigned big-endian integers:
            //   ab * xy
            // =  b *  y
            // +  b * x0
            // + a0 *  y
            // + a0 * x0
            // = b * y
            // + b * x * 2^32
            // + a * y * 2^32
            // + a * x * 2^64
            //
            // Summation using a character for each byte:
            //
            //             byby byby
            // +      bxbx bxbx 0000
            // +      ayay ayay 0000
            // + axax axax 0000 0000
            //
            // The summation can be rearranged to ensure no overflow given
            // that the result of two unsigned 32-bit integers multiplied together
            // plus two full 32-bit integers cannot overflow 64 bits:
            // > long x = (1L << 32) - 1
            // > x * x + x + x == -1 (all bits set, no overflow)
            //
            // The carry is a composed intermediate which will never overflow:
            //
            //             byby byby
            // +           bxbx 0000
            // +      ayay ayay 0000
            //
            // +      bxbx 0000 0000
            // + axax axax 0000 0000

            final long a = value1 >>> 32;
            final long b = value1 & INT_TO_UNSIGNED_BYTE_MASK;
            final long x = value2 >>> 32;
            final long y = value2 & INT_TO_UNSIGNED_BYTE_MASK;


            final long by = b * y;
            final long bx = b * x;
            final long ay = a * y;
            final long ax = a * x;

            // Cannot overflow
            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                ay;
            // Note:
            // low = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK)

            return (bx >>> 32) + (carry >>> 32) + ax;
        }

        /**
         * Multiply the two values as if unsigned 64-bit longs to produce the high
         * 64-bits of the 128-bit unsigned result.
         *
         * @param value1 the first value
         * @param value2 the second value
         * @param low low 64-bits of the 128-bit result
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedMultiplyHigh(long value1, long value2, long[] low) {
            final long a = value1 >>> 32;
            final long b = value1 & INT_TO_UNSIGNED_BYTE_MASK;
            final long x = value2 >>> 32;
            final long y = value2 & INT_TO_UNSIGNED_BYTE_MASK;


            final long by = b * y;
            final long bx = b * x;
            final long ay = a * y;
            final long ax = a * x;

            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                ay;

            low[0] = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK);

            return (bx >>> 32) + (carry >>> 32) + ax;
        }

        /**
         * Multiply the value as an unsigned 64-bit long by the constant
         * {@code ML = 0xd605bbb58c8abbfdL} to produce the high 64-bits
         * of the 128-bit unsigned result.
         *
         * <p>This is a specialised version of {@link #unsignedMultiplyHigh(long, long)}
         * for use in the 128-bit LCG sub-generator of the LXM family.
         *
         * @param value the value
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedMultiplyHighML(long value) {
            final long a = value >>> 32;
            final long b = value & INT_TO_UNSIGNED_BYTE_MASK;

            final long by = b * Y;
            final long bx = b * X;
            final long ay = a * Y;
            final long ax = a * X;

            // Cannot overflow
            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                ay;

            return (bx >>> 32) + (carry >>> 32) + ax;
        }
    }

    /**
     * Encapsulates a method to compute an unsigned multiply of two 128-bit values to create
     * a truncated 128-bit result. The upper 128-bits of the 256-bit result are discarded.
     * The upper and optionally low 64-bits of the truncated 128-bit result are computed.
     *
     * <p>This tests methods used during computation of jumps of size {@code 2^k} for a
     * 128-bit linear congruential generator (LCG).
     */
    @State(Scope.Benchmark)
    public static class UnsignedMultiply128Source {
        /** A mask to convert an {@code int} to an unsigned integer stored as a {@code long}. */
        private static final long INT_TO_UNSIGNED_BYTE_MASK = 0xffffffffL;

        /**
         * The method to compute the value.
         */
        @Param({BASELINE1,
            "unsignedMultiplyHighPlusProducts",
            "unsignedMultiply128AndLow",
            "unsignedMultiplyHighPlusProducts (square)",
            "unsignedSquareHighPlusProducts",
            "unsignedMultiply128AndLow (square)",
            "unsignedSquare128AndLow",
            })
        private String method;

        /** Flag to indicate numbers should be precomputed.
         * Note: The multiply method is extremely fast and number generation can take
         * significant part of the overall generation time. */
        @Param({"true", "false"})
        private boolean precompute;

        /** The generator of the next value. */
        private LongSupplier gen;

        /**
         * Compute the next value.
         *
         * @return the value
         */
        long next() {
            return gen.getAsLong();
        }

        /**
         * Create the generator of output values.
         */
        @Setup
        public void setup() {
            final JumpableUniformRandomProvider rng =
                (JumpableUniformRandomProvider) RandomSource.XO_RO_SHI_RO_128_PP.create();

            LongSupplier ga;
            LongSupplier gb;
            LongSupplier gc;
            LongSupplier gd;

            // Optionally precompute numbers.
            if (precompute) {
                final long[] values = LongStream.generate(rng::nextLong).limit(1024).toArray();
                class Gen implements LongSupplier {
                    private int i;
                    private final int inc;
                    Gen(int inc) {
                        this.inc = inc;
                    }
                    @Override
                    public long getAsLong() {
                        return values[(i += inc) & 1023];
                    }
                }
                // Using any odd increment will sample all values (after wrapping)
                ga = new Gen(1);
                gb = new Gen(3);
                gc = new Gen(5);
                gd = new Gen(7);
            } else {
                ga = rng::nextLong;
                gb = rng.jump()::nextLong;
                gc = rng.jump()::nextLong;
                gd = rng.jump()::nextLong;
            }

            if (BASELINE2.equals(method)) {
                gen = () -> ga.getAsLong() ^ gb.getAsLong();
            } else if (BASELINE4.equals(method)) {
                gen = () -> ga.getAsLong() ^ gb.getAsLong() ^ gc.getAsLong() ^ gd.getAsLong();
            } else if ("unsignedMultiplyHighPlusProducts".equals(method)) {
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long c = gc.getAsLong();
                    final long d = gd.getAsLong();
                    final long hi = UnsignedMultiplyHighSource.unsignedMultiplyHigh(b, d) +
                                    a * d + b * c;
                    final long lo = b * d;
                    return hi ^ lo;
                };
            } else if ("unsignedMultiply128AndLow".equals(method)) {
                // Note:
                // Running this benchmark should show this method has no
                // benefit over the explicit computation using unsignedMultiplyHigh
                // and may actually be slower.
                final long[] lo = {0};
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long c = gc.getAsLong();
                    final long d = gd.getAsLong();
                    final long hi = unsignedMultiply128(a, b, c, d, lo);
                    return hi ^ lo[0];
                };
            } else if ("unsignedMultiplyHighPlusProducts (square)".equals(method)) {
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long hi = UnsignedMultiplyHighSource.unsignedMultiplyHigh(b, b) +
                                    2 * a * b;
                    final long lo = b * b;
                    return hi ^ lo;
                };
            } else if ("unsignedSquareHighPlusProducts".equals(method)) {
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long hi = unsignedSquareHigh(b) +
                                    2 * a * b;
                    final long lo = b * b;
                    return hi ^ lo;
                };
            } else if ("unsignedMultiply128AndLow (square)".equals(method)) {
                final long[] lo = {0};
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long hi = unsignedMultiply128(a, b, a, b, lo);
                    return hi ^ lo[0];
                };
            } else if ("unsignedSquare128AndLow".equals(method)) {
                // Note:
                // Running this benchmark should show this method has no
                // benefit over the computation using unsignedMultiply128.
                // The dedicated square method saves only 1 shift, 1 mask
                // and 1 multiply operation.
                final long[] lo = {0};
                gen = () -> {
                    final long a = ga.getAsLong();
                    final long b = gb.getAsLong();
                    final long hi = unsignedSquare128(a, b, lo);
                    return hi ^ lo[0];
                };
            } else {
                throw new IllegalStateException("Unknown 128-bit method: " + method);
            }
        }

        /**
         * Multiply the two values as if unsigned 128-bit longs to produce the 128-bit unsigned result.
         * Note the result is a truncation of the full 256-bit result.
         *
         * <p>This is a extended version of {@link UnsignedMultiplyHighSource#unsignedMultiplyHigh(long, long)}
         * for use in the 128-bit LCG sub-generator of the LXM family. It computes the equivalent of:
         * <pre>
         * long hi = unsignedMultiplyHigh(value1l, value2l) +
         *           value1h * value2l + value1l * value2h;
         * long lo = value1l * value2l;
         * </pre>
         *
         * @param value1h the high bits of the first value
         * @param value1l the low bits of the first value
         * @param value2h the high bits of the second value
         * @param value2l the low bits of the second value
         * @param low the low bits of the 128-bit result (output to array index [0])
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedMultiply128(long value1h, long value1l, long value2h, long value2l,
            long[] low) {

            // Multiply the two low halves to compute an initial 128-bit result
            final long a = value1l >>> 32;
            final long b = value1l & INT_TO_UNSIGNED_BYTE_MASK;
            final long x = value2l >>> 32;
            final long y = value2l & INT_TO_UNSIGNED_BYTE_MASK;

            final long by = b * y;
            final long bx = b * x;
            final long ay = a * y;
            final long ax = a * x;

            // Cannot overflow
            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                ay;
            low[0] = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK);
            final long high = (bx >>> 32) + (carry >>> 32) + ax;

            // Incorporate the remaining high bits that do not overflow 128-bits
            return high + value1h * value2l + value1l * value2h;
        }

        /**
         * Square the value as if an unsigned 128-bit long to produce the 128-bit unsigned result.
         *
         * <p>This is a specialisation of {@link UnsignedMultiplyHighSource#unsignedMultiplyHigh(long, long)}
         * for use in the 128-bit LCG sub-generator of the LXM family. It computes the equivalent of:
         * <pre>
         * unsignedMultiplyHigh(value, value);
         * </pre>
         *
         * @param value the high bits of the value
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedSquareHigh(long value) {
            final long a = value >>> 32;
            final long b = value & INT_TO_UNSIGNED_BYTE_MASK;

            final long by = b * b;
            final long bx = b * a;
            final long ax = a * a;

            // Cannot overflow
            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                bx;

            return (bx >>> 32) + (carry >>> 32) + ax;
        }

        /**
         * Square the value as if an unsigned 128-bit long to produce the 128-bit unsigned result.
         *
         * <p>This is a specialisation of {@link #unsignedMultiply128(long, long, long, long, long[])}
         * for use in the 128-bit LCG sub-generator of the LXM family. It computes the equivalent of:
         * <pre>
         * long hi = unsignedMultiplyHigh(valueh, valuel, valueh, valuel) +
         *           2 * valueh * valuel;
         * long lo = valuel * valuel;
         * </pre>
         *
         * @param valueh the high bits of the value
         * @param valuel the low bits of the value
         * @param low the low bits of the 128-bit result (output to array index [0])
         * @return the high 64-bits of the 128-bit result
         */
        static long unsignedSquare128(long valueh, long valuel, long[] low) {

            // Multiply the two low halves to compute an initial 128-bit result
            final long a = valuel >>> 32;
            final long b = valuel & INT_TO_UNSIGNED_BYTE_MASK;
            // x = a, y = b

            final long by = b * b;
            final long bx = b * a;
            // ay = bx
            final long ax = a * a;

            // Cannot overflow
            final long carry = (by >>> 32) +
                               (bx & INT_TO_UNSIGNED_BYTE_MASK) +
                                bx;
            low[0] = (carry << 32) | (by & INT_TO_UNSIGNED_BYTE_MASK);
            final long high = (bx >>> 32) + (carry >>> 32) + ax;

            // Incorporate the remaining high bits that do not overflow 128-bits
            return high + 2 * valueh * valuel;
        }
    }

    /**
     * Encapsulates a method to compute an update step on a 128-bit linear congruential
     * generator (LCG). This benchmark source targets optimisation of the 128-bit LCG update
     * step, in particular the branch to compute carry propagation from the low to high half.
     */
    @State(Scope.Benchmark)
    public static class LCG128Source {
        /** Low half of 128-bit LCG multiplier. The upper half is {@code 1L}. */
        static final long ML = 0xd605bbb58c8abbfdL;

        /**
         * The method to compute the value.
         *
         * <p>Final LCG additive parameters make no significant difference.
         */
        @Param({
            "reference",
            //"referenceFinal",
            "compareUnsigned",
            "conditional",
            //"conditionalFinal",
            })
        private String method;

        /**
         * The low half of the additive parameter.
         * This parameter determines how frequent the carry propagation
         * must occur. The value is unsigned. When close to 0 then carry propagation
         * is less frequent; increasingly large additions will make carry more likely.
         * A value of -1 will always trigger a carry.
         */
        @Param({
            // Uniformly spread in [0, 2^64)
            // 1, [1 .. 4] * 2^62 - 1
            //"1", "4611686018427387903", "9223372036854775807", "-4611686018427387905", "-1",

            // 1, [1 .. 8] * 2^61 - 1
            "1", "2305843009213693951", "4611686018427387903", "6917529027641081855",
            "9223372036854775807", "-6917529027641081857", "-4611686018427387905",
            "-2305843009213693953", "-1",
            })
        private long add;

        /**
         * Range for a random addition to the additive parameter.
         * <ul>
         * <li>{@code range == 0}: no addition
         * <li>{@code range == Long.MIN_VALUE}: random addition
         * <li>{@code range > 0}: random in [0, range)
         * <li>{@code range < 0}: random in [0, -range) + 2^63
         * </ul>
         * If positive then any value in [0, range) is created.
         * If negative then any value in 2^63 + [0, range) is created.
         */
        @Param({"0",
            // 2^61
            "2305843009213693952",
            // MIN_VALUE
            //"-9223372036854775808",
            })
        private long range;

        /** The generator of the next value. */
        private LongSupplier gen;

        /**
         * Compute the next value.
         *
         * @return the value
         */
        long next() {
            return gen.getAsLong();
        }

        /**
         * Create the generator of output values.
         */
        @Setup(Level.Iteration)
        public void setup() {
            final long ah = ThreadLocalRandom.current().nextLong();
            long al = add;
            // Choose to randomise the add parameter
            if (range == Long.MIN_VALUE) {
                // random addition
                al += ThreadLocalRandom.current().nextLong();
            } else if (range > 0) {
                // [0, range)
                al += ThreadLocalRandom.current().nextLong(range);
            } else if (range < 0) {
                // [0, -range) + 2^63
                al += ThreadLocalRandom.current().nextLong(-range) + Long.MIN_VALUE;
            }
            // else range == 0: no addition
            if ("reference".equals(method)) {
                gen = new ReferenceLcg128(ah, al)::getAsLong;
            } else if ("referenceFinal".equals(method)) {
                gen = new ReferenceLcg128Final(ah, al)::getAsLong;
            } else if ("compareUnsigned".equals(method)) {
                gen = new CompareUnsignedLcg128(ah, al)::getAsLong;
            } else if ("conditional".equals(method)) {
                gen = new ConditionalLcg128(ah, al)::getAsLong;
            } else if ("conditionalFinal".equals(method)) {
                gen = new ConditionalLcg128Final(ah, al)::getAsLong;
            } else {
                throw new IllegalStateException("Unknown LCG method: " + method);
            }
        }

        /**
         * Base class for the 128-bit LCG. This holds the LCG state. It is initialised
         * to 1 on construction. The value does not matter. The LCG additive parameter
         * is what determines how frequent the carry propagation branch will be executed.
         */
        abstract static class BaseLcg128 implements LongSupplier {
            /** High half of the 128-bit state of the LCG generator. */
            protected long lsh;
            /** Low half of the 128-bit state of the LCG generator. */
            protected long lsl = 1;
        }

        /**
         * Implements the 128-bit LCG using the reference code in Steele & Vigna (2021).
         *
         * <p>Note: the additive parameter is not final. This is due to the requirement
         * for the LXM generator to implement
         * {@link org.apache.commons.rng.RestorableUniformRandomProvider}.
         */
        static class ReferenceLcg128 extends BaseLcg128 {
            /** High half of the 128-bit per-instance LCG additive parameter. */
            private long lah;
            /** Low half of the 128-bit per-instance LCG additive parameter (must be odd). */
            private long lal;

            /**
             * @param ah High-half of add
             * @param al Low-half of add
             */
            ReferenceLcg128(long ah, long al) {
                lah = ah;
                lal = al | 1;
            }

            @Override
            public long getAsLong() {
                // LCG update
                // The LCG is, in effect, "s = m * s + a" where m = ((1LL << 64) + ML)
                final long sh = lsh;
                final long sl = lsl;
                final long u = ML * sl;
                // High half
                lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah;
                // Low half
                lsl = u + lal;
                // Carry propagation
                if (Long.compareUnsigned(lsl, u) < 0) {
                    ++lsh;
                }
                return sh;
            }
        }

        /**
         * Implements the 128-bit LCG using the reference code in Steele & Vigna (2021).
         * This uses a final additive parameter allowing the JVM to optimise.
         */
        static class ReferenceLcg128Final extends BaseLcg128 {
            /** High half of the 128-bit per-instance LCG additive parameter. */
            private final long lah;
            /** Low half of the 128-bit per-instance LCG additive parameter (must be odd). */
            private final long lal;

            /**
             * @param ah High-half of add
             * @param al Low-half of add
             */
            ReferenceLcg128Final(long ah, long al) {
                lah = ah;
                lal = al | 1;
            }

            @Override
            public long getAsLong() {
                final long sh = lsh;
                final long sl = lsl;
                final long u = ML * sl;
                // High half
                lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah;
                // Low half
                lsl = u + lal;
                // Carry propagation
                if (Long.compareUnsigned(lsl, u) < 0) {
                    ++lsh;
                }
                return sh;
            }
        }

        /**
         * Implements the 128-bit LCG using the reference code in Steele & Vigna (2021)
         * but directly implements the {@link Long#compareUnsigned(long, long)}.
         *
         * <p>Note: the additive parameter is not final. This is due to the requirement
         * for the LXM generator to implement
         * {@link org.apache.commons.rng.RestorableUniformRandomProvider}.
         */
        static class CompareUnsignedLcg128 extends BaseLcg128 {
            /** High half of the 128-bit per-instance LCG additive parameter. */
            private long lah;
            /** Low half of the 128-bit per-instance LCG additive parameter (must be odd). */
            private long lal;

            /**
             * @param ah High-half of add
             * @param al Low-half of add
             */
            CompareUnsignedLcg128(long ah, long al) {
                lah = ah;
                lal = al | 1;
            }

            @Override
            public long getAsLong() {
                final long sh = lsh;
                final long sl = lsl;
                final long u = ML * sl;
                // High half
                lsh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah;
                // Low half
                lsl = u + lal;
                // Carry propagation using an unsigned compare
                if (lsl + Long.MIN_VALUE < u + Long.MIN_VALUE) {
                    ++lsh;
                }
                return sh;
            }
        }

        /**
         * Implements the 128-bit LCG using a conditional load in place of a branch
         * statement for the carry. Uses a non-final additive parameter.
         */
        static class ConditionalLcg128 extends BaseLcg128 {
            /** High half of the 128-bit per-instance LCG additive parameter. */
            private long lah;
            /** Low half of the 128-bit per-instance LCG additive parameter (must be odd). */
            private long lal;

            /**
             * @param ah High-half of add
             * @param al Low-half of add
             */
            ConditionalLcg128(long ah, long al) {
                lah = ah;
                lal = al | 1;
            }

            @Override
            public long getAsLong() {
                long sh = lsh;
                long sl = lsl;
                final long z = sh;
                final long u = ML * sl;
                // High half
                sh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah;
                // Low half
                sl = u + lal;
                // Carry propagation using a conditional add of 1 or 0
                lsh = (sl + Long.MIN_VALUE < u + Long.MIN_VALUE ? 1 : 0) + sh;
                lsl = sl;
                return z;
            }
        }

        /**
         * Implements the 128-bit LCG using a conditional load in place of a branch
         * statement for the carry. Uses a final additive parameter.
         */
        static class ConditionalLcg128Final extends BaseLcg128 {
            /** High half of the 128-bit per-instance LCG additive parameter. */
            private final long lah;
            /** Low half of the 128-bit per-instance LCG additive parameter (must be odd). */
            private final long lal;

            /**
             * @param ah High-half of add
             * @param al Low-half of add
             */
            ConditionalLcg128Final(long ah, long al) {
                lah = ah;
                lal = al | 1;
            }

            @Override
            public long getAsLong() {
                long sh = lsh;
                long sl = lsl;
                final long z = sh;
                final long u = ML * sl;
                // High half
                sh = ML * sh + UnsignedMultiplyHighSource.unsignedMultiplyHigh(ML, sl) + sl + lah;
                // Low half
                sl = u + lal;
                // Carry propagation using a conditional add of 1 or 0
                lsh = (sl + Long.MIN_VALUE < u + Long.MIN_VALUE ? 1 : 0) + sh;
                lsl = sl;
                return z;
            }
        }
    }

    /**
     * Unsigned multiply benchmark.
     *
     * @param data the data
     * @return the value
     */
    @Benchmark
    public long unsignedMultiply(UnsignedMultiplyHighSource data) {
        return data.next();
    }

    /**
     * 128-bit unsigned multiply benchmark.
     *
     * @param data the data
     * @return the value
     */
    @Benchmark
    public long unsignedMultiply128(UnsignedMultiply128Source data) {
        return data.next();
    }

    /**
     * 128-bit linear congruential generator benchmark.
     *
     * @param data the data
     * @return the value
     */
    @Benchmark
    public long lcg128(LCG128Source data) {
        return data.next();
    }
}
