package org.apache.sling.distribution.service;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Value;

@XmlRootElement
@Value
@Builder
public class SubscriberStatus {
    String subSlingId;
    String subAgentName;
    String queueId;
    String packageId;
    long offset;
    DistributionStatus status;
}
