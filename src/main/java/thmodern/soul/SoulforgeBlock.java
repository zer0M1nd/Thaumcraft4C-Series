package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thmodern.Horizons;

public final class SoulforgeBlock extends thmodern.node.NodeDeviceBlock<SoulforgeEntity> {
    public SoulforgeBlock(){super(Properties.of().strength(3,15).sound(SoundType.METAL),()->Horizons.SOULFORGE_ENTITY.get(),SoulforgeEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof SoulforgeEntity forge&&player instanceof ServerPlayer server){
            return forge.bottle(server,player.getItemInHand(hand))?InteractionResult.CONSUME:InteractionResult.PASS;
        }return level.isClientSide&&player.getItemInHand(hand).is(tc4revival.content.TCItems.ESSENTIA_JAR.get())?InteractionResult.SUCCESS:InteractionResult.PASS;
    }
}
