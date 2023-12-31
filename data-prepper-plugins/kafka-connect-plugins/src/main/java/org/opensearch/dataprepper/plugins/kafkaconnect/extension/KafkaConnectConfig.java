/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.kafkaconnect.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.dataprepper.plugins.kafka.configuration.AuthConfig;
import org.opensearch.dataprepper.plugins.kafka.configuration.AwsConfig;
import org.opensearch.dataprepper.plugins.kafka.configuration.EncryptionConfig;
import org.opensearch.dataprepper.plugins.kafka.util.KafkaClusterAuthConfig;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class KafkaConnectConfig implements KafkaClusterAuthConfig {
    private static final long CONNECTOR_TIMEOUT_MS = 360000L; // 360 seconds
    private static final long CONNECT_TIMEOUT_MS = 60000L; // 60 seconds

    @JsonProperty("worker_properties")
    private WorkerProperties workerProperties = new WorkerProperties();

    @JsonProperty("connect_start_timeout")
    private Duration connectStartTimeout = Duration.ofMillis(CONNECT_TIMEOUT_MS);

    @JsonProperty("connector_start_timeout")
    private Duration connectorStartTimeout = Duration.ofMillis(CONNECTOR_TIMEOUT_MS);

    @JsonProperty("bootstrap_servers")
    private List<String> bootstrapServers;

    private AuthConfig authConfig;
    private EncryptionConfig encryptionConfig;
    private AwsConfig awsConfig;

    public Duration getConnectStartTimeout() {
        return connectStartTimeout;
    }

    public Duration getConnectorStartTimeout() {
        return connectorStartTimeout;
    }

    public void setBootstrapServers(final List<String> bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        if (Objects.nonNull(bootstrapServers)) {
            this.workerProperties.setBootstrapServers(String.join(",", bootstrapServers));;
        }
    }

    public void setAuthProperties(final Properties authProperties) {
        this.workerProperties.setAuthProperties(authProperties);
    }

    public void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public void setAwsConfig(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }

    public void setEncryptionConfig(EncryptionConfig encryptionConfig) {
        this.encryptionConfig = encryptionConfig;
    }

    public WorkerProperties getWorkerProperties() {
        return workerProperties;
    }

    @Override
    public AwsConfig getAwsConfig() {
        return awsConfig;
    }

    @Override
    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    @Override
    public EncryptionConfig getEncryptionConfig() {
        return encryptionConfig;
    }

    @Override
    public List<String> getBootstrapServers() {
        if (Objects.nonNull(bootstrapServers)) {
            return bootstrapServers;
        }
        return null;
    }
}
