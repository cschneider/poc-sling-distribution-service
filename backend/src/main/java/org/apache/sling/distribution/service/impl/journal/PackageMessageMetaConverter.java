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

package org.apache.sling.distribution.service.impl.journal;

import java.io.IOException;

import javax.ws.rs.core.Link;

import org.apache.sling.distribution.journal.MessageInfo;
import org.apache.sling.distribution.journal.messages.Messages;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.PackageMessageMeta.PackageMessageMetaBuilder;
import org.apache.sling.distribution.service.impl.binary.BinaryRepository;

import com.google.protobuf.ByteString;

class PackageMessageMetaConverter {

    static PackageMessageMeta convert(MessageInfo info, Messages.PackageMessage message, BinaryRepository repository) {
        try {
            
            PackageMessageMetaBuilder builder = PackageMessageMeta.builder()
                                     .pkgId(message.getPkgId())
                                     .pkgType(message.getPkgType())
                                     .reqType(convert(message.getReqType()))
                                     .imsOrg(info.getOrg())
                                     .source(info.getSource())
                                     .userId(message.getUserId())
                                     .pubAgentName(message.getPubAgentName())
                                     .paths(message.getPathsList())
                                     .deepPaths(message.getDeepPathsList())
                                     .position(info.getOffset())
                                     .pubSlingId(message.getPubSlingId());
            ByteString pkgBinary = message.getPkgBinary();
            if (!pkgBinary.isEmpty()) {
                String id = repository.upload(pkgBinary.newInput());
                Link packageLink = Link.fromUri("http://localhost:8080/distribution/binary/{id}").build(id);
                builder.contentPackage(packageLink);
            }
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static Messages.PackageMessage toPackageMessage(PackageMessageMeta message) {
        return Messages.PackageMessage.newBuilder()
                                      .setPkgId(message.getPkgId())
                                      .setPkgType(message.getPkgType())
                                      .setReqType(revert(message.getReqType()))
                                      .setUserId(message.getUserId())
                                      .setPubAgentName(message.getPubAgentName())
                                      .addAllPaths(message.getPaths())
                                      .addAllDeepPaths(message.getDeepPaths())
                                      .setPubSlingId(message.getPubSlingId())
                                      .build();
    }

    //*********************************************
    // Private
    //*********************************************

    private PackageMessageMetaConverter() {};
    private static PackageMessageMeta.ReqType convert(Messages.PackageMessage.ReqType reqType) {
        switch (reqType) {
            case ADD: return PackageMessageMeta.ReqType.ADD;
            case DELETE: return PackageMessageMeta.ReqType.DELETE;
            case TEST: return PackageMessageMeta.ReqType.TEST;
            default: throw new IllegalArgumentException("Unexpected ReqType: [" + reqType + "]");
        }
    }

    private static Messages.PackageMessage.ReqType revert(PackageMessageMeta.ReqType reqType) {
        switch (reqType) {
            case ADD: return Messages.PackageMessage.ReqType.ADD;
            case DELETE: return Messages.PackageMessage.ReqType.DELETE;
            case TEST: return Messages.PackageMessage.ReqType.TEST;
            default: throw new IllegalArgumentException("Unexpected ReqType: [" + reqType + "]");
        }
    }

}
