package io.repositree.notification.repository;

import io.repositree.notification.domain.Notification;
import io.repositree.notification.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);

    List<Notification> findByTenantIdAndUserIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID userId, NotificationStatus status);
}
