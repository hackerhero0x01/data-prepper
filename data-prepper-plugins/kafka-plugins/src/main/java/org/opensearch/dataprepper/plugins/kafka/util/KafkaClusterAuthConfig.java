/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.kafka.util;

import org.opensearch.dataprepper.plugins.kafka.configuration.AuthConfig;
import org.opensearch.dataprepper.plugins.kafka.configuration.AwsConfig;
import org.opensearch.dataprepper.plugins.kafka.configuration.EncryptionConfig;

import java.util.Collection;

public interface KafkaClusterAuthConfig {
    AwsConfig getAwsConfig();

    AuthConfig getAuthConfig();

    EncryptionConfig getEncryptionConfig();

    Collection<String> getBootstrapServers();
}
