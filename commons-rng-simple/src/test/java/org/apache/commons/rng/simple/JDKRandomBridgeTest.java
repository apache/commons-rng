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
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link JDKRandomBridge} adaptor class.
 */
public class JDKRandomBridgeTest {
    @Test
    public void testJDKRandomEquivalence() {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng1 = new Random(seed);
        final Random rng2 = new JDKRandomBridge(RandomSource.JDK, seed);
        checkSameSequence(rng1, rng2);

        // Reseed.
        final long newSeed = RandomSource.createLong();
        Assert.assertNotEquals(seed, newSeed);
        rng1.setSeed(newSeed);
        rng2.setSeed(newSeed);
        checkSameSequence(rng1, rng2);
    }

    @Test
    public void testSerialization()
        throws IOException,
               ClassNotFoundException {
        // Initialize.
        final long seed = RandomSource.createLong();
        final Random rng = new JDKRandomBridge(RandomSource.SPLIT_MIX_64, seed);

        // Serialize.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(rng);

        // Retrieve from serialized stream.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        final Random serialRng = (Random) (ois.readObject());

        // Check that the serialized data recreated the orginal state.
        checkSameSequence(rng, serialRng);
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
            Assert.assertEquals(rng1.nextInt(),
                                rng2.nextInt());
        }
        for (int i = 0; i < 7; i++) {
            Assert.assertEquals(rng1.nextLong(),
                                rng2.nextLong());
        }
        for (int i = 0; i < 9; i++) {
            Assert.assertEquals(rng1.nextFloat(),
                                rng2.nextFloat(),
                                0f);
        }
        for (int i = 0; i < 12; i++) {
            Assert.assertEquals(rng1.nextDouble(),
                                rng2.nextDouble(),
                                0d);
        }
        for (int i = 0; i < 17; i++) {
            Assert.assertEquals(rng1.nextGaussian(),
                                rng2.nextGaussian(),
                                0d);
        }
        for (int i = 0; i < 18; i++) {
            Assert.assertEquals(rng1.nextBoolean(),
                                rng2.nextBoolean());
        }
        for (int i = 0; i < 19; i++) {
            final int max = i + 123456;
            Assert.assertEquals(rng1.nextInt(max),
                                rng2.nextInt(max));
        }

        final int len = 233;
        final byte[] store1 = new byte[len];
        final byte[] store2 = new byte[len];
        rng1.nextBytes(store1);
        rng2.nextBytes(store2);
        for (int i = 0; i < len; i++) {
            Assert.assertEquals(store1[i],
                                store2[i]);
        }
    }
}
