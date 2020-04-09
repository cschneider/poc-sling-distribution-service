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

import org.apache.commons.io.IOUtils;
import org.apache.sling.distribution.journal.HandlerAdapter;
import org.apache.sling.distribution.journal.MessageSender;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.apache.sling.distribution.journal.messages.Messages;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.impl.core.JournalAgent;

import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;

import com.google.protobuf.GeneratedMessage;
import lombok.SneakyThrows;

import static org.apache.sling.distribution.service.impl.core.legacy.Converters.convert;
import static org.apache.sling.distribution.service.impl.core.legacy.Converters.revert;

public class PackageJournal implements JournalAgent<String, PackageMessageMeta> {

    public PackageJournal(MessagingProvider provider) {
        this.provider = provider;
    }

    @Override
    public void subscribe(
            String position, BiConsumer<String, PackageMessageMeta> handler
    ) {
        var poller = provider.createPoller(TOPIC_PACKAGES, Reset.earliest, position, packageHandler(handler));
        subscribers.put(handler, poller);
        this.sender = provider.createSender();
    }

    @SneakyThrows
    @Override
    public void unsubscribe(BiConsumer<String, PackageMessageMeta> handler) {
        var poller = subscribers.remove(handler);
        poller.close();
    }

    @Override
    public void send(PackageMessageMeta message) {
        sender.send(TOPIC_PACKAGES, revert(message));
    }

    @Override
    public void close() {
        for (var e : subscribers.entrySet()) {
            var subsctiber = e.getKey();
            var poller = e.getValue();
            IOUtils.closeQuietly(poller);
        }
    }

    //*********************************************
    // Private
    //*********************************************

    private static final String TOPIC_PACKAGES = "aemdistribution_package";
    private final MessagingProvider provider;
    private IdentityHashMap<BiConsumer<String, PackageMessageMeta>, Closeable> subscribers;
    private MessageSender<GeneratedMessage> sender;

    private static HandlerAdapter<Messages.PackageMessage> packageHandler(
            BiConsumer<String, PackageMessageMeta> consumer
    ) {
        return new HandlerAdapter<>(
                Messages.PackageMessage.class,
                (info, message) -> {
                    var result = convert(info, message);
                    consumer.accept(String.valueOf(info.getOffset()), result);
                }
        );
    }

}
