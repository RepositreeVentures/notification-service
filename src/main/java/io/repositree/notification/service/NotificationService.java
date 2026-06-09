package io.repositree.notification.service;

import io.repositree.common.id.RepositreeId;
import io.repositree.notification.domain.Notification;
import io.repositree.notification.domain.NotificationChannel;
import io.repositree.notification.repository.NotificationRepository;
import io.repositree.outbox.ActorKind;
import io.repositree.outbox.EventClass;
import io.repositree.outbox.OutboxPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OutboxPublisher outboxPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                                OutboxPublisher outboxPublisher) {
        this.notificationRepository = notificationRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public Notification enqueue(UUID tenantId, UUID userId,
                                 NotificationChannel channel,
                                 String notificationType, String payload,
                                 ActorKind actorKind,
                                 UUID agentRunId, String promptVersion) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(notificationType, "notificationType must not be null");
        Objects.requireNonNull(actorKind, "actorKind must not be null");

        UUID id = RepositreeId.generate().value();
        Notification notification = Notification.create(id, tenantId, userId, channel, notificationType, payload);
        Notification saved = notificationRepository.save(notification);

        outboxPublisher.publish(
                "Notification",
                RepositreeId.of(id),
                "NOTIFICATION_ENQUEUED",
                payload,
                actorKind,
                EventClass.LIVE,
                agentRunId == null ? null : RepositreeId.of(agentRunId),
                promptVersion,
                RepositreeId.of(tenantId)
        );

        return saved;
    }

    @Transactional
    public Notification markDispatched(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.dispatch();
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markFailed(UUID notificationId, String reason) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.fail(reason);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.markRead();
        return notificationRepository.save(notification);
    }
}
