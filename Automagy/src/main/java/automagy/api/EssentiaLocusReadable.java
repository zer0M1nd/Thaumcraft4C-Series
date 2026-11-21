package automagy.api;

/** Opt-in observation interface for an addon's container. Return contents and per-aspect capacity;
 * observing a locus must never extract essentia or expose the locus itself as a container. */
public interface EssentiaLocusReadable extends AspectReadingProvider {}
