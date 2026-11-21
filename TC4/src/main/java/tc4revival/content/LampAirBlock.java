package tc4revival.content;

import net.minecraft.world.level.block.AirBlock;

/** The old airy metadata 3 has no tile entity despite an unused TileArcaneLampLight class. */
public  class LampAirBlock extends AirBlock {
    public LampAirBlock() { super(Properties.of().air().replaceable().noCollission().noLootTable().lightLevel(state->15)); }
}
