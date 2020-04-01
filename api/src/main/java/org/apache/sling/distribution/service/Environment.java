package org.apache.sling.distribution.service;

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
public class Environment {
    Map<String, DistributionQueueInfo> queues;
    
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement(name = "_links")
    Map<String, Link> links;
}
