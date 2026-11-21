package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No ticking or persistent payload is needed for the exposed void surface. */
public  class EldritchVoidBlockEntity extends BlockEntity {
    public EldritchVoidBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.VOID_ENTITY.get(),pos,state); }
}
