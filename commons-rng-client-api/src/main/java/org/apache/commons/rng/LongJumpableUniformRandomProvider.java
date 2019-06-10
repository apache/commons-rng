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
package org.apache.commons.rng;

/**
 * Applies to generators that can be advanced a very large number of
 * steps of the output sequence in a single operation.
 *
 * @since 1.3
 */
public interface LongJumpableUniformRandomProvider extends JumpableUniformRandomProvider {
    /**
     * Creates a copy of the JumpableUniformRandomProvider and then advances the
     * state of the current instance. The copy is returned.
     *
     * <p>The current state will be advanced in a single operation by the equivalent of a
     * number of sequential calls to a method that updates the state of the provider. The
     * size of the long jump is implementation dependent.</p>
     *
     * <p>Repeat invocations of this method will create a series of generators
     * that are uniformly spaced at intervals of the output sequence. Each generator provides
     * non-overlapping output for the length of the long jump for use in parallel computations.</p>
     *
     * <p>The returned copy may be jumped {@code m / n} times before overlap with the current
     * instance where {@code m} is the long jump length and {@code n}
     * is the jump length of the {@link #jump()} method.
     *
     * @return A copy of the current state.
     */
    JumpableUniformRandomProvider longJump();
}
