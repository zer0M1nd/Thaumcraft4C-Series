package thmodern.plane;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.CastingWandItem;
import tc4revival.aspect.TCAspects;
public final class CloudBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
 private final CloudTypes.Type type;
 public CloudBlock(CloudTypes.Type type){super(Properties.of().strength(Float.MAX_VALUE,Float.MAX_VALUE).noCollission().noOcclusion().lightLevel(s->type.glowing()?15:0).sound(SoundType.WOOL));this.type=type;}
 public CloudTypes.Type type(){return type;}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public boolean skipRendering(BlockState state,BlockState other,Direction face){return other.is(this)||super.skipRendering(state,other,face);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CloudEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,CloudBlocks.ENTITY.get(),(l,p,s,e)->e.tick());}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(level.isClientSide)return;if(CloudTypes.burns(type))entity.setSecondsOnFire(6);else if(type.id().equals("acid_cloud"))entity.hurt(CloudEntity.dissolve(level),1);}
 @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context){
  if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
  var level=player.serverLevel();var pos=context.getClickedPos();
  if(!(stack.getItem() instanceof CastingWandItem wand)||!player.mayBuild()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack)||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"planarClouds"))return InteractionResult.FAIL;
  var state=level.getBlockState(pos);if(!state.is(this))return InteractionResult.FAIL;
  var cost=Map.of(TCAspects.catalog().require("aer"),100);
  if(net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0||!level.getBlockState(pos).equals(state)||!wand.consumeForPlayer(stack,player,cost,false,false,false))return InteractionResult.FAIL;
  var entity=level.getBlockEntity(pos);var saved=entity==null?null:entity.saveWithFullMetadata();
  var drop=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(this));drop.setInvulnerable(true);
  if(!level.addFreshEntity(drop))return InteractionResult.FAIL;
  if(!level.getBlockState(pos).equals(state)||!level.removeBlock(pos,false)){drop.discard();return InteractionResult.FAIL;}
  if(!wand.consumeForPlayer(stack,player,cost,false,false,true)){level.setBlock(pos,state,3);if(saved!=null&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).load(saved);drop.discard();return InteractionResult.FAIL;}
  return InteractionResult.SUCCESS;
 }
}
