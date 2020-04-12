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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.sling.distribution.service.impl.core.MessageFetcher.State.ABANDONED;
import static org.apache.sling.distribution.service.impl.core.MessageFetcher.State.CAUGHT_UP;
import static org.apache.sling.distribution.service.impl.core.MessageFetcher.State.FETCHING;
import static org.apache.sling.distribution.service.impl.core.MessageFetcher.State.NEW;
import static org.apache.sling.distribution.service.impl.core.MessageFetcher.State.STOPPED;

class MessageFetcher<K, V> implements BiConsumer<K, V>, AutoCloseable {

    MessageFetcher(Consumer<MessageFetcher<K, V>> unregister, PromiseCache<K, V> cache, long timeout, int maxTracked) {
        this.unregister = unregister;
        this.cache = cache;
        this.timeout = timeout;
        this.lastUsedTracker = new LastUsedTracker(maxTracked);
    }

    Promise<K, V> start(K key) {
        requireState(NEW);
        var promise = new Promise<K, V>(key, false);
        var cachedPromise = cache.putIfAbsent(promise);
        if (cachedPromise == null) {
            LOGGER.info("{}: starting fetching from {}", this, key);
            state.set(FETCHING);
            requestedRef.set(promise);
            trackUsage(promise);
            lastUsed.set(currentTimeMillis());
            return promise;
        } else {
            throw new IllegalStateException(format("[%s] is already being handled", cachedPromise));
        }
    }

    Optional<Promise<K, V>> subscribe(K key) {
        requireState(FETCHING);
        return this.cache.get(key);
    }

    @Override
    public void accept(K key, V value) {
        LOGGER.info("{}: received ({}, {})", this, key, value);
        if (state.get() == FETCHING) {
            requireState(FETCHING);
            var next = new Promise<K, V>(key, true);
            var requested = requestedRef.getAndSet(next);
            LOGGER.info("{}: notifying {}, requested: {}", this, requested, next);
            requested.complete(value);
            trackUsage(requested);
            var cached = cache.putIfAbsent(next);
            if (cached != null) {
                LOGGER.info("{}: Caught up with cache at {}. Stopping.", this, next);
                requestedRef.set(null);
                unregister.accept(this);
                state.set(CAUGHT_UP);
            }
        } else {
            LOGGER.warn("{}: Already stopped. Ignoring notification {} {}", this, key, value);
        }
    }

    @Override
    public void close() {
        unregister.accept(this);
        state.set(STOPPED);
    }

    @Override
    public String toString() {
        var state = this.state.get();
        if (state == FETCHING) {
            return format("[%s] at [%s]", state, requestedRef.get());
        } else {
            return format("[%s]", state);
        }
    }

    //*********************************************
    // Private
    //*********************************************

    enum State { NEW, FETCHING, CAUGHT_UP, STOPPED, ABANDONED }
    private final static Logger LOGGER = LoggerFactory.getLogger(MessageFetcher.class);
    private final AtomicLong lastUsed = new AtomicLong(currentTimeMillis());
    private final AtomicReference<State> state = new AtomicReference<>(NEW);
    private final AtomicReference<Promise<K, V>> requestedRef = new AtomicReference<>();
    private final PromiseCache<K, V> cache;
    private final long timeout;
    private final LastUsedTracker lastUsedTracker;
    private final Consumer<MessageFetcher<K, V>> unregister;

    private void trackUsage(Promise<K, V> promise) {
        lastUsedTracker.push(promise);
        var lastUsed = lastUsedTracker.getLastUsed();
        if (currentTimeMillis() - lastUsed > timeout) {
            LOGGER.info(
                    "{} was not utilized since {} which exceeds timout of {} sec. Abandoning",
                    this, lastUsed, timeout / 1000
            );

            unregister.accept(this);
            state.set(ABANDONED);
        }
    }

    private void requireState(State expected) {
        var actual = this.state.get();
        if (actual != expected) {
            throw new IllegalStateException(format(
                    "%s: expected state %s but was %s",
                    this, expected, actual
            ));
        }
    }

}
