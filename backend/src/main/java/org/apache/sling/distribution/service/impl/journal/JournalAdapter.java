package org.apache.sling.distribution.service.impl.journal;

import java.io.Closeable;
import java.io.IOException;

import org.apache.sling.distribution.journal.HandlerAdapter;
import org.apache.sling.distribution.journal.MessageInfo;
import org.apache.sling.distribution.journal.MessageSender;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.apache.sling.distribution.journal.messages.Messages.PackageMessage;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.PackageMessageMeta.ReqType;
import org.apache.sling.distribution.service.impl.binary.BinaryRepository;
import org.apache.sling.distribution.service.impl.publisher.DistributionPublisher;
import org.apache.sling.distribution.service.impl.subscriber.PackageRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.google.protobuf.GeneratedMessage;

@Component(service = JournalAdapter.class, immediate = true)
public class JournalAdapter implements DistributionPublisher, Closeable {
    private static final String DEFAULT_PACKAGE_TOPIC = "aem_va7_dist_0001_nonprod_package";

    private final PackageRepository repository;
    private final BinaryRepository binaryRepository;
    private final String topicName;
    private final Closeable consumer;
    private final MessageSender<GeneratedMessage> sender;

    @Activate
    public JournalAdapter(
            @Reference MessagingProvider messagingProvider, 
            @Reference PackageRepository repository,
            @Reference BinaryRepository binaryRepository
            ) {
        this.repository = repository;
        this.binaryRepository = binaryRepository;
        this.topicName = DEFAULT_PACKAGE_TOPIC;
        consumer = messagingProvider.createPoller(topicName, Reset.earliest, //, "0:153959000",
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
        try {
            PackageMessageMeta pkg = PackageMessageMetaConverter.convert(info, message, binaryRepository);
            if (pkg.getReqType() != ReqType.TEST) {
                repository.addPackage(pkg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
