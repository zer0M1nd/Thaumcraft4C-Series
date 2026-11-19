package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public final class AmnesiacStoneBlock extends BaseEntityBlock {
 public AmnesiacStoneBlock(){super(Properties.of().strength(2.5f).sound(SoundType.STONE));}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AmnesiacStoneBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.AMNESIAC_STONE_TILE.get(),AmnesiacStoneBlockEntity::tick);}
 @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
 @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return l.getBlockEntity(p) instanceof AmnesiacStoneBlockEntity t?t.comparator():0;}
 @Override public net.minecraft.world.InteractionResult use(BlockState s,Level l,BlockPos p,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof AmnesiacStoneBlockEntity tile){tile.drain(player,player.isShiftKeyDown()?5:1,false);if(player.isShiftKeyDown())player.hurt(l.damageSources().magic(),.01f);}return net.minecraft.world.InteractionResult.sidedSuccess(l.isClientSide);}
}
