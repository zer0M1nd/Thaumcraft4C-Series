package thaumcraft.api.config;

/** Profiles affect additions only. FOLLOW_CORE is available to addons, never to the core. */
public enum RevivalProfile {
    EVERYTHING, VANILLA, EASIEST, HARDEST, FOLLOW_CORE;

    public RevivalProfile resolve(RevivalProfile core) {
        return this == FOLLOW_CORE ? (core == FOLLOW_CORE ? EVERYTHING : core) : this;
    }
}
