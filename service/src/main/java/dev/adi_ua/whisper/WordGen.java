package dev.adi_ua.whisper;

/**
 * JNI bridge to the Rust whisper-wordgen library.
 * Loads libwhisper_wordgen from java.library.path.
 */
public final class WordGen {

    static {
        System.loadLibrary("whisper_wordgen");
    }

    /**
     * Generate a two-word phrase for the given group and rotation count.
     * The phrase is deterministic (same inputs = same output) and avoids
     * any phrase present in {@code history}.
     */
    public static native String generatePhrase(String groupId, long rotation, String[] history);

    private WordGen() {}
}
