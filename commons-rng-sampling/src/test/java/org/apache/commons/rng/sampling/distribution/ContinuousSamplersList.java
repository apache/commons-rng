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
import org.apache.commons.rng.sampling.RandomAssert;

/**
 * List of samplers.
 */
public final class ContinuousSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<ContinuousSamplerTestData> LIST = new ArrayList<>();

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
                RandomAssert.createRNG());
            // Gaussian (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                new BoxMullerGaussianSampler(RandomAssert.createRNG(), meanNormal, sigmaNormal));
            // Gaussian ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new BoxMullerNormalizedGaussianSampler(RandomAssert.createRNG()),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new MarsagliaNormalizedGaussianSampler(RandomAssert.createRNG()),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(new ZigguratNormalizedGaussianSampler(RandomAssert.createRNG()),
                                   meanNormal, sigmaNormal));
            // Gaussian ("Modified ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, meanNormal, sigmaNormal),
                GaussianSampler.of(ZigguratSampler.NormalizedGaussian.of(RandomAssert.createRNG()),
                                   meanNormal, sigmaNormal));

            // Beta ("inverse method").
            final double alphaBeta = 4.3;
            final double betaBeta = 2.1;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                RandomAssert.createRNG());
            // Beta ("Cheng").
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBeta, betaBeta),
                ChengBetaSampler.of(RandomAssert.createRNG(), alphaBeta, betaBeta));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBeta, alphaBeta),
                ChengBetaSampler.of(RandomAssert.createRNG(), betaBeta, alphaBeta));
            // Beta ("Cheng", alternate algorithm).
            final double alphaBetaAlt = 0.5678;
            final double betaBetaAlt = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, alphaBetaAlt, betaBetaAlt),
                ChengBetaSampler.of(RandomAssert.createRNG(), alphaBetaAlt, betaBetaAlt));
            add(LIST, new org.apache.commons.math3.distribution.BetaDistribution(unusedRng, betaBetaAlt, alphaBetaAlt),
                ChengBetaSampler.of(RandomAssert.createRNG(), betaBetaAlt, alphaBetaAlt));

            // Cauchy ("inverse method").
            final double medianCauchy = 0.123;
            final double scaleCauchy = 4.5;
            add(LIST, new org.apache.commons.math3.distribution.CauchyDistribution(unusedRng, medianCauchy, scaleCauchy),
                RandomAssert.createRNG());

            // Chi-square ("inverse method").
            final int dofChi2 = 12;
            add(LIST, new org.apache.commons.math3.distribution.ChiSquaredDistribution(unusedRng, dofChi2),
                RandomAssert.createRNG());

            // Exponential ("inverse method").
            final double meanExp = 3.45;
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                RandomAssert.createRNG());
            // Exponential.
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                AhrensDieterExponentialSampler.of(RandomAssert.createRNG(), meanExp));
            // Exponential ("Modified ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.ExponentialDistribution(unusedRng, meanExp),
                ZigguratSampler.Exponential.of(RandomAssert.createRNG(), meanExp));

            // F ("inverse method").
            final int numDofF = 4;
            final int denomDofF = 7;
            add(LIST, new org.apache.commons.math3.distribution.FDistribution(unusedRng, numDofF, denomDofF),
                RandomAssert.createRNG());

            // Gamma ("inverse method").
            final double alphaGammaSmallerThanOne = 0.1234;
            final double alphaGammaLargerThanOne = 2.345;
            final double thetaGamma = 3.456;
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                RandomAssert.createRNG());
            // Gamma (alpha < 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaSmallerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomAssert.createRNG(),
                                                          alphaGammaSmallerThanOne, thetaGamma));
            // Gamma (alpha > 1).
            add(LIST, new org.apache.commons.math3.distribution.GammaDistribution(unusedRng, alphaGammaLargerThanOne, thetaGamma),
                AhrensDieterMarsagliaTsangGammaSampler.of(RandomAssert.createRNG(),
                                                          alphaGammaLargerThanOne, thetaGamma));

            // Gumbel ("inverse method").
            final double muGumbel = -4.56;
            final double betaGumbel = 0.123;
            add(LIST, new org.apache.commons.math3.distribution.GumbelDistribution(unusedRng, muGumbel, betaGumbel),
                RandomAssert.createRNG());

            // Laplace ("inverse method").
            final double muLaplace = 12.3;
            final double betaLaplace = 5.6;
            add(LIST, new org.apache.commons.math3.distribution.LaplaceDistribution(unusedRng, muLaplace, betaLaplace),
                RandomAssert.createRNG());

            // Levy ("inverse method").
            final double muLevy = -1.098;
            final double cLevy = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, muLevy, cLevy),
                RandomAssert.createRNG());
            // Levy sampler
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, muLevy, cLevy),
                LevySampler.of(RandomAssert.createRNG(), muLevy, cLevy));
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, 0.0, 1.0),
                LevySampler.of(RandomAssert.createRNG(), 0.0, 1.0));

            // Log normal ("inverse method").
            final double muLogNormal = 2.345;
            final double sigmaLogNormal = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal, sigmaLogNormal),
                RandomAssert.createRNG());
            // Log-normal (DEPRECATED "Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal, sigmaLogNormal),
                new BoxMullerLogNormalSampler(RandomAssert.createRNG(), muLogNormal, sigmaLogNormal));
            // Log-normal ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal, sigmaLogNormal),
                LogNormalSampler.of(new BoxMullerNormalizedGaussianSampler(RandomAssert.createRNG()),
                                    muLogNormal, sigmaLogNormal));
            // Log-normal ("Marsaglia").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal, sigmaLogNormal),
                LogNormalSampler.of(new MarsagliaNormalizedGaussianSampler(RandomAssert.createRNG()),
                                    muLogNormal, sigmaLogNormal));
            // Log-normal ("Ziggurat").
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal, sigmaLogNormal),
                LogNormalSampler.of(new ZigguratNormalizedGaussianSampler(RandomAssert.createRNG()),
                                    muLogNormal, sigmaLogNormal));
            // Log-normal negative mean
            final double muLogNormal2 = -1.1;
            final double sigmaLogNormal2 = 2.3;
            add(LIST, new org.apache.commons.math3.distribution.LogNormalDistribution(unusedRng, muLogNormal2, sigmaLogNormal2),
                    LogNormalSampler.of(new ZigguratNormalizedGaussianSampler(RandomAssert.createRNG()),
                                        muLogNormal2, sigmaLogNormal2));

            // Logistic ("inverse method").
            final double muLogistic = -123.456;
            final double sLogistic = 7.89;
            add(LIST, new org.apache.commons.math3.distribution.LogisticDistribution(unusedRng, muLogistic, sLogistic),
                RandomAssert.createRNG());

            // Nakagami ("inverse method").
            final double muNakagami = 78.9;
            final double omegaNakagami = 23.4;
            final double inverseAbsoluteAccuracyNakagami = org.apache.commons.math3.distribution.NakagamiDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY;
            add(LIST, new org.apache.commons.math3.distribution.NakagamiDistribution(unusedRng, muNakagami, omegaNakagami, inverseAbsoluteAccuracyNakagami),
                RandomAssert.createRNG());

            // Pareto ("inverse method").
            final double scalePareto = 23.45;
            final double shapePareto = 0.1234;
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                RandomAssert.createRNG());
            // Pareto.
            add(LIST, new org.apache.commons.math3.distribution.ParetoDistribution(unusedRng, scalePareto, shapePareto),
                InverseTransformParetoSampler.of(RandomAssert.createRNG(), scalePareto, shapePareto));

            // Stable distributions.
            // Gaussian case: alpha=2
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, 0, Math.sqrt(2)),
                StableSampler.of(RandomAssert.createRNG(), 2, 0));
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(unusedRng, 3.4, 0.75 * Math.sqrt(2)),
                    StableSampler.of(RandomAssert.createRNG(), 2, 0, 0.75, 3.4));
            // Cauchy case: alpha=1, beta=0, gamma=2.73, delta=0.87
            add(LIST, new org.apache.commons.math3.distribution.CauchyDistribution(unusedRng, 0.87, 2.73),
                StableSampler.of(RandomAssert.createRNG(), 1, 0, 2.73, 0.87));
            // Levy case: alpha=0.5, beta=0, gamma=5.7, delta=-1.23
            // The 0-parameterization requires the reference distribution (1-parameterization) is shifted:
            // S0(Z) = S1(Z) + gamma * beta * tan(pi * alpha / 2); gamma = 5.7, beta = -1, alpha = 0.5
            // = gamma * -1 * tan(pi/4) = -gamma
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, -1.23 - 5.7, 5.7),
                StableSampler.of(RandomAssert.createRNG(), 0.5, 1.0, 5.7, -1.23));
            // Levy case: alpha=0.5, beta=0.
            // The 0-parameterization requires the reference distribution is shifted by -tan(pi/4) = -1
            add(LIST, new org.apache.commons.math3.distribution.LevyDistribution(unusedRng, -1.0, 1.0),
                    StableSampler.of(RandomAssert.createRNG(), 0.5, 1.0, 1.0, 0.0));

            // No Stable distribution in Commons Math: Deciles computed using Nolan's STABLE program:
            // https://edspace.american.edu/jpnolan/stable/

            // General case (alpha > 1): alpha=1.3, beta=0.4, gamma=1.5, delta=-6.4
            add(LIST, new double[] {-8.95069776039550, -7.89186827865320, -7.25070352695719, -6.71497820795024,
                -6.19542020516881, -5.63245847779003, -4.94643432673952, -3.95462242999135,
                -1.90020994991840, Double.POSITIVE_INFINITY},
                StableSampler.of(RandomAssert.createRNG(), 1.3, 0.4, 1.5, -6.4));
            // General case (alpha < 1): alpha=0.8, beta=-0.3, gamma=0.75, delta=3.25
            add(LIST, new double[] {-1.60557902637291, 1.45715153372767, 2.39577970333297, 2.86274746879986,
                3.15907259287483, 3.38633464572309, 3.60858199662215, 3.96001854555454, 5.16261950198042,
                Double.POSITIVE_INFINITY},
                StableSampler.of(RandomAssert.createRNG(), 0.8, -0.3, 0.75, 3.25));
            // Alpha 1 case: alpha=1.0, beta=0.3
            add(LIST, new double[] {-2.08189340389400, -0.990511737972781, -0.539025554211755, -0.204710171216492,
                0.120388569770401, 0.497197960523146, 1.01228394387185, 1.89061920660563, 4.20559140293206,
                Double.POSITIVE_INFINITY},
                StableSampler.of(RandomAssert.createRNG(), 1.0, 0.3));
            // Symmetric case (beta=0): alpha=1.3, beta=0.0
            add(LIST, new double[] {-2.29713832179280, -1.26781259700375, -0.739212223404616, -0.346771353386198,
                0.00000000000000, 0.346771353386198, 0.739212223404616, 1.26781259700376, 2.29713832179280,
                Double.POSITIVE_INFINITY},
                StableSampler.of(RandomAssert.createRNG(), 1.3, 0.0));

            // This is the smallest alpha where the CDF can be reliably computed.
            // Small alpha case: alpha=0.1, beta=-0.2
            add(LIST, new double[] {-14345498.0855558, -4841.68845914421, -22.6430159400915, -0.194461655962062,
                0.299822962206354E-1, 0.316768853375197E-1, 0.519382255860847E-1, 21.8595769961580,
                147637.033822552, Double.POSITIVE_INFINITY},
                StableSampler.of(RandomAssert.createRNG(), 0.1, -0.2));

            // T ("inverse method").
            final double dofT = 0.76543;
            add(LIST, new org.apache.commons.math3.distribution.TDistribution(unusedRng, dofT),
                RandomAssert.createRNG());
            // T.
            add(LIST, new org.apache.commons.math3.distribution.TDistribution(unusedRng, dofT),
                TSampler.of(RandomAssert.createRNG(), dofT));
            // T with 'large' degrees of freedom.
            final double dofTlarge = 30;
            add(LIST, new org.apache.commons.math3.distribution.TDistribution(unusedRng, dofTlarge),
                TSampler.of(RandomAssert.createRNG(), dofTlarge));
            // T with 'huge' degrees of freedom (approaches a normal distribution).
            // Deciles are computed incorrectly using Commons Math; values computed using Matlab.
            // Note: DF is below the switch to using a normal distribution.
            final double dofTHuge = 1e15;
            add(LIST, new double[] {-1.2815515655446015, -0.84162123357291463, -0.52440051270804089,
                -0.25334710313579983, 0, 0.25334710313579983, 0.52440051270804089, 0.84162123357291474,
                1.2815515655446015, Double.POSITIVE_INFINITY},
                TSampler.of(RandomAssert.createRNG(), dofTHuge));

            // Triangular ("inverse method").
            final double aTriangle = -0.76543;
            final double cTriangle = -0.65432;
            final double bTriangle = -0.54321;
            add(LIST, new org.apache.commons.math3.distribution.TriangularDistribution(unusedRng, aTriangle, cTriangle, bTriangle),
                RandomAssert.createRNG());

            // Uniform ("inverse method").
            final double loUniform = -1.098;
            final double hiUniform = 0.76;
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                RandomAssert.createRNG());
            // Uniform.
            add(LIST, new org.apache.commons.math3.distribution.UniformRealDistribution(unusedRng, loUniform, hiUniform),
                ContinuousUniformSampler.of(RandomAssert.createRNG(), loUniform, hiUniform));

            // Weibull ("inverse method").
            final double alphaWeibull = 678.9;
            final double betaWeibull = 98.76;
            add(LIST, new org.apache.commons.math3.distribution.WeibullDistribution(unusedRng, alphaWeibull, betaWeibull),
                RandomAssert.createRNG());
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
    private static void add(List<ContinuousSamplerTestData> list,
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
        list.add(new ContinuousSamplerTestData(inverseMethodSampler,
                                               getDeciles(dist)));
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param sampler Sampler.
     */
    private static void add(List<ContinuousSamplerTestData> list,
                            final org.apache.commons.math3.distribution.RealDistribution dist,
                            final ContinuousSampler sampler) {
        list.add(new ContinuousSamplerTestData(sampler,
                                               getDeciles(dist)));
    }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param deciles Deciles of the given distribution.
     * @param sampler Sampler.
     */
    private static void add(List<ContinuousSamplerTestData> list,
                            final double[] deciles,
                            final ContinuousSampler sampler) {
        list.add(new ContinuousSamplerTestData(sampler,
                                               deciles));
    }

    /**
     * Subclasses that are "parametric" tests can forward the call to
     * the "@Parameters"-annotated method to this method.
     *
     * @return the list of all generators.
     */
    public static Iterable<ContinuousSamplerTestData> list() {
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
