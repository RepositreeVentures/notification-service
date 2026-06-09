package io.repositree.notification.dto;

import io.repositree.notification.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationRequest(
        @NotNull UUID tenantId,
        @NotNull UUID userId,
        @NotNull NotificationChannel channel,
        @NotBlank String notificationType,
        String payload
) {
}
