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
 * Data store for {@link ContinuousSamplerParametricTest}.
 */
class ContinuousSamplerTestData {
    private final ContinuousSampler sampler;
    private final double[] deciles;

    public ContinuousSamplerTestData(ContinuousSampler sampler,
                                     double[] deciles) {
        this.sampler = sampler;
        this.deciles = deciles.clone();
    }

    public ContinuousSampler getSampler() {
        return sampler;
    }

    public double[] getDeciles() {
        return deciles.clone();
    }

    @Override
    public String toString() {
        return sampler.toString() + ": deciles=" + Arrays.toString(deciles);
    }
}
