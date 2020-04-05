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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import io.prometheus.client.Summary;
//import io.prometheus.client.hotspot.GarbageCollectorExports;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptySet;

/** Servlet that provides metric values in openmetrics format.
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = PrometheusMetricsServletConfig.class, factory=true)
public class PrometheusMetricsServlet extends HttpServlet {

    private static final long serialVersionUID = 1091923523250301446L;

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricsServlet.class);

    static final String FORMAT_HTML = "html";

    private String[] servletPaths;

    private boolean disabled;

    @Reference
    private PrometheusMetricsRegistry metricRegistry;

    @Reference
    private HttpService httpService;

    private Summary doGetTiming;


    @Activate
    protected final void activate(final PrometheusMetricsServletConfig configuration) {
        String servletPath = configuration.servletPath();
        LOG.info("servletPath={}", servletPath);

        this.disabled = configuration.disabled();
        LOG.info("disabled={}", disabled);

        if (disabled) {
            LOG.info("Health Check Servlet is disabled by configuration");
            return;
        }

        doGetTiming = metricRegistry.summary("metrics_servlet", "request probe for metrics servlet");
        // agk: The following introduces com.sun.management dependency
        // that I don't know how to resolve
//        GarbageCollectorExports gcCollector = new GarbageCollectorExports();
//        ((PrometheusMetricsRegistryImpl) metricRegistry).register(gcCollector);

        Map<String, HttpServlet> servletsToRegister = new LinkedHashMap<>();
        servletsToRegister.put(servletPath, this);

        for (final Map.Entry<String, HttpServlet> servlet : servletsToRegister.entrySet()) {
            try {
                LOG.info("Registering HC servlet {} to path {}", getClass().getSimpleName(), servlet.getKey());
                this.httpService.registerServlet(servlet.getKey(), servlet.getValue(), null, null);
            } catch (Exception e) {
                LOG.error("Could not register health check servlet: " + e, e);
            }
        }
        this.servletPaths = servletsToRegister.keySet().toArray(new String[0]);
    }

    @Deactivate
    public void deactivate(final ComponentContext componentContext) {
        if (disabled || this.servletPaths == null) {
            return;
        }

        for (final String servletPath : this.servletPaths) {
            try {
                LOG.debug("Unregistering path {}", servletPath);
                this.httpService.unregister(servletPath);
            } catch (Exception e) {
                LOG.error("Could not unregister health check servlet: " + e, e);
            }
        }
        this.servletPaths = null;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try (Summary.Timer ignore = doGetTiming.startTimer()) {
            resp.setContentType("text/plain");
            ((PrometheusMetricsRegistryImpl) metricRegistry).writeReport(resp.getWriter(), emptySet());
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doGet(req, resp);
    }

}
