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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.sling.distribution.service.impl.core.Utils.mkString;

class PromiseCache<K, V> {

    Optional<Promise<K, V>> get(K key) {
        synchronized (lock) {
            var state = stateMap.get(key);
            if (state != null) {
                assert state.getKey().equals(key) : format(
                        "Stored state position [%s] does not match requested %s",
                        state, key
                );
                return Optional.of(state);
            } else {
                return Optional.empty();
            }
        }
    }

    /** Puts new values into this cache only if it is not already present
     * otherwise invocation is ignored and current value is returned.
     * @param promise new value
     * @return Either pre-existing value if cache was not updated, or null
     * if it was updated.
     */
    Promise<K, V> putIfAbsent(Promise<K, V> promise) {
        synchronized (lock) {
            while (history.size() > sizeLimit) {
                assert history.size() == stateMap.size();
                var s = history.removeLast();
                assert promise.isDone() : format("Evicted promise has not been completed: %s", promise);
                stateMap.remove(s.getKey());
            }
            var existing = stateMap.putIfAbsent(promise.getKey(), promise);
            history.push(promise);
            return existing;
        }
    }

    boolean isEmpty() {
        synchronized (lock) {
            assert history.isEmpty() == stateMap.isEmpty();
            return history.isEmpty();
        }
    }

    void close() {
        synchronized (lock) {
            history.forEach(Promise::cancel);
            history.clear();
            stateMap.clear();
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return format("[%s]{%s}",
                    history.size(),
                    mkString(history, ", ", 100)
            );
        }
    }

    //*********************************************
    // Private
    //*********************************************

    private final int sizeLimit = 100;
    private final Object lock = new Object();
    private final HashMap<K, Promise<K, V>> stateMap = new HashMap<>(sizeLimit);
    private final Deque<Promise<K, V>> history = new ArrayDeque<>(sizeLimit);

}
