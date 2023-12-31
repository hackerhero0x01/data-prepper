/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.model.sink;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Data Prepper Output Codec Context class.
 * The context contains information that are shared and may be used among {@link org.opensearch.dataprepper.model.codec.OutputCodec}
 */
public class OutputCodecContext {

    private final String tagsTargetKey;

    private final List<String> includeKeys;
    private final List<String> excludeKeys;
    private final Predicate<String> inclusionPredicate;

    public OutputCodecContext() {
        this(null, Collections.emptyList(), Collections.emptyList());
    }


    public OutputCodecContext(String tagsTargetKey, List<String> includeKeys, List<String> excludeKeys) {
        this.tagsTargetKey = tagsTargetKey;
        this.includeKeys = includeKeys;
        this.excludeKeys = excludeKeys;

        if (includeKeys != null && !includeKeys.isEmpty()) {
            inclusionPredicate = k -> includeKeys.contains(k);
        } else if (excludeKeys != null && !excludeKeys.isEmpty()) {
            inclusionPredicate = k -> !excludeKeys.contains(k);
        } else {
            inclusionPredicate = k -> true;
        }
    }


    public static OutputCodecContext fromSinkContext(SinkContext sinkContext) {
        if (sinkContext == null) {
            return new OutputCodecContext();
        }
        return new OutputCodecContext(sinkContext.getTagsTargetKey(), sinkContext.getIncludeKeys(), sinkContext.getExcludeKeys());
    }

    public String getTagsTargetKey() {
        return tagsTargetKey;
    }

    public List<String> getIncludeKeys() {
        return includeKeys;
    }

    public List<String> getExcludeKeys() {
        return excludeKeys;
    }

    /**
     * Returns true if the key should be included according to the values of
     * {@link #getExcludeKeys()} and {@link #getIncludeKeys()}.
     *
     * @param key The key to include or exclude.
     * @return True if included; false if excluded.
     */
    public boolean shouldIncludeKey(String key) {
        return inclusionPredicate.test(key);
    }

    /**
     * Returns true if the key should not be included according to the values of
     * {@link #getExcludeKeys()} and {@link #getIncludeKeys()}.
     * <p>
     * This version may be helpful for unit testing since mocks of it would return false by default.
     *
     * @param key The key to include or exclude.
     * @return True if excluded; false if included.
     */
    public boolean shouldNotIncludeKey(String key) {
        return !inclusionPredicate.test(key);
    }
}
