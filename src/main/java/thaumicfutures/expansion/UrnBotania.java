package thaumicfutures.expansion;

import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.block.PetalApothecary;

/** Loaded only with Botania. The API owns state updates and water/lava exclusivity. */
final class UrnBotania {
    static int fill(BlockEntity target,int amount,boolean execute) {
        if(amount<1000||!(target instanceof PetalApothecary apothecary)||apothecary.getFluid()!=PetalApothecary.State.EMPTY)return 0;
        if(execute){apothecary.setFluid(PetalApothecary.State.WATER);target.setChanged();}
        return 1000;
    }
}
