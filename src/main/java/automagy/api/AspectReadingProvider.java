package automagy.api;

import automagy.logic.AspectReading;

/** Read-only observation for Automagy readers and essentia-locus aggregation; grants no extraction permission. */
public interface AspectReadingProvider {
    AspectReading aspectReading();
}
