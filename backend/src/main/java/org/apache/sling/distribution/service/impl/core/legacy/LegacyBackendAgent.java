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

package org.apache.sling.distribution.service.impl.core.legacy;

import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.impl.core.BackendAgent;
import org.apache.sling.distribution.service.impl.core.JournalAgent;

public class LegacyBackendAgent implements BackendAgent<String> {


    public LegacyBackendAgent(MessagingProvider provider) {
        this.provider = provider;
    }
    @Override
    public JournalAgent<String, PackageMessageMeta> getTopicAgent(
            String topic
    ) {
        return new PackageJournalAgent(provider);
    }

    private final MessagingProvider provider;

}
