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

import static java.lang.Long.max;

public class LastUsedTracker {

    public LastUsedTracker(int maxLength) {
        this.maxLength = maxLength;
    }

    void push(Promise<?, ?> promise) {
        while (history.size() > maxLength) {
            history.removeLast();
        }
        history.push(promise);
    }

    long getLastUsed() {
        long result = 0;
        for (var p : history) {
            result = max(result, p.lastUsed());
        }
        return result;
    }

    private final Deque<Promise<?, ?>> history = new ArrayDeque<>();
    private final int maxLength;

}
