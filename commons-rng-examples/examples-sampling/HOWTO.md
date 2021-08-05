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

Apache Commons RNG Examples Sampling
====================================

The sampling test application will compute histograms of the sampling output from distribution
samplers.

Build the application using:

        mvn package -P examples-sampling

The application contains help information describing the usage. Obtain the help by running the
application with the `-h` flag to obtain the available commands and to obtain information for
the commands:

        java -jar target/examples-sampling.jar -h
        java -jar target/examples-sampling.jar density -h
        java -jar target/examples-sampling.jar visual -h

The `density` command computes a histogram of the distribution and uses this to create an
approximation of the probability denssity function for the distribution. The data is recorded to
a file named using the sampler. For example to output a histogram of a
`ZigguratGaussianSampler` to the file `pdf.gauss.ziggurat.txt` use:

        java -jar target/examples-sampling.jar density -s ZigguratGaussianSampler

The `visual` command computes samples from Gaussian random samplers and outputs the samples
that fall within a very small range. The lower limit of the range is specified and the number
of IEEE 754 double values above the lower limit is used to specify the sampled range.
For example to sample the 1000 double values above 0.1:

        java -jar target/examples-sampling.jar visual -b 1000 -s 10
