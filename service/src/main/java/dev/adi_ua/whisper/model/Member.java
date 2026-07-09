package dev.adi_ua.whisper.model;

import java.time.Instant;

public record Member(
    String id,
    String groupId,
    String name,
    String channel,
    Instant joinedAt
) {}
