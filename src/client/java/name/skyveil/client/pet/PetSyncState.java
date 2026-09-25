package name.skyveil.client.pet;

/** Tracks confidence in the equipped-pet observation, separately from the pet's identity. */
public enum PetSyncState {
    UNSYNCED,
    SYNCING,
    SYNCED,
    // Retained data needs fresh server evidence before being considered synchronized.
    STALE
}
