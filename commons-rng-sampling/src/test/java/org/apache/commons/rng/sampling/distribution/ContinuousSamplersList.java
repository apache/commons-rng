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

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * List of samplers.
 */
public final class ContinuousSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<ContinuousSamplerTestData[]> LIST =
        new ArrayList<ContinuousSamplerTestData[]>();

    static {
        try {
            // This test uses reference distributions from commons-math3 to compute the expected
            // PMF. These distributions have a dual functionality to compute the PMF and perform
            // sampling. When no sampling is needed for the created distribution, it is advised
            // to pass null as the random generator via the appropriate constructors to avoid the
            // additional initialisation overhead.
            org.apache.commons.math3.random.RandomGenerator unusedRng = null;

            // List of distributions to test.

            // Gaussian ("inverse method").
            final double meanNormal = -123.45;
            final double sigmaNormal = 6.789;
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                RandomSource.create(RandomSource.KISS));
            // Gaussian (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                new BoxMullerGaussianSampler(RandomSource.create(RandomSource.MT), meanNormal, sigmaNormal));
            // Gaussian ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new BoxMullerNormalizedGaussianSampler(RandomSource.create(RandomSource.MT)),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new MarsagliaNormalizedGaussianSampler(RandomSource.create(RandomSource.MT)),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new ZigguratNormalizedGaussianSampler(RandomSource.create(RandomSource.MT)),
                                   meanNormal, sigmaNormal));

            // Beta ("inverse method").
            final double alphaBeta = 4.3;
            final double betaBeta = 2.1;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                RandomSource.create(RandomSource.ISAAC));
            // Beta ("Cheng").
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                ChengBetaSampler.of(RandomSource.create(RandomSource.MWC_256), alphaBeta, betaBeta));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBeta, alphaBeta),
                ChengBetaSampler.of(RandomSource.create(RandomSource.WELL_19937_A), betaBeta, alphaBeta));
            // Beta ("Cheng", alternate algorithm).
            final double alphaBetaAlt = 0.5678;
            final double betaBetaAlt = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBetaAlt, betaBetaAlt),
                ChengBetaSampler.of(RandomSource.create(RandomSource.WELL_512_A), alphaBetaAlt, betaBetaAlt));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBetaAlt, alphaBetaAlt),
                ChengBetaSampler.of(RandomSource.create(RandomSource.WELL_19937_C), betaBetaAlt, alphaBetaAlt));

            // Cauchy ("inverse method").
            final double medianCauchy = 0.123;
            final double scaleCauchy = 4.5;
            add(LIST, new org.apache.commons.math3.distribution.CauchyDistribution(unusedRng, medianCauchy, scaleCauchy),
                RandomSource.create(RandomSource.WELL_19937_C));

            // Chi-square ("inverse method").
            final int dofChi2 = 12;
            add(LIST, new org.apache.commons.math3.distribution.ChiSquaredDistribution(unusedRng, dofChi2),
                RandomSource.create(RandomSource.WELL_19937_A));

            // Exponential ("inverse method").
            final double meanExp = 3.45;
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                RandomSource.create(RandomSource.WELL_44497_A));
            // Exponential.
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                AhrensDieterExponentialSampler.of(RandomSource.create(RandomSource.MT), meanExp));

            // F ("inverse method").
            final int numDofF = 4;
            final int denomDofF = 7;
            add(LIST, new org.apache.commons.math3.distribution.FDistribution(unusedRng, numDofF, denomDofF),
                RandomSource.create(RandomSource.MT_64));

            // Gamma ("inverse method").
            final double alphaGammaSmallerThanOne = 0.1234;
            final double alphaGammaLargerThanOne = 2.345;
            final double thetaGamma = 3.456;
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                RandomSource.create(RandomSource.SPLIT_MIX_64));
            // Gamma (alpha < 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaSmallerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomSource.create(RandomSource.XOR_SHIFT_1024_S),
                                                           alphaGammaSmallerThanOne, thetaGamma));
            // Gamma (alpha > 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomSource.create(RandomSource.WELL_44497_B),
                                                           alphaGammaLargerThanOne, thetaGamma));

            // Gumbel ("inverse method").
            final double muGumbel = -4.56;
            final double betaGumbel = 0.123;
            add(LIST, new org.apache.commons.math3.distribution.GumbelDistribution(unusedRng, muGumbel, betaGumbel),
                RandomSource.create(RandomSource.WELL_1024_A));

            // Laplace ("inverse method").
            final double muLaplace = 12.3;
            final double betaLaplace = 5.6;
            add(LIST, new org.apache.commons.math3.distribution.LaplaceDistribution(unusedRng, muLaplace, betaLaplace),
                RandomSource.create(RandomSource.MWC_256));

            // Levy ("inverse method").
            final double muLevy = -1.098;
            final double cLevy = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, muLevy, cLevy),
                RandomSource.create(RandomSource.TWO_CMRES));

            // Log normal ("inverse method").
            final double scaleLogNormal = 2.345;
            final double shapeLogNormal = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                RandomSource.create(RandomSource.KISS));
            // Log-normal (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                new BoxMullerLogNormalSampler(RandomSource.create(RandomSource.XOR_SHIFT_1024_S), scaleLogNormal, shapeLogNormal));
            // Log-normal ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new BoxMullerNormalizedGaussianSampler(RandomSource.create(RandomSource.XOR_SHIFT_1024_S)),
                                    scaleLogNormal, shapeLogNormal));
            // Log-normal ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new MarsagliaNormalizedGaussianSampler(RandomSource.create(RandomSource.MT_64)),
                                    scaleLogNormal, shapeLogNormal));
            // Log-normal ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new ZigguratNormalizedGaussianSampler(RandomSource.create(RandomSource.MWC_256)),
                                    scaleLogNormal, shapeLogNormal));

            // Logistic ("inverse method").
            final double muLogistic = -123.456;
            final double sLogistic = 7.89;
            add(LIST, new org.apache.commons.math3.distribution.LogisticDistribution(unusedRng, muLogistic, sLogistic),
                RandomSource.create(RandomSource.TWO_CMRES_SELECT, null, 2, 6));

            // Nakagami ("inverse method").
            final double muNakagami = 78.9;
            final double omegaNakagami = 23.4;
            final double inverseAbsoluteAccuracyNakagami = org.apache.commons.math3.distribution.NakagamiDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;
            add(LIST, new org.apache.commons.math3.distribution.NakagamiDistribution(unusedRng, muNakagami, omegaNakagami, inverseAbsoluteAccuracyNakagami),
                RandomSource.create(RandomSource.TWO_CMRES_SELECT, null, 5, 3));

            // Pareto ("inverse method").
            final double scalePareto = 23.45;
            final double shapePareto = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                RandomSource.create(RandomSource.TWO_CMRES_SELECT, null, 9, 11));
            // Pareto.
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                InverseTransformParetoSampler.of(RandomSource.create(RandomSource.XOR_SHIFT_1024_S), scalePareto, shapePareto));

            // T ("inverse method").
            final double dofT = 0.76543;
            add(LIST, new org.apache.commons.math3.distribution.TDistribution(unusedRng, dofT),
                RandomSource.create(RandomSource.ISAAC));

            // Triangular ("inverse method").
            final double aTriangle = -0.76543;
            final double cTriangle = -0.65432;
            final double bTriangle = -0.54321;
            add(LIST, new org.apache.commons.math3.distribution.TriangularDistribution(unusedRng, aTriangle, cTriangle, bTriangle),
                RandomSource.create(RandomSource.MT));

            // Uniform ("inverse method").
            final double loUniform = -1.098;
            final double hiUniform = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                RandomSource.create(RandomSource.TWO_CMRES));
            // Uniform.
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                ContinuousUniformSampler.of(RandomSource.create(RandomSource.MT_64), loUniform, hiUniform));

            // Weibull ("inverse method").
            final double alphaWeibull = 678.9;
            final double betaWeibull = 98.76;
            add(LIST, new org.apache.commons.math3.distribution.WeibullDistribution(unusedRng, alphaWeibull, betaWeibull),
                RandomSource.create(RandomSource.WELL_44497_B));
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
    private ContinuousSamplersList() {}

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param rng Generator of uniformly distributed sequences.
     */
    private static void add(List<ContinuousSamplerTestData[]> list,
                            final org.apache.commons.math3.distribution.RealDistribution dist,
                            UniformRandomProvider rng) {
        final ContinuousSampler inverseMethodSampler =
            InverseTransformContinuousSampler.of(rng,
                new ContinuousInverseCumulativeProbabilityFunction() {
                    @Override
                    public double inverseCumulativeProbability(double p) {
                        return dist.inverseCumulativeProbability(p);
                    }
                    @Override
                    public String toString() {
                        return dist.toString();
                    }
                });
        list.add(new ContinuousSamplerTestData[] {new ContinuousSamplerTestData(inverseMethodSampler,
                                                                                getDeciles(dist))});
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param sampler Sampler.
     */
    private static void add(List<ContinuousSamplerTestData[]> list,
                            final org.apache.commons.math3.distribution.RealDistribution dist,
                            final ContinuousSampler sampler) {
        list.add(new ContinuousSamplerTestData[] {new ContinuousSamplerTestData(sampler,
                                                                                getDeciles(dist))});
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<ContinuousSamplerTestData[]> list() {
        return Collections.unmodifiableList(LIST);
    }

    /**
     * @param dist Distribution.
     * @return the deciles of the given distribution.
     */
    private static double[] getDeciles(org.apache.commons.math3.distribution.RealDistribution dist) {
        final int last = 9;
        final double[] deciles = new double[last];
        final double ten = 10;
        for (int i = 0; i < last; i++) {
            deciles[i] = dist.inverseCumulativeProbability((i + 1) / ten);
        }
        return deciles;
    }
}
