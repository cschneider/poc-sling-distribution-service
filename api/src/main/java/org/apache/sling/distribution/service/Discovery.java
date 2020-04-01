package org.apache.sling.distribution.service;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Value;

@XmlRootElement
@Value
@Builder
public class Discovery {
    String subSlingId;               // Subscriber agent Sling identifier
    String subAgentName;             // Subscriber agent name
    boolean editable;
    int maxRetries;   // The max number of retry attempts to process this package. A value smaller than zero indicates an infinite number of retry attempts. A value greater or equal to zero indicates a specific number of retry attempts.
    List<SubscriberState> subscriberState;

    @Value
    @Builder
    public static class SubscriberState {
        String pubAgentName; // Publisher agent name
        String offset;       // Last processed offset on the Subscriber agent
        int retries;         // Nb of retries for the current offset on the Subscriber agent
     }
}
