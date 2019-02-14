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

Apache Commons RNG Stress Test Example
===================

The stress test module contains an application for calling external tools that perform stringent
uniformity tests. The following shows an example of how to run DieHarder and TestU01.

Installation on Linux
---------------------

This assumes that the source code has been checked out and the current working directory is
`commons-rng-examples/examples-stress`.

### TestU01

This can be installed from [Test01](http://simul.iro.umontreal.ca/testu01/tu01.html) or using
the available packages:

        > apt-get install libtestu01 libtestu01-dev testu01-bin

A simple bridge is provided to read the integers from `stdin` and pass them to the TestU01 library.
This can be compiled using:

        > gcc src/main/c/stdin2testu01.c -o stdin2testu01 -ltestu01 -lprobdist -lmylib -lm

### DieHarder

This can be install from [DieHarder](http://webhome.phy.duke.edu/~rgb/General/dieharder.php) or
using the available package:

        > apt-get install dieharder

The `dieharder` executable can read integers from `stdin` for analysis.

Running on Linux
----------------

Build the `examples-stress` jar file:

        > mvn package -P examples-stress

This will create a single jar file with all the dependencies in the `target` directory.

The jar file requires the following arguments:

| Argument  | Description | Example |
| --------- | ----------- | ------- |
| path | Output filename prefix | target/dh_ |
| n | Number of processors | 4 |
| GeneratorsList class | Fully qualified class name of a provider for the supported random generators | org.apache.commons.rng.examples.stress.(GeneratorsList/IntGeneratorsList/LongGeneratorsList) |
| executable | The test tool | dieharder, stdin2testu01 |
| ... | Arguments for the test tool | dieharder: -a -g 200 -Y 1 -k 2, stdin2testu01: SmallCrush, Crush, BigCrush |

### TestU01

        > java -jar target/examples-stress.jar target/tu_ 4 org.apache.commons.rng.examples.stress.GeneratorsList ./stdin2testu01 BigCrush

### DieHarder

        > java -jar target/examples-stress.jar target/dh_ 4 org.apache.commons.rng.examples.stress.GeneratorsList /usr/bin/dieharder -a -g 200 -Y 1 -k 2
