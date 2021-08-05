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

Apache Commons RNG Examples Stress Utilities
============================================

Application for calling external tools that perform stringent uniformity tests.

Build the package using:

    mvn package -Pexamples-stress

The application contains help information describing the usage. Obtain the help by running the
application with the `-h` flag to obtain the available commands and to obtain information for
the commands:

    java -jar target/examples-stress.jar -h
    java -jar target/examples-stress.jar stress -h

The principle command of the application is the `stress` command. The command will create
random generators from Commons RNG and pass the raw byte output to a program that will test
uniformity. The program must be installed separately. Examples of how to run the stress test using
**Dieharder**, **TestU01** and **PractRand** is described in detail on the
[stress_test](stress_test.md) page.

Passing byte output to an external program that reads 4 bytes or 8 bytes as 32-bits or 64-bits
requires knowledge of the endianness of the current runtime platform.
This is described in detail on the [endianness](endianness.md) page.

The application also provides the `output` command to print output from a named random generator
in a variety of formats. To create a random file of 16K (2 buffers of 8196 bytes) using the
`JSF_64` generator:

    java -jar target/examples-stress.jar output JSF_64 -f BINARY -n 2 > target/raw

To output numbers from the `KISS` generator as raw bits, integers and unsigned integers:

    java -jar target/examples-stress.jar output -f BITS -x 0123456789abcdef

Since the seed is provided this output should be reproducible across platforms:

    10011001 11100000 01000011 01101011  2581611371 -1713355925
    01010110 01011101 00110011 10110010  1448948658  1448948658
    11101001 10001111 11000110 11000000  3918513856  -376453440
    10111101 00110111 11110101 11100000  3174561248 -1120406048
    11100100 10111001 00110011 11111001  3837342713  -457624583
    01010110 01111110 01000011 01001111  1451115343  1451115343
    11011011 00000100 00111111 11011011  3674488795  -620478501
    11010101 11100011 00111011 01101000  3588438888  -706528408
    11110011 11001001 11100000 10001010  4090093706  -204873590
    01011111 10000001 10001111 00000100  1602326276  1602326276
