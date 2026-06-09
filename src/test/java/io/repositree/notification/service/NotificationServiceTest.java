package io.repositree.notification.service;

import io.repositree.common.id.RepositreeId;
import io.repositree.notification.domain.Notification;
import io.repositree.notification.domain.NotificationChannel;
import io.repositree.notification.domain.NotificationStatus;
import io.repositree.notification.repository.NotificationRepository;
import io.repositree.outbox.ActorKind;
import io.repositree.outbox.EventClass;
import io.repositree.outbox.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    private NotificationService service;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, outboxPublisher);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void enqueue_persistsNotificationAsPending() {
        UUID savedId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return n;
        });

        Notification result = service.enqueue(
                tenantId, userId, NotificationChannel.IN_APP,
                "USER_REGISTERED", "{\"message\":\"Welcome!\"}",
                ActorKind.SYSTEM, null, null
        );

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(result.getNotificationType()).isEqualTo("USER_REGISTERED");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void enqueue_writesOutboxEvent() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.enqueue(
                tenantId, userId, NotificationChannel.EMAIL,
                "ORDER_CONFIRMED", "{\"orderId\":\"abc\"}",
                ActorKind.HUMAN, null, null
        );

        verify(outboxPublisher).publish(
                eq("Notification"),
                any(RepositreeId.class),
                eq("NOTIFICATION_ENQUEUED"),
                any(String.class),
                eq(ActorKind.HUMAN),
                eq(EventClass.LIVE),
                isNull(),
                isNull(),
                any(RepositreeId.class)
        );
    }

    @Test
    void markDispatched_updatesStatusAndTimestamp() {
        UUID notifId = UUID.randomUUID();
        Notification existing = Notification.create(
                notifId, tenantId, userId,
                NotificationChannel.PUSH, "TEST_EVENT", "{}");
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.markDispatched(notifId);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.DISPATCHED);
        assertThat(result.getDispatchedAt()).isNotNull();
    }

    @Test
    void markDispatched_throwsWhenNotFound() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markDispatched(notifId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(notifId.toString());
    }

    @Test
    void markFailed_updatesStatusToFailed() {
        UUID notifId = UUID.randomUUID();
        Notification existing = Notification.create(
                notifId, tenantId, userId,
                NotificationChannel.SMS, "TEST_EVENT", "{}");
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = service.markFailed(notifId, "Provider timeout");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Provider timeout");
    }

    @Test
    void enqueue_nullTenantId_throws() {
        assertThatThrownBy(() -> service.enqueue(
                null, userId, NotificationChannel.IN_APP,
                "TEST", "{}", ActorKind.SYSTEM, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void enqueue_nullUserId_throws() {
        assertThatThrownBy(() -> service.enqueue(
                tenantId, null, NotificationChannel.IN_APP,
                "TEST", "{}", ActorKind.SYSTEM, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
