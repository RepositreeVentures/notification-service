package io.repositree.notification.dto;

import io.repositree.notification.domain.Notification;
import io.repositree.notification.domain.NotificationChannel;
import io.repositree.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        NotificationChannel channel,
        String notificationType,
        NotificationStatus status,
        String payload,
        Instant createdAt,
        Instant dispatchedAt,
        String failureReason
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTenantId(), n.getUserId(),
                n.getChannel(), n.getNotificationType(),
                n.getStatus(), n.getPayload(),
                n.getCreatedAt(), n.getDispatchedAt(), n.getFailureReason()
        );
    }
}
