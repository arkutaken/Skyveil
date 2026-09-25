package name.skyveil.client.combat;

/** Damage categories exposed as independent Compact Damage options. */
enum DamageSource {
    MELEE,
    CRIMSON_SWIPE,
    FEROCITY,
    VENOMOUS,
    FIRE,
    THUNDERLORD,
    PET,
    // Unclassified effects remain distinct from confirmed melee for source filtering.
    OTHER
}
