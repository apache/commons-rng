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

import java.util.Random;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.rng.core.util.NumberFactory;

import java.io.ByteArrayInputStream;

/**
 * A provider that uses the {@link Random#nextInt()} method of the JDK's
 * {@link Random} class as the source of randomness.
 *
 * <p>
 * <b>Caveat:</b> All the other calls will be redirected to the methods
 * implemented within this library.
 * </p>
 *
 * <p>
 * The state of this source of randomness is saved and restored through
 * the serialization of the {@link Random} instance.
 * </p>
 *
 * @since 1.0
 */
public class JDKRandom extends IntProvider {
    /** Delegate.  Cannot be "final" (to allow serialization). */
    private Random delegate;

    /**
     * An <code>ObjectInputStream</code> that's restricted to deserialize
     * only {@link java.util.Random} using look-ahead deserialization.
     *
     * <p>Adapted from o.a.c.io.serialization.ValidatingObjectInputStream.</p>
     *
     * @see <a href="http://www.ibm.com/developerworks/library/se-lookahead/">
     *  IBM DeveloperWorks Article: Look-ahead Java deserialization</a>
     */
    private static class ValidatingObjectInputStream extends ObjectInputStream {
        /**
         * @param in Input stream
         * @throws IOException Signals that an I/O exception has occurred.
         */
        ValidatingObjectInputStream(final InputStream in) throws IOException {
            super(in);
        }

        /** {@inheritDoc} */
        @Override
        protected Class<?> resolveClass(final ObjectStreamClass osc) throws IOException,
            ClassNotFoundException {
            // For legacy reasons the Random class is serialized using only primitives
            // even though modern implementations use AtomicLong.
            // The only expected class is java.util.Random.
            if (!Random.class.getName().equals(osc.getName())) {
                throw new IllegalStateException("Stream does not contain java.util.Random: " + osc.getName());
            }
            return super.resolveClass(osc);
        }
    }

    /**
     * Creates an instance with the given seed.
     *
     * @param seed Initial seed.
     */
    public JDKRandom(final Long seed) {
        delegate = new Random(seed);
    }

    /**
     * {@inheritDoc}
     *
     * @see Random#nextInt()
     */
    @Override
    public int next() {
        return delegate.nextInt();
    }

    /** {@inheritDoc} */
    @Override
    protected byte[] getStateInternal() {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);

            // Serialize the "delegate".
            oos.writeObject(delegate);

            final byte[] state = bos.toByteArray();
            final int stateSize = state.length; // To allow state recovery.
            // Compose the size with the state
            final byte[] sizeAndState = composeStateInternal(NumberFactory.makeByteArray(stateSize),
                                                             state);
            return composeStateInternal(sizeAndState,
                                        super.getStateInternal());
        } catch (final IOException e) {
            // Workaround checked exception.
            throw new IllegalStateException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void setStateInternal(final byte[] s) {
        // First obtain the state size
        final byte[][] s2 = splitStateInternal(s, 4);
        final int stateSize = NumberFactory.makeInt(s2[0]);

        // Second obtain the state
        final byte[][] c = splitStateInternal(s2[1], stateSize);

        // Use look-ahead deserialization to validate the state byte[] contains java.util.Random.
        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream(c[0]);
            final ObjectInputStream ois = new ValidatingObjectInputStream(bis);

            delegate = (Random) ois.readObject();
        } catch (final ClassNotFoundException e) {
            // Workaround checked exception.
            throw new IllegalStateException(e);
        } catch (final IOException e) {
            // Workaround checked exception.
            throw new IllegalStateException(e);
        }

        super.setStateInternal(c[1]);
    }
}
