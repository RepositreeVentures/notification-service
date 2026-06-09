package io.repositree.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    protected Notification() {
    }

    public static Notification create(UUID id, UUID tenantId, UUID userId,
                                      NotificationChannel channel,
                                      String notificationType, String payload) {
        Notification n = new Notification();
        n.id = id;
        n.tenantId = tenantId;
        n.userId = userId;
        n.channel = channel;
        n.notificationType = notificationType;
        n.payload = payload;
        n.status = NotificationStatus.PENDING;
        n.createdAt = Instant.now();
        return n;
    }

    public void dispatch() {
        this.status = NotificationStatus.DISPATCHED;
        this.dispatchedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public void markRead() {
        this.status = NotificationStatus.READ;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public String getNotificationType() { return notificationType; }
    public NotificationStatus getStatus() { return status; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public String getFailureReason() { return failureReason; }
}
