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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import lombok.SneakyThrows;

import static java.lang.System.currentTimeMillis;

class Promise<K, V> {

    Promise(K key, boolean speculative) {
        this.key = key;
        this.delegate = new CompletableFuture<>();
        this.lastUsed = new AtomicLong(speculative ? 0 : currentTimeMillis());
    }

    K getKey() {
        return key;
    }

    @SneakyThrows
    V getValue() {
        lastUsed.set(currentTimeMillis());
        return delegate.get();
    }

    boolean isDone() {
        return delegate.isDone();
    }

    void complete(V value) {
        this.delegate.complete(value);
    }

    public void cancel() {
        delegate.cancel(false);
    }

    long lastUsed() {
        return lastUsed.get();
    }

    @Override
    public String toString() {
        if (delegate.isDone()) {
            return key + ":d";
        } else if (delegate.isCancelled()) {
            return key + ":c";
        } else if (delegate.isCompletedExceptionally()) {
            return key + ":x";
        } else {
            return key + ":w(" + delegate.getNumberOfDependents() + ")";
        }
    }

    //*********************************************
    // Private
    //*********************************************

    private final K key;
    private final CompletableFuture<V> delegate;
    private final AtomicLong lastUsed;

}

