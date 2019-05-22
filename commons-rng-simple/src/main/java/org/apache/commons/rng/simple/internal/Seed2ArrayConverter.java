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
 * Seed converter to create an output array type.
 *
 * @param <IN> Input seed type.
 * @param <OUT> Output seed type.
 *
 * @since 1.3
 */
public interface Seed2ArrayConverter<IN, OUT> extends SeedConverter<IN, OUT> {
    /**
     * Converts seed from input type to output type. The output type is expected to be an array.
     *
     * @param seed Original seed value.
     * @param outputSize Output size.
     * @return the converted seed value.
     */
    OUT convert(IN seed, int outputSize);
}
