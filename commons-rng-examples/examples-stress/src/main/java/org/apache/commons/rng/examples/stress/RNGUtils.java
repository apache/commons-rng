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
    /** Name prefix for byte-reversed RNGs. */
    private static final String BIT_REVERSED = "Bit-reversed ";
    /** Name prefix for hashcode mixed RNGs. */
    private static final String HASH_CODE = "HashCode ^ ";
    /** Name prefix for ThreadLocalRandom xor mixed RNGs. */
    private static final String TLR_MIXED = "ThreadLocalRandom ^ ";
    /** Name of xor operator for xor mixed RNGs. */
    private static final String XOR = " ^ ";
    /** Message for an unrecognised native output type. */
    private static final String UNRECOGNISED_NATIVE_TYPE = "Unrecognised native output type: ";
    /** Message when not a RandomLongSource. */
    private static final String NOT_LONG_SOURCE = "Not a 64-bit long generator: ";

    /** No public construction. */
    private RNGUtils() {}

    /**
     * Wrap the random generator with a new instance that will reverse the byte order of
     * the native type. The input must be either a {@link RandomIntSource} or
     * {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the byte reversed random generator.
     * @throws ApplicationException If the input source native type is not recognised.
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
     * @throws ApplicationException If the input source native type is not recognised.
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
     * Wrap the random generator with an {@link IntProvider} that will use
     * {@link UniformRandomProvider#nextInt()}.
     * An input {@link RandomIntSource} is returned unmodified.
     *
     * @param rng The random generator.
     * @return the int random generator.
     */
    static UniformRandomProvider createIntProvider(final UniformRandomProvider rng) {
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
     * Wrap the random generator with an {@link IntProvider} that will use the upper
     * 32-bits of the {@code long} from {@link UniformRandomProvider#nextLong()}.
     * The input must be a {@link RandomLongSource}.
     *
     * @param rng The random generator.
     * @return the upper bits random generator.
     * @throws ApplicationException If the input source native type is not 64-bit.
     */
    static UniformRandomProvider createLongUpperBitsIntProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomLongSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return (int) (rng.nextLong() >>> 32);
                }

                @Override
                public String toString() {
                    return "Long upper-bits " + rng.toString();
                }
            };
        }
        throw new ApplicationException(NOT_LONG_SOURCE + rng);
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
    static UniformRandomProvider createLongLowerBitsIntProvider(final UniformRandomProvider rng) {
        if (rng instanceof RandomLongSource) {
            return new IntProvider() {
                @Override
                public int next() {
                    return (int) rng.nextLong();
                }

                @Override
                public String toString() {
                    return "Long lower-bits " + rng.toString();
                }
            };
        }
        throw new ApplicationException(NOT_LONG_SOURCE + rng);
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
     * @throws ApplicationException If the input source native type is not recognised.
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
     * @throws ApplicationException If the input source native type is not recognised.
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
     * @throws ApplicationException If the input source native type is not recognised.
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
     * sequentially from the upper then lower 32-bits of the {@code long}. This setting is
     * significant depending on the byte order. If using the Java standard big-endian
     * representation the flag has no effect and the output will be the same. If using little
     * endian the output bytes will be written as:</p>
     *
     * <pre>
     * 76543210  ->  4567  0123
     * </pre>
     *
     * @param rng The random generator.
     * @param raw64 Set to true for 64-bit byte output.
     * @param out Output stream.
     * @param byteSize Number of bytes values to write.
     * @param byteOrder Byte order.
     * @return the data output
     * @throws ApplicationException If the input source native type is not recognised.
     */
    static RngDataOutput createDataOutput(final UniformRandomProvider rng, boolean raw64,
        OutputStream out, int byteSize, ByteOrder byteOrder) {
        if (rng instanceof RandomIntSource) {
            return RngDataOutput.ofInt(out, byteSize / 4, byteOrder);
        }
        if (rng instanceof RandomLongSource) {
            return raw64 ?
                RngDataOutput.ofLong(out, byteSize / 8, byteOrder) :
                RngDataOutput.ofLongAsInt(out, byteSize / 8, byteOrder);
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
     * @throws ApplicationException If the argument is not recognised
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
