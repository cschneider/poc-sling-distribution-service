package org.apache.sling.distribution.service;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.Builder;
import lombok.Value;

@XmlRootElement
@Value
@Builder
public class PackageMessageMeta {

    String pkgId; // Content package identifier
    long position;
    ReqType reqType;
    String pubSlingId; // Publisher sling Id
    String pubAgentName; // Publisher agent name
    String userId;
    List<String> paths;
    List<String> deepPaths;
    String pkgType;
    
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement(name = "_links")
    Map<String, Link> links;

    public enum ReqType {
        ADD, DELETE, TEST
    }
}
