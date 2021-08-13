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

package org.apache.commons.rng.sampling;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.AliasMethodDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteUniformSampler;
import org.apache.commons.rng.sampling.distribution.GuideTableDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.LongSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaTsangWangDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateContinuousSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateDiscreteSampler;
import org.apache.commons.rng.sampling.distribution.SharedStateLongSampler;

/**
 * Factory class to create a sampler that combines sampling from multiple samplers.
 *
 * <p>The composite sampler is constructed using a {@link Builder builder} for the type of samplers
 * that will form the composite. Each sampler has a weight in the composition.
 * Samples are returned using a 2 step algorithm:
 *
 * <ol>
 *  <li>Select a sampler based on its weighting
 *  <li>Return a sample from the selected sampler
 * </ol>
 *
 * <p>The weights used for each sampler create a discrete probability distribution. This is
 * sampled using a discrete probability distribution sampler. The builder provides methods
 * to change the default implementation.
 *
 * <p>The following example will create a sampler to uniformly sample the border of a triangle
 * using the line segment lengths as weights:
 *
 * <pre>
 * UniformRandomProvider rng = RandomSource.KISS.create();
 * double[] a = {1.23, 4.56};
 * double[] b = {6.78, 9.01};
 * double[] c = {3.45, 2.34};
 * ObjectSampler&lt;double[]&gt; sampler =
 *     CompositeSamplers.&lt;double[]&gt;newObjectSamplerBuilder()
 *         .add(LineSampler.of(rng, a, b), Math.hypot(a[0] - b[0], a[1] - b[1]))
 *         .add(LineSampler.of(rng, b, c), Math.hypot(b[0] - c[0], b[1] - c[1]))
 *         .add(LineSampler.of(rng, c, a), Math.hypot(c[0] - a[0], c[1] - a[1]))
 *         .build(rng);
 * </pre>
 *
 * @since 1.4
 */
public final class CompositeSamplers {
    /**
     * A factory for creating a sampler of a user-defined
     * <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">
     * discrete probability distribution</a>.
     */
    public interface DiscreteProbabilitySamplerFactory {
        /**
         * Creates the sampler.
         *
         * @param rng Source of randomness.
         * @param probabilities Discrete probability distribution.
         * @return the sampler
         */
        DiscreteSampler create(UniformRandomProvider rng,
                               double[] probabilities);
    }

    /**
     * The DiscreteProbabilitySampler class defines implementations that sample from a user-defined
     * <a href="http://en.wikipedia.org/wiki/Probability_distribution#Discrete_probability_distribution">
     * discrete probability distribution</a>.
     *
     * <p>All implementations support the {@link SharedStateDiscreteSampler} interface.
     */
    public enum DiscreteProbabilitySampler implements DiscreteProbabilitySamplerFactory {
        /** Sample using a guide table (see {@link GuideTableDiscreteSampler}). */
        GUIDE_TABLE {
            @Override
            public SharedStateDiscreteSampler create(UniformRandomProvider rng, double[] probabilities) {
                return GuideTableDiscreteSampler.of(rng, probabilities);
            }
        },
        /** Sample using the alias method (see {@link AliasMethodDiscreteSampler}). */
        ALIAS_METHOD {
            @Override
            public SharedStateDiscreteSampler create(UniformRandomProvider rng, double[] probabilities) {
                return AliasMethodDiscreteSampler.of(rng, probabilities);
            }
        },
        /**
         * Sample using an optimised look-up table (see
         * {@link org.apache.commons.rng.sampling.distribution.MarsagliaTsangWangDiscreteSampler.Enumerated
         * MarsagliaTsangWangDiscreteSampler.Enumerated}).
         */
        LOOKUP_TABLE {
            @Override
            public SharedStateDiscreteSampler create(UniformRandomProvider rng, double[] probabilities) {
                return MarsagliaTsangWangDiscreteSampler.Enumerated.of(rng, probabilities);
            }
        };
    }

    /**
     * A class to implement the SharedStateDiscreteSampler interface for a discrete probability
     * sampler given a factory and the probability distribution. Each new instance will recreate
     * the distribution sampler using the factory.
     */
    private static class SharedStateDiscreteProbabilitySampler implements SharedStateDiscreteSampler {
        /** The sampler. */
        private final DiscreteSampler sampler;
        /** The factory to create a new discrete sampler. */
        private final DiscreteProbabilitySamplerFactory factory;
        /** The probabilities. */
        private final double[] probabilities;

        /**
         * @param sampler Sampler of the discrete distribution.
         * @param factory Factory to create a new discrete sampler.
         * @param probabilities Probabilities of the discrete distribution.
         * @throws NullPointerException if the {@code sampler} is null
         */
        SharedStateDiscreteProbabilitySampler(DiscreteSampler sampler,
                                              DiscreteProbabilitySamplerFactory factory,
                                              double[] probabilities) {
            this.sampler = Objects.requireNonNull(sampler, "discrete sampler");
            // Assume the factory and probabilities are not null
            this.factory = factory;
            this.probabilities = probabilities;
        }

        @Override
        public int sample() {
            // Delegate
            return sampler.sample();
        }

        @Override
        public SharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
            // The factory may destructively modify the probabilities
            return new SharedStateDiscreteProbabilitySampler(factory.create(rng, probabilities.clone()),
                                                             factory, probabilities);
        }
    }

    /**
     * Builds a composite sampler.
     *
     * <p>A composite sampler is a combination of multiple samplers
     * that all return the same sample type. Each sampler has a weighting in the composition.
     * Samples are returned using a 2 step algorithm:
     *
     * <ol>
     *  <li>Select a sampler based on its weighting
     *  <li>Return a sample from the selected sampler
     * </ol>
     *
     * <p>Step 1 requires a discrete sampler constructed from a discrete probability distribution.
     * The probability for each sampler is the sampler weight divided by the sum of the weights:
     * <pre>
     * p(i) = w(i) / sum(w)
     * </pre>
     *
     * <p>The builder provides a method to set the factory used to generate the discrete sampler.
     *
     * @param <S> Type of sampler
     */
    public interface Builder<S> {
        /**
         * Return the number of samplers in the composite. The size must be non-zero before
         * the {@link #build(UniformRandomProvider) build} method can create a sampler.
         *
         * @return the size
         */
        int size();

        /**
         * Adds the sampler to the composite. A sampler with a zero weight is ignored.
         *
         * @param sampler Sampler.
         * @param weight Weight for the composition.
         * @return a reference to this builder
         * @throws IllegalArgumentException if {@code weight} is negative, infinite or {@code NaN}.
         * @throws NullPointerException if {@code sampler} is null.
         */
        Builder<S> add(S sampler, double weight);

        /**
         * Sets the factory to use to generate the composite's discrete sampler from the sampler
         * weights.
         *
         * <p>Note: If the factory is not explicitly set then a default will be used.
         *
         * @param factory Factory.
         * @return a reference to this builder
         * @throws NullPointerException if {@code factory} is null.
         */
        Builder<S> setFactory(DiscreteProbabilitySamplerFactory factory);

        /**
         * Builds the composite sampler. The {@code rng} is the source of randomness for selecting
         * which sampler to use for each sample.
         *
         * <p>Note: When the sampler is created the builder is reset to an empty state.
         * This prevents building multiple composite samplers with the same samplers and
         * their identical underlying source of randomness.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @return the sampler
         * @throws IllegalStateException if no samplers have been added to create a composite.
         * @see #size()
         */
        S build(UniformRandomProvider rng);
    }

    /**
     * Builds a composite sampler.
     *
     * <p>A single builder can be used to create composites of different implementing classes
     * which support different sampler interfaces. The type of sampler is generic. The individual
     * samplers and their weights can be collected by the builder. The build method creates
     * the discrete probability distribution from the weights. The final composite is created
     * using a factory to create the class.
     *
     * @param <S> Type of sampler
     */
    private static class SamplerBuilder<S> implements Builder<S> {
        /** The specialisation of the sampler. */
        private final Specialisation specialisation;
        /** The weighted samplers. */
        private final List<WeightedSampler<S>> weightedSamplers;
        /** The factory to create the discrete probability sampler from the weights. */
        private DiscreteProbabilitySamplerFactory factory;
        /** The factory to create the composite sampler. */
        private final SamplerFactory<S> compositeFactory;

        /**
         * The specialisation of composite sampler to build.
         * This is used to determine if specialised interfaces from the sampler
         * type must be supported, e.g. {@link SharedStateSampler}.
         */
        enum Specialisation {
            /** Instance of {@link SharedStateSampler}. */
            SHARED_STATE_SAMPLER,
            /** No specialisation. */
            NONE;
        }

        /**
         * A factory for creating composite samplers.
         *
         * <p>This interface is used to build concrete implementations
         * of different sampler interfaces.
         *
         * @param <S> Type of sampler
         */
        interface SamplerFactory<S> {
            /**
             * Creates a new composite sampler.
             *
             * <p>If the composite specialisation is a
             * {@link Specialisation#SHARED_STATE_SAMPLER shared state sampler}
             * the discrete sampler passed to this method will be an instance of
             * {@link SharedStateDiscreteSampler}.
             *
             * @param discreteSampler Discrete sampler.
             * @param samplers Samplers.
             * @return the sampler
             */
            S createSampler(DiscreteSampler discreteSampler,
                            List<S> samplers);
        }

        /**
         * Contains a weighted sampler.
         *
         * @param <S> Sampler type
         */
        private static class WeightedSampler<S> {
            /** The weight. */
            private final double weight;
            /** The sampler. */
            private final S sampler;

            /**
             * @param weight the weight
             * @param sampler the sampler
             * @throws IllegalArgumentException if {@code weight} is negative, infinite or {@code NaN}.
             * @throws NullPointerException if {@code sampler} is null.
             */
            WeightedSampler(double weight, S sampler) {
                this.weight = requirePositiveFinite(weight, "weight");
                this.sampler = Objects.requireNonNull(sampler, "sampler");
            }

            /**
             * Gets the weight.
             *
             * @return the weight
             */
            double getWeight() {
                return weight;
            }

            /**
             * Gets the sampler.
             *
             * @return the sampler
             */
            S getSampler() {
                return sampler;
            }

            /**
             * Checks that the specified value is positive finite and throws a customized
             * {@link IllegalArgumentException} if it is not.
             *
             * @param value the value
             * @param message detail message to be used in the event that a {@code
             *                IllegalArgumentException} is thrown
             * @return {@code value} if positive finite
             * @throws IllegalArgumentException if {@code weight} is negative, infinite or {@code NaN}.
             */
            private static double requirePositiveFinite(double value, String message) {
                // Must be positive finite
                if (!(value >= 0 && value < Double.POSITIVE_INFINITY)) {
                    throw new IllegalArgumentException(message + " is not positive finite: " + value);
                }
                return value;
            }
        }

        /**
         * @param specialisation Specialisation of the sampler.
         * @param compositeFactory Factory to create the final composite sampler.
         */
        SamplerBuilder(Specialisation specialisation,
                       SamplerFactory<S> compositeFactory) {
            this.specialisation = specialisation;
            this.compositeFactory = compositeFactory;
            weightedSamplers = new ArrayList<>();
            factory = DiscreteProbabilitySampler.GUIDE_TABLE;
        }

        @Override
        public int size() {
            return weightedSamplers.size();
        }

        @Override
        public Builder<S> add(S sampler, double weight) {
            // Ignore zero weights. The sampler and weight are validated by the WeightedSampler.
            if (weight != 0) {
                weightedSamplers.add(new WeightedSampler<>(weight, sampler));
            }
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>If the weights are uniform the factory is ignored and composite's discrete sampler
         * is a {@link DiscreteUniformSampler uniform distribution sampler}.
         */
        @Override
        public Builder<S> setFactory(DiscreteProbabilitySamplerFactory samplerFactory) {
            this.factory = Objects.requireNonNull(samplerFactory, "factory");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>If only one sampler has been added to the builder then the sampler is returned
         * and the builder is reset.
         *
         * @throws IllegalStateException if no samplers have been added to create a composite.
         */
        @Override
        public S build(UniformRandomProvider rng) {
            final List<WeightedSampler<S>> list = this.weightedSamplers;
            final int n = list.size();
            if (n == 0) {
                throw new IllegalStateException("No samplers to build the composite");
            }
            if (n == 1) {
                // No composite
                final S sampler = list.get(0).sampler;
                reset();
                return sampler;
            }

            // Extract the weights and samplers.
            final double[] weights = new double[n];
            final ArrayList<S> samplers = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                final WeightedSampler<S> weightedItem = list.get(i);
                weights[i] = weightedItem.getWeight();
                samplers.add(weightedItem.getSampler());
            }

            reset();

            final DiscreteSampler discreteSampler = createDiscreteSampler(rng, weights);

            return compositeFactory.createSampler(discreteSampler, samplers);
        }

        /**
         * Reset the builder.
         */
        private void reset() {
            weightedSamplers.clear();
        }

        /**
         * Creates the discrete sampler of the enumerated probability distribution.
         *
         * <p>If the specialisation is a {@link Specialisation#SHARED_STATE_SAMPLER shared state sampler}
         * the discrete sampler will be an instance of {@link SharedStateDiscreteSampler}.
         *
         * @param rng Generator of uniformly distributed random numbers.
         * @param weights Weight associated to each item.
         * @return the sampler
         */
        private DiscreteSampler createDiscreteSampler(UniformRandomProvider rng,
                                                      double[] weights) {
            // Edge case. Detect uniform weights.
            final int n = weights.length;
            if (uniform(weights)) {
                // Uniformly sample from the size.
                // Note: Upper bound is inclusive.
                return DiscreteUniformSampler.of(rng, 0, n - 1);
            }

            // If possible normalise with a simple sum.
            final double sum = sum(weights);
            if (sum < Double.POSITIVE_INFINITY) {
                // Do not use f = 1.0 / sum and multiplication by f.
                // Use of divide handles a sub-normal sum.
                for (int i = 0; i < n; i++) {
                    weights[i] /= sum;
                }
            } else {
                // The sum is not finite. We know the weights are all positive finite.
                // Compute the mean without overflow and divide by the mean and number of items.
                final double mean = mean(weights);
                for (int i = 0; i < n; i++) {
                    // Two step division avoids using the denominator (mean * n)
                    weights[i] = weights[i] / mean / n;
                }
            }

            // Create the sampler from the factory.
            // Check if a SharedStateSampler is required.
            // If a default factory then the result is a SharedStateDiscreteSampler,
            // otherwise the sampler must be checked.
            if (specialisation == Specialisation.SHARED_STATE_SAMPLER &&
                !(factory instanceof DiscreteProbabilitySampler)) {
                // If the factory was user defined then clone the weights as they may be required
                // to create a SharedStateDiscreteProbabilitySampler.
                final DiscreteSampler sampler = factory.create(rng, weights.clone());
                return sampler instanceof SharedStateDiscreteSampler ?
                     sampler :
                     new SharedStateDiscreteProbabilitySampler(sampler, factory, weights);
            }

            return factory.create(rng, weights);
        }

        /**
         * Check if all the values are the same.
         *
         * <p>Warning: This method assumes there are input values. If the length is zero an
         * {@link ArrayIndexOutOfBoundsException} will be thrown.
         *
         * @param values the values
         * @return true if all values are the same
         */
        private static boolean uniform(double[] values) {
            final double value = values[0];
            for (int i = 1; i < values.length; i++) {
                if (value != values[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Compute the sum of the values.
         *
         * @param values the values
         * @return the sum
         */
        private static double sum(double[] values) {
            double sum = 0;
            for (final double value : values) {
                sum += value;
            }
            return sum;
        }

        /**
         * Compute the mean of the values. Uses a rolling algorithm to avoid overflow of a simple sum.
         * This method can be used to compute the mean of observed counts for normalisation to a
         * probability:
         *
         * <pre>
         * double[] values = ...;
         * int n = values.length;
         * double mean = mean(values);
         * for (int i = 0; i &lt; n; i++) {
         *     // Two step division avoids using the denominator (mean * n)
         *     values[i] = values[i] / mean / n;
         * }
         * </pre>
         *
         * <p>Warning: This method assumes there are input values. If the length is zero an
         * {@link ArrayIndexOutOfBoundsException} will be thrown.
         *
         * @param values the values
         * @return the mean
         */
        private static double mean(double[] values) {
            double mean = values[0];
            int i = 1;
            while (i < values.length) {
                // Deviation from the mean
                final double dev = values[i] - mean;
                i++;
                mean += dev / i;
            }
            return mean;
        }
    }

    /**
     * A composite sampler.
     *
     * <p>The source sampler for each sampler is chosen based on a user-defined continuous
     * probability distribution.
     *
     * @param <S> Type of sampler
     */
    private static class CompositeSampler<S> {
        /** Continuous sampler to choose the individual sampler to sample. */
        protected final DiscreteSampler discreteSampler;
        /** Collection of samplers to be sampled from. */
        protected final List<S> samplers;

        /**
         * @param discreteSampler Continuous sampler to choose the individual sampler to sample.
         * @param samplers Collection of samplers to be sampled from.
         */
        CompositeSampler(DiscreteSampler discreteSampler,
                         List<S> samplers) {
            this.discreteSampler = discreteSampler;
            this.samplers = samplers;
        }

        /**
         * Gets the next sampler to use to create a sample.
         *
         * @return the sampler
         */
        S nextSampler() {
            return samplers.get(discreteSampler.sample());
        }
    }

    /**
     * A factory for creating a composite ObjectSampler.
     *
     * @param <T> Type of sample
     */
    private static class ObjectSamplerFactory<T> implements
            SamplerBuilder.SamplerFactory<ObjectSampler<T>> {
        /** The instance. */
        @SuppressWarnings("rawtypes")
        private static final ObjectSamplerFactory INSTANCE = new ObjectSamplerFactory();

        /**
         * Get an instance.
         *
         * @param <T> Type of sample
         * @return the factory
         */
        @SuppressWarnings("unchecked")
        static <T> ObjectSamplerFactory<T> instance() {
            return (ObjectSamplerFactory<T>) INSTANCE;
        }

        @Override
        public ObjectSampler<T> createSampler(DiscreteSampler discreteSampler,
                                              List<ObjectSampler<T>> samplers) {
            return new CompositeObjectSampler<>(discreteSampler, samplers);
        }

        /**
         * A composite object sampler.
         *
         * @param <T> Type of sample
         */
        private static class CompositeObjectSampler<T>
                extends CompositeSampler<ObjectSampler<T>>
                implements ObjectSampler<T> {
            /**
             * @param discreteSampler Discrete sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeObjectSampler(DiscreteSampler discreteSampler,
                                   List<ObjectSampler<T>> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public T sample() {
                return nextSampler().sample();
            }
        }
    }

    /**
     * A factory for creating a composite SharedStateObjectSampler.
     *
     * @param <T> Type of sample
     */
    private static class SharedStateObjectSamplerFactory<T> implements
            SamplerBuilder.SamplerFactory<SharedStateObjectSampler<T>> {
        /** The instance. */
        @SuppressWarnings("rawtypes")
        private static final SharedStateObjectSamplerFactory INSTANCE = new SharedStateObjectSamplerFactory();

        /**
         * Get an instance.
         *
         * @param <T> Type of sample
         * @return the factory
         */
        @SuppressWarnings("unchecked")
        static <T> SharedStateObjectSamplerFactory<T> instance() {
            return (SharedStateObjectSamplerFactory<T>) INSTANCE;
        }

        @Override
        public SharedStateObjectSampler<T> createSampler(DiscreteSampler discreteSampler,
                                                         List<SharedStateObjectSampler<T>> samplers) {
            // The input discrete sampler is assumed to be a SharedStateDiscreteSampler
            return new CompositeSharedStateObjectSampler<>(
                (SharedStateDiscreteSampler) discreteSampler, samplers);
        }

        /**
         * A composite object sampler with shared state support.
         *
         * <p>The source sampler for each sampler is chosen based on a user-defined
         * discrete probability distribution.
         *
         * @param <T> Type of sample
         */
        private static class CompositeSharedStateObjectSampler<T>
                extends CompositeSampler<SharedStateObjectSampler<T>>
                implements SharedStateObjectSampler<T> {
            /**
             * @param discreteSampler Discrete sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeSharedStateObjectSampler(SharedStateDiscreteSampler discreteSampler,
                                              List<SharedStateObjectSampler<T>> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public T sample() {
                return nextSampler().sample();
            }

            @Override
            public CompositeSharedStateObjectSampler<T> withUniformRandomProvider(UniformRandomProvider rng) {
                // Duplicate each sampler with the same source of randomness
                return new CompositeSharedStateObjectSampler<>(
                    ((SharedStateDiscreteSampler) this.discreteSampler).withUniformRandomProvider(rng),
                    copy(samplers, rng));
            }
        }
    }

    /**
     * A factory for creating a composite DiscreteSampler.
     */
    private static class DiscreteSamplerFactory implements
            SamplerBuilder.SamplerFactory<DiscreteSampler> {
        /** The instance. */
        static final DiscreteSamplerFactory INSTANCE = new DiscreteSamplerFactory();

        @Override
        public DiscreteSampler createSampler(DiscreteSampler discreteSampler,
                                             List<DiscreteSampler> samplers) {
            return new CompositeDiscreteSampler(discreteSampler, samplers);
        }

        /**
         * A composite discrete sampler.
         */
        private static class CompositeDiscreteSampler
                extends CompositeSampler<DiscreteSampler>
                implements DiscreteSampler {
            /**
             * @param discreteSampler Discrete sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeDiscreteSampler(DiscreteSampler discreteSampler,
                                     List<DiscreteSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public int sample() {
                return nextSampler().sample();
            }
        }
    }

    /**
     * A factory for creating a composite SharedStateDiscreteSampler.
     */
    private static class SharedStateDiscreteSamplerFactory implements
            SamplerBuilder.SamplerFactory<SharedStateDiscreteSampler> {
        /** The instance. */
        static final SharedStateDiscreteSamplerFactory INSTANCE = new SharedStateDiscreteSamplerFactory();

        @Override
        public SharedStateDiscreteSampler createSampler(DiscreteSampler discreteSampler,
                                                        List<SharedStateDiscreteSampler> samplers) {
            // The input discrete sampler is assumed to be a SharedStateDiscreteSampler
            return new CompositeSharedStateDiscreteSampler(
                (SharedStateDiscreteSampler) discreteSampler, samplers);
        }

        /**
         * A composite discrete sampler with shared state support.
         */
        private static class CompositeSharedStateDiscreteSampler
                extends CompositeSampler<SharedStateDiscreteSampler>
                implements SharedStateDiscreteSampler {
            /**
             * @param discreteSampler Discrete sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeSharedStateDiscreteSampler(SharedStateDiscreteSampler discreteSampler,
                                                List<SharedStateDiscreteSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public int sample() {
                return nextSampler().sample();
            }

            @Override
            public CompositeSharedStateDiscreteSampler withUniformRandomProvider(UniformRandomProvider rng) {
                // Duplicate each sampler with the same source of randomness
                return new CompositeSharedStateDiscreteSampler(
                    ((SharedStateDiscreteSampler) this.discreteSampler).withUniformRandomProvider(rng),
                    copy(samplers, rng));
            }
        }
    }

    /**
     * A factory for creating a composite ContinuousSampler.
     */
    private static class ContinuousSamplerFactory implements
            SamplerBuilder.SamplerFactory<ContinuousSampler> {
        /** The instance. */
        static final ContinuousSamplerFactory INSTANCE = new ContinuousSamplerFactory();

        @Override
        public ContinuousSampler createSampler(DiscreteSampler discreteSampler,
                                               List<ContinuousSampler> samplers) {
            return new CompositeContinuousSampler(discreteSampler, samplers);
        }

        /**
         * A composite continuous sampler.
         */
        private static class CompositeContinuousSampler
                extends CompositeSampler<ContinuousSampler>
                implements ContinuousSampler {
            /**
             * @param discreteSampler Continuous sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeContinuousSampler(DiscreteSampler discreteSampler,
                                       List<ContinuousSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public double sample() {
                return nextSampler().sample();
            }
        }
    }

    /**
     * A factory for creating a composite SharedStateContinuousSampler.
     */
    private static class SharedStateContinuousSamplerFactory implements
            SamplerBuilder.SamplerFactory<SharedStateContinuousSampler> {
        /** The instance. */
        static final SharedStateContinuousSamplerFactory INSTANCE = new SharedStateContinuousSamplerFactory();

        @Override
        public SharedStateContinuousSampler createSampler(DiscreteSampler discreteSampler,
                                                          List<SharedStateContinuousSampler> samplers) {
            // The sampler is assumed to be a SharedStateContinuousSampler
            return new CompositeSharedStateContinuousSampler(
                (SharedStateDiscreteSampler) discreteSampler, samplers);
        }

        /**
         * A composite continuous sampler with shared state support.
         */
        private static class CompositeSharedStateContinuousSampler
                extends CompositeSampler<SharedStateContinuousSampler>
                implements SharedStateContinuousSampler {
            /**
             * @param discreteSampler Continuous sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeSharedStateContinuousSampler(SharedStateDiscreteSampler discreteSampler,
                                                  List<SharedStateContinuousSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public double sample() {
                return nextSampler().sample();
            }

            @Override
            public CompositeSharedStateContinuousSampler withUniformRandomProvider(UniformRandomProvider rng) {
                // Duplicate each sampler with the same source of randomness
                return new CompositeSharedStateContinuousSampler(
                    ((SharedStateDiscreteSampler) this.discreteSampler).withUniformRandomProvider(rng),
                    copy(samplers, rng));
            }
        }
    }

    /**
     * A factory for creating a composite LongSampler.
     */
    private static class LongSamplerFactory implements
            SamplerBuilder.SamplerFactory<LongSampler> {
        /** The instance. */
        static final LongSamplerFactory INSTANCE = new LongSamplerFactory();

        @Override
        public LongSampler createSampler(DiscreteSampler discreteSampler,
                                         List<LongSampler> samplers) {
            return new CompositeLongSampler(discreteSampler, samplers);
        }

        /**
         * A composite long sampler.
         */
        private static class CompositeLongSampler
                extends CompositeSampler<LongSampler>
                implements LongSampler {
            /**
             * @param discreteSampler Long sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeLongSampler(DiscreteSampler discreteSampler,
                                 List<LongSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public long sample() {
                return nextSampler().sample();
            }
        }
    }

    /**
     * A factory for creating a composite SharedStateLongSampler.
     */
    private static class SharedStateLongSamplerFactory implements
            SamplerBuilder.SamplerFactory<SharedStateLongSampler> {
        /** The instance. */
        static final SharedStateLongSamplerFactory INSTANCE = new SharedStateLongSamplerFactory();

        @Override
        public SharedStateLongSampler createSampler(DiscreteSampler discreteSampler,
                                                    List<SharedStateLongSampler> samplers) {
            // The input discrete sampler is assumed to be a SharedStateLongSampler
            return new CompositeSharedStateLongSampler(
                (SharedStateDiscreteSampler) discreteSampler, samplers);
        }

        /**
         * A composite long sampler with shared state support.
         */
        private static class CompositeSharedStateLongSampler
                extends CompositeSampler<SharedStateLongSampler>
                implements SharedStateLongSampler {
            /**
             * @param discreteSampler Long sampler to choose the individual sampler to sample.
             * @param samplers Collection of samplers to be sampled from.
             */
            CompositeSharedStateLongSampler(SharedStateDiscreteSampler discreteSampler,
                                            List<SharedStateLongSampler> samplers) {
                super(discreteSampler, samplers);
            }

            @Override
            public long sample() {
                return nextSampler().sample();
            }

            @Override
            public CompositeSharedStateLongSampler withUniformRandomProvider(UniformRandomProvider rng) {
                // Duplicate each sampler with the same source of randomness
                return new CompositeSharedStateLongSampler(
                    ((SharedStateDiscreteSampler) this.discreteSampler).withUniformRandomProvider(rng),
                    copy(samplers, rng));
            }
        }
    }

    /** No public instances. */
    private CompositeSamplers() {}

    /**
     * Create a new builder for a composite {@link ObjectSampler}.
     *
     * <p>Note: If the compiler cannot infer the type parameter of the sampler it can be specified
     * within the diamond operator {@code <T>} preceding the call to
     * {@code newObjectSamplerBuilder()}, for example:
     *
     * <pre>{@code
     * CompositeSamplers.<double[]>newObjectSamplerBuilder()
     * }</pre>
     *
     * @param <T> Type of the sample.
     * @return the builder
     */
    public static <T> Builder<ObjectSampler<T>> newObjectSamplerBuilder() {
        final SamplerBuilder.SamplerFactory<ObjectSampler<T>> factory = ObjectSamplerFactory.instance();
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.NONE, factory);
    }

    /**
     * Create a new builder for a composite {@link SharedStateObjectSampler}.
     *
     * <p>Note: If the compiler cannot infer the type parameter of the sampler it can be specified
     * within the diamond operator {@code <T>} preceding the call to
     * {@code newSharedStateObjectSamplerBuilder()}, for example:
     *
     * <pre>{@code
     * CompositeSamplers.<double[]>newSharedStateObjectSamplerBuilder()
     * }</pre>
     *
     * @param <T> Type of the sample.
     * @return the builder
     */
    public static <T> Builder<SharedStateObjectSampler<T>> newSharedStateObjectSamplerBuilder() {
        final SamplerBuilder.SamplerFactory<SharedStateObjectSampler<T>> factory =
            SharedStateObjectSamplerFactory.instance();
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.SHARED_STATE_SAMPLER, factory);
    }

    /**
     * Create a new builder for a composite {@link DiscreteSampler}.
     *
     * @return the builder
     */
    public static Builder<DiscreteSampler> newDiscreteSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.NONE, DiscreteSamplerFactory.INSTANCE);
    }

    /**
     * Create a new builder for a composite {@link SharedStateDiscreteSampler}.
     *
     * @return the builder
     */
    public static Builder<SharedStateDiscreteSampler> newSharedStateDiscreteSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.SHARED_STATE_SAMPLER, SharedStateDiscreteSamplerFactory.INSTANCE);
    }

    /**
     * Create a new builder for a composite {@link ContinuousSampler}.
     *
     * @return the builder
     */
    public static Builder<ContinuousSampler> newContinuousSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.NONE, ContinuousSamplerFactory.INSTANCE);
    }

    /**
     * Create a new builder for a composite {@link SharedStateContinuousSampler}.
     *
     * @return the builder
     */
    public static Builder<SharedStateContinuousSampler> newSharedStateContinuousSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.SHARED_STATE_SAMPLER, SharedStateContinuousSamplerFactory.INSTANCE);
    }

    /**
     * Create a new builder for a composite {@link LongSampler}.
     *
     * @return the builder
     */
    public static Builder<LongSampler> newLongSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.NONE, LongSamplerFactory.INSTANCE);
    }

    /**
     * Create a new builder for a composite {@link SharedStateLongSampler}.
     *
     * @return the builder
     */
    public static Builder<SharedStateLongSampler> newSharedStateLongSamplerBuilder() {
        return new SamplerBuilder<>(
            SamplerBuilder.Specialisation.SHARED_STATE_SAMPLER, SharedStateLongSamplerFactory.INSTANCE);
    }

    /**
     * Create a copy instance of each sampler in the list of samplers using the given
     * uniform random provider as the source of randomness.
     *
     * @param <T> the type of sampler
     * @param samplers Source to copy.
     * @param rng Generator of uniformly distributed random numbers.
     * @return the copy
     */
    private static <T extends SharedStateSampler<T>> List<T> copy(List<T> samplers,
                                                                  UniformRandomProvider rng) {
        final ArrayList<T> newSamplers = new ArrayList<>(samplers.size());
        for (final T s : samplers) {
            newSamplers.add(s.withUniformRandomProvider(rng));
        }
        return newSamplers;
    }
}
