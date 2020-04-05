/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.metrics;

import java.util.function.Function;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;

/**
 * Defines metric factory API. Returned metrics are
 * properly registered.
 */
public interface PrometheusMetricsRegistry {

    Counter counter(String name, String help);

    Gauge gauge(String name, String help);

    Summary summary(String name, String help);

    Histogram histogram(String name, String help);

    Counter counter(String name, String help, Function<Counter.Builder, Counter.Builder> configure);

    Gauge gauge(String name, String help, Function<Gauge.Builder, Gauge.Builder> configure);

    Summary summary(String name, String help, Function<Summary.Builder, Summary.Builder> configure);

    Histogram histogram(String name, String help, Function<Histogram.Builder, Histogram.Builder> configure);

}
