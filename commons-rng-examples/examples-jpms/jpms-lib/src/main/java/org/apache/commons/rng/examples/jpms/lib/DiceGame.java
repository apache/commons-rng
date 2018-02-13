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

package org.apache.commons.rng.examples.jpms.lib;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.ContinuousSampler;
import org.apache.commons.rng.sampling.distribution.GaussianSampler;
import org.apache.commons.rng.sampling.distribution.ZigguratNormalizedGaussianSampler;

/**
 * Example application.
 */
public class DiceGame {
    /** Underlying RNG. */
    private final UniformRandomProvider rng;
    /** Sampler. */
    private final ContinuousSampler sampler;
    /** Number of rounds in the game. */
    private final int rounds;
    /** Number of players in the game. */
    private int players;

    /**
     * @param players Number of players.
     * @param rounds Number of rounds.
     * @param mu Mean.
     * @param sigma Standard deviation.
     * @param rng RNG.
     */
    public DiceGame(int players,
                    int rounds,
                    UniformRandomProvider rng,
                    double mu,
                    double sigma) {
        this.rng = rng;
        this.rounds = rounds;
        this.players = players;
        sampler = new GaussianSampler(new ZigguratNormalizedGaussianSampler(rng), mu, sigma);
    }

    /**
     * Play a game.
     *
     * @return the scores of all the players.
     */
    public int[] play() {
        final int[] scores = new int[players];

        for (int i = 0; i < rounds; i++) {
            doRound(scores);
        }
        return scores;
    }

    /**
     * Play a round and update the scores.
     *
     * @param currentScores Scores of the players.
     */
    private void doRound(int[] currentScores) {
        for (int i = 0; i < players; i++) {
            currentScores[i] += roll();
        }
    }

    /**
     * @return the score of one round.
     */
    private int roll() {
        int score = 0;
        final int n = numberOfDice();
        for (int i = 0; i < n; i++) {
            score += singleRoll();
        }
        return score;
    }

    /**
     * @return a number between 1 and 6.
     */
    private int singleRoll() {
        return rng.nextInt(6);
    }

    /**
     * @return the number of dice to roll.
     */
    private int numberOfDice() {
        final double n = Math.round(sampler.sample());
        return n <= 0 ? 0 : (int) n;
    }
}
