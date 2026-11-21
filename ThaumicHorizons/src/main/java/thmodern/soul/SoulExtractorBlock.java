package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thmodern.Horizons;

public final class SoulExtractorBlock extends thmodern.node.NodeDeviceBlock<SoulExtractorEntity> {
    public SoulExtractorBlock(){super(Properties.of().strength(3,15).sound(SoundType.WOOD),()->Horizons.SOUL_EXTRACTOR_ENTITY.get(),SoulExtractorEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof SoulExtractorEntity extractor){
            var held=player.getItemInHand(hand);if(extractor.input.isItemValid(0,held))player.setItemInHand(hand,extractor.input.insertItem(0,held,false));
            else SoulDeviceMenu.open(player,pos);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof SoulExtractorEntity extractor)extractor.broken();super.onRemove(state,level,pos,next,moving);}
}
