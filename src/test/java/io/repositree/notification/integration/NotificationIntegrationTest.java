package io.repositree.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.notification.domain.NotificationChannel;
import io.repositree.notification.domain.NotificationStatus;
import io.repositree.notification.repository.NotificationRepository;
import io.repositree.outbox.OutboxPublisher;
import io.repositree.test.Containers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController — full HTTP stack via MockMvc.
 *
 * Real Postgres 16 via Testcontainers.
 * OutboxPublisher is @MockBean (no Kafka required in tests).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgres = Containers.postgres()
            .withDatabaseName("notification_service")
            .withUsername("notif_svc")
            .withPassword("secret");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private OutboxPublisher outboxPublisher;

    @AfterEach
    void cleanup() {
        notificationRepository.deleteAll();
    }

    @Test
    void postNotification_createsAndReturnsPending() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "userId", userId.toString(),
                                "channel", "IN_APP",
                                "notificationType", "USER_REGISTERED",
                                "payload", "{\"message\":\"Welcome!\"}"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.channel").value("IN_APP"))
                .andExpect(jsonPath("$.notificationType").value("USER_REGISTERED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getNotificationsByUser_returnsList() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Create one first
        mvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "tenantId", tenantId.toString(),
                                "userId", userId.toString(),
                                "channel", "EMAIL",
                                "notificationType", "ORDER_CONFIRMED",
                                "payload", "{\"orderId\":\"123\"}"
                        ))))
                .andExpect(status().isCreated());

        mvc.perform(get("/notifications")
                        .param("userId", userId.toString())
                        .param("tenantId", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].notificationType").value("ORDER_CONFIRMED"));
    }

    @Test
    void postNotification_missingChannel_returns400() throws Exception {
        mvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "tenantId", UUID.randomUUID().toString(),
                                "userId", UUID.randomUUID().toString(),
                                "notificationType", "TEST"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealth_returnsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
