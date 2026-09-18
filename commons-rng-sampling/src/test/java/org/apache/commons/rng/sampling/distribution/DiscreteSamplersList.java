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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;

/**
 * List of samplers.
 */
public final class DiscreteSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<DiscreteSamplerTestData> LIST = new ArrayList<>();

    static {
        try {
            // This test uses reference distributions from commons-math3 to compute the expected
            // PMF. These distributions have a dual functionality to compute the PMF and perform
            // sampling. When no sampling is needed for the created distribution, it is advised
            // to pass null as the random generator via the appropriate constructors to avoid the
            // additional initialisation overhead.
            org.apache.commons.math3.random.RandomGenerator unusedRng = null;

            // List of distributions to test.

            // Binomial ("inverse method").
            final int trialsBinomial = 20;
            final double probSuccessBinomial = 0.67;
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(unusedRng, trialsBinomial, probSuccessBinomial),
                MathArrays.sequence(8, 9, 1),
                RandomAssert.createRNG());
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(unusedRng, trialsBinomial, probSuccessBinomial),
                // range [9,16]
                MathArrays.sequence(8, 9, 1),
                MarsagliaTsangWangDiscreteSampler.Binomial.of(RandomAssert.createRNG(), trialsBinomial, probSuccessBinomial));
            // Inverted
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(unusedRng, trialsBinomial, 1 - probSuccessBinomial),
                // range [4,11] = [20-16, 20-9]
                MathArrays.sequence(8, 4, 1),
                MarsagliaTsangWangDiscreteSampler.Binomial.of(RandomAssert.createRNG(), trialsBinomial, 1 - probSuccessBinomial));

            // Geometric ("inverse method").
            final double probSuccessGeometric = 0.21;
            add(LIST, new org.apache.commons.math3.distribution.GeometricDistribution(unusedRng, probSuccessGeometric),
                MathArrays.sequence(10, 0, 1),
                RandomAssert.createRNG());
            // Geometric.
            add(LIST, new org.apache.commons.math3.distribution.GeometricDistribution(unusedRng, probSuccessGeometric),
                MathArrays.sequence(10, 0, 1),
                GeometricSampler.of(RandomAssert.createRNG(), probSuccessGeometric));

            // Hypergeometric ("inverse method").
            final int popSizeHyper = 34;
            final int numSuccessesHyper = 11;
            final int sampleSizeHyper = 12;
            add(LIST, new org.apache.commons.math3.distribution.HypergeometricDistribution(unusedRng, popSizeHyper, numSuccessesHyper, sampleSizeHyper),
                MathArrays.sequence(10, 0, 1),
                RandomAssert.createRNG());

            // Pascal ("inverse method").
            final int numSuccessesPascal = 6;
            final double probSuccessPascal = 0.2;
            add(LIST, new org.apache.commons.math3.distribution.PascalDistribution(unusedRng, numSuccessesPascal, probSuccessPascal),
                MathArrays.sequence(18, 1, 1),
                RandomAssert.createRNG());

            // Uniform ("inverse method").
            final int loUniform = -3;
            final int hiUniform = 4;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                RandomAssert.createRNG());
            // Uniform (power of 2 range).
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                DiscreteUniformSampler.of(RandomAssert.createRNG(), loUniform, hiUniform));
            // Uniform (large range).
            final int halfMax = Integer.MAX_VALUE / 2;
            final int hiLargeUniform = halfMax + 10;
            final int loLargeUniform = -hiLargeUniform;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loLargeUniform, hiLargeUniform),
                MathArrays.sequence(20, -halfMax, halfMax / 10),
                DiscreteUniformSampler.of(RandomAssert.createRNG(), loLargeUniform, hiLargeUniform));
            // Uniform (non-power of 2 range).
            final int rangeNonPowerOf2Uniform = 11;
            final int hiNonPowerOf2Uniform = loUniform + rangeNonPowerOf2Uniform;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiNonPowerOf2Uniform),
                MathArrays.sequence(rangeNonPowerOf2Uniform, -3, 1),
                DiscreteUniformSampler.of(RandomAssert.createRNG(), loUniform, hiNonPowerOf2Uniform));

            // Zipf ("inverse method").
            final int numElementsZipf = 5;
            final double exponentZipf = 2.345;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                RandomAssert.createRNG());
            // Zipf.
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                RejectionInversionZipfSampler.of(RandomAssert.createRNG(), numElementsZipf, exponentZipf));
            // Zipf (exponent close to 1).
            final double exponentCloseToOneZipf = 1 - 1e-10;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentCloseToOneZipf),
                MathArrays.sequence(5, 1, 1),
                RejectionInversionZipfSampler.of(RandomAssert.createRNG(), numElementsZipf, exponentCloseToOneZipf));
            // Zipf (exponent = 0).
            add(LIST, MathArrays.sequence(5, 1, 1), new double[] {0.2, 0.2, 0.2, 0.2, 0.2}, false,
                RejectionInversionZipfSampler.of(RandomAssert.createRNG(), numElementsZipf, 0.0));

            // Poisson ("inverse method").
            final double epsilonPoisson = org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_EPSILON;
            final int maxIterationsPoisson = org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_MAX_ITERATIONS;
            final double meanPoisson = 3.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                RandomAssert.createRNG());
            // Poisson.
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                PoissonSampler.of(RandomAssert.createRNG(), meanPoisson));
            // Dedicated small mean poisson samplers
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                SmallMeanPoissonSampler.of(RandomAssert.createRNG(), meanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                KempSmallMeanPoissonSampler.of(RandomAssert.createRNG(), meanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomAssert.createRNG(), meanPoisson));
            // LargeMeanPoissonSampler should work at small mean.
            // Note: This hits a code path where the sample from the normal distribution is rejected.
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                LargeMeanPoissonSampler.of(RandomAssert.createRNG(), meanPoisson));
            // Poisson (40 < mean < 80).
            final double largeMeanPoisson = 67.89;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                PoissonSampler.of(RandomAssert.createRNG(), largeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                LargeMeanPoissonSampler.of(RandomAssert.createRNG(), largeMeanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomAssert.createRNG(), largeMeanPoisson));
            // Poisson (mean >> 40).
            final double veryLargeMeanPoisson = 543.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                PoissonSampler.of(RandomAssert.createRNG(), veryLargeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                LargeMeanPoissonSampler.of(RandomAssert.createRNG(), veryLargeMeanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomAssert.createRNG(), veryLargeMeanPoisson));

            // Any discrete distribution
            final int[] discretePoints = {0, 1, 2, 3, 4};
            final double[] discreteProbabilities = {0.1, 0.2, 0.3, 0.4, 0.5};
            final long[] discreteFrequencies = {1, 2, 3, 4, 5};
            add(LIST, discretePoints, discreteProbabilities, false,
                MarsagliaTsangWangDiscreteSampler.Enumerated.of(RandomAssert.createRNG(), discreteProbabilities));
            add(LIST, discretePoints, discreteProbabilities, false,
                GuideTableDiscreteSampler.of(RandomAssert.createRNG(), discreteProbabilities));
            add(LIST, discretePoints, discreteProbabilities, false,
                AliasMethodDiscreteSampler.of(RandomAssert.createRNG(), discreteProbabilities));
            add(LIST, discretePoints, discreteProbabilities, false,
                FastLoadedDiceRollerDiscreteSampler.of(RandomAssert.createRNG(), discreteFrequencies));
            add(LIST, discretePoints, discreteProbabilities, false,
                FastLoadedDiceRollerDiscreteSampler.of(RandomAssert.createRNG(), discreteProbabilities));

            // Zeta distribution: x in [1, infinity].
            // Range points generated with scipy.stats (1.18.1) zipf(s).
            // from scipy.stats import zipf
            // import numpy as np; np.set_printoptions(precision=17)
            // x = np.unique(zipf(s).ppf([0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9])).astype(int)
            // p = np.append(zipf(s).cdf(x), [1])
            // Convert cumulative to range probabilities:
            // p[1:] -= p[:-1]

            // s = 1.15
            add(LIST,
                new int[] {1, 3, 6, 17, 58, 256, 1742, 26005, 2641987, Integer.MAX_VALUE},
                new double[] {
                    0.13784177793759045, 0.10108148525222727, 0.06720522534524914,
                    0.09570277096807939, 0.09903685404346751, 0.09925485973116599,
                    0.09988225352737812, 0.09999485138279085, 0.09999992334440055,
                    0.09999999846765073},
                true,
                ZetaSampler.of(RandomAssert.createRNG(), 1.15));
            // s = 1.35. Skewed to 1
            add(LIST,
                new int[] {1, 2, 4, 8, 18, 58, 417, Integer.MAX_VALUE},
                new double[] {
                    0.28908106624157415, 0.11340420378808813, 0.11008788688054949,
                    0.09700240404611993, 0.09297268629333943, 0.09863885643999604,
                    0.09887693680911303, 0.0999359595012198},
                true,
                ZetaSampler.of(RandomAssert.createRNG(), 1.35));
            // s = 2.35. Very skewed to 1
            add(LIST,
                new int[] {1, 2, 3, Integer.MAX_VALUE},
                new double[] {
                    0.7107946035897981, 0.13941953571184507, 0.0537661789726942,
                    0.09601968172566266},
                true,
                ZetaSampler.of(RandomAssert.createRNG(), 2.35));
            // s = 1.02. Skewed to infinity. Tests sampler with truncation of the distribution.
            // scipy zipf is too slow to explore small s as it sums the pmf for the cdf.
            // Computed using CDF=(zeta(s, x+1) - zeta(s)) / zeta(s) using the
            // the zeta function from mpmath (1.14.1) with x=[2**3, 2**7, 2**11, 2**15, 2**20, 2**25]
            // and 50 digits of precision.
            add(LIST,
                new int[] {8, 128, 2048, 32768, 1048576, 33554432, Integer.MAX_VALUE},
                new double[] {0.05287102382096685, 0.0500627338177098, 0.04832778811226474,
                    0.04577927420430394, 0.05377155271772868, 0.05017084772170899,
                    // Most of the density is above 2^31
                    0.699016779605317},
                true,
                ZetaSampler.of(RandomAssert.createRNG(), 1.02));
        } catch (Exception e) {
            // CHECKSTYLE: stop Regexp
            System.err.println("Unexpected exception while creating the list of samplers: " + e);
            e.printStackTrace(System.err);
            // CHECKSTYLE: resume Regexp
            throw new RuntimeException(e);
        }
    }

    /**
     * Class contains only static methods.
     */
    private DiscreteSamplersList() {}

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param points Outcomes selection.
     * @param rng Generator of uniformly distributed sequences.
     */
    private static void add(List<DiscreteSamplerTestData> list,
                            final org.apache.commons.math3.distribution.IntegerDistribution dist,
                            int[] points,
                            UniformRandomProvider rng) {
        final DiscreteSampler inverseMethodSampler =
            InverseTransformDiscreteSampler.of(rng,
                new DiscreteInverseCumulativeProbabilityFunction() {
                    @Override
                    public int inverseCumulativeProbability(double p) {
                        return dist.inverseCumulativeProbability(p);
                    }
                    @Override
                    public String toString() {
                        return dist.toString();
                    }
                });
        list.add(new DiscreteSamplerTestData(inverseMethodSampler,
                                             points,
                                             getProbabilities(dist, points),
                                             false));
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param points Outcomes selection.
     * @param sampler Sampler.
     */
    private static void add(List<DiscreteSamplerTestData> list,
                            final org.apache.commons.math3.distribution.IntegerDistribution dist,
                            int[] points,
                            final DiscreteSampler sampler) {
        list.add(new DiscreteSamplerTestData(sampler,
                                             points,
                                             getProbabilities(dist, points),
                                             false));
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param points Outcomes selection.
     * @param probabilities Probability distribution to which the samples are supposed to conform.
     * @param sampler Sampler.
     */
    private static void add(List<DiscreteSamplerTestData> list,
                            int[] points,
                            final double[] probabilities,
                            boolean range,
                            final DiscreteSampler sampler) {
        list.add(new DiscreteSamplerTestData(sampler,
                                             points,
                                             probabilities,
                                             range));
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<DiscreteSamplerTestData> list() {
        return Collections.unmodifiableList(LIST);
    }

    /**
     * @param dist Distribution.
     * @param points Points.
     * @return the probabilities of the given points according to the distribution.
     */
    private static double[] getProbabilities(org.apache.commons.math3.distribution.IntegerDistribution dist,
                                             int[] points) {
        final int len = points.length;
        final double[] prob = new double[len];
        for (int i = 0; i < len; i++) {
            prob[i] = dist instanceof org.apache.commons.math3.distribution.UniformIntegerDistribution ? // XXX Workaround.
                getProbability((org.apache.commons.math3.distribution.UniformIntegerDistribution) dist) :
                dist.probability(points[i]);

            if (prob[i] < 0) {
                throw new IllegalStateException(dist + ": p < 0 (at " + points[i] + ", p=" + prob[i]);
            }
        }
        return prob;
    }

    /**
     * Workaround bugs in Commons Math's "UniformIntegerDistribution" (cf. MATH-1396).
     */
    private static double getProbability(org.apache.commons.math3.distribution.UniformIntegerDistribution dist) {
        return 1 / ((double) dist.getSupportUpperBound() - (double) dist.getSupportLowerBound() + 1);
    }
}
