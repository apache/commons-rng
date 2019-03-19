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
package org.apache.commons.rng.examples.stress;

import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

/**
 * List of generators.
 */
public class GeneratorsList implements Iterable<UniformRandomProvider> {
    /**
     * The RandomSource values to ignore when auto generating the list
     * from the enumeration.
     */
    private static final EnumSet<RandomSource> toIgnore =
            EnumSet.of(RandomSource.TWO_CMRES_SELECT);

    /** List of generators. */
    private final List<UniformRandomProvider> list = new ArrayList<>();

    /**
     * Creates the list.
     */
    public GeneratorsList() {
        // Auto-generate using the order of the RandomSource enum
        for (final RandomSource source : RandomSource.values()) {
            // Ignore those generators known to take arguments
            if (toIgnore.contains(source)) {
                continue;
            }
            // Currently we cannot detect if the source requires arguments,
            // e.g. RandomSource.TWO_CMRES_SELECT. So try and create
            // using no arguments and allow this to throw
            // IllegalStateException if it cannot be created.
            //
            // Implementation note:
            // Handle such generators by adding to the ignore set and
            // if they must be included add as a special case below.
            list.add(RandomSource.create(source));
        }

        // --------------------------------------------------------------------
        // Note:
        // Add any special cases that cannot be created without arguments here.
        // --------------------------------------------------------------------
        // The list must then be sorted using the order defined by
        // RandomSource.ordinal(). Currently this is not required so
        // has not been implemented.
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<UniformRandomProvider> iterator() {
        return list.iterator();
    }
}
