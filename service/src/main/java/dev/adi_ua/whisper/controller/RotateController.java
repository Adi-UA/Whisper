package dev.adi_ua.whisper.controller;

import dev.adi_ua.whisper.service.RotationService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triggers phrase rotation for all groups.
 *
 * <p>Rate-limited to prevent abuse. The limit is configurable via
 * {@code whisper.rate-limit.rotate-rpm} (default: 10 requests per minute).
 * Uses an in-memory token bucket per client IP (or a single global bucket
 * for the common case where only one cron caller exists).
 */
@RestController
public class RotateController {

    private final RotationService rotationService;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int rpm;

    public RotateController(
            RotationService rotationService,
            @Value("${whisper.rate-limit.rotate-rpm:10}") int rpm) {
        this.rotationService = rotationService;
        this.rpm = rpm;
    }

    /**
     * Trigger a rotation for all groups.
     *
     * <p>Called by GitHub Actions cron, Windows Task Scheduler, or any external
     * scheduler. Responds 429 if the caller exceeds the rate limit.
     *
     * @return a JSON map with the number of groups rotated
     */
    @PostMapping("/api/rotate")
    public Map<String, Object> rotate() {
        Bucket bucket = buckets.computeIfAbsent("global", k -> createBucket());
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
        int count = rotationService.rotateAll();
        return Map.of("rotated", count);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(rpm, Duration.ofMinutes(1)))
                .build();
    }
}
