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

package org.apache.commons.rng.examples.jpms.app;

import java.util.Arrays;
import java.util.Comparator;
import java.lang.module.ModuleDescriptor;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.rng.examples.jpms.lib.DiceGame;

/**
 * Test that "commons-rng-simple" can be used as a module in Java 9.
 */
public class DiceGameApplication {
    /** Line separator. */
    private static final String LINE_SEP = System.getProperty("line.separator");
    /** Required functionality. */
    private final DiceGame game;

    /**
     * Application.
     *
     * @param numPlayers Number of players.
     * @param numRounds Number of rounds per game.
     * @param identifier RNG algorithm identifier.
     */
    private DiceGameApplication(int numPlayers,
                                int numRounds,
                                RandomSource identifier) {
        game = new DiceGame(numPlayers, numRounds,
                            RandomSource.create(identifier),
                            4.3, 2.1);
    }

    /**
     * Application's entry point.
     *
     * @param args Arguments, in the following order
     * <ol>
     *  <li>Number of games</li>
     *  <li>Number of players</li>
     *  <li>Number of rounds per game</li>
     *  <li>RNG {@link RandomSource indentifier}</li>
     * </ol>
     */
    public static void main(String[] args) {
        final int numGames = Integer.parseInt(args[0]);
        final DiceGameApplication app = new DiceGameApplication(Integer.parseInt(args[1]),
                                                                Integer.parseInt(args[2]),
                                                                RandomSource.valueOf(args[3]));

        app.displayModuleInfo();

        for (int i = 1; i <= numGames; i++) {
            System.out.println("--- Game " + i + " ---");
            System.out.println(display(app.game.play()));
        }
    }

    /**
     * Display the list of players in decreasing order of scores.
     *
     * @param scores Scores returned by {@link #play()}.
     * @return a text diplaying the result of the game.
     */
    private static String display(int[] scores) {
        final int[][] a = new int[scores.length][2];
        for (int i = 0; i < scores.length; i++) {
            a[i][0] = i;
            a[i][1] = scores[i];
        }
        Arrays.sort(a, Comparator.comparingInt(x -> -x[1]));

        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < scores.length; i++) {
            result.append("Player ").append(a[i][0] + 1)
                .append(" has ").append(a[i][1])
                .append(" points").append(LINE_SEP);
        }

        return result.toString();
    }

    /**
     * Display JPMS information.
     */
    private void displayModuleInfo() {
        final StringBuilder str = new StringBuilder();

        for (Module mod : new Module[] { DiceGame.class.getModule(),
                                         DiceGameApplication.class.getModule() }) {
            System.out.println("--- " + mod + " ---");
            final ModuleDescriptor desc = mod.getDescriptor();

            for (ModuleDescriptor.Requires r : desc.requires()) {
                System.out.println(mod.getName() + " requires " + r.name());
            }

            System.out.println();
        }
    }
}
