package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Breakable containers use the same data-driven rarity pools as the old loot bags. */
public  class EldritchLootBlock extends Block {
    private final boolean crate;
    private final int rarity;
    public EldritchLootBlock(boolean crate,int rarity) {
        super(Properties.of().strength(.15f,0).noOcclusion().sound(crate ? SoundType.WOOD : urnSound()));
        if (rarity<0 || rarity>2) throw new IllegalArgumentException("Invalid eldritch loot rarity");
        this.crate=crate; this.rarity=rarity;
    }
    private static SoundType urnSound() {
        return new net.minecraftforge.common.util.ForgeSoundType(1,.7f,TCSounds.URN_BREAK,
                TCSounds.URN_BREAK,TCSounds.URN_BREAK,
                ()->net.minecraft.sounds.SoundEvents.STONE_HIT,()->net.minecraft.sounds.SoundEvents.STONE_FALL);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return crate ? Block.box(1,0,1,15,14,15) : Block.box(2,1,2,14,13,14);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return getShape(state,level,pos,context); }

}
