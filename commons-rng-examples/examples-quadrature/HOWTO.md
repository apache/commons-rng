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

Apache Commons RNG Examples Quadrature
======================================

The quadrature test application will output an approximation for &pi; (3.14159...) using
Monte-Carlo integration. The application generates random points within a unit box and counts
the number of points within the unit circle. The approximation to &pi; is made using the ratio of
the points within the circle to the total number of points, which should be &pi;/4.

Build the application using:

        mvn package -P examples-quadrature

Run the application using 2 arguments:

1. The number of random points
2. The name of the random generator

For example:

        java -jar target/examples-quadrature.jar 1000000 KISS

This will output:

        After generating 2000000 random numbers, the error on |𝛑 - 3.14224| is 6.473464102070281E-4
