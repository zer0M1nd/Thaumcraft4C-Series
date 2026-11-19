package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.content.CastingWandItem;
import thmodern.Horizons;

public final class VisDynamoBlock extends NodeDeviceBlock<VisDynamoEntity>{
    public VisDynamoBlock(){super(Properties.of().strength(.7f,1).sound(SoundType.GLASS).lightLevel(s->7),()->Horizons.VIS_DYNAMO_ENTITY.get(),VisDynamoEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof CastingWandItem)return InteractionResult.PASS;
        if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof VisDynamoEntity node)
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new VisDynamoMenu(id,inventory,pos),Component.translatable(getDescriptionId())),pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
