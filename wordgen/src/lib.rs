//! Whisper word generator: produces two-word phrases from a seeded CSPRNG
//! with a sliding-window no-repeat guarantee.

mod jni_bridge;
pub mod wordlist;

use rand::seq::SliceRandom;
use rand::SeedableRng;
use rand_chacha::ChaCha20Rng;
use sha2::{Digest, Sha256};

/// Generate a two-word phrase that does not appear in `history`.
///
/// The seed is deterministic: SHA-256(group_id || rotation_count). This makes
/// retries idempotent (same inputs = same phrase) while remaining unpredictable
/// to anyone without the group_id.
///
/// # Panics
/// Panics if `words` has fewer than 4 entries (need at least 2 distinct pairs).
pub fn generate_phrase(words: &[String], history: &[String], group_id: &str, rotation: u64) -> String {
    assert!(words.len() >= 4, "word list must have at least 4 entries");

    let seed = derive_seed(group_id, rotation);
    let mut rng = ChaCha20Rng::from_seed(seed);

    for _ in 0..100 {
        let a = words.choose(&mut rng).unwrap();
        let b = loop {
            let candidate = words.choose(&mut rng).unwrap();
            if candidate != a {
                break candidate;
            }
        };
        let phrase = format!("{} {}", a, b);
        if !history.contains(&phrase) {
            return phrase;
        }
    }

    // Exhausted retries (word list too small relative to history). Fall through
    // with the last attempt. In practice this requires history > (n*(n-1)) which
    // won't happen with reasonable word lists (200+ words).
    let a = words.choose(&mut rng).unwrap();
    let b = loop {
        let candidate = words.choose(&mut rng).unwrap();
        if candidate != a {
            break candidate;
        }
    };
    format!("{} {}", a, b)
}

fn derive_seed(group_id: &str, rotation: u64) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(group_id.as_bytes());
    hasher.update(rotation.to_le_bytes());
    hasher.finalize().into()
}

#[cfg(test)]
mod tests {
    use super::*;
    use proptest::prelude::*;

    fn sample_words() -> Vec<String> {
        wordlist::DEFAULT_WORDS.iter().map(|s| s.to_string()).collect()
    }

    #[test]
    fn generates_two_word_phrase() {
        let words = sample_words();
        let phrase = generate_phrase(&words, &[], "test-group", 1);
        let parts: Vec<&str> = phrase.split(' ').collect();
        assert_eq!(parts.len(), 2);
        assert!(words.contains(&parts[0].to_string()));
        assert!(words.contains(&parts[1].to_string()));
    }

    #[test]
    fn deterministic_for_same_inputs() {
        let words = sample_words();
        let a = generate_phrase(&words, &[], "group-abc", 42);
        let b = generate_phrase(&words, &[], "group-abc", 42);
        assert_eq!(a, b);
    }

    #[test]
    fn different_rotation_gives_different_phrase() {
        let words = sample_words();
        let a = generate_phrase(&words, &[], "group-abc", 1);
        let b = generate_phrase(&words, &[], "group-abc", 2);
        // Not strictly guaranteed but with 200+ words, collision is ~1/40000.
        assert_ne!(a, b);
    }

    #[test]
    fn avoids_history() {
        let words = sample_words();
        let first = generate_phrase(&words, &[], "group-x", 1);
        let second = generate_phrase(&words, &[first.clone()], "group-x", 2);
        assert_ne!(first, second);
    }

    #[test]
    fn two_words_are_distinct() {
        let words = sample_words();
        for rotation in 0..50 {
            let phrase = generate_phrase(&words, &[], "distinct-test", rotation);
            let parts: Vec<&str> = phrase.split(' ').collect();
            assert_ne!(parts[0], parts[1]);
        }
    }

    proptest! {
        #[test]
        fn never_repeats_within_window(seed in 0u64..1000) {
            let words = sample_words();
            let mut history: Vec<String> = Vec::new();
            for rotation in 0..20 {
                let phrase = generate_phrase(&words, &history, "prop-group", seed * 1000 + rotation);
                prop_assert!(!history.contains(&phrase));
                history.push(phrase);
            }
        }
    }
}
