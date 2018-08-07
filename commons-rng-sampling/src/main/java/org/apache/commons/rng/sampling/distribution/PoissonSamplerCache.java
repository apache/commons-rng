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

import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.PoissonSampler;
import org.apache.commons.rng.sampling.distribution.LargeMeanPoissonSampler.LargeMeanPoissonSamplerState;

/**
 * Create a sampler for the
 * <a href="http://mathworld.wolfram.com/PoissonDistribution.html">Poisson
 * distribution</a> using a cache to minimise construction cost.
 * <p>
 * The cache will return a sampler equivalent to
 * {@link org.apache.commons.rng.sampling.distribution#PoissonSampler(UniformRandomProvider, double)}.
 * <p>
 * The cache allows the {@link PoissonSampler} construction cost to be minimised
 * for low size Poisson samples. The cache is advantageous under the following
 * conditions:
 * <ul>
 * <li>The mean of the Poisson distribution falls within a known range.</li>
 * <li>The sample size to be made with the <strong>same</strong> sampler is
 * small.</li>
 * </ul>
 * <p>
 * If the sample size to be made with the <strong>same</strong> sampler is large
 * then the construction cost is minimal compared to the sampling time.
 * <p>
 * Performance improvement is dependent on the speed of the
 * {@link UniformRandomProvider}. A fast provider can obtain a two-fold speed
 * improvement for a single-use Poisson sampler.
 * <p>
 * The cache is thread safe. Note that concurrent threads using the cache
 * must ensure a thread safe {@link UniformRandomProvider} is used when creating
 * samplers, e.g. a unique sampler per thread.
 */
public class PoissonSamplerCache {

    /**
     * The minimum N covered by the cache where
     * {@code N = (int)Math.floor(mean)}.
     */
    private final int minN;
    /**
     * The maximum N covered by the cache where
     * {@code N = (int)Math.floor(mean)}.
     */
    private final int maxN;
    /** The cache of states between {@link minN} and {@link maxN}. */
    private final AtomicReferenceArray<LargeMeanPoissonSamplerState> values;

    /**
     * @param minMean The minimum mean covered by the cache.
     * @param maxMean The maximum mean covered by the cache.
     * @throws IllegalArgumentException if {@code maxMean < minMean}
     */
    public PoissonSamplerCache(double minMean,
                               double maxMean) {

        // Although a mean of 0 is invalid for a Poisson sampler this case
        // is handled to make the cache user friendly. Any low means will
        // be handled by the SmallMeanPoissonSampler and not cached.
        if (minMean < 0) {
            minMean = 0;
        }
        checkMeanRange(minMean, maxMean);

        // The cache can only be used for the LargeMeanPoissonSampler.
        if (maxMean < PoissonSampler.PIVOT) {
            // The upper limit is too small so no cache will be used.
            // This class will just construct new samplers.
            minN = 0;
            maxN = 0;
            values = null;
        } else {
            // Convert the mean into integers.
            // Note the minimum is clipped to the algorithm switch point.
            this.minN = (int) Math.floor(Math.max(minMean, PoissonSampler.PIVOT));
            this.maxN = (int) Math.floor(maxMean);
            values = new AtomicReferenceArray<LargeMeanPoissonSamplerState>(
                    maxN - minN + 1);
        }
    }

    /**
     * @param minN   The minimum N covered by the cache where {@code N = (int)Math.floor(mean)}.
     * @param maxN   The maximum N covered by the cache where {@code N = (int)Math.floor(mean)}.
     * @param states The precomputed states.
     */
    private PoissonSamplerCache(int minN,
                                int maxN,
                                LargeMeanPoissonSamplerState[] states) {
        this.minN = minN;
        this.maxN = maxN;
        this.values = new AtomicReferenceArray<LargeMeanPoissonSamplerState>(states);
    }

    /**
     * Check the mean range.
     *
     * @param minMean The minimum mean covered by the cache.
     * @param maxMean The maximum mean covered by the cache.
     * @throws IllegalArgumentException if {@code maxMean < minMean}
     */
    private static void checkMeanRange(double minMean, double maxMean)
    {
        // Allow minMean == maxMean so that the cache can be used across
        // concurrent threads to create samplers with distinct RNGs and the
        // same mean.
        if (maxMean < minMean) {
            throw new IllegalArgumentException(
                    "Max mean: " + maxMean + " < " + minMean);
        }
    }

    /**
     * Creates a Poisson sampler. The returned sampler will function exactly the
     * same as
     * {@link org.apache.commons.rng.sampling.distribution#PoissonSampler(UniformRandomProvider, double)}.
     * <p>
     * A value of {@code mean} outside the range of the cache is valid.
     *
     * @param rng  Generator of uniformly distributed random numbers.
     * @param mean Mean.
     * @return A Poisson sampler
     * @throws IllegalArgumentException if {@code mean <= 0}.
     */
    public DiscreteSampler getPoissonSampler(UniformRandomProvider rng,
                                             double mean) {
        // Ensure the same functionality as the PoissonSampler by
        // using a SmallMeanPoissonSampler under the switch point.
        if (mean < PoissonSampler.PIVOT) {
            return new SmallMeanPoissonSampler(rng, mean);
        }

        // Convert the mean into an integer.
        final int n = (int) Math.floor(mean);
        // Check maxN first as the cache is likely to be used from min=0
        if (n > maxN || n < minN) {
            // Outside the range of the cache.
            return new LargeMeanPoissonSampler(rng, mean);
        }

        // Look in the cache for a state that can be reused.
        // Note: The cache is offset by minN.
        final int index = n - minN;
        // From the java.util.concurrent.atomic Javadoc:
        // get has the memory effects of reading a volatile variable.
        LargeMeanPoissonSamplerState state = values.get(index);
        if (state == null) {
            // Compute and store for reuse
            state = LargeMeanPoissonSamplerState.create(n);
            // Set this but do not worry about strict ordering
            // as would be imposed for .set(int, Object) since any later
            // objects that may be written by other threads will be the same.
            // Allows concurrent threads to set the state without
            // excess synchronisation over the exact object that is stored.
            //
            // From the java.util.concurrent.atomic Javadoc:
            // lazySet has the memory effects of writing (assigning) a volatile
            // variable except that it permits reorderings with subsequent (but
            // not previous) memory actions that do not themselves impose
            // reordering constraints with ordinary non-volatile writes.
            values.lazySet(index, state);
        }
        // Compute the remaining fraction of the mean
        final double lambdaFractional = mean - n;
        return new LargeMeanPoissonSampler(rng, state, lambdaFractional);
    }

    /**
     * Check if the mean is within the range where the cache can minimise the
     * construction cost of the {@link PoissonSampler}.
     *
     * @param mean
     *            the mean
     * @return true, if within the cache range
     */
    public boolean withinRange(double mean) {
        if (mean < PoissonSampler.PIVOT) {
            // Construction is optimal
            return true;
        }
        // Convert the mean into an integer.
        final int n = (int) Math.floor(mean);
        return n <= maxN && n >= minN;
    }

    /**
     * Checks if the cache covers a valid range of mean values.
     * <p>
     * Note that the cache is only valid for one of the Poisson sampling
     * algorithms. In the instance that a range was requested that was too
     * low then there is nothing to cache and this functions returns
     * {@code false}.
     * <p>
     * The cache can still be used to create a {@link PoissonSampler} using
     * {@link #getPoissonSampler(UniformRandomProvider, double)}.
     * <p>
     * This method can be used to determine if the cache has value.
     *
     * @return true, if the cache covers a range of mean values
     */
    public boolean isValidRange() {
        return values != null;
    }

    /**
     * Gets the minimum mean covered by the cache.
     * <p>
     * This value is the inclusive lower bound and is equal to
     * the lowest integer-valued mean that is covered by the cache.
     * <p>
     * Note that this value may not match the value passed to the constructor
     * due to the following reasons:
     * <ul>
     *  <li>At small mean values a different algorithm is used for Poisson
     *      sampling and the cache is unnecessary.</li>
     *  <li>The minimum is always an integer so may be below the constructor
     *      minimum mean.</li>
     * </ul>
     * <p>
     * If {@link #isValidRange()} returns {@code true} the cache will store
     * state to reduce construction cost of samplers in
     * the range {@link #getMinMean()} inclusive to {@link #getMaxMean()}
     * inclusive. Otherwise this method returns 0;
     *
     * @return The minimum mean covered by the cache.
     */
    public double getMinMean()
    {
        return minN;
    }

    /**
     * Gets the maximum mean covered by the cache.
     * <p>
     * This value is the inclusive upper bound and is equal to
     * the double value below the first integer-valued mean that is
     * above range covered by the cache.
     * <p>
     * Note that this value may not match the value passed to the constructor
     * due to the following reasons:
     * <ul>
     *  <li>At small mean values a different algorithm is used for Poisson
     *      sampling and the cache is unnecessary.</li>
     *  <li>The maximum is always the double value below an integer so
     *      may be above the constructor maximum mean.</li>
     * </ul>
     * <p>
     * If {@link #isValidRange()} returns {@code true} the cache will store
     * state to reduce construction cost of samplers in
     * the range {@link #getMinMean()} inclusive to {@link #getMaxMean()}
     * inclusive. Otherwise this method returns 0;
     *
     * @return The maximum mean covered by the cache.
     */
    public double getMaxMean()
    {
        if (isValidRange()) {
            return Math.nextAfter(maxN + 1.0, -1);
        }
        return 0;
    }

    /**
     * Create a new {@link PoissonSamplerCache} with the given range
     * reusing the current cache values.
     * <p>
     * This will create a new object even if the range is smaller or the
     * same as the current cache.
     *
     * @param minMean The minimum mean covered by the cache.
     * @param maxMean The maximum mean covered by the cache.
     * @throws IllegalArgumentException if {@code maxMean < minMean}
     * @return the poisson sampler cache
     */
    public PoissonSamplerCache withRange(double minMean,
                                         double maxMean) {
        if (values == null) {
            // Nothing to reuse
            return new PoissonSamplerCache(minMean, maxMean);
        }
        if (minMean < 0) {
            minMean = 0;
        }
        checkMeanRange(minMean, maxMean);

        // The cache can only be used for the LargeMeanPoissonSampler.
        if (maxMean < PoissonSampler.PIVOT) {
            return new PoissonSamplerCache(0, 0);
        }

        // Convert the mean into integers.
        // Note the minimum is clipped to the algorithm switch point.
        int nextMinN = (int) Math.floor(Math.max(minMean, PoissonSampler.PIVOT));
        int nextMaxN = (int) Math.floor(maxMean);
        LargeMeanPoissonSamplerState[] states =
                new LargeMeanPoissonSamplerState[nextMaxN - nextMinN + 1];

        // Preserve values from the current array to the next
        int currentIndex;
        int nextIndex;
        if (this.minN <= nextMinN) {
            // The current array starts before the next array
            currentIndex = nextMinN - this.minN;
            nextIndex = 0;
        } else {
            // The next array starts before the current array
            currentIndex = 0;
            nextIndex = this.minN - nextMinN;
        }
        while (currentIndex < values.length() && nextIndex < states.length) {
            LargeMeanPoissonSamplerState state = values.get(currentIndex);
            if (state != null) {
                // Validate with assert
                assert nextMinN + nextIndex == state.getLambda() :
                    String.format("Unexpected lambda value: expected <%d>, found <%d>",
                                  nextMinN + nextIndex,
                                  state.getLambda());
                states[nextIndex] = state;
            }
            currentIndex++;
            nextIndex++;
        }

        return new PoissonSamplerCache(nextMinN, nextMaxN, states);
    }
}
