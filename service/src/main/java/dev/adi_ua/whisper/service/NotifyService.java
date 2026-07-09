package dev.adi_ua.whisper.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Dispatches push notifications via <a href="https://ntfy.sh">ntfy.sh</a>.
 *
 * <p>Each member's delivery channel is stored as {@code "ntfy:<topic>"} where
 * {@code <topic>} is a private, unguessable topic name (UUID or similar).
 * ntfy.sh delivers instantly to Android (native push), iOS (via the ntfy app),
 * and desktop (browser notification or polling).
 *
 * <p>No signup required on the sender or receiver side. The topic name is the
 * only credential.
 */
@Service
public class NotifyService {

    private final WebClient client;

    /**
     * @param baseUrl the ntfy server base URL (default: {@code https://ntfy.sh}).
     *                Override with a self-hosted ntfy instance via
     *                {@code WHISPER_NTFY_URL} environment variable.
     */
    public NotifyService(@Value("${whisper.ntfy.base-url}") String baseUrl) {
        this.client = WebClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Send a push notification to a single ntfy topic.
     *
     * @param topic  the ntfy topic name (must match what the recipient subscribed to)
     * @param phrase the safe-word phrase to deliver
     * @throws org.springframework.web.reactive.function.client.WebClientException
     *         if the ntfy server rejects or is unreachable
     */
    public void send(String topic, String phrase) {
        client.post()
                .uri("/" + topic)
                .header("Title", "\uD83E\uDD2B Whisper")
                .bodyValue(phrase)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
