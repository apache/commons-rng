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
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * Tests for {@link RngDataOutput}.
 */
class RngDataOutputTest {
    /**
     * A factory for creating RngDataOutput objects.
     */
    interface RngDataOutputFactory {
        /**
         * Create a new instance.
         *
         * @param out Output stream.
         * @param size Number of values to write.
         * @param byteOrder Byte order.
         * @return the data output
         */
        RngDataOutput create(OutputStream out, int size, ByteOrder byteOrder);
    }

    @Test
    void testIntBigEndian() throws IOException {
        assertRngOutput(RandomSource.PCG_MCG_XSH_RS_32,
            UnaryOperator.identity(),
            RngDataOutputTest::writeInt,
            RngDataOutput::ofInt, ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testIntLittleEndian() throws IOException {
        assertRngOutput(RandomSource.PCG_MCG_XSH_RS_32,
            RNGUtils::createReverseBytesProvider,
            RngDataOutputTest::writeInt,
            RngDataOutput::ofInt, ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    void testLongBigEndian() throws IOException {
        assertRngOutput(RandomSource.SPLIT_MIX_64,
            UnaryOperator.identity(),
            RngDataOutputTest::writeLong,
            RngDataOutput::ofLong, ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testLongLittleEndian() throws IOException {
        assertRngOutput(RandomSource.SPLIT_MIX_64,
            RNGUtils::createReverseBytesProvider,
            RngDataOutputTest::writeLong,
            RngDataOutput::ofLong, ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    void testLongAsIntBigEndian() throws IOException {
        assertRngOutput(RandomSource.SPLIT_MIX_64,
            // Convert SplitMix64 to an int provider so it is detected as requiring double the
            // length output.
            rng -> new IntProvider() {
                @Override
                public int next() {
                    return rng.nextInt();
                }
            },
            RngDataOutputTest::writeInt,
            RngDataOutput::ofLongAsInt, ByteOrder.BIG_ENDIAN);
    }

    @Test
    void testLongAsIntLittleEndian() throws IOException {
        assertRngOutput(RandomSource.SPLIT_MIX_64,
            // Convert SplitMix64 to an int provider so it is detected as requiring double the
            // length output. Then reverse the bytes.
            rng -> new IntProvider() {
                @Override
                public int next() {
                    return Integer.reverseBytes(rng.nextInt());
                }
            },
            RngDataOutputTest::writeInt,
            RngDataOutput::ofLongAsInt, ByteOrder.LITTLE_ENDIAN);
    }

    private static void writeInt(DataOutputStream sink, UniformRandomProvider rng) {
        try {
            sink.writeInt(rng.nextInt());
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    private static void writeLong(DataOutputStream sink, UniformRandomProvider rng) {
        try {
            sink.writeLong(rng.nextLong());
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    /**
     * Assert the byte output from the source is the same. Creates two instances of the same
     * RNG with the same seed. The first is converted to a new RNG using the {@code rngConverter}.
     * This is used to output raw bytes using a {@link DataOutputStream} via the specified
     * {@code pipe}. The second is used to output raw bytes via the {@link RngDataOutput} class
     * created using the {@code factory}.
     *
     * <p>The random source should output native {@code int} or {@code long} values.
     *
     * @param source Random source.
     * @param rngConverter Converter for the raw RNG.
     * @param pipe Pipe to send data from the RNG to the DataOutputStream.
     * @param factory Factory for the RngDataOutput.
     * @param byteOrder Byte order for the RngDataOutput.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void assertRngOutput(RandomSource source,
        UnaryOperator<UniformRandomProvider> rngConverter,
        BiConsumer<DataOutputStream, UniformRandomProvider> pipe,
        RngDataOutputFactory factory,
        ByteOrder byteOrder) throws IOException {
        final long seed = RandomSource.createLong();
        UniformRandomProvider rng1 = source.create(seed);
        UniformRandomProvider rng2 = source.create(seed);
        final int size = 37;
        for (int repeats = 1; repeats <= 2; repeats++) {
            byte[] expected = createBytes(rng1, size, repeats, rngConverter, pipe);
            byte[] actual = writeRngOutput(rng2, size, repeats, byteOrder, factory);
            Assertions.assertArrayEquals(expected, actual);
        }
    }

    /**
     * Convert the RNG and then creates bytes from the RNG using the pipe.
     *
     * @param rng RNG.
     * @param size The number of values to send to the pipe.
     * @param repeats The number of repeat iterations.
     * @param rngConverter Converter for the raw RNG.
     * @param pipe Pipe to send data from the RNG to the DataOutputStream.
     * @return the bytes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static byte[] createBytes(UniformRandomProvider rng, int size, int repeats,
        UnaryOperator<UniformRandomProvider> rngConverter,
        BiConsumer<DataOutputStream, UniformRandomProvider> pipe) throws IOException {
        UniformRandomProvider rng2 = rngConverter.apply(rng);
        // If the factory converts to an IntProvider then output twice the size
        if (rng instanceof RandomLongSource && rng2 instanceof RandomIntSource) {
            size *= 2;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream sink = new DataOutputStream(out)) {
            for (int j = 0; j < repeats; j++) {
                for (int i = 0; i < size; i++) {
                    pipe.accept(sink, rng2);
                }
            }
        }
        return out.toByteArray();
    }

    /**
     * Write the RNG to the RngDataOutput built by the factory.
     *
     * @param rng RNG.
     * @param size The number of values to send to the RngDataOutput.
     * @param repeats The number of repeat iterations.
     * @param byteOrder Byte order for the RngDataOutput.
     * @param factory Factory for the RngDataOutput.
     * @return the bytes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static byte[] writeRngOutput(UniformRandomProvider rng, int size, int repeats,
        ByteOrder byteOrder, RngDataOutputFactory factory) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (RngDataOutput sink = factory.create(out, size, byteOrder)) {
            for (int j = 0; j < repeats; j++) {
                sink.write(rng);
            }
        }
        return out.toByteArray();
    }
}
