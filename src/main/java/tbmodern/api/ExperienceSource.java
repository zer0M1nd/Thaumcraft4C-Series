package tbmodern.api;

/** Optional contract for nearby experience containers. Called only on the server thread. */
public interface ExperienceSource {
    /** Remove and return at most maximum raw experience points. Return zero when unavailable. */
    int extractExperience(int maximum);
}
