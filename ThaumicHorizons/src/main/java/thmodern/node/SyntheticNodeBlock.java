package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import tc4revival.content.TCItems;
import thmodern.Horizons;

public final class SyntheticNodeBlock extends NodeDeviceBlock<SyntheticNodeEntity>{
    public SyntheticNodeBlock(){super(Properties.of().strength(.7f,1).sound(SoundType.GLASS).lightLevel(s->7),()->Horizons.SYNTHETIC_NODE_ENTITY.get(),SyntheticNodeEntity::tick);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(4.8,0,4.8,11.2,16,11.2);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var stack=player.getItemInHand(hand);
        if(!stack.is(TCItems.WISP_ESSENCE.get()))return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof SyntheticNodeEntity node)node.addEssence(stack);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof SyntheticNodeEntity node)node.dropEssences();
        super.onRemove(state,level,pos,next,moving);
    }
}
