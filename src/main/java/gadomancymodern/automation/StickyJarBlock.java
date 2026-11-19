package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public class StickyJarBlock extends tc4revival.content.EssentiaJarBlock {
    public static final BooleanProperty VOID=BooleanProperty.create("void");
    public StickyJarBlock(){registerDefaultState(defaultBlockState().setValue(VOID,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(VOID);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new StickyJarBlockEntity(pos,state);}
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(level.getBlockEntity(pos) instanceof StickyJarBlockEntity jar)jar.changed();
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Gadomancy.STICKY_BE.get(),(world,pos,block,jar)->jar.tick());}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(!(level.getBlockEntity(pos) instanceof StickyJarBlockEntity jar))return super.getShape(state,level,pos,context);
        return switch(jar.support()){case DOWN->box(3,0,3,13,12,13);case UP->box(3,4,3,13,16,13);case NORTH->box(3,3,0,13,13,12);case SOUTH->box(3,3,4,13,13,16);case WEST->box(0,3,3,12,13,13);case EAST->box(4,3,3,16,13,13);};
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var entity=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);return entity instanceof StickyJarBlockEntity jar?java.util.List.of(jar.asItem()):java.util.List.of();
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof StickyJarBlockEntity jar))return InteractionResult.PASS;
        var local=jar.local(hit.getDirection());var held=player.getItemInHand(hand);
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return InteractionResult.FAIL;
        if(!player.isShiftKeyDown()&&held.is(tc4revival.content.TCItems.JAR_LABEL.get())&&jar.filter()==null){
            if(!level.isClientSide){var named=tc4revival.content.TCItems.JAR_LABEL.get().storedAspects(held);var aspect=jar.amount()>0?jar.aspect():named.isEmpty()?null:named.aspects().get(0);if(aspect!=null){jar.label(aspect,local);held.shrink(1);}}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state,level,pos,player,hand,new BlockHitResult(hit.getLocation(),local,pos,hit.isInside()));
    }
}
