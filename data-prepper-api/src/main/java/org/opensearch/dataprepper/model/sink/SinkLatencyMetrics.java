/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.model.sink;

import org.opensearch.dataprepper.metrics.PluginMetrics;
import io.micrometer.core.instrument.DistributionSummary;
import org.opensearch.dataprepper.model.event.EventHandle;

import java.time.Duration;
import java.time.Instant;

public class SinkLatencyMetrics {
    public static final String INTERNAL_LATENCY = "PipelineLatency";
    public static final String EXTERNAL_LATENCY = "EndToEndLatency";
    private final DistributionSummary internalLatencySummary;
    private final DistributionSummary externalLatencySummary;

    public SinkLatencyMetrics(PluginMetrics pluginMetrics) {
        internalLatencySummary = pluginMetrics.summary(INTERNAL_LATENCY);
        externalLatencySummary = pluginMetrics.summary(EXTERNAL_LATENCY);
    }
    public void update(final EventHandle eventHandle) {
        Instant now = Instant.now();
        internalLatencySummary.record(Duration.between(eventHandle.getInternalOriginationTime(), now).toMillis());
        if (eventHandle.getExternalOriginationTime() == null) {
            return;
        }
        externalLatencySummary.record(Duration.between(eventHandle.getExternalOriginationTime(), now).toMillis());
    }
}
