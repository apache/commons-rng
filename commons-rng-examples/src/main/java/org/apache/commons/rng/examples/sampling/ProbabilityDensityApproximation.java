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

package org.apache.commons.rng.examples.sampling;

import java.io.PrintWriter;
import java.io.IOException;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.BoxMullerNormalizedGaussianSampler;
import org.apache.commons.rng.sampling.distribution.ChengBetaSampler;
import org.apache.commons.rng.sampling.distribution.AhrensDieterExponentialSampler;
import org.apache.commons.rng.sampling.distribution.AhrensDieterMarsagliaTsangGammaSampler;
import org.apache.commons.rng.sampling.distribution.InverseTransformParetoSampler;
import org.apache.commons.rng.sampling.distribution.LogNormalSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousUniformSampler;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;

/**
 * Approximation of the probability density by the histogram of the sampler output.
 */
public class ProbabilityDensityApproximation {
    /** Number of (equal-width) bins in the histogram. */
    private final int numBins;
    /** Number of samples to be generated. */
    private final long numSamples;

    /**
     * Application.
     *
     * @param numBins Number of "equal-width" bins.
     * @param numSamples Number of samples.
     */
    private ProbabilityDensityApproximation(int numBins,
                                            long numSamples) {
        this.numBins = numBins;
        this.numSamples = numSamples;
    }

    /**
     * @param sampler Sampler.
     * @param min Right abscissa of the first bin: every sample smaller
     * than that value will increment an additional bin (of infinite width)
     * placed before the first "equal-width" bin.
     * @param Left abscissa of the last bin: every sample larger than or
     * equal to that value will increment an additional bin (of infinite
     * width) placed after the last "equal-width" bin.
     * @param output Filename.
     */
    private void createDensity(ContinuousSampler sampler,
                               double min,
                               double max,
                               String outputFile)
        throws IOException {
        final double binSize = (max - min) / numBins;
        final long[] histogram = new long[numBins];

        long n = 0;
        long belowMin = 0;
        long aboveMax = 0;
        while (++n < numSamples) {
            final double r = sampler.sample();

            if (r < min) {
                ++belowMin;
                continue;
            }

            if (r >= max) {
                ++aboveMax;
                continue;
            }

            final int binIndex = (int) ((r - min) / binSize);
            ++histogram[binIndex];
        }

        final double binHalfSize = 0.5 * binSize;
        final double norm = 1 / (binSize * numSamples);

        final PrintWriter out = new PrintWriter(outputFile);
        out.println("# Sampler: " + sampler);
        out.println("# Number of bins: " + numBins);
        out.println("# Min: " + min + " (fraction of samples below: " + (belowMin / (double) numSamples) + ")");
        out.println("# Max: " + max + " (fraction of samples above: " + (aboveMax / (double) numSamples) + ")");
        out.println("# Bin width: " + binSize);
        out.println("# Histogram normalization factor: " + norm);
        out.println("#");
        out.println("# " + (min - binHalfSize) + " " + (belowMin * norm));
        for (int i = 0; i < numBins; i++) {
            out.println((min + (i + 1) * binSize - binHalfSize) + " " + (histogram[i] * norm));
        }
        out.println("# " + (max + binHalfSize) + " " + (aboveMax * norm));
        out.close();
    }

    /**
     * Program entry point.
     *
     * @param args Argument. They must be provided, in the following order:
     * <ol>
     *  <li>Number of "equal-width" bins.</li>
     *  <li>Number of samples.</li>
     * </ol>
     * @throws IOException if failure occurred while writing to files.
     */
    public static void main(String[] args)
        throws IOException {
        final int numBins = Integer.valueOf(args[0]);
        final long numSamples = Long.valueOf(args[1]);
        final ProbabilityDensityApproximation app = new ProbabilityDensityApproximation(numBins, numSamples);

        final UniformRandomProvider rng = RandomSource.create(RandomSource.XOR_SHIFT_1024_S);

        final double gaussMean = 1;
        final double gaussSigma = 2;
        final double gaussMin = -9;
        final double gaussMax = 11;
        app.createDensity(new GaussianSampler(new ZigguratNormalizedGaussianSampler(rng),
                                              gaussMean, gaussSigma),
                          gaussMin, gaussMax, "gauss.ziggurat.txt");
        app.createDensity(new GaussianSampler(new MarsagliaNormalizedGaussianSampler(rng),
                                              gaussMean, gaussSigma),
                          gaussMin, gaussMax, "gauss.marsaglia.txt");
        app.createDensity(new GaussianSampler(new BoxMullerNormalizedGaussianSampler(rng),
                                              gaussMean, gaussSigma),
                          gaussMin, gaussMax, "gauss.boxmuller.txt");

        final double alphaBeta = 4.3;
        final double betaBeta = 2.1;
        final double betaMin = 0;
        final double betaMax = 1;
        app.createDensity(new ChengBetaSampler(rng, alphaBeta, betaBeta),
                          betaMin, betaMax, "beta.case1.txt");
        final double alphaBetaAlt = 0.5678;
        final double betaBetaAlt = 0.1234;
        app.createDensity(new ChengBetaSampler(rng, alphaBetaAlt, betaBetaAlt),
                          betaMin, betaMax, "beta.case2.txt");

        final double meanExp = 3.45;
        final double expMin = 0;
        final double expMax = 60;
        app.createDensity(new AhrensDieterExponentialSampler(rng, meanExp),
                          expMin, expMax, "exp.txt");

        final double thetaGammaSmallerThanOne = 0.1234;
        final double alphaGamma = 3.456;
        final double gammaMin = 0;
        final double gammaMax1 = 40;
        app.createDensity(new AhrensDieterMarsagliaTsangGammaSampler(rng, alphaGamma, thetaGammaSmallerThanOne),
                          gammaMin, gammaMax1, "gamma.case1.txt");
        final double thetaGammaLargerThanOne = 2.345;
        final double gammaMax2 = 70;
        app.createDensity(new AhrensDieterMarsagliaTsangGammaSampler(rng, alphaGamma, thetaGammaLargerThanOne),
                          gammaMin, gammaMax2, "gamma.case2.txt");

        final double scalePareto = 23.45;
        final double shapePareto = 0.789;
        final double paretoMin = 23;
        final double paretoMax = 400;
        app.createDensity(new InverseTransformParetoSampler(rng, scalePareto, shapePareto),
                          paretoMin, paretoMax, "pareto.txt");

        final double loUniform = -9.876;
        final double hiUniform = 5.432;
        app.createDensity(new ContinuousUniformSampler(rng, loUniform, hiUniform),
                          loUniform, hiUniform, "uniform.txt");

        final double scaleLogNormal = 2.345;
        final double shapeLogNormal = 0.1234;
        final double logNormalMin = 5;
        final double logNormalMax = 25;
        app.createDensity(new LogNormalSampler(new ZigguratNormalizedGaussianSampler(rng),
                                               scaleLogNormal, shapeLogNormal),
                          logNormalMin, logNormalMax, "lognormal.ziggurat.txt");
        app.createDensity(new LogNormalSampler(new MarsagliaNormalizedGaussianSampler(rng),
                                               scaleLogNormal, shapeLogNormal),
                          logNormalMin, logNormalMax, "lognormal.marsaglia.txt");
        app.createDensity(new LogNormalSampler(new BoxMullerNormalizedGaussianSampler(rng),
                                               scaleLogNormal, shapeLogNormal),
                          logNormalMin, logNormalMax, "lognormal.boxmuller.txt");
    }
}
