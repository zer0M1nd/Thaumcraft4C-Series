package automagy.api;

import net.minecraft.core.Direction;

/** Raw signal (0..32767) sent toward a world direction. Vanilla redstone output must still clamp to 15. */
public interface AmplifiedSignal {
    int amplifiedSignal(Direction output);
}
