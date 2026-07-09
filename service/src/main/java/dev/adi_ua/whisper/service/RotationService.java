package dev.adi_ua.whisper.service;

import dev.adi_ua.whisper.WordGen;
import dev.adi_ua.whisper.model.Group;
import dev.adi_ua.whisper.model.Member;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the phrase rotation lifecycle for all groups.
 *
 * <p>On each rotation cycle:
 * <ol>
 *   <li>Fetch the group's recent phrase history (sliding window of 100).</li>
 *   <li>Call the Rust word generator via JNI to produce a new phrase that does
 *       not appear in the window.</li>
 *   <li>Persist the phrase to the {@code history} table.</li>
 *   <li>Fan out push notifications to every group member via {@link NotifyService}.</li>
 * </ol>
 *
 * <p>Exposes Prometheus counters: {@code whisper.rotations.total},
 * {@code whisper.notifications.sent}, {@code whisper.notifications.failed}.
 */
@Service
public class RotationService {

    private static final Logger log = LoggerFactory.getLogger(RotationService.class);

    /** Number of past phrases the Rust generator uses to guarantee no repeats. */
    private static final int HISTORY_WINDOW = 100;

    private final GroupRepository repo;
    private final NotifyService notify;
    private final boolean jniAvailable;

    private final Counter rotationsCounter;
    private final Counter notificationsSentCounter;
    private final Counter notificationsFailedCounter;

    public RotationService(GroupRepository repo, NotifyService notify, MeterRegistry registry) {
        this.repo = repo;
        this.notify = notify;
        this.jniAvailable = tryLoadNativeLib();
        this.rotationsCounter = Counter.builder("whisper.rotations.total")
                .description("Total successful phrase rotations")
                .register(registry);
        this.notificationsSentCounter = Counter.builder("whisper.notifications.sent")
                .description("Total push notifications delivered")
                .register(registry);
        this.notificationsFailedCounter = Counter.builder("whisper.notifications.failed")
                .description("Total push notification delivery failures")
                .register(registry);
        if (!jniAvailable) {
            log.warn("Rust native library (whisper_wordgen) not found on java.library.path. "
                + "Using fallback phrase generator. Set -Djava.library.path=wordgen/target/release to enable JNI.");
        }
    }

    /**
     * Rotate all groups whose schedule matches today.
     *
     * <p>Called once daily by the systemd timer. Groups with {@code schedule=daily}
     * rotate every call. Groups with {@code schedule=weekly} rotate only on Mondays
     * (in the group's configured timezone).
     *
     * @return the number of groups rotated
     */
    public int rotateAll() {
        List<Group> groups = repo.findAll();
        int count = 0;
        for (Group group : groups) {
            if (!shouldRotateToday(group)) {
                log.debug("Skipping group {} (schedule={}, not due today)", group.id(), group.schedule());
                continue;
            }
            try {
                rotate(group);
                count++;
            } catch (Exception e) {
                log.error("Rotation failed for group {}: {}", group.id(), e.getMessage(), e);
            }
        }
        return count;
    }

    /**
     * Determine whether a group should rotate on the current call.
     * Daily groups always rotate. Weekly groups rotate on Mondays only
     * (evaluated in the group's configured timezone).
     */
    private boolean shouldRotateToday(Group group) {
        return switch (group.schedule().toLowerCase()) {
            case "daily" -> true;
            case "weekly" -> {
                java.time.ZoneId zone = java.time.ZoneId.of(group.timezone());
                java.time.DayOfWeek today = java.time.ZonedDateTime.now(zone).getDayOfWeek();
                yield today == java.time.DayOfWeek.MONDAY;
            }
            default -> {
                log.warn("Unknown schedule '{}' for group {}, skipping", group.schedule(), group.id());
                yield false;
            }
        };
    }

    /**
     * Rotate a single group: generate a phrase, store it, and push to all members.
     *
     * @param group the group to rotate
     * @return the generated phrase
     */
    public String rotate(Group group) {
        List<String> history = repo.findHistory(group.id(), HISTORY_WINDOW);
        long rotation = history.size() + 1;

        String phrase = generatePhrase(group.id(), rotation, history);
        repo.addHistory(group.id(), phrase);
        rotationsCounter.increment();
        log.info("Rotated group {} → \"{}\"", group.id(), phrase);

        List<Member> members = repo.findMembers(group.id());
        for (Member member : members) {
            dispatchNotification(member, phrase, group.id());
        }

        return phrase;
    }

    private String generatePhrase(String groupId, long rotation, List<String> history) {
        if (jniAvailable) {
            return WordGen.generatePhrase(groupId, rotation, history.toArray(new String[0]));
        }
        // Fallback: deterministic but not cryptographically fair. Test use only.
        return "fallback-" + rotation;
    }

    private void dispatchNotification(Member member, String phrase, String groupId) {
        String channel = member.channel();
        if (channel.startsWith("ntfy:")) {
            String topic = channel.substring(5);
            try {
                notify.send(topic, phrase);
                notificationsSentCounter.increment();
                log.info("Notified member {} ({}) in group {}", member.name(), topic, groupId);
            } catch (Exception e) {
                notificationsFailedCounter.increment();
                // Log but continue: one failed delivery should not block others.
                log.error("Failed to notify member {} in group {}: {}", member.name(), groupId, e.getMessage());
            }
        } else {
            log.warn("Unknown channel type for member {} in group {}: {}", member.name(), groupId, channel);
        }
    }

    /**
     * Try to load the native Rust library. Returns {@code true} if successful.
     * Logs a warning on failure rather than throwing, so the service starts
     * cleanly in environments without the native build (CI, quick local tests).
     */
    private static boolean tryLoadNativeLib() {
        try {
            System.loadLibrary("whisper_wordgen");
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
