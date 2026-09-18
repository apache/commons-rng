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

import org.junit.jupiter.api.Assertions;

/**
 * Data store for {@link DiscreteSamplerParametricTest}.
 *
 * <p>By default the probabilities are for {@code p_i(x_i)} where {@code x_i} is the point.
 * The data should contain probabilities for the entire range of {@code x} expected to
 * be produced by the sampler.
 *
 * <p>The data can be used to represent a cumulative probability distribution. Points
 * must be sorted and the probabilities correspond to:
 * <pre>{@code
 *  P_0(X <= x_0)
 *  P_i+1(x_i < X <= x_i+1) , i in [0, n)
 * }</pre>
 */
class DiscreteSamplerTestData {
    private final DiscreteSampler sampler;
    private final int[] points;
    private final double[] probabilities;
    private final boolean range;

    DiscreteSamplerTestData(DiscreteSampler sampler,
                            int[] points,
                            double[] probabilities,
                            boolean range) {
        this.sampler = sampler;
        this.points = points.clone();
        this.probabilities = probabilities.clone();
        this.range = range;
        if (range) {
            for (int k = 1; k < points.length; k++) {
                Assertions.assertTrue(points[k - 1] < points[k],
                    "Points must be sorted for range probabilities");
            }
        }
    }

    public DiscreteSampler getSampler() {
        return sampler;
    }

    public int[] getPoints() {
        return points.clone();
    }

    public double[] getProbabilities() {
        return probabilities.clone();
    }

    /**
     * Check if the probabilities are for a range. Points must be sorted.
     *
     * @return true if the probabilities are for a range
     * @since 1.8
     */
    public boolean isRange() {
        return range;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(2048)
            .append(sampler.toString()).append(':');
        final int len = points.length;
        if (range) {
            sb.append(" p(<=").append(points[0]).append(")=")
                .append(probabilities[0]);
            for (int i = 1; i < len; i++) {
                // Use a half-open interval, e.g. p((4,5])=
                sb.append(" p((").append(points[i - 1]).append(',')
                    .append(points[i]).append("])=")
                    .append(probabilities[i]);
            }
        } else {
            for (int i = 0; i < len; i++) {
                sb.append(" p(").append(points[i]).append(")=")
                    .append(probabilities[i]);
            }
        }
        return sb.toString();
    }
}
