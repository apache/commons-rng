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

import org.apache.commons.rng.core.util.NumberFactory;

/**
 * Converts a {@code Long} to an {@code Integer}.
 *
 * @since 1.0
 */
public class Long2Int implements SeedConverter<Long, Integer> {
    /** Create an instance. */
    public Long2Int() {}

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    public Integer convert(Long seed) {
        return NumberFactory.makeInt(seed);
    }
}
