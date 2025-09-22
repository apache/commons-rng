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
package org.apache.commons.rng.simple.internal;


/**
 * Composes two {@link SeedConverter converters}.
 *
 * @param <T> Input seed type.
 * @param <S> Transitional seed type.
 * @param <R> Output seed type.
 *
 * @since 1.0
 */
public class SeedConverterComposer<T, S, R> implements SeedConverter<T, R> {
    /** First conversion. */
    private final SeedConverter<T, S> first;
    /** Second conversion. */
    private final SeedConverter<S, R> second;

    /**
     * Create an instance.
     *
     * @param first First conversion.
     * @param second second conversion.
     */
    public SeedConverterComposer(SeedConverter<T, S> first,
                                 SeedConverter<S, R> second) {
        this.first = first;
        this.second = second;
    }

    /** {@inheritDoc} */
    @Override
    public R convert(T seed) {
        final S trans = first.convert(seed);
        return second.convert(trans);
    }
}
