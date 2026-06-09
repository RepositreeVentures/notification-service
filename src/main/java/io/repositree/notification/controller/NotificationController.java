package io.repositree.notification.controller;

import io.repositree.notification.dto.NotificationRequest;
import io.repositree.notification.dto.NotificationResponse;
import io.repositree.notification.repository.NotificationRepository;
import io.repositree.notification.service.NotificationService;
import io.repositree.outbox.ActorKind;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationService notificationService,
                                   NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse enqueue(@Valid @RequestBody NotificationRequest request) {
        return NotificationResponse.from(notificationService.enqueue(
                request.tenantId(), request.userId(),
                request.channel(), request.notificationType(),
                request.payload() != null ? request.payload() : "{}",
                ActorKind.SYSTEM, null, null
        ));
    }

    @GetMapping
    public List<NotificationResponse> listForUser(
            @RequestParam UUID tenantId,
            @RequestParam UUID userId) {
        return notificationRepository
                .findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        try {
            return NotificationResponse.from(notificationService.markRead(id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
