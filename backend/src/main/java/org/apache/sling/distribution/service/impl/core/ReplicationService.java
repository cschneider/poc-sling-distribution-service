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

import org.apache.sling.distribution.service.PackageMessageMeta;

import java.util.Optional;

/** Represents replication service as whole. Uses backend storage
 * (journal) is represented by BackendAgent instance. In our case
 * it is just an adapter to MessagingService
 * @param <K> key type
 */
public class ReplicationService<K> {

    private static final String TOPIC_PACKAGE = "aemdistribution_package";
    private static final long ABANDON_TIMEOUT = 1000 * 60; // 1min
    private static final int TRACKING_HISTORY_LENGTH = 10;

    public ReplicationService(BackendAgent<K> backendAgent) {
        this.packageReader = new TopicReader<>(
                backendAgent.getTopicAgent(TOPIC_PACKAGE),
                ABANDON_TIMEOUT,
                TRACKING_HISTORY_LENGTH
        );
    }

    /** returns a message at the specified position
     * either from cache or fetching and waiting when necessary
     * @param position position from which to retrieve a message
     * @return fetched or cached message instance
     */
    public PackageMessageMeta getPackageMeta(K position) {
        return packageReader.getMessage(position);
    }

    //*********************************************
    // Private
    //*********************************************

    private final TopicReader<K, PackageMessageMeta> packageReader;

}
