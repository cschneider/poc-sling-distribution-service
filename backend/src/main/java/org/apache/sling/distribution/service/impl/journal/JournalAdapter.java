package org.apache.sling.distribution.service.impl.journal;


import java.io.Closeable;
import java.io.IOException;

import org.apache.sling.distribution.journal.HandlerAdapter;
import org.apache.sling.distribution.journal.MessageInfo;
import org.apache.sling.distribution.journal.MessageSender;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.apache.sling.distribution.journal.messages.Messages.PackageMessage;
import org.apache.sling.distribution.journal.messages.Messages.PackageMessage.ReqType;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.impl.binary.BinaryRepository;
import org.apache.sling.distribution.service.impl.publisher.DistributionPublisher;
import org.apache.sling.distribution.service.impl.subscriber.PackageRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.GeneratedMessage;

@Component(service = DistributionPublisher.class, immediate = true)
public class JournalAdapter implements DistributionPublisher, Closeable {
    private static final String DEFAULT_PACKAGE_TOPIC = "aem_va7_dist_0001_nonprod_package";
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PackageRepository repository;
    private final BinaryRepository binaryRepository;
    private final String topicName;
    private final Closeable consumer;
    private final MessageSender<GeneratedMessage> sender;

    @Activate
    public JournalAdapter(@Reference MessagingProvider messagingProvider, @Reference PackageRepository repository,
            @Reference BinaryRepository binaryRepository) {
        this.repository = repository;
        this.binaryRepository = binaryRepository;
        this.topicName = DEFAULT_PACKAGE_TOPIC;
        consumer = messagingProvider.createPoller(topicName, Reset.earliest, // , "0:153959000",
                HandlerAdapter.create(PackageMessage.class, this::handlePackage));
        sender = messagingProvider.createSender();
    }

    @Override
    public void publish(PackageMessageMeta pkgMeta) {
        PackageMessage pkgMessage = PackageMessageMetaConverter.toPackageMessage(pkgMeta);
        sender.send(topicName, pkgMessage);
    }

    @Deactivate
    @Override
    public void close() throws IOException {
        consumer.close();
    }

    private void handlePackage(MessageInfo info, PackageMessage message) {
        if (message.getReqType() != ReqType.TEST) {
            try {
                PackageMessageMeta pkg = PackageMessageMetaConverter.convert(info, message, binaryRepository);
                repository.addPackage(pkg);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
    
}
