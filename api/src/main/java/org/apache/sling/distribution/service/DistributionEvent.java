package org.apache.sling.distribution.service;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Value;

@XmlRootElement
@Value
@Builder
public class DistributionEvent {
    String queueId;
    long offset;
    String packageId;
    DistributionStatus status;
}
