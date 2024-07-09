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

package org.apache.commons.rng.sampling;

import org.junit.jupiter.api.Assertions;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.LongSampler;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Utility class for testing random samplers.
 */
public final class RandomAssert {
    /** Number of samples to generate to test for equal sequences. */
    private static final int SAMPLES = 10;
    /** Default seed for the default generator. */
    private static final Long DEFAULT_SEED;

    static {
        DEFAULT_SEED = Long.valueOf(RandomSource.createLong());
        // Record this to allow debugging of tests that failed with the seeded generator
        Logger.getLogger(RandomAssert.class.getName()).log(Level.INFO,
            () -> "Default seed: " + DEFAULT_SEED);
    }

    /** The sources for a new random generator instance. */
    private static final RandomSource[] SOURCES;

    static {
        final EnumSet<RandomSource> set = EnumSet.allOf(RandomSource.class);
        // Remove all generators that do not pass Test U01 BigCrush or
        // fail PractRand before 4 TiB of output.
        // See: https://commons.apache.org/proper/commons-rng/userguide/rng.html#a5._Quality
        set.remove(RandomSource.JDK);
        set.remove(RandomSource.WELL_512_A);
        set.remove(RandomSource.WELL_1024_A);
        set.remove(RandomSource.WELL_19937_A);
        set.remove(RandomSource.WELL_19937_C);
        set.remove(RandomSource.WELL_44497_A);
        set.remove(RandomSource.WELL_44497_B);
        set.remove(RandomSource.MT);
        set.remove(RandomSource.XOR_SHIFT_1024_S);
        set.remove(RandomSource.TWO_CMRES);
        set.remove(RandomSource.TWO_CMRES_SELECT);
        set.remove(RandomSource.MT_64);
        set.remove(RandomSource.XOR_SHIFT_1024_S_PHI);
        set.remove(RandomSource.XO_RO_SHI_RO_64_S);
        set.remove(RandomSource.XO_SHI_RO_128_PLUS);
        set.remove(RandomSource.XO_RO_SHI_RO_128_PLUS);
        set.remove(RandomSource.XO_SHI_RO_256_PLUS);
        set.remove(RandomSource.XO_SHI_RO_512_PLUS);
        set.remove(RandomSource.PCG_MCG_XSH_RS_32);
        set.remove(RandomSource.XO_RO_SHI_RO_1024_S);
        SOURCES = set.toArray(new RandomSource[0]);
    }

    /**
     * Class contains only static methods.
     */
    private RandomAssert() {}

    /**
     * Exercise the {@link ContinuousSampler} interface, and
     * ensure that the two samplers produce the same sequence.
     *
     * @param sampler1 First sampler.
     * @param sampler2 Second sampler.
     */
    public static void assertProduceSameSequence(ContinuousSampler sampler1,
                                                 ContinuousSampler sampler2) {
        for (int i = 0; i < SAMPLES; i++) {
            Assertions.assertEquals(sampler1.sample(), sampler2.sample());
        }
    }

    /**
     * Exercise the {@link DiscreteSampler} interface, and
     * ensure that the two samplers produce the same sequence.
     *
     * @param sampler1 First sampler.
     * @param sampler2 Second sampler.
     */
    public static void assertProduceSameSequence(DiscreteSampler sampler1,
                                                 DiscreteSampler sampler2) {
        for (int i = 0; i < SAMPLES; i++) {
            Assertions.assertEquals(sampler1.sample(), sampler2.sample());
        }
    }

    /**
     * Exercise the {@link LongSampler} interface, and
     * ensure that the two samplers produce the same sequence.
     *
     * @param sampler1 First sampler.
     * @param sampler2 Second sampler.
     */
    public static void assertProduceSameSequence(LongSampler sampler1,
                                                 LongSampler sampler2) {
        for (int i = 0; i < SAMPLES; i++) {
            Assertions.assertEquals(sampler1.sample(), sampler2.sample());
        }
    }

    /**
     * Exercise the {@link ObjectSampler} interface, and
     * ensure that the two samplers produce the same sequence.
     *
     * <p>Arrays are tested using {@link Assertions#assertArrayEquals(Object[], Object[])}
     * which handles primitive arrays using exact equality and objects using
     * {@link Object#equals(Object)}. Otherwise {@link Assertions#assertEquals(Object, Object)}
     * is used which makes use of {@link Object#equals(Object)}.</p>
     *
     * <p>This should be used to test samplers of any type by wrapping the sample method
     * to an anonymous {@link ObjectSampler} class.</p>
     *
     * @param sampler1 First sampler.
     * @param sampler2 Second sampler.
     */
    public static <T> void assertProduceSameSequence(ObjectSampler<T> sampler1,
                                                     ObjectSampler<T> sampler2) {
        for (int i = 0; i < SAMPLES; i++) {
            final T value1 = sampler1.sample();
            final T value2 = sampler2.sample();
            if (isArray(value1) && isArray(value2)) {
                // JUnit assertArrayEquals will handle nested primitive arrays
                Assertions.assertArrayEquals(new Object[] {value1}, new Object[] {value2});
            } else {
                Assertions.assertEquals(value1, value2);
            }
        }
    }

    /**
     * Checks if the object is an array.
     *
     * @param object Object.
     * @return true if an array
     */
    private static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

    /**
     * Create a new random generator instance. The implementation will be randomly chosen
     * from a selection of high-quality generators.
     *
     * <p>This is a helper method to return a generator for use in testing where the
     * underlying source should not impact the test. This ensures the test is robust upon
     * repeat invocation across different JVM instances where the generator will most
     * likely be different.
     *
     * <p>Note that use of this method is preferable to use of a fixed seed generator. Any
     * test that is flaky when using this method may require an update to the test
     * assumptions and assertions.
     *
     * <p>It should be noted that repeat invocations of a failing test by the surefire plugin
     * will receive a different instance.
     *
     * @return the uniform random provider
     * @see RandomSource#create()
     * @see #createRNG(int)
     */
    public static UniformRandomProvider createRNG() {
        return SOURCES[ThreadLocalRandom.current().nextInt(SOURCES.length)].create();
    }

    /**
     * Create a count of new identical random generator instances. The implementation will be
     * randomly chosen from a selection of high-quality generators. Each instance
     * will be a copy created from the same seed and thus outputs the same sequence.
     *
     * <p>This is a helper method to return generators for use in parallel testing
     * where the underlying source should not impact the test. This ensures the test
     * is robust upon repeat invocation across different JVM instances where the
     * generator will most likely be different.
     *
     * <p>Note that use of this method is preferable to use of a fixed seed
     * generator. Any test that is flaky when using this method may require an
     * update to the test assumptions and assertions.
     *
     * <p>It should be noted that repeat invocations of a failing test by the
     * surefire plugin will receive different instances.
     *
     * @param count Number of copies to create.
     * @return the uniform random provider
     * @see RandomSource#create()
     * @see RandomSource#createSeed()
     * @see #createRNG()
     */
    public static UniformRandomProvider[] createRNG(int count) {
        final RandomSource source = SOURCES[ThreadLocalRandom.current().nextInt(SOURCES.length)];
        final byte[] seed = source.createSeed();
        return Stream.generate(() -> source.create(seed))
                     .limit(count)
                     .toArray(UniformRandomProvider[]::new);
    }

    /**
     * Create a new random generator instance with a fixed seed. The implementation is a
     * high-quality generator with a low construction cost. The seed is expected to be
     * different between invocations of the Java Virtual Machine.
     *
     * <p>This is a helper method to return a generator for use in testing where the
     * underlying source should not impact the test. This ensures the test is robust upon
     * repeat invocation across different JVM instances where the generator will most
     * likely be different.
     *
     * <p>Note that use of this method is preferable to use of a fixed seed generator as it provides
     * a single entry point to update tests that use a deterministic output from a RNG.
     *
     * <p>This method should be used in preference to {@link RandomAssert#createRNG()} when:
     * <ul>
     *  <li>the test requires multiple instances of a generator with the same output
     *  <li>the test requires a functioning generator but the output is not of consequence
     * </ul>
     *
     * <p>It should be noted that repeat invocations of a failing test by the surefire plugin
     * will receive an instance with the same seed. If a test may fail due to stochastic conditions
     * then consider using {@link RandomAssert#createRNG()} or {@link #createRNG(int)} which will
     * obtain a different RNG for repeat test executions. 
     *
     * @return the uniform random provider
     * @see RandomSource#create()
     * @see #createRNG()
     */
    public static UniformRandomProvider seededRNG() {
        return RandomSource.SPLIT_MIX_64.create(DEFAULT_SEED);
    }
}
