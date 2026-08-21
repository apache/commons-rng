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
package org.apache.commons.rng.sampling.distribution;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Test that public sampler factory entry points reject non-finite (infinite or NaN)
 * distribution parameters instead of silently creating a sampler that returns
 * NaN, infinite or degenerate-constant samples forever.
 *
 * <p>This test collects all samplers identified for the 1.8 release that did not
 * verify finite arguments. Other samplers validating for finite arguments
 * have tests in their respective test class.
 *
 * <p>See RNG-194.
 */
class NonFiniteParameterValidationTest {
    /** Positive infinity. */
    private static final double INF = Double.POSITIVE_INFINITY;
    /** Not a number. */
    private static final double NAN = Double.NaN;


    @Test
    void testBoxMullerGaussianSamplerThrowsWithNonFiniteParameters() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> new BoxMullerGaussianSampler(rng, INF, 1.0));
        assertThrowsIAE(() -> new BoxMullerGaussianSampler(rng, 0.0, INF));
        assertThrowsIAE(() -> new BoxMullerGaussianSampler(rng, NAN, 1.0));
        assertThrowsIAE(() -> new BoxMullerGaussianSampler(rng, 0.0, NAN));
    }

    @Test
    void testChengBetaSamplerThrowsWithNonFiniteShape() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> ChengBetaSampler.of(rng, INF, 2.0));
        assertThrowsIAE(() -> ChengBetaSampler.of(rng, 2.0, INF));
        assertThrowsIAE(() -> ChengBetaSampler.of(rng, NAN, 2.0));
        assertThrowsIAE(() -> ChengBetaSampler.of(rng, 2.0, NAN));
    }

    @Test
    void testGammaSamplerThrowsWithNonFiniteParameters() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, INF, 1.0));
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, 0.5, INF));
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, 1.5, INF));
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, NAN, 1.0));
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, 0.5, NAN));
        assertThrowsIAE(() -> AhrensDieterMarsagliaTsangGammaSampler.of(rng, 1.5, NAN));
    }

    @Test
    void testLogNormalSamplerThrowsWithNonFiniteParameters() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        final NormalizedGaussianSampler gaussian = ZigguratSampler.NormalizedGaussian.of(rng);
        assertThrowsIAE(() -> LogNormalSampler.of(gaussian, INF, 1.0));
        assertThrowsIAE(() -> LogNormalSampler.of(gaussian, 0.0, INF));
        assertThrowsIAE(() -> LogNormalSampler.of(gaussian, NAN, 1.0));
        assertThrowsIAE(() -> LogNormalSampler.of(gaussian, 0.0, NAN));
    }

    @Test
    void testLevySamplerThrowsWithNonFiniteParameters() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> LevySampler.of(rng, INF, 1.0));
        assertThrowsIAE(() -> LevySampler.of(rng, 0.0, INF));
        assertThrowsIAE(() -> LevySampler.of(rng, NAN, 1.0));
        assertThrowsIAE(() -> LevySampler.of(rng, 0.0, NAN));
    }

    @Test
    void testExponentialSamplersThrowWithNonFiniteParameters() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> AhrensDieterExponentialSampler.of(rng, INF));
        assertThrowsIAE(() -> ZigguratSampler.Exponential.of(rng, INF));
        assertThrowsIAE(() -> AhrensDieterExponentialSampler.of(rng, NAN));
        assertThrowsIAE(() -> ZigguratSampler.Exponential.of(rng, NAN));
    }

    @Test
    void testParetoSamplerThrowsWithNonFiniteScale() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> InverseTransformParetoSampler.of(rng, INF, 1.0));
        assertThrowsIAE(() -> InverseTransformParetoSampler.of(rng, NAN, 1.0));
        // Note: an infinite shape is a supported limit of the distribution
        // (all samples equal the scale); it is deliberately not rejected.
        assertThrowsIAE(() -> InverseTransformParetoSampler.of(rng, 1.0, NAN));
    }

    @Test
    void testContinuousUniformSamplerThrowsWithNonFiniteBounds() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, INF, 1.0));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, 0.0, INF));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, INF, 1.0, true));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, 0.0, INF, false));
        assertThrowsIAE(() -> new ContinuousUniformSampler(rng, INF, 1.0));
        assertThrowsIAE(() -> new ContinuousUniformSampler(rng, 0.0, INF));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, NAN, 1.0));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, 0.0, NAN));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, NAN, 1.0, true));
        assertThrowsIAE(() -> ContinuousUniformSampler.of(rng, 0.0, NAN, false));
        assertThrowsIAE(() -> new ContinuousUniformSampler(rng, NAN, 1.0));
        assertThrowsIAE(() -> new ContinuousUniformSampler(rng, 0.0, NAN));
    }

    @Test
    void testRejectionInversionZipfSamplerThrowsWithNonFiniteExponent() {
        final UniformRandomProvider rng = RandomAssert.seededRNG();
        assertThrowsIAE(() -> RejectionInversionZipfSampler.of(rng, 10, INF));
        assertThrowsIAE(() -> RejectionInversionZipfSampler.of(rng, 10, NAN));
    }

    /**
     * Assert the executable throws an {@link IllegalArgumentException}.
     *
     * @param executable the executable
     */
    private static void assertThrowsIAE(Executable executable) {
        Assertions.assertThrows(IllegalArgumentException.class, executable);
    }
}
