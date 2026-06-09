package io.repositree.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.notification.domain.NotificationChannel;
import io.repositree.notification.service.NotificationService;
import io.repositree.outbox.ActorKind;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes domain events from other services and enqueues in-app notifications.
 * Only processes LIVE events (event_class != BACKFILL) to avoid backfill storms.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(NotificationService notificationService,
                                      ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.domain-events:domain-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode event = objectMapper.readTree(record.value());

            String eventClass = event.path("event_class").asText("LIVE");
            if ("BACKFILL".equals(eventClass)) {
                return;
            }

            String eventType = event.path("event_type").asText();
            String tenantIdStr = event.path("tenant_id").asText();
            String aggregateIdStr = event.path("aggregate_id").asText();
            String actorKindStr = event.path("actor_kind").asText("SYSTEM");

            if (tenantIdStr.isBlank() || aggregateIdStr.isBlank()) {
                log.warn("Skipping event with missing tenant_id or aggregate_id: {}", eventType);
                return;
            }

            UUID tenantId = UUID.fromString(tenantIdStr);
            UUID userId = UUID.fromString(aggregateIdStr);
            ActorKind actorKind = parseActorKind(actorKindStr);

            String payload = event.path("payload").toString();

            notificationService.enqueue(
                    tenantId, userId,
                    NotificationChannel.IN_APP,
                    eventType, payload,
                    actorKind, null, null
            );

        } catch (Exception e) {
            log.error("Failed to process notification event from topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }

    private ActorKind parseActorKind(String raw) {
        try {
            return ActorKind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ActorKind.SYSTEM;
        }
    }
}
