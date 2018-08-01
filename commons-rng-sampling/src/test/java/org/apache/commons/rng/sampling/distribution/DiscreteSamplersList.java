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
public class DiscreteSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<DiscreteSamplerTestData[]> LIST =
        new ArrayList<DiscreteSamplerTestData[]>();

    static {
        try {
            // List of distributions to test.

            // Binomial ("inverse method").
            final int trialsBinomial = 20;
            final double probSuccessBinomial = 0.67;
            add(LIST, new org.apache.commons.math3.distribution.BinomialDistribution(trialsBinomial, probSuccessBinomial),
                MathArrays.sequence(8, 9, 1),
                RandomSource.create(RandomSource.KISS));

            // Geometric ("inverse method").
            final double probSuccessGeometric = 0.21;
            add(LIST, new org.apache.commons.math3.distribution.GeometricDistribution(probSuccessGeometric),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.ISAAC));

            // Hypergeometric ("inverse method").
            final int popSizeHyper = 34;
            final int numSuccessesHyper = 11;
            final int sampleSizeHyper = 12;
            add(LIST, new org.apache.commons.math3.distribution.HypergeometricDistribution(popSizeHyper, numSuccessesHyper, sampleSizeHyper),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.MT));

            // Pascal ("inverse method").
            final int numSuccessesPascal = 6;
            final double probSuccessPascal = 0.2;
            add(LIST, new org.apache.commons.math3.distribution.PascalDistribution(numSuccessesPascal, probSuccessPascal),
                MathArrays.sequence(18, 1, 1),
                RandomSource.create(RandomSource.TWO_CMRES));

            // Uniform ("inverse method").
            final int loUniform = -3;
            final int hiUniform = 4;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                RandomSource.create(RandomSource.SPLIT_MIX_64));
            // Uniform.
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(loUniform, hiUniform),
                MathArrays.sequence(8, -3, 1),
                new DiscreteUniformSampler(RandomSource.create(RandomSource.MT_64), loUniform, hiUniform));
            // Uniform (large range).
            final int halfMax = Integer.MAX_VALUE / 2;
            final int hiLargeUniform = halfMax + 10;
            final int loLargeUniform = -hiLargeUniform;
            add(LIST, new org.apache.commons.math3.distribution.UniformIntegerDistribution(loLargeUniform, hiLargeUniform),
                MathArrays.sequence(20, -halfMax, halfMax / 10),
                new DiscreteUniformSampler(RandomSource.create(RandomSource.WELL_1024_A), loLargeUniform, hiLargeUniform));

            // Zipf ("inverse method").
            final int numElementsZipf = 5;
            final double exponentZipf = 2.345;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                RandomSource.create(RandomSource.XOR_SHIFT_1024_S));
            // Zipf.
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(numElementsZipf, exponentZipf),
                MathArrays.sequence(5, 1, 1),
                new RejectionInversionZipfSampler(RandomSource.create(RandomSource.WELL_19937_C), numElementsZipf, exponentZipf));
            // Zipf (exponent close to 1).
            final double exponentCloseToOneZipf = 1 - 1e-10;
            add(LIST, new org.apache.commons.math3.distribution.ZipfDistribution(numElementsZipf, exponentCloseToOneZipf),
                MathArrays.sequence(5, 1, 1),
                new RejectionInversionZipfSampler(RandomSource.create(RandomSource.WELL_19937_C), numElementsZipf, exponentCloseToOneZipf));

            // Poisson ("inverse method").
            final double meanPoisson = 3.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(meanPoisson),
                MathArrays.sequence(10, 0, 1),
                RandomSource.create(RandomSource.MWC_256));
            // Poisson.
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(meanPoisson),
                MathArrays.sequence(10, 0, 1),
                new PoissonSampler(RandomSource.create(RandomSource.KISS), meanPoisson));
            // Dedicated small mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(meanPoisson),
                MathArrays.sequence(10, 0, 1),
                new SmallMeanPoissonSampler(RandomSource.create(RandomSource.KISS), meanPoisson));
            // Poisson (40 < mean < 80).
            final double largeMeanPoisson = 67.89;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(largeMeanPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                new PoissonSampler(RandomSource.create(RandomSource.SPLIT_MIX_64), largeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(largeMeanPoisson),
                MathArrays.sequence(50, (int) (largeMeanPoisson - 25), 1),
                new LargeMeanPoissonSampler(RandomSource.create(RandomSource.SPLIT_MIX_64), largeMeanPoisson));
            // Poisson (mean >> 40).
            final double veryLargeMeanPoisson = 543.21;
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(veryLargeMeanPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                new PoissonSampler(RandomSource.create(RandomSource.SPLIT_MIX_64), veryLargeMeanPoisson));
            // Dedicated large mean poisson sampler
            add(LIST, new org.apache.commons.math3.distribution.PoissonDistribution(veryLargeMeanPoisson),
                MathArrays.sequence(100, (int) (veryLargeMeanPoisson - 50), 1),
                new LargeMeanPoissonSampler(RandomSource.create(RandomSource.SPLIT_MIX_64), veryLargeMeanPoisson));
        } catch (Exception e) {
            System.err.println("Unexpected exception while creating the list of samplers: " + e);
            e.printStackTrace(System.err);
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
            new InverseTransformDiscreteSampler(rng,
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
        list.add(new DiscreteSamplerTestData[] { new DiscreteSamplerTestData(inverseMethodSampler,
                                                                             points,
                                                                             getProbabilities(dist, points)) });
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
        list.add(new DiscreteSamplerTestData[] { new DiscreteSamplerTestData(sampler,
                                                                             points,
                                                                             getProbabilities(dist, points)) });
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
