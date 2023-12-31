package org.opensearch.dataprepper.plugins.kafka.common.key;

import org.opensearch.dataprepper.plugins.kafka.common.aws.AwsContext;
import org.opensearch.dataprepper.plugins.kafka.configuration.TopicConfig;

import java.util.List;
import java.util.function.Supplier;

public class KeyFactory {
    private final List<InnerKeyProvider> orderedKeyProviders;

    public KeyFactory(AwsContext awsContext) {
        this(List.of(
                new KmsKeyProvider(awsContext),
                new UnencryptedKeyProvider()
        ));
    }

    KeyFactory(List<InnerKeyProvider> orderedKeyProviders) {
        this.orderedKeyProviders = orderedKeyProviders;
    }

    public Supplier<byte[]> getKeySupplier(TopicConfig topicConfig) {
        if (topicConfig.getEncryptionKey() == null)
            return null;

        InnerKeyProvider keyProvider = orderedKeyProviders
                .stream()
                .filter(innerKeyProvider -> innerKeyProvider.supportsConfiguration(topicConfig))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find an inner key provider. This is a programming error - UnencryptedKeyProvider should always work."));

        return () -> keyProvider.apply(topicConfig);
    }
}
