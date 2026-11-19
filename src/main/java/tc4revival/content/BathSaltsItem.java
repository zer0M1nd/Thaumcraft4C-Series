package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public  class BathSaltsItem extends Item {
    public BathSaltsItem() { super(new Properties()); }
    @Override public int getEntityLifespan(ItemStack stack,Level level) { return 200; }
    @SubscribeEvent public static void expire(ItemExpireEvent event) {
        var entity=event.getEntity(); var level=entity.level();
        if (level.isClientSide || !entity.getItem().is(TCCleansing.BATH_SALTS.get())) return;
        var pos=entity.blockPosition(); var state=level.getBlockState(pos);
        // A complete expired stack makes one source, as in the old ItemExpireEvent handler.
        if (state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL)==0)
            level.setBlock(pos,TCCleansing.PURE_WATER.get().defaultBlockState(),3);
    }
}
