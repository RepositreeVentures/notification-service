package io.repositree.notification.repository;

import io.repositree.outbox.OutboxEvent;
import io.repositree.outbox.OutboxRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, UUID>, OutboxRepository {
}
