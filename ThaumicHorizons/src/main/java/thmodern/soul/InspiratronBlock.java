package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thmodern.Horizons;

public final class InspiratronBlock extends thmodern.node.NodeDeviceBlock<InspiratronEntity> {
    public InspiratronBlock(){super(Properties.of().strength(.7f,1).sound(SoundType.GLASS),()->Horizons.INSPIRATRON_ENTITY.get(),InspiratronEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){SoulDeviceMenu.open(player,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof InspiratronEntity device)device.broken();super.onRemove(state,level,pos,next,moving);}
}
