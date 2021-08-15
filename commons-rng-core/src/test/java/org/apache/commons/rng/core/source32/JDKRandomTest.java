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
package org.apache.commons.rng.core.source32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.core.RandomAssert;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.apache.commons.rng.core.util.NumberFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.Test;

public class JDKRandomTest {
    /**
     * A class that is Serializable.
     * It contains member fields so there is something to serialize and malicious
     * deserialization code.
     */
    static class SerializableTestObject implements Serializable {
        private static final long serialVersionUID = 1L;

        private int state0;
        private double state1;
        private long state2;
        private boolean state3;

        /**
         * This simulates doing something malicious when deserializing.
         *
         * @param input Input stream.
         * @throws IOException if an error occurs.
         * @throws ClassNotFoundException if an error occurs.
         */
        private void readObject(ObjectInputStream input)
                throws IOException,
                       ClassNotFoundException {
            Assertions.fail("*** Malicious code ***. This should not be run during the test");
        }
    }

    @Test
    public void testReferenceCode() {
        final long refSeed = -1357111213L;
        final JDKRandom rng = new JDKRandom(refSeed);
        final Random jdk = new Random(refSeed);

        // This is a trivial test since "JDKRandom" delegates to "Random".

        final int numRepeats = 1000;
        for (int[] r = {0}; r[0] < numRepeats; r[0]++) {
            Assertions.assertEquals(jdk.nextInt(), rng.nextInt(), () -> r[0] + " nextInt");
        }
    }

    /**
     * Test the state can be used to restore a new instance that has not previously had a call
     * to save the state.
     */
    @Test
    public void testRestoreToNewInstance()  {
        final long seed = 8796746234L;
        final JDKRandom rng1 = new JDKRandom(seed);
        final JDKRandom rng2 = new JDKRandom(seed + 1);

        // Ensure different
        final int numRepeats = 10;
        for (int[] r = {0}; r[0] < numRepeats; r[0]++) {
            Assertions.assertNotEquals(rng1.nextInt(), rng2.nextInt(), () -> r[0] + " nextInt");
        }

        final RandomProviderState state = rng1.saveState();
        // This second instance will not know the state size required to write
        // java.util.Random to serialized form. This is only known when the saveState
        // method is called.
        rng2.restoreState(state);

        // Ensure the same
        RandomAssert.assertNextIntEquals(numRepeats, rng1, rng2);
    }

    /**
     * Test the deserialization code identifies bad states that do not contain a Random instance.
     * This test exercises the code that uses a custom deserialization ObjectInputStream.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test(expected = IllegalStateException.class)
    public void testRestoreWithInvalidClass() throws IOException  {
        // Serialize something
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(new SerializableTestObject());
        }

        // Compose the size with the state.
        // This is what is expected by the JDKRandom class.
        final byte[] state = bos.toByteArray();
        final int stateSize = state.length;
        final byte[] sizeAndState = new byte[4 + stateSize];
        System.arraycopy(NumberFactory.makeByteArray(stateSize), 0, sizeAndState, 0, 4);
        System.arraycopy(state, 0, sizeAndState, 4, stateSize);

        final RandomProviderDefaultState dummyState = new RandomProviderDefaultState(sizeAndState);

        final JDKRandom rng = new JDKRandom(13L);
        // This should throw
        rng.restoreState(dummyState);
    }
}
