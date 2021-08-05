<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

Apache Commons RNG Examples JPMS Integration Test
=================================================

The JPMS integration test uses the Common RNG packages in a JPMS application.

Build the packages using:

    mvn package

To run the JPMS application use the script [runApp.sh](runApp.sh). This has a hard-coded
value for the version of the packaged jars. Ensure the `RNGVERSION` value is correct.

The application contains a game where players roll a six sided die. Each round
a player will roll the die a number of times and sum the score. The number of rolls for
a player in a round is taken from a Gaussian distribution. The game has a configurable
number of players and rounds. All sampling is done using a named RNG. The application
thus links together the `client-api`, `core`, `simple` and `sampling` JPMS modules from Commons RNG.

Set the parameters within the `runApp.sh` script and run the application using:

    ./runApp.sh

This will output the results of the dice game:

    --- Game 1 ---
    Player 3 has 244 points
    Player 2 has 226 points
    Player 1 has 219 points
    Player 4 has 186 points
    
    --- Game 2 ---
    Player 4 has 188 points
    Player 2 has 180 points
    Player 3 has 173 points
    Player 1 has 152 points
    
    --- Game 3 ---
    Player 3 has 235 points
    Player 1 has 211 points
    Player 2 has 188 points
    Player 4 has 164 points
