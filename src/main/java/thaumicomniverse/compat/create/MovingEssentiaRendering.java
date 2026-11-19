package thaumicomniverse.compat.create;

import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.content.EssentiaReservoirBlockEntity;
import tc4revival.content.EssentiaVessel;

/** Keep transparent vessels and their contents in the same vanilla buffer pass. */
public final class MovingEssentiaRendering {
    private MovingEssentiaRendering() {}

    public static boolean inlineBody(BlockEntity blockEntity) {
        return blockEntity instanceof EssentiaVessel
                || blockEntity instanceof EssentiaReservoirBlockEntity
                || blockEntity instanceof EssentiaSpout.Tile;
    }
}
