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

import org.apache.commons.rng.UniformRandomProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Tests for {@link ThreadLocalRandomSource}.
 */
public class ThreadLocalRandomSourceTest {
    /**
     * A set of all the RandomSource options that requires arguments. This should be
     * ignored in certain tests since they are not supported.
     */
    private static EnumSet<RandomSource> toIgnore;

    @BeforeClass
    public static void createToIgnoreSet() {
        toIgnore = EnumSet.of(RandomSource.TWO_CMRES_SELECT);
    }

    private static Object[] getData(Object ... data) {
        return Arrays.copyOf(data, data.length);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testCurrentThrowsForNullRandomSource() {
        ThreadLocalRandomSource.current(null);
    }

    //@Test(expected=IllegalArgumentException.class)
    public void testCurrentThrowsForRandomSourceWithDataArguments() {
        ThreadLocalRandomSource.current(RandomSource.TWO_CMRES_SELECT);
    }

    @Test
    public void testCurrentForAllRandomSources() {
        final RandomSource[] sources = RandomSource.values();
        final UniformRandomProvider[] rngs = new UniformRandomProvider[sources.length];

        for (int i = 0; i < sources.length; i++) {
            if (toIgnore.contains(sources[i])) {
                continue;
            }
            final UniformRandomProvider rng = getCurrent(sources[i]);
            Assert.assertNotNull("Failed to create source: " + sources[i], rng);
            rngs[i] = rng;
        }
        for (int i = 0; i < sources.length; i++) {
            if (toIgnore.contains(sources[i])) {
                continue;
            }
            final UniformRandomProvider rng = getCurrent(sources[i]);
            Assert.assertSame("Failed to return same source: " + sources[i], rngs[i], rng);
        }

        // Build on a new thread. It should be different
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sources.length; i++) {
                    if (toIgnore.contains(sources[i])) {
                        continue;
                    }
                    final UniformRandomProvider rng = getCurrent(sources[i]);
                    Assert.assertNotSame("Failed to return different source: " + sources[i], rngs[i], rng);
                }
            }
        }).start();
    }

    private static UniformRandomProvider getCurrent(RandomSource source) {
        try {
            return ThreadLocalRandomSource.current(source);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to get current: " + source, ex);
        }
    }
}
