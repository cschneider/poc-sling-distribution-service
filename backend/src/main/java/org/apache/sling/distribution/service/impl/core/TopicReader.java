/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */

package org.apache.sling.distribution.service.impl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;

class TopicReader<K, V> {

    TopicReader(JournalAgent<K, V> journalAgent, long abandonTimeout, int usageHistoryLength) {
        this.journalAgent = journalAgent;
        this.abandonTimeout = abandonTimeout;
        this.usageHistoryLength = usageHistoryLength;
    }

    @SneakyThrows
    V getMessage(K position) {
        Promise<K, V> promise;
        synchronized (packageFetchers) {
            promise = cache.get(position).orElseGet(
                    () -> newPromise(position)
            );
        }
        return promise.getValue();
    }

    @SneakyThrows
    public void close() {
        journalAgent.close();
        for (var fetcher : packageFetchers) {
            fetcher.close();
        }
    }

    //*********************************************
    // Private
    //*********************************************

    private final JournalAgent<K, V> journalAgent;
    private final List<MessageFetcher<K, V>> packageFetchers = new ArrayList<>();
    private final PromiseCache<K, V> cache = new PromiseCache<>();
    private final long abandonTimeout;
    private final int usageHistoryLength;

    private void unregisterFetcher(MessageFetcher<K, V> fetcher) {
        synchronized (packageFetchers) {
            journalAgent.unsubscribe(fetcher);
            packageFetchers.remove(fetcher);
        }
    }

    private Optional<Promise<K, V>> existingPromise(K position) {
        for (var fetcher : packageFetchers) {
            var maybePromise = fetcher.subscribe(position);
            if (maybePromise.isPresent()) {
                return maybePromise;
            }
        }
        return Optional.empty();
    }

    private Promise<K, V> newPromise(K position) {
        var fetcher = new MessageFetcher<>(
                this::unregisterFetcher,
                cache,
                abandonTimeout,
                usageHistoryLength
        );
        var promise = fetcher.start(position);
        packageFetchers.add(fetcher);
        journalAgent.subscribe(position, fetcher);
        return promise;
    }

}
