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
                RandomSource.KISS.create());
            // Gaussian (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                new BoxMullerGaussianSampler(RandomSource.MT.create(), meanNormal, sigmaNormal));
            // Gaussian ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new BoxMullerNormalizedGaussianSampler(RandomSource.MT.create()),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new MarsagliaNormalizedGaussianSampler(RandomSource.MT.create()),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new ZigguratNormalizedGaussianSampler(RandomSource.MT.create()),
                                   meanNormal, sigmaNormal));

            // Beta ("inverse method").
            final double alphaBeta = 4.3;
            final double betaBeta = 2.1;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                RandomSource.ISAAC.create());
            // Beta ("Cheng").
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                ChengBetaSampler.of(RandomSource.MWC_256.create(), alphaBeta, betaBeta));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBeta, alphaBeta),
                ChengBetaSampler.of(RandomSource.WELL_19937_A.create(), betaBeta, alphaBeta));
            // Beta ("Cheng", alternate algorithm).
            final double alphaBetaAlt = 0.5678;
            final double betaBetaAlt = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBetaAlt, betaBetaAlt),
                ChengBetaSampler.of(RandomSource.WELL_512_A.create(), alphaBetaAlt, betaBetaAlt));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBetaAlt, alphaBetaAlt),
                ChengBetaSampler.of(RandomSource.WELL_19937_C.create(), betaBetaAlt, alphaBetaAlt));

            // Cauchy ("inverse method").
            final double medianCauchy = 0.123;
            final double scaleCauchy = 4.5;
            add(LIST, new org.apache.commons.math3.distribution.CauchyDistribution(unusedRng, medianCauchy, scaleCauchy),
                RandomSource.WELL_19937_C.create());

            // Chi-square ("inverse method").
            final int dofChi2 = 12;
            add(LIST, new org.apache.commons.math3.distribution.ChiSquaredDistribution(unusedRng, dofChi2),
                RandomSource.WELL_19937_A.create());

            // Exponential ("inverse method").
            final double meanExp = 3.45;
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                RandomSource.WELL_44497_A.create());
            // Exponential.
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                AhrensDieterExponentialSampler.of(RandomSource.MT.create(), meanExp));

            // F ("inverse method").
            final int numDofF = 4;
            final int denomDofF = 7;
            add(LIST, new org.apache.commons.math3.distribution.FDistribution(unusedRng, numDofF, denomDofF),
                RandomSource.MT_64.create());

            // Gamma ("inverse method").
            final double alphaGammaSmallerThanOne = 0.1234;
            final double alphaGammaLargerThanOne = 2.345;
            final double thetaGamma = 3.456;
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                RandomSource.SPLIT_MIX_64.create());
            // Gamma (alpha < 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaSmallerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomSource.XOR_SHIFT_1024_S_PHI.create(),
                                                          alphaGammaSmallerThanOne, thetaGamma));
            // Gamma (alpha > 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomSource.WELL_44497_B.create(),
                                                          alphaGammaLargerThanOne, thetaGamma));

            // Gumbel ("inverse method").
            final double muGumbel = -4.56;
            final double betaGumbel = 0.123;
            add(LIST, new org.apache.commons.math3.distribution.GumbelDistribution(unusedRng, muGumbel, betaGumbel),
                RandomSource.WELL_1024_A.create());

            // Laplace ("inverse method").
            final double muLaplace = 12.3;
            final double betaLaplace = 5.6;
            add(LIST, new org.apache.commons.math3.distribution.LaplaceDistribution(unusedRng, muLaplace, betaLaplace),
                RandomSource.MWC_256.create());

            // Levy ("inverse method").
            final double muLevy = -1.098;
            final double cLevy = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, muLevy, cLevy),
                RandomSource.TWO_CMRES.create());
            // Levy sampler
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, muLevy, cLevy),
                LevySampler.of(RandomSource.JSF_64.create(), muLevy, cLevy));
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, 0.0, 1.0),
                LevySampler.of(RandomSource.JSF_64.create(), 0.0, 1.0));

            // Log normal ("inverse method").
            final double scaleLogNormal = 2.345;
            final double shapeLogNormal = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                RandomSource.KISS.create());
            // Log-normal (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                new BoxMullerLogNormalSampler(RandomSource.XOR_SHIFT_1024_S_PHI.create(), scaleLogNormal, shapeLogNormal));
            // Log-normal ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new BoxMullerNormalizedGaussianSampler(RandomSource.XOR_SHIFT_1024_S_PHI.create()),
                                    scaleLogNormal, shapeLogNormal));
            // Log-normal ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new MarsagliaNormalizedGaussianSampler(RandomSource.MT_64.create()),
                                    scaleLogNormal, shapeLogNormal));
            // Log-normal ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, scaleLogNormal, shapeLogNormal),
                LogNormalSampler.of(new ZigguratNormalizedGaussianSampler(RandomSource.MWC_256.create()),
                                    scaleLogNormal, shapeLogNormal));

            // Logistic ("inverse method").
            final double muLogistic = -123.456;
            final double sLogistic = 7.89;
            add(LIST, new org.apache.commons.math3.distribution.LogisticDistribution(unusedRng, muLogistic, sLogistic),
                RandomSource.TWO_CMRES_SELECT.create((Object) null, 2, 6));

            // Nakagami ("inverse method").
            final double muNakagami = 78.9;
            final double omegaNakagami = 23.4;
            final double inverseAbsoluteAccuracyNakagami = org.apache.commons.math3.distribution.NakagamiDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;
            add(LIST, new org.apache.commons.math3.distribution.NakagamiDistribution(unusedRng, muNakagami, omegaNakagami, inverseAbsoluteAccuracyNakagami),
                RandomSource.TWO_CMRES_SELECT.create((Object) null, 5, 3));

            // Pareto ("inverse method").
            final double scalePareto = 23.45;
            final double shapePareto = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                RandomSource.TWO_CMRES_SELECT.create((Object) null, 9, 11));
            // Pareto.
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                InverseTransformParetoSampler.of(RandomSource.XOR_SHIFT_1024_S_PHI.create(), scalePareto, shapePareto));

            // T ("inverse method").
            final double dofT = 0.76543;
            add(LIST, new org.apache.commons.math3.distribution.TDistribution(unusedRng, dofT),
                RandomSource.ISAAC.create());

            // Triangular ("inverse method").
            final double aTriangle = -0.76543;
            final double cTriangle = -0.65432;
            final double bTriangle = -0.54321;
            add(LIST, new org.apache.commons.math3.distribution.TriangularDistribution(unusedRng, aTriangle, cTriangle, bTriangle),
                RandomSource.MT.create());

            // Uniform ("inverse method").
            final double loUniform = -1.098;
            final double hiUniform = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                RandomSource.TWO_CMRES.create());
            // Uniform.
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                ContinuousUniformSampler.of(RandomSource.MT_64.create(), loUniform, hiUniform));

            // Weibull ("inverse method").
            final double alphaWeibull = 678.9;
            final double betaWeibull = 98.76;
            add(LIST, new org.apache.commons.math3.distribution.WeibullDistribution(unusedRng, alphaWeibull, betaWeibull),
                RandomSource.WELL_44497_B.create());
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
