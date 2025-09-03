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
package org.apache.commons.rng.examples.stress;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.core.source32.IntProvider;
import org.apache.commons.rng.core.source32.RandomIntSource;
import org.apache.commons.rng.core.source64.RandomLongSource;
import org.apache.commons.rng.core.util.NumberFactory;
import org.apache.commons.rng.core.source64.LongProvider;

import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility methods for a {@link UniformRandomProvider}.
 */
final class RNGUtils {

    /** Name prefix for bit-reversed RNGs. */
    private static final String BYTE_REVERSED = "Byte-reversed ";

    /** Name prefix for bit-reversed RNGs. */
    private static final String BIT_REVERSED = "Bit-reversed ";

    /** Name prefix for hash code mixed RNGs. */
    private static final String HASH_CODE = "HashCode ^ ";

    /** Name prefix for ThreadLocalRandom xor mixed RNGs. */
    private static final String TLR_MIXED = "ThreadLocalRandom ^ ";

    /** Name of xor operator for xor mixed RNGs. */
    private static final String XOR = " ^ ";

    /** Message for an unrecognized source64 mode. */
    private static final String UNRECOGNISED_SOURCE_64_MODE = "Unrecognized source64 mode: ";

    /** Message for an unrecognized native output type. */
    private static final String UNRECOGNISED_NATIVE_TYPE = "Unrecognized native output type: ";

    /** The source64 mode for the default LongProvider caching implementation. */
    private static final Source64Mode SOURCE_64_DEFAULT = Source64Mode.LO_HI;

    /** No public construction. */
    private RNGUtils() {}

    /**
     * Gets the source64 mode for the default caching implementation in {@link LongProvider}.
     *
     * @return the source64 default mode
     */
    static Source64Mode getSource64Default() {
        return SOURCE_64_DEFAULT;
    }

    /**
     * Wrap the random generator with a new instance that will reverse the byte order of
     * the native type. The input must be either a {@link RandomIntSource} or
     * {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the byte reversed random generator.
     * @throws ApplicationException If the input source native type is not recognized.
     * @see Integer#reverseBytes(int)
     * @see Long#reverseBytes(long)
     */
    static UniformRandomProvider createReverseBytesProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomIntSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return Integer.reverseBytes(rng.nextInt());
                }

                @Override
                public String toString() {
                    return BYTE_REVERSED + rng.toString();
                }
            };
        }
        if (rng instanceof RandomLongSource) {
            return new LongProvider() {
                @Override
                public long next() {
                    return Long.reverseBytes(rng.nextLong());
                }

                @Override
                public String toString() {
                    return BYTE_REVERSED + rng.toString();
                }
            };
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng);
    }

    /**
     * Wrap the random generator with a new instance that will reverse the bits of
     * the native type. The input must be either a {@link RandomIntSource} or
     * {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the bit reversed random generator.
     * @throws ApplicationException If the input source native type is not recognized.
     * @see Integer#reverse(int)
     * @see Long#reverse(long)
     */
    static UniformRandomProvider createReverseBitsProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomIntSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return Integer.reverse(rng.nextInt());
                }

                @Override
                public String toString() {
                    return BIT_REVERSED + rng.toString();
                }
            };
        }
        if (rng instanceof RandomLongSource) {
            return new LongProvider() {
                @Override
                public long next() {
                    return Long.reverse(rng.nextLong());
                }

                @Override
                public String toString() {
                    return BIT_REVERSED + rng.toString();
                }
            };
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng);
    }

    /**
     * Wrap the {@link RandomLongSource} with an {@link IntProvider} that will use the
     * specified part of {@link RandomLongSource#next()} to create the int value.
     *
     * @param <R> The type of the generator.
     * @param rng The random generator.
     * @param mode the mode
     * @return the int random generator.
     * @throws ApplicationException If the input source native type is not 64-bit.
     */
    static <R extends RandomLongSource & UniformRandomProvider>
            UniformRandomProvider createIntProvider(final R rng, Source64Mode mode) {
        switch (mode) {
        case INT:
            return createIntProvider(rng);
        case LO_HI:
            return createLongLowerUpperBitsIntProvider(rng);
        case HI_LO:
            return createLongUpperLowerBitsIntProvider(rng);
        case HI:
            return createLongUpperBitsIntProvider(rng);
        case LO:
            return createLongLowerBitsIntProvider(rng);
        case LONG:
        default:
            throw new IllegalArgumentException("Unsupported mode " + mode);
        }
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use
     * {@link UniformRandomProvider#nextInt()}.
     * An input {@link RandomIntSource} is returned unmodified.
     *
     * @param rng The random generator.
     * @return the int random generator.
     */
    private static UniformRandomProvider createIntProvider(final UniformRandomProvider rng) {
        if (!(rng instanceof RandomIntSource)) {
            return new IntProvider() {
                @Override
                public int next() {
                    return rng.nextInt();
                }

                @Override
                public String toString() {
                    return "Int bits " + rng.toString();
                }
            };
        }
        return rng;
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use the lower then upper
     * 32-bits from {@link UniformRandomProvider#nextLong()}.
     * An input {@link RandomIntSource} is returned unmodified.
     *
     * @param rng The random generator.
     * @return the int random generator.
     */
    private static UniformRandomProvider createLongLowerUpperBitsIntProvider(final RandomLongSource rng) {
        return new IntProvider() {
            private long source = -1;

            @Override
            public int next() {
                long next = source;
                if (next < 0) {
                    // refill
                    next = rng.next();
                    // store hi
                    source = next >>> 32;
                    // extract low
                    return (int) next;
                }
                final int v = (int) next;
                // reset
                source = -1;
                return v;
            }

            @Override
            public String toString() {
                return "Long lower-upper bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use the lower then upper
     * 32-bits from {@link UniformRandomProvider#nextLong()}.
     * An input {@link RandomIntSource} is returned unmodified.
     *
     * @param rng The random generator.
     * @return the int random generator.
     */
    private static UniformRandomProvider createLongUpperLowerBitsIntProvider(final RandomLongSource rng) {
        return new IntProvider() {
            private long source = -1;

            @Override
            public int next() {
                long next = source;
                if (next < 0) {
                    // refill
                    next = rng.next();
                    // store low
                    source = next & 0xffff_ffffL;
                    // extract hi
                    return (int) (next >>> 32);
                }
                final int v = (int) next;
                // reset
                source = -1;
                return v;
            }

            @Override
            public String toString() {
                return "Long upper-lower bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use the upper
     * 32-bits of the {@code long} from {@link UniformRandomProvider#nextLong()}.
     * The input must be a {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the upper bits random generator.
     * @throws ApplicationException If the input source native type is not 64-bit.
     */
    private static UniformRandomProvider createLongUpperBitsIntProvider(final RandomLongSource rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return (int) (rng.next() >>> 32);
            }

            @Override
            public String toString() {
                return "Long upper-bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with an {@link IntProvider} that will use the lower
     * 32-bits of the {@code long} from {@link UniformRandomProvider#nextLong()}.
     * The input must be a {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the lower bits random generator.
     * @throws ApplicationException If the input source native type is not 64-bit.
     */
    private static UniformRandomProvider createLongLowerBitsIntProvider(final RandomLongSource rng) {
        return new IntProvider() {
            @Override
            public int next() {
                return (int) rng.next();
            }

            @Override
            public String toString() {
                return "Long lower-bits " + rng.toString();
            }
        };
    }

    /**
     * Wrap the random generator with a new instance that will combine the bits
     * using a {@code xor} operation with a generated hash code. The input must be either
     * a {@link RandomIntSource} or {@link RandomLongSource}.
     *
     * <pre>
     * {@code
     * System.identityHashCode(new Object()) ^ rng.nextInt()
     * }
     * </pre>
     *
     * Note: This generator will be slow.
     *
     * @param rng The random generator.
     * @return the combined random generator.
     * @throws ApplicationException If the input source native type is not recognized.
     * @see System#identityHashCode(Object)
     */
    static UniformRandomProvider createHashCodeProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomIntSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return System.identityHashCode(new Object()) ^ rng.nextInt();
                }

                @Override
                public String toString() {
                    return HASH_CODE + rng.toString();
                }
            };
        }
        if (rng instanceof RandomLongSource) {
            return new LongProvider() {
                @Override
                public long next() {
                    final long mix = NumberFactory.makeLong(System.identityHashCode(new Object()),
                                                            System.identityHashCode(new Object()));
                    return mix ^ rng.nextLong();
                }

                @Override
                public String toString() {
                    return HASH_CODE + rng.toString();
                }
            };
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng);
    }

    /**
     * Wrap the random generator with a new instance that will combine the bits
     * using a {@code xor} operation with the output from {@link ThreadLocalRandom}.
     * The input must be either a {@link RandomIntSource} or {@link RandomLongSource}.
     *
     * <pre>
     * {@code
     * ThreadLocalRandom.current().nextInt() ^ rng.nextInt()
     * }
     * </pre>
     *
     * @param rng The random generator.
     * @return the combined random generator.
     * @throws ApplicationException If the input source native type is not recognized.
     */
    static UniformRandomProvider createThreadLocalRandomProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomIntSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return ThreadLocalRandom.current().nextInt() ^ rng.nextInt();
                }

                @Override
                public String toString() {
                    return TLR_MIXED + rng.toString();
                }
            };
        }
        if (rng instanceof RandomLongSource) {
            return new LongProvider() {
                @Override
                public long next() {
                    return ThreadLocalRandom.current().nextLong() ^ rng.nextLong();
                }

                @Override
                public String toString() {
                    return TLR_MIXED + rng.toString();
                }
            };
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng);
    }

    /**
     * Combine the two random generators using a {@code xor} operations.
     * The input must be either a {@link RandomIntSource} or {@link RandomLongSource}.
     * The returned type will match the native output type of {@code rng1}.
     *
     * <pre>
     * {@code
     * rng1.nextInt() ^ rng2.nextInt()
     * }
     * </pre>
     *
     * @param rng1 The first random generator.
     * @param rng2 The second random generator.
     * @return the combined random generator.
     * @throws ApplicationException If the input source native type is not recognized.
     */
    static UniformRandomProvider createXorProvider(final UniformRandomProvider rng1,
        final UniformRandomProvider rng2) {
        if (rng1 instanceof RandomIntSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return rng1.nextInt() ^ rng2.nextInt();
                }

                @Override
                public String toString() {
                    return rng1.toString() + XOR + rng2.toString();
                }
            };
        }
        if (rng1 instanceof RandomLongSource) {
            return new LongProvider() {
                @Override
                public long next() {
                    return rng1.nextLong() ^ rng2.nextLong();
                }

                @Override
                public String toString() {
                    return rng1.toString() + XOR + rng2.toString();
                }
            };
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng1);
    }

    /**
     * Create a new instance to write batches of byte data from the specified RNG to the
     * specified output stream.
     *
     * <p>This will detect the native output type of the RNG and create an appropriate
     * data output for the raw bytes. The input must be either a {@link RandomIntSource} or
     * {@link RandomLongSource}.</p>
     *
     * <p>If the RNG is a {@link RandomLongSource} then the byte output can be 32-bit or 64-bit.
     * If 32-bit then the 64-bit output will be written as if 2 {@code int} values were generated
     * sequentially from the {@code long} (order depends on the source64 mode). This setting is
     * significant depending on the byte order. For example for a high-low source64 mode and
     * using the Java standard big-endian representation the output is the same as the raw 64-bit
     * output. If using little-endian the output bytes will be written as:</p>
     *
     * <pre>{@code
     * 76543210  ->  4567  0123
     * }</pre>
     *
     * <h2>Note</h2>
     *
     * <p>The output from an implementation of RandomLongSource from the RNG core package
     * may output the long bits as integers: in high-low order; in low-high order; using
     * only the low bits; using only the high bits; or other combinations. This method
     * allows testing the long output as if it were two int outputs, i.e. using the full
     * bit output of a long provider with a stress test application that targets 32-bit
     * random values (e.g. Test U01).
     *
     * <p>The results of stress testing can be used to determine if the provider
     * implementation can use the upper, lower or both parts of the long output for int
     * generation. In the case of the combined upper-lower output it is not expected that
     * the order low-high or high-low is important given the stress test will consume
     * thousands of numbers per test. The default 32-bit mode for a 64-bit source is high-low
     * for backwards compatibility.
     *
     * @param rng The random generator.
     * @param source64 The output mode for a 64-bit source
     * @param out Output stream.
     * @param byteSize Number of bytes values to write.
     * @param byteOrder Byte order.
     * @return the data output
     * @throws ApplicationException If the input source native type is not recognized; or if
     * the mode for a RandomLongSource is not one of: raw; hi-lo; or lo-hi.
     */
    static RngDataOutput createDataOutput(final UniformRandomProvider rng, Source64Mode source64,
        OutputStream out, int byteSize, ByteOrder byteOrder) {
        if (rng instanceof RandomIntSource) {
            return RngDataOutput.ofInt(out, byteSize / 4, byteOrder);
        }
        if (rng instanceof RandomLongSource) {
            switch (source64) {
            case HI_LO:
                return RngDataOutput.ofLongAsHLInt(out, byteSize / 8, byteOrder);
            case LO_HI:
                return RngDataOutput.ofLongAsLHInt(out, byteSize / 8, byteOrder);
            case LONG:
                return RngDataOutput.ofLong(out, byteSize / 8, byteOrder);
            // Note other options should have already been converted to an IntProvider
            case INT:
            case LO:
            case HI:
            default:
                throw new ApplicationException(UNRECOGNISED_SOURCE_64_MODE + source64);
            }
        }
        throw new ApplicationException(UNRECOGNISED_NATIVE_TYPE + rng);
    }

    /**
     * Parses the argument into an object suitable for the RandomSource constructor. Supports:
     *
     * <ul>
     *   <li>Integer
     * </ul>
     *
     * @param argument the argument
     * @return the object
     * @throws ApplicationException If the argument is not recognized
     */
    static Object parseArgument(String argument) {
        try {
            // Currently just support TWO_CMRES_SELECT which uses integers.
            // Future RandomSource implementations may require other parsing, for example
            // recognising a long by the suffix 'L'. This functionality
            // could use Commons Lang NumberUtils.createNumber(String).
            return Integer.parseInt(argument);
        } catch (final NumberFormatException ex) {
            throw new ApplicationException("Failed to parse RandomSource argument: " + argument, ex);
        }
    }
}
