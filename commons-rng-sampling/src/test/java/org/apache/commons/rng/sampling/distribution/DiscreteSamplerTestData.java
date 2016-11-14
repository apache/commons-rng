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

/**
 * Data store for {@link DiscreteSamplerParametricTest}.
 */
class DiscreteSamplerTestData {
    private final DiscreteSampler sampler;
    private final int[] points;
    private final double[] probabilities;

    public DiscreteSamplerTestData(DiscreteSampler sampler,
                                   int[] points,
                                   double[] probabilities) {
        this.sampler = sampler;
        this.points = points.clone();
        this.probabilities = probabilities.clone();
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

    @Override
    public String toString() {
        final int len = points.length;
        final String[] p = new String[len];
        for (int i = 0; i < len; i++) {
            p[i] = "p(" + points[i] + ")=" + probabilities[i];
        }
        return sampler.toString() + ": " + Arrays.toString(p);
    }
}
