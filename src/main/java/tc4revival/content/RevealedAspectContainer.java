package tc4revival.content;

import tc4revival.aspect.AspectAmounts;

/** Read-only contents exposed by revealing goggles; does not grant discovery or allow extraction. */
public interface RevealedAspectContainer {
    AspectAmounts revealedAspects();
    default float popupHeight() { return 0; }
}
