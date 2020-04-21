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

package org.apache.sling.distribution.service;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discovery {
    String subSlingId;               // Subscriber agent Sling identifier
    String subAgentName;             // Subscriber agent name
    boolean editable;
    int maxRetries;   // The max number of retry attempts to process this package. A value smaller than zero indicates an infinite number of retry attempts. A value greater or equal to zero indicates a specific number of retry attempts.
    List<SubscriberState> subscriberState;

    @Data
    @Builder
    public static class SubscriberState {
        String pubAgentName; // Publisher agent name
        String offset;       // Last processed offset on the Subscriber agent
        int retries;         // Nb of retries for the current offset on the Subscriber agent
     }
}
