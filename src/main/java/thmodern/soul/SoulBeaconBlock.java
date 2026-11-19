package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thmodern.Horizons;
public final class SoulBeaconBlock extends thmodern.node.NodeDeviceBlock<SoulBeaconEntity> {
    public SoulBeaconBlock(){super(Properties.of().strength(7,7).lightLevel(s->15).sound(net.minecraft.world.level.block.SoundType.METAL),()->Horizons.SOUL_BEACON_ENTITY.get(),e->{});}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof SoulBeaconEntity beacon)beacon.bind(server);return InteractionResult.sidedSuccess(level.isClientSide);}
}
