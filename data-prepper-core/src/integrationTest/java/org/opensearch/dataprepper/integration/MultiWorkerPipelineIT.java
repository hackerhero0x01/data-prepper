/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.InMemorySinkAccessor;
import org.opensearch.dataprepper.plugins.InMemorySourceAccessor;
import org.opensearch.dataprepper.test.framework.DataPrepperTestRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

class MultiWorkerPipelineIT {
    private static final String IN_MEMORY_IDENTIFIER = "MultiWorkerPipelineIT";
    private static final String PIPELINE_CONFIGURATION_UNDER_TEST = "multi-worker.yaml";
    private static final int WORKER_THREADS = 4;
    private static final int BATCH_SIZE = 10;
    private DataPrepperTestRunner dataPrepperTestRunner;
    private InMemorySourceAccessor inMemorySourceAccessor;
    private InMemorySinkAccessor inMemorySinkAccessor;

    @BeforeEach
    void setUp() {
        dataPrepperTestRunner = DataPrepperTestRunner.builder()
                .withPipelinesDirectoryOrFile(PIPELINE_CONFIGURATION_UNDER_TEST)
                .build();

        inMemorySourceAccessor = dataPrepperTestRunner.getInMemorySourceAccessor();
        inMemorySinkAccessor = dataPrepperTestRunner.getInMemorySinkAccessor();
        dataPrepperTestRunner.start();
    }

    @AfterEach
    void tearDown() {
        dataPrepperTestRunner.stop();
    }

    @Test
    void run_with_no_data() throws InterruptedException {

        final List<Record<Event>> preRecords = inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER);
        assertThat(preRecords, is(empty()));

        Thread.sleep(1000);

        final List<Record<Event>> postRecords = inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER);
        assertThat(postRecords, is(empty()));
    }

    @Test
    void run_with_single_record() {

        final Event event = JacksonEvent.fromMessage(UUID.randomUUID().toString());
        final Record<Event> eventRecord = new Record<>(event);

        inMemorySourceAccessor.submit(IN_MEMORY_IDENTIFIER, Collections.singletonList(eventRecord));

        await().atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
            assertThat(inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER), not(empty()));
        });

        assertThat(inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER).size(), equalTo(1));
    }

    @Test
    void run_with_single_batch_of_records() {

        final int recordsToCreate = BATCH_SIZE;
        final List<Record<Event>> inputRecords = IntStream.range(0, recordsToCreate)
                .mapToObj(i -> UUID.randomUUID().toString())
                .map(JacksonEvent::fromMessage)
                .map(Record::new)
                .collect(Collectors.toList());

        inMemorySourceAccessor.submit(IN_MEMORY_IDENTIFIER, inputRecords);

        await().atMost(400, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
            assertThat(inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER), not(empty()));
        });

        assertThat(inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER).size(), equalTo(recordsToCreate));
    }

    @Test
    void run_with_multiple_batches_available_per_thread() {

        final int recordsToCreate = BATCH_SIZE * WORKER_THREADS;

        final int iterations = 10;

        final Set<String> allThreadNames = new HashSet<>();

        for (int iteration = 0; iteration < iterations; iteration++) {
            final List<Record<Event>> inputRecordsBatch = IntStream.range(0, recordsToCreate)
                    .mapToObj(i -> UUID.randomUUID().toString())
                    .map(JacksonEvent::fromMessage)
                    .map(Record::new)
                    .collect(Collectors.toList());

            inMemorySourceAccessor.submit(IN_MEMORY_IDENTIFIER, inputRecordsBatch);

            await().atMost(400, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(inMemorySinkAccessor.get(IN_MEMORY_IDENTIFIER).size(), equalTo(recordsToCreate));
                    });
            final List<Record<Event>> receivedRecords = inMemorySinkAccessor.getAndClear(IN_MEMORY_IDENTIFIER);
            assertThat(receivedRecords.size(), equalTo(recordsToCreate));

            final Set<String> allThreadNamesThisIteration = receivedRecords.stream()
                    .map(Record::getData)
                    .map(event -> event.get("thread_name", String.class))
                    .collect(Collectors.toSet());

            assertThat(allThreadNamesThisIteration.size(), lessThanOrEqualTo(WORKER_THREADS));
            allThreadNames.addAll(allThreadNamesThisIteration);
        }

        assertThat(allThreadNames.size(), equalTo(WORKER_THREADS));
    }
}
