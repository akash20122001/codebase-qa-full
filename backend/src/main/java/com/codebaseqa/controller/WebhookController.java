package com.codebaseqa.controller;

import com.codebaseqa.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling GitHub webhook events.
 * Receives push events and triggers incremental re-indexing.
 */
@RestController
@RequestMapping("/api/repos/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Handle GitHub push webhook events.
     * Verifies signature and triggers incremental re-indexing for changed files.
     *
     * @param signature GitHub signature header (X-Hub-Signature-256)
     * @param payload   GitHub push event payload
     * @return Success response
     */
    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("=== GitHub Webhook Received ===");
        log.info("Signature present: {}", signature != null);
        log.info("Payload keys: {}", payload.keySet());

        // Process the webhook
        Map<String, Object> result = webhookService.processGitHubPushEvent(signature, payload);

        log.info("Webhook processed successfully: {}", result);
        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint for webhook configuration.
     */
    @GetMapping("/github/health")
    public ResponseEntity<Map<String, String>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Webhook endpoint is ready to receive GitHub push events"
        ));
    }
}
