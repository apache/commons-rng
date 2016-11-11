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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.ContinuousSampler;
import org.apache.commons.rng.simple.RandomSource;

/**
 * List of samplers.
 */
public class ContinuousSamplersList {
    /** List of all RNGs implemented in the library. */
    private static final List<ContinuousSamplerTestData[]> LIST =
        new ArrayList<ContinuousSamplerTestData[]>();

    static {
        try {
            // List of distributions to test.

            // Gaussian ("inverse method").
            final double meanNormal = -123.45;
            final double sigmaNormal = 6.789;
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(meanNormal, sigmaNormal),
                RandomSource.create(RandomSource.KISS));
            // Gaussian ("Box-Muller").
            add(LIST, new org.apache.commons.math3.distribution.NormalDistribution(meanNormal, sigmaNormal),
                new BoxMullerGaussianSampler(RandomSource.create(RandomSource.MT), meanNormal, sigmaNormal));

        } catch (Exception e) {
            System.err.println("Unexpected exception while creating the list of samplers: " + e);
            e.printStackTrace(System.err);
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
            new InverseMethodContinuousSampler(rng,
                                               new ContinuousInverseCumulativeProbabilityFunction() {
                                                   @Override
                                                   public double inverseCumulativeProbability(double p) {
                                                       return dist.inverseCumulativeProbability(p);
                                                   }
                                               });
        list.add(new ContinuousSamplerTestData[] { new ContinuousSamplerTestData(inverseMethodSampler,
                                                                                 getDeciles(dist)) });
     }

    /**
     * @param list List of data (one the "parameters" tested by the Junit parametric test).
     * @param dist Distribution to which the samples are supposed to conform.
     * @param sampler Sampler.
     */
    private static void add(List<ContinuousSamplerTestData[]> list,
                            final org.apache.commons.math3.distribution.RealDistribution dist,
                            final ContinuousSampler sampler) {
        list.add(new ContinuousSamplerTestData[] { new ContinuousSamplerTestData(sampler,
                                                                                 getDeciles(dist)) });
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
