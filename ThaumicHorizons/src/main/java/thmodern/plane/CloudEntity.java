package thmodern.plane;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Rain is authoritative on the server and follows the overworld even in a pocket plane. */
public final class CloudEntity extends BlockEntity {
 private boolean raining;private int remaining=-2,distance=256,ticks;
 public CloudEntity(BlockPos pos,BlockState state){super(CloudBlocks.ENTITY.get(),pos,state);}
 public CloudTypes.Type type(){return ((CloudBlock)getBlockState().getBlock()).type();}
 public boolean raining(){return raining;}public int distance(){return distance;}public int remaining(){return remaining;}
 public static DamageSource dissolve(Level level){return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumcraft:dissolve"))));}
 public void tick(){
  if(!(level instanceof ServerLevel server))return;
  if(remaining==-2||remaining==0)remaining=type().interval();
  if(ticks++%100==0){boolean next=server.getServer().overworld().isRaining();if(next!=raining){raining=next;sync();}}
  if(!raining)return;
  if(ticks%10==1){int old=distance;findGround(server);if(old!=distance)sync();weatherGround(server);}
  weatherEntities(server);
  if(remaining>0){remaining--;setChanged();if(remaining==0){CloudDrops.release(this,server);remaining=type().interval()/2+server.random.nextInt(type().interval()/2);}}
 }
 private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
 public BlockPos ground(){return worldPosition.below(distance);}
 private void findGround(ServerLevel server){
  int depth=Math.min(256,worldPosition.getY()-server.getMinBuildHeight());
  var hit=server.clip(new ClipContext(new Vec3(worldPosition.getX()+.5,worldPosition.getY()-.01,worldPosition.getZ()+.5),new Vec3(worldPosition.getX()+.5,worldPosition.getY()-depth,worldPosition.getZ()+.5),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,null));
  distance=hit.getType()==HitResult.Type.BLOCK?Math.max(1,worldPosition.getY()-hit.getBlockPos().getY()):Math.max(1,depth);
 }
 public boolean warm(){return level.getBiome(worldPosition).value().coldEnoughToSnow(worldPosition)==false;}
 private void weatherGround(ServerLevel server){
  var pos=ground();var state=server.getBlockState(pos);if(state.hasBlockEntity()||state.getDestroySpeed(server,pos)<0||server.isOutsideBuildHeight(pos))return;
  boolean burns=CloudTypes.burns(type()),acid=type().id().equals("acid_cloud");
  if(burns||acid){
   String behavior=thmodern.focus.LiquefactionFocusItem.behavior(state);if(state.getBlock() instanceof BushBlock||state.is(Blocks.SUGAR_CANE))behavior="air";boolean flammable=false;for(var face:Direction.values())flammable|=state.isFlammable(server,pos,face);
   BlockState result=null;
   if(behavior.equals("air")||flammable)result=(acid?Blocks.AIR:Blocks.FIRE).defaultBlockState();
   else if(behavior.equals("dirt"))result=Blocks.DIRT.defaultBlockState();
   else if(burns&&behavior.equals("water"))result=(server.dimensionType().ultraWarm()?Blocks.AIR:Blocks.WATER).defaultBlockState();
   else if(burns&&behavior.equals("tnt")){var tnt=new net.minecraft.world.entity.item.PrimedTnt(server,pos.getX()+.5,pos.getY(),pos.getZ()+.5,null);if(server.addFreshEntity(tnt))result=Blocks.AIR.defaultBlockState();}
   if(result!=null)server.setBlockAndUpdate(pos,result);
  }else if(warm()){
   if(state.is(Blocks.FARMLAND))server.setBlockAndUpdate(pos,state.setValue(FarmBlock.MOISTURE,7));
   else if(server.getBlockState(pos.above()).is(Blocks.FIRE))server.removeBlock(pos.above(),false);
   else if(state.is(Blocks.CAULDRON)||state.is(Blocks.WATER_CAULDRON))server.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
  }else if(state.isSolidRender(server,pos)&&server.isEmptyBlock(pos.above()))server.setBlockAndUpdate(pos.above(),Blocks.SNOW.defaultBlockState());
 }
 private void weatherEntities(ServerLevel server){
  var area=new AABB(worldPosition.getX(),worldPosition.getY()-distance,worldPosition.getZ(),worldPosition.getX()+1,worldPosition.getY(),worldPosition.getZ()+1);
  for(var entity:server.getEntities((Entity)null,area,e->e.isAlive())){
   if(CloudTypes.burns(type()))entity.setSecondsOnFire(6);
   else if(type().id().equals("acid_cloud"))entity.hurt(dissolve(server),1);
   else if(warm()){
    if(entity instanceof EnderMan||entity instanceof Blaze||entity.getType()==tc4revival.content.TCEntities.FIRE_BAT.get())entity.hurt(server.damageSources().drown(),1);
    entity.clearFire();
    if(type().id().equals("spore_cloud")&&entity instanceof Cow cow&&!(cow instanceof MushroomCow)){
     var replacement=EntityType.MOOSHROOM.create(server);if(replacement==null)continue;
     replacement.moveTo(cow.getX(),cow.getY(),cow.getZ(),cow.getYRot(),cow.getXRot());replacement.setHealth(cow.getHealth());replacement.setAge(cow.getAge());replacement.setCustomName(cow.getCustomName());replacement.setCustomNameVisible(cow.isCustomNameVisible());if(cow.isPersistenceRequired())replacement.setPersistenceRequired();
     if(server.addFreshEntity(replacement)){cow.discard();server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,replacement.getX(),replacement.getY()+.5,replacement.getZ(),1,0,0,0,0);}
    }
   }
  }
 }
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Raining",raining);tag.putInt("Remaining",remaining);tag.putInt("Distance",distance);}
 @Override public void load(CompoundTag tag){super.load(tag);raining=tag.getBoolean("Raining");remaining=tag.contains("Remaining")?Math.max(-2,Math.min(550,tag.getInt("Remaining"))):-2;distance=Math.max(1,Math.min(256,tag.getInt("Distance")));}
 @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).expandTowards(0,-distance,0);}
}
