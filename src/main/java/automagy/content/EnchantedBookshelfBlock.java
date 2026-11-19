package automagy.content;

import automagy.api.InventariumShelf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class EnchantedBookshelfBlock extends Block implements InventariumShelf {
    private final int slots;
    private final float enchantmentPower;
    public EnchantedBookshelfBlock(int slots,float enchantmentPower){super(Properties.of().strength(1.5f).sound(SoundType.WOOD));this.slots=slots;this.enchantmentPower=enchantmentPower;}
    @Override public int inventariumSlots(BlockState state,BlockGetter level,BlockPos position){return slots;}
    @Override public float getEnchantPowerBonus(BlockState state,LevelReader level,BlockPos position){return enchantmentPower;}
}
