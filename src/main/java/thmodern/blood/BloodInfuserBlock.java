package thmodern.blood;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thmodern.Horizons;
import thmodern.node.NodeDeviceBlock;

public final class BloodInfuserBlock extends NodeDeviceBlock<BloodInfuserEntity> {
    public BloodInfuserBlock(){super(Properties.of().strength(1,1).sound(SoundType.METAL),()->Horizons.BLOOD_INFUSER_ENTITY.get(),BloodInfuserEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof BloodInfuserEntity tile)net.minecraftforge.network.NetworkHooks.openScreen(server,tile,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof BloodInfuserEntity tile){Containers.dropContents(level,pos,tile);level.updateNeighbourForOutputSignal(pos,this);}
        super.onRemove(state,level,pos,next,moving);
    }
}
