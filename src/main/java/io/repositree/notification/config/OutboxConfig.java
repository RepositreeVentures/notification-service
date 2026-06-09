package io.repositree.notification.config;

import io.repositree.notification.repository.OutboxJpaRepository;
import io.repositree.outbox.OutboxPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfig {

    @Bean
    public OutboxPublisher outboxPublisher(OutboxJpaRepository outboxJpaRepository) {
        return new OutboxPublisher(outboxJpaRepository);
    }
}
