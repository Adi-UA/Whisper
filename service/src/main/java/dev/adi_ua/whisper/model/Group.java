package dev.adi_ua.whisper.model;

import java.time.Instant;

public record Group(
    String id,
    String name,
    String pinHash,
    String schedule,
    String timezone,
    Instant createdAt
) {}
