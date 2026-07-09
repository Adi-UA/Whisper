//! Built-in word lists for phrase generation.

/// Default word list: 200 evocative, easy-to-remember words.
/// Mix of adjectives and nouns so any two-word combination sounds natural.
pub const DEFAULT_WORDS: &[&str] = &[
    // Adjectives / descriptors
    "amber", "arctic", "bold", "brass", "bright", "calm", "cedar", "cobalt",
    "coral", "crimson", "crystal", "dusk", "ember", "faded", "fierce", "flint",
    "frost", "gentle", "gilded", "golden", "granite", "hollow", "iron", "ivory",
    "jade", "keen", "lunar", "marble", "misty", "mossy", "neon", "noble",
    "obsidian", "opal", "pale", "pearl", "pine", "plum", "polar", "quiet",
    "rapid", "raven", "rosy", "rusted", "sable", "sage", "scarlet", "shadow",
    "silent", "silver", "slate", "smoky", "solar", "stark", "steel", "stone",
    "stormy", "sunlit", "swift", "tawny", "tidal", "timber", "twilight", "velvet",
    "violet", "vivid", "warm", "wild", "woven", "zinc",
    // Nouns / objects
    "anchor", "arrow", "atlas", "badge", "beacon", "blade", "bloom", "bolt",
    "bridge", "canyon", "castle", "cipher", "cliff", "comet", "compass", "crown",
    "dagger", "delta", "dove", "dragon", "drift", "eagle", "echo", "falcon",
    "feather", "flame", "flare", "forge", "fox", "gate", "glacier", "grove",
    "harbor", "hawk", "haven", "heron", "horizon", "island", "jaguar", "lantern",
    "lark", "ledge", "lightning", "lion", "lotus", "lynx", "maple", "meadow",
    "mirror", "moon", "moth", "nebula", "oak", "ocean", "orbit", "otter",
    "owl", "panther", "peak", "pebble", "phoenix", "pilot", "prism", "quartz",
    "reef", "ridge", "river", "rocket", "sage", "sail", "sequoia", "shark",
    "shelter", "sierra", "signal", "sparrow", "sphinx", "spire", "summit",
    "thunder", "tide", "tiger", "torch", "trail", "trident", "tulip", "tunnel",
    "viper", "voyage", "walrus", "wave", "willow", "wolf", "wren", "zenith",
    // Extra flavor
    "breeze", "canyon", "chapel", "cinder", "clover", "coral", "cosmos",
    "crescent", "dune", "ember", "fable", "fjord", "glyph", "hearth", "hive",
    "jewel", "karma", "kindle", "lumen", "mango", "nexus", "oasis", "orchid",
    "panda", "quill", "relic", "rumble", "saffron", "thistle", "umbra", "vapor",
];
