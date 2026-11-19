package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class EldritchCrystalBlockEntity extends BlockEntity {
    public EldritchCrystalBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.CRYSTAL_ENTITY.get(),pos,state); }
}
