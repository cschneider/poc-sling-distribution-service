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

import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PackageMessageMeta {

    String pkgId; // Content package identifier
    long position;
    ReqType reqType;
    String imsOrg;
    String source;
    String pubSlingId; // Publisher sling Id
    String pubAgentName; // Publisher agent name
    String userId;
    List<String> paths;
    List<String> deepPaths;
    String pkgType;
    
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    Link contentPackage;
    
    public enum ReqType {
        ADD, DELETE, TEST
    }
}
