package wgmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.*;
import net.minecraft.sounds.*;

/** The two self-contained stone devices preserve their original tick rates and working volumes. */
public class StoneDevices {
    public static class Ageing extends BaseEntityBlock {
        public Ageing(){super(Properties.copy(Blocks.STONE).strength(4,6).isValidSpawn((state,level,pos,type)->false));}
        @Override public String getDescriptionId(){return "tile.WG_StoneDevice.timeStone";}
        @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AgeingTile(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,Content.AGEING_TILE.get(),AgeingTile::tick);}
    }
    public static class AgeingTile extends BlockEntity {
        public AgeingTile(BlockPos p,BlockState s){super(Content.AGEING_TILE.get(),p,s);}
        public static void tick(Level l,BlockPos p,BlockState s,AgeingTile t){for(var animal:l.getEntitiesOfClass(AgeableMob.class,new AABB(p.getX()-3,p.getY()-2,p.getZ()-3,p.getX()+4,p.getY()+3,p.getZ()+4))){int age=animal.getAge();if(age!=0)animal.setAge(age+(age<0?1:-1));}}
    }
    public static class Extruder extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
        public static final DirectionProperty FACING=BlockStateProperties.FACING;
        public Extruder(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f,6).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
        @Override public String getDescriptionId(){return "tile.WG_WoodenDevice.cobbleGen";}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
        @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ExtruderTile(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,Content.EXTRUDER_TILE.get(),ExtruderTile::tick);}
        @Override public InteractionResult onWandUseFirst(ItemStack wand,net.minecraft.world.item.context.UseOnContext c){if(!c.getLevel().isClientSide)c.getLevel().setBlockAndUpdate(c.getClickedPos(),c.getLevel().getBlockState(c.getClickedPos()).setValue(FACING,c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown()?c.getClickedFace().getOpposite():c.getClickedFace()));return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
    }
    public static class ExtruderTile extends BlockEntity {
        public int progress;
        public ExtruderTile(BlockPos p,BlockState s){super(Content.EXTRUDER_TILE.get(),p,s);}
        public boolean canWork(){if(level==null||level.hasNeighborSignal(worldPosition))return false;var dir=getBlockState().getValue(Extruder.FACING);var neighbor=level.getBlockEntity(worldPosition.relative(dir));if(neighbor==null)return true;var handler=neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER,dir.getOpposite()).orElse(null);if(handler==null&&neighbor instanceof Container container)handler=new net.minecraftforge.items.wrapper.InvWrapper(container);return handler==null||ItemHandlerHelper.insertItemStacked(handler,new ItemStack(Items.COBBLESTONE),true).isEmpty();}
        public static void tick(Level l,BlockPos p,BlockState s,ExtruderTile tile){if(!tile.canWork()){tile.progress=0;return;}if(++tile.progress==32&&!l.isClientSide)l.playSound(null,p,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.5f,2.6f+(l.random.nextFloat()-l.random.nextFloat())*.8f);if(tile.progress<40)return;tile.progress=0;if(l.isClientSide)return;var dir=s.getValue(Extruder.FACING);var stack=new ItemStack(Items.COBBLESTONE);var neighbor=l.getBlockEntity(p.relative(dir));if(neighbor!=null){var handler=neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER,dir.getOpposite()).orElse(null);if(handler==null&&neighbor instanceof WorldlyContainer container)handler=new net.minecraftforge.items.wrapper.SidedInvWrapper(container,dir.getOpposite());else if(handler==null&&neighbor instanceof Container container)handler=new net.minecraftforge.items.wrapper.InvWrapper(container);if(handler!=null)stack=ItemHandlerHelper.insertItemStacked(handler,stack,false);}if(!stack.isEmpty()){var item=new ItemEntity(l,p.getX()+.5+dir.getStepX()*.66,p.getY()+(dir.getAxis()==Direction.Axis.Y?.5+dir.getStepY()*.66:.4),p.getZ()+.5+dir.getStepZ()*.66,stack);item.setDeltaMovement(dir.getAxis()==Direction.Axis.Y?.025:dir.getStepX()*.075,dir.getAxis()==Direction.Axis.Y?dir.getStepY()*.075:.025,dir.getAxis()==Direction.Axis.Y?.025:dir.getStepZ()*.075);l.addFreshEntity(item);}l.playSound(null,p,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.5f,2.6f+(l.random.nextFloat()-l.random.nextFloat())*.8f);}
    }
}
