package io.repositree.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
// OutboxEvent lives in the repositree-outbox lib package, outside the app base package.
@EntityScan(basePackages = {"io.repositree.notification", "io.repositree.outbox"})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
