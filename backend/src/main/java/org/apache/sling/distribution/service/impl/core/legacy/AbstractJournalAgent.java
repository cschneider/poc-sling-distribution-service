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
import org.apache.sling.distribution.journal.MessageInfo;
import org.apache.sling.distribution.journal.MessageSender;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.apache.sling.distribution.service.impl.core.JournalAgent;

import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;

import com.google.protobuf.GeneratedMessage;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

abstract class AbstractJournalAgent<Message, BackendMessage extends GeneratedMessage> implements JournalAgent<String, Message> {

    public AbstractJournalAgent(String topic, MessagingProvider provider, Class<BackendMessage> backendClass) {
        this.provider = provider;
        this.topic = topic;
        this.backendClass = backendClass;
    }

    @Override
    public void subscribe(
            String position, BiConsumer<String, Message> handler
    ) {
        LOGGER.info("{}: Adding subscriber {} at {}", this, handler, position);
        var poller = provider.createPoller(topic, Reset.earliest, position, createHandler(handler));
        subscribers.put(handler, poller);
        this.sender = provider.createSender();
    }

    @SneakyThrows
    @Override
    public void unsubscribe(BiConsumer<String, Message> handler) {
        var poller = subscribers.remove(handler);
        if (poller == null) {
            LOGGER.warn("{}: not subscribed: {}", this, handler);
        } else {
            LOGGER.info("{}: removing subscriber {}", this, handler);
            poller.close();
        }
    }

    @Override
    public void send(Message message) {
        sender.send(topic, revert(message));
    }

    @Override
    public void close() {
        for (var e : subscribers.entrySet()) {
            var subscriber = e.getKey();
            var poller = e.getValue();
            IOUtils.closeQuietly(poller);
        }
    }

    @Override
    public String toString() {
        return format(
                "%s[%s]",
                topic,
                backendClass.getSimpleName()
        );
    }

    //*********************************************
    // Specialization
    //*********************************************

    protected abstract Message convert(MessageInfo info, BackendMessage message);
    protected abstract BackendMessage revert(Message message);

    //*********************************************
    // Private
    //*********************************************

    private HandlerAdapter<BackendMessage> createHandler(
            BiConsumer<String, Message> consumer
    ) {
        return new HandlerAdapter<>(
                backendClass,
                (info, message) -> {
                    var result = convert(info, message);
                    consumer.accept(String.valueOf(info.getOffset()), result);
                }
        );
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJournalAgent.class);
    private final String topic;
    private final MessagingProvider provider;
    private final IdentityHashMap<BiConsumer<String, Message>, Closeable> subscribers = new IdentityHashMap<>();
    private MessageSender<GeneratedMessage> sender;
    private final Class<BackendMessage> backendClass;

}
