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
package org.apache.commons.rng.simple;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.StreamCorruptedException;
import java.util.Random;
import java.util.stream.Stream;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

/**
 * Tests for the {@link JDKRandomBridge} adaptor class.
 */
class JDKRandomBridgeTest {
    @Test
    void testJDKRandomEquivalence() {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng1 = new Random(seed);
        final Random rng2 = new JDKRandomBridge(RandomSource.JDK, seed);
        checkSameSequence(rng1, rng2);

        // Reseed.
        final long newSeed = RandomSource.createLong();
        Assertions.assertNotEquals(seed, newSeed);
        rng1.setSeed(newSeed);
        rng2.setSeed(newSeed);
        checkSameSequence(rng1, rng2);
    }

    /**
     * Test serialization with all sources. This ensures the maximum state size limit
     * is suitable for all implementations in the library.
     *
     * <p>Excludes TWO_CMRES_SELECT which is does not currently save the subcycle generator
     * instance in the state. The save/restore functionality is meant to operate on the same
     * instance of the generator where the subcycle generators are already known.
     */
    @ParameterizedTest
    @EnumSource(value=RandomSource.class, mode=Mode.EXCLUDE, names={"TWO_CMRES_SELECT"})
    void testSerialization(RandomSource source)
        throws IOException,
               ClassNotFoundException {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng = new JDKRandomBridge(source, seed);

        // Serialize.
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(rng);

        // Retrieve from serialized stream.
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final Random serialRng = (Random) ois.readObject();

        // Check that the serialized data recreated the original state.
        checkSameSequence(rng, serialRng);

        // Check that the restored object can be seeded
        rng.setSeed(seed);
        serialRng.setSeed(seed);
        checkSameSequence(rng, serialRng);
    }

    static Stream<RandomSource> testDeserializationWithBadStateSizeThrows() {
      // Note: This test is not valid for generators where the state bytes are large
      // and are written in multiple blocks, e.g. WELL_44497_A.
      return Stream.of(RandomSource.SPLIT_MIX_64,
                       RandomSource.XO_SHI_RO_128_PP,
                       RandomSource.L128_X256_MIX);
    }

    @ParameterizedTest
    @MethodSource
    void testDeserializationWithBadStateSizeThrows(RandomSource source) throws IOException {
        final long seed = 46531265234L;
        final Random rng = new JDKRandomBridge(source, seed);

        // Serialize.
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(rng);
        oos.close();
        final byte[] data = bos.toByteArray();

        // Locate the state size written by the custom writeObject. The custom class
        // data is the last data in the stream:
        //   [... block-data header] [int size] [state bytes] [TC_ENDBLOCKDATA]
        // The expected state size is obtained from an identical generator.
        // TC_ENDBLOCKDATA = 1 byte:
        //   offset = data.length - 1 - size - 4
        // Note: A large state may be written in multiple blocks so the test is not
        // valid for generators with a large state. These will fail the sanity check.
        final byte[] state = ((RandomProviderDefaultState)
            source.create(seed).saveState()).getState();
        final int size = state.length;
        final int offset = data.length - 1 - size - 4;
        Assertions.assertEquals(size, readInt(data, offset),
            "Sanity check failed: unexpected state size location");

        // Tamper the declared size: a huge value with no matching payload must be
        // rejected before any allocation is attempted.
        writeInt(data, offset, Integer.MAX_VALUE);
        assertDeserializationThrows(data);

        // Tamper the declared size: a negative value must be rejected.
        writeInt(data, offset, -1);
        assertDeserializationThrows(data);
    }

    /**
     * Assert deserialization of the data throws a {@link StreamCorruptedException}.
     *
     * @param data Serialized data.
     */
    private static void assertDeserializationThrows(byte[] data) {
        Assertions.assertThrows(StreamCorruptedException.class, () -> {
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                ois.readObject();
            }
        });
    }

    /**
     * Read a big-endian int from the data.
     *
     * @param data Data.
     * @param offset Offset to read from.
     * @return the int
     */
    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) |
               ((data[offset + 1] & 0xff) << 16) |
               ((data[offset + 2] & 0xff) << 8) |
                (data[offset + 3] & 0xff);
    }

    /**
     * Write a big-endian int to the data.
     *
     * @param data Data.
     * @param offset Offset to write to.
     * @param value Value to write.
     */
    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    /**
     * Ensure that both generators produce the same sequences.
     *
     * @param rng1 RNG.
     * @param rng2 RNG.
     */
    private void checkSameSequence(Random rng1,
                                   Random rng2) {
        for (int i = 0; i < 4; i++) {
            Assertions.assertEquals(rng1.nextInt(),
                                    rng2.nextInt());
        }
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(rng1.nextLong(),
                                    rng2.nextLong());
        }
        for (int i = 0; i < 9; i++) {
            Assertions.assertEquals(rng1.nextFloat(),
                                    rng2.nextFloat());
        }
        for (int i = 0; i < 12; i++) {
            Assertions.assertEquals(rng1.nextDouble(),
                                    rng2.nextDouble());
        }
        for (int i = 0; i < 17; i++) {
            Assertions.assertEquals(rng1.nextGaussian(),
                                    rng2.nextGaussian());
        }
        for (int i = 0; i < 18; i++) {
            Assertions.assertEquals(rng1.nextBoolean(),
                                    rng2.nextBoolean());
        }
        for (int i = 0; i < 19; i++) {
            final int max = i + 123456;
            Assertions.assertEquals(rng1.nextInt(max),
                                    rng2.nextInt(max));
        }

        final int len = 233;
        final byte[] store1 = new byte[len];
        final byte[] store2 = new byte[len];
        rng1.nextBytes(store1);
        rng2.nextBytes(store2);
        for (int i = 0; i < len; i++) {
            Assertions.assertEquals(store1[i],
                                    store2[i]);
        }
    }
}
