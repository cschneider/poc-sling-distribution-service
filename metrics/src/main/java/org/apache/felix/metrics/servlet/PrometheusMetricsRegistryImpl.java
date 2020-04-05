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
package org.apache.felix.metrics.servlet;

import org.apache.felix.metrics.PrometheusMetricsRegistry;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.Function;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.osgi.service.component.annotations.Component;

import static io.prometheus.client.exporter.common.TextFormat.write004;

@Component(service= PrometheusMetricsRegistry.class)
public class PrometheusMetricsRegistryImpl implements PrometheusMetricsRegistry {

    @Override
    public Counter counter(String name, String help) {
        return counter(name, help, identity());
    }

    @Override
    public Gauge gauge(String name, String help) {
        return gauge(name, help, identity());
    }

    @Override
    public Summary summary(String name, String help) {
        return summary(name, help, identity());
    }

    @Override
    public Histogram histogram(String name, String help) {
        return histogram(name, help, identity());
    }

    @Override
    public Counter counter(String name, String help, Function<Counter.Builder, Counter.Builder> configure) {
        Counter.Builder builder = Counter.build().name(name).help(help);
        return configure.apply(builder).register(registry);
    }

    @Override
    public Gauge gauge(String name, String help, Function<Gauge.Builder, Gauge.Builder> configure) {
        Gauge.Builder builder = Gauge.build().name(name).help(help);
        return configure.apply(builder).register(registry);
    }

    @Override
    public Summary summary(String name, String help, Function<Summary.Builder, Summary.Builder> configure) {
        Summary.Builder builder = Summary.build().name(name).help(help);
        return configure.apply(builder).register(registry);
    }

    @Override
    public Histogram histogram(String name, String help, Function<Histogram.Builder, Histogram.Builder> configure) {
        Histogram.Builder builder = Histogram.build().name(name).help(help);
        return configure.apply(builder).register(registry);
    }

    void register(Collector collector) {
        registry.register(collector);
    }

    void writeReport(Writer out, Set<String> filters) throws IOException {
         write004(out, registry.filteredMetricFamilySamples(filters));
    }

    private final CollectorRegistry registry = new CollectorRegistry(true);

    private static <T> Function<T, T> identity() {
        return t -> t;
    }
}
