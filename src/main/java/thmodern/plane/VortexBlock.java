package thmodern.plane;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.Horizons;
public final class VortexBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 public VortexBlock(){super(Properties.of().strength(-1,20000).noCollission().noOcclusion().noLootTable().sound(SoundType.EMPTY));}
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof VortexEntity vortex)vortex.removedWithEscrow();super.onRemove(state,level,pos,next,moving);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VortexEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Horizons.VORTEX_ENTITY.get(),(l,p,s,e)->e.tick());}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity player,net.minecraft.world.item.ItemStack stack){if(level.isClientSide||!(level.getBlockEntity(pos) instanceof VortexEntity vortex))return;var aspects=new tc4revival.aspect.AspectAmounts();var catalog=tc4revival.aspect.TCAspects.catalog();var compounds=catalog.all().stream().filter(a->!a.isPrimal()).toList();int entries=level.random.nextInt(4)+1;for(int i=0;i<entries;i++){var choices=level.random.nextInt(3)==0&&!compounds.isEmpty()?compounds:catalog.primals();int amount=level.random.nextInt(30);if(amount>0)aspects.merge(choices.get(level.random.nextInt(choices.size())),amount);}vortex.initialize(aspects,stack.is(Horizons.STABLE_VORTEX_ITEM.get()));}
 @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){if(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player&&context.getLevel().mayInteract(player,context.getClickedPos())&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof VortexEntity vortex)vortex.extract(player);return net.minecraft.world.InteractionResult.SUCCESS;}
}
