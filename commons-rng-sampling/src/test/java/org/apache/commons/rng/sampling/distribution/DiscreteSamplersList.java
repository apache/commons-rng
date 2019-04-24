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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.math3.util.MathArrays;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * List of samplers.
 */
public final class DiscreteSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<DiscreteSamplerTestData[]> LIST =
        new ArrayList<DiscreteSamplerTestData[]>();

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
                RandomSource.create(RandomSource.KISS));
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(unusedRng, trialsBinomial, probSuccessBinomial),
                // range [9,16]
                MathArrays.sequence(8, 9, 1),
                MarsagliaTsangWangDiscreteSampler.Binomial.of(RandomSource.create(RandomSource.WELL_19937_A), trialsBinomial, probSuccessBinomial));
            // Inverted
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(unusedRng, trialsBinomial, 1 - probSuccessBinomial),
                // range [4,11] = [20-16, 20-9]
                MathArrays.sequence(8, 4, 1),
                MarsagliaTsangWangDiscreteSampler.Binomial.of(RandomSource.create(RandomSource.WELL_19937_C), trialsBinomial, 1 - probSuccessBinomial));

            // Geometric ("inverse method").
            final double probSuccessGeometric = 0.21;
            add(LIST, new org.apache.commons.math3.distribution.GeometricDistribution(unusedRng, probSuccessGeometric),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.ISAAC));
            // Geometric.
            add(LIST, new org.apache.commons.math3.distribution.GeometricDistribution(unusedRng, probSuccessGeometric),
                MathArrays.sequence(10, 0, 1),
                GeometricSampler.of(RandomSource.create(RandomSource.XOR_SHIFT_1024_S), probSuccessGeometric));

            // Hypergeometric ("inverse method").
            final int popSizeHyper = 34;
            final int numSuccessesHyper = 11;
            final int sampleSizeHyper = 12;
            add(LIST, new org.apache.commons.math3.distribution.HypergeometricDistribution(unusedRng, popSizeHyper, numSuccessesHyper, sampleSizeHyper),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.MT));

            // Pascal ("inverse method").
            final int numSuccessesPascal = 6;
            final double probSuccessPascal = 0.2;
            add(LIST, new org.apache.commons.math3.distribution.PascalDistribution(unusedRng, numSuccessesPascal, probSuccessPascal),
                MathArrays.sequence(18, 1, 1),
                RandomSource.create(RandomSource.TWO_CMRES));

            // Uniform ("inverse method").
            final int loUniform = -3;
            final int hiUniform = 4;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                RandomSource.create(RandomSource.SPLIT_MIX_64));
            // Uniform (power of 2 range).
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                DiscreteUniformSampler.of(RandomSource.create(RandomSource.MT_64), loUniform, hiUniform));
            // Uniform (large range).
            final int halfMax = Integer.MAX_VALUE / 2;
            final int hiLargeUniform = halfMax + 10;
            final int loLargeUniform = -hiLargeUniform;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loLargeUniform, hiLargeUniform),
                MathArrays.sequence(20, -halfMax, halfMax / 10),
                DiscreteUniformSampler.of(RandomSource.create(RandomSource.WELL_1024_A), loLargeUniform, hiLargeUniform));
            // Uniform (non-power of 2 range).
            final int rangeNonPowerOf2Uniform = 11;
            final int hiNonPowerOf2Uniform = loUniform + rangeNonPowerOf2Uniform;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(unusedRng, loUniform, hiNonPowerOf2Uniform),
                MathArrays.sequence(rangeNonPowerOf2Uniform, -3, 1),
                DiscreteUniformSampler.of(RandomSource.create(RandomSource.XO_SHI_RO_256_SS), loUniform, hiNonPowerOf2Uniform));

            // Zipf ("inverse method").
            final int numElementsZipf = 5;
            final double exponentZipf = 2.345;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                RandomSource.create(RandomSource.XOR_SHIFT_1024_S));
            // Zipf.
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                RejectionInversionZipfSampler.of(RandomSource.create(RandomSource.WELL_19937_C), numElementsZipf, exponentZipf));
            // Zipf (exponent close to 1).
            final double exponentCloseToOneZipf = 1 - 1e-10;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(unusedRng, numElementsZipf, exponentCloseToOneZipf),
                MathArrays.sequence(5, 1, 1),
                RejectionInversionZipfSampler.of(RandomSource.create(RandomSource.WELL_19937_C), numElementsZipf, exponentCloseToOneZipf));

            // Poisson ("inverse method").
            final double epsilonPoisson = org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_EPSILON;
            final int maxIterationsPoisson = org.apache.commons.math3.distribution.PoissonDistribution.DEFAULT_MAX_ITERATIONS;
            final double meanPoisson = 3.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.MWC_256));
            // Poisson.
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                PoissonSampler.of(RandomSource.create(RandomSource.KISS), meanPoisson));
            // Dedicated small mean poisson samplers
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                SmallMeanPoissonSampler.of(RandomSource.create(RandomSource.XO_SHI_RO_256_PLUS), meanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                KempSmallMeanPoissonSampler.of(RandomSource.create(RandomSource.XO_SHI_RO_128_PLUS), meanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomSource.create(RandomSource.XO_SHI_RO_128_PLUS), meanPoisson));
            // LargeMeanPoissonSampler should work at small mean.
            // Note: This hits a code path where the sample from the normal distribution is rejected.
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, meanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(10, 0, 1),
                LargeMeanPoissonSampler.of(RandomSource.create(RandomSource.PCG_MCG_XSH_RR_32), meanPoisson));
            // Poisson (40 < mean < 80).
            final double largeMeanPoisson = 67.89;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                PoissonSampler.of(RandomSource.create(RandomSource.SPLIT_MIX_64), largeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                LargeMeanPoissonSampler.of(RandomSource.create(RandomSource.SPLIT_MIX_64), largeMeanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, largeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomSource.create(RandomSource.XO_RO_SHI_RO_128_PLUS), largeMeanPoisson));
            // Poisson (mean >> 40).
            final double veryLargeMeanPoisson = 543.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                PoissonSampler.of(RandomSource.create(RandomSource.SPLIT_MIX_64), veryLargeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                LargeMeanPoissonSampler.of(RandomSource.create(RandomSource.SPLIT_MIX_64), veryLargeMeanPoisson));
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(unusedRng, veryLargeMeanPoisson, epsilonPoisson, maxIterationsPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                MarsagliaTsangWangDiscreteSampler.Poisson.of(RandomSource.create(RandomSource.XO_RO_SHI_RO_64_SS), veryLargeMeanPoisson));

            // Any discrete distribution
            final double[] discreteProbabilities = new double[] {0.1, 0.2, 0.3, 0.4, 0.5};
            add(LIST, discreteProbabilities,
                MarsagliaTsangWangDiscreteSampler.Enumerated.of(RandomSource.create(RandomSource.XO_SHI_RO_512_PLUS), discreteProbabilities));
            add(LIST, discreteProbabilities,
                GuideTableDiscreteSampler.of(RandomSource.create(RandomSource.XO_SHI_RO_512_SS), discreteProbabilities));
            add(LIST, discreteProbabilities,
                    AliasMethodDiscreteSampler.of(RandomSource.create(RandomSource.KISS), discreteProbabilities));
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
    private static void add(List<DiscreteSamplerTestData[]> list,
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
        list.add(new DiscreteSamplerTestData[] {new DiscreteSamplerTestData(inverseMethodSampler,
                                                                            points,
                                                                            getProbabilities(dist, points))});
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param points Outcomes selection.
     * @param sampler Sampler.
     */
    private static void add(List<DiscreteSamplerTestData[]> list,
                            final org.apache.commons.math3.distribution.IntegerDistribution dist,
                            int[] points,
                            final DiscreteSampler sampler) {
        list.add(new DiscreteSamplerTestData[] {new DiscreteSamplerTestData(sampler,
                                                                            points,
                                                                            getProbabilities(dist, points))});
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param probabilities Probability distribution to which the samples are supposed to conform.
     * @param sampler Sampler.
     */
    private static void add(List<DiscreteSamplerTestData[]> list,
                            final double[] probabilities,
                            final DiscreteSampler sampler) {
        list.add(new DiscreteSamplerTestData[] {new DiscreteSamplerTestData(sampler,
                                                                            MathArrays.natural(probabilities.length),
                                                                            probabilities)});
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<DiscreteSamplerTestData[]> list() {
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
