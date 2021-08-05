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

Apache Commons RNG Examples JMH Benchmark
=========================================

Code for running benchmarks that assess the performance of the random generators and samplers
in Commons RNG.

Build the package using:

    mvn package -Pexamples-jmh

The Java Microbenchmark Harness ([JMH](http://openjdk.java.net/projects/code-tools/jmh/))
is used to run the benchmarks. Options to control the benchmark
can be detailed using the help flag:

    java -jar target/examples-jmh.jar -h

For example controlling the length of each benchmark, the number of iterations and warm-up
iterations, or the results format.

Executing the jar with no options will run all benchmarks. This will take a long time.
The source code is organised into packages that reflect the module structure of Commons RNG.
Browse the source for `core`, `sampling` and `simple` to see what benchmarks are available.
Benchmarks can be targeted by using a regular expression naming the benchmark class and optionally
the method.

By default JMH will output results to the console. To save to file use the `-rff` option and specify
the format using the `-rf` option.

Examples
--------

Run a benchmark of the `UniformRandomProvider.nextDouble` method for all random providers saving the
results to file in JSON format:

    java -jar target/examples-jmh.jar NextDoubleGenerationPerformance -rf json -rff out.json

Run a benchmark of the `UniformRandomProvider.nextInt` method for the named random providers:

    java -jar target/examples-jmh.jar NextIntGen.*nextInt$ -p randomSourceName=MSWS,PCG_MCG_XSH_RS_32

Run a benchmark of the `ContinuousSampler.sample` method for the named sampler using all random providers:

    java -jar target/examples-jmh.jar ContinuousSamplersPerformance.sample -p samplerType=ZigguratNormalizedGaussianSampler

Run a benchmark of `DiscreteUniformSampler.sample` or `UniformRandomProvider.nextInt(int)` for a
specified number of samples for a given range. As the number of samples increases the use of a
sampler is preferred over the use of the raw provider due to optimisations allowed during sampler
construction.

    java -jar target/examples-jmh.jar DiscreteUniformSamplerGenerationPerformance -p samples=1000,10000 -p upperBound=155

Compare the speed of generation of Gaussian samples using the named random provider. The relative
speed of the two implementations of the ziggurat method can vary significantly across JDK version
and target platform. The `ZigguratSampler.NormalisedGaussian` is more consistent in performance and
currently the preferred Gaussian sampler.

    java -jar target/examples-jmh.jar NextGaussianPerformance -p randomSourceName=XO_RO_SHI_RO_128_PP
