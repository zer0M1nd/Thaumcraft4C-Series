package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BedrockPortalBlock extends Block {
    public BedrockPortalBlock(){super(Properties.copy(Blocks.BEDROCK).noCollission().noOcclusion().noLootTable().lightLevel(state->8));}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter world,BlockPos pos,Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(!world.isClientSide)BedrockTravel.replace(player,pos,Blocks.BEDROCK.defaultBlockState(),false);return InteractionResult.sidedSuccess(world.isClientSide);}
    @Override public void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){if(entity instanceof net.minecraft.server.level.ServerPlayer player)BedrockTravel.travel(player,pos);}
}
