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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests for {@link ThreadLocalRandomSource}.
 */
class ThreadLocalRandomSourceTest {
    /**
     * A set of all the RandomSource options that requires arguments. This should be
     * ignored in certain tests since they are not supported.
     */
    private static EnumSet<RandomSource> toIgnore;

    @BeforeAll
    static void createToIgnoreSet() {
        toIgnore = EnumSet.of(RandomSource.TWO_CMRES_SELECT);
    }

    @Test
    void testCurrentThrowsForNullRandomSource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ThreadLocalRandomSource.current(null));
    }

    @Test
    void testCurrentThrowsForRandomSourceWithDataArguments() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> ThreadLocalRandomSource.current(RandomSource.TWO_CMRES_SELECT));
    }

    @Test
    void testCurrentForAllRandomSources()
            throws InterruptedException, ExecutionException, TimeoutException {
        final RandomSource[] sources = RandomSource.values();
        final UniformRandomProvider[] rngs = new UniformRandomProvider[sources.length];

        for (int i = 0; i < sources.length; i++) {
            final RandomSource source = sources[i];
            if (toIgnore.contains(source)) {
                continue;
            }
            final UniformRandomProvider rng = getCurrent(source);
            Assertions.assertNotNull(rng, () -> "Failed to create source: " + source);
            rngs[i] = rng;
        }
        for (int i = 0; i < sources.length; i++) {
            final RandomSource source = sources[i];
            if (toIgnore.contains(source)) {
                continue;
            }
            final UniformRandomProvider rng = getCurrent(source);
            Assertions.assertSame(rngs[i], rng, () -> "Failed to return same source: " + source);
        }

        // Build on a new thread
        final UniformRandomProvider[] rngs2 = new UniformRandomProvider[rngs.length];
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(
            new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < sources.length; i++) {
                        if (toIgnore.contains(sources[i])) {
                            continue;
                        }
                        rngs2[i] = getCurrent(sources[i]);
                    }
                }
            });

        // Shutdown and wait for task to end
        executor.shutdown();
        future.get(30, TimeUnit.SECONDS);

        // The RNG from the new thread should be different
        for (int i = 0; i < sources.length; i++) {
            final RandomSource source = sources[i];
            if (toIgnore.contains(source)) {
                continue;
            }
            Assertions.assertNotSame(rngs[i], rngs2[i], () -> "Failed to return different source: " + source);
        }
    }

    private static UniformRandomProvider getCurrent(RandomSource source) {
        try {
            return ThreadLocalRandomSource.current(source);
        } catch (final RuntimeException ex) {
            throw new RuntimeException("Failed to get current: " + source, ex);
        }
    }
}
