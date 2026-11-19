package thmodern.alchemy;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import tc4revival.content.*;
import thaumcraft.api.nodes.NodeApi;
public final class AlchemiteExplosion extends Explosion {
 private final ServerLevel world;
 public AlchemiteExplosion(ServerLevel world,AlchemiteEntity source){
  super(world,source,null,new ExplosionDamageCalculator(){
   @Override public Optional<Float> getBlockExplosionResistance(Explosion explosion,BlockGetter level,BlockPos pos,BlockState state,FluidState fluid){return level.getBlockEntity(pos) instanceof AuraNodeBlockEntity?Optional.empty():super.getBlockExplosionResistance(explosion,level,pos,state,fluid);}
  },source.getX(),source.getY(),source.getZ(),5,false,BlockInteraction.DESTROY);this.world=world;
 }
 public void detonate(){if(net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world,this))return;explode();finalizeExplosion(true);for(var player:world.players())if(player.distanceToSqr(getPosition())<4096)player.connection.send(new net.minecraft.network.protocol.game.ClientboundExplodePacket(getPosition().x,getPosition().y,getPosition().z,5,List.of(),getHitPlayers().get(player)));}
 @Override public void finalizeExplosion(boolean particles){
  world.playSound(null,getPosition().x,getPosition().y,getPosition().z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.BLOCKS,4,(1+(world.random.nextFloat()-world.random.nextFloat())*.2f)*.7f);
  if(particles)world.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,getPosition().x,getPosition().y,getPosition().z,1,0,0,0,0);
  for(var pos:List.copyOf(getToBlow()))transform(pos);
 }
 public void transform(BlockPos pos){
  if(!world.hasChunkAt(pos)||world.isOutsideBuildHeight(pos)||!world.getWorldBorder().isWithinBounds(pos))return;var state=world.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(world,pos)<0)return;
  if(world.getBlockEntity(pos) instanceof AuraNodeBlockEntity node){
   double roll=world.random.nextDouble();if(roll<.25){var outputs=CrystalDrops.spawn(world,pos.getCenter(),node.aspects(),1);if(outputs!=null)EnergizedNodeBlock.collapse(world,pos);}
   else if(roll<.5)NodeApi.edit(node,NodeApi.getBase(node),node.permanentType(),AuraNodeModifier.FADING);
   else NodeApi.edit(node,NodeApi.getBase(node),AuraNodeType.UNSTABLE,null);return;
  }
  var input=new net.minecraft.world.item.ItemStack(state.getBlock());var aspects=thmodern.focus.DisintegrationFocusItem.aspects(input);
  if(aspects.isEmpty()){if(state.canDropFromExplosion(world,pos,this))Block.dropResources(state,world,pos,world.getBlockEntity(pos),getDirectSourceEntity(),net.minecraft.world.item.ItemStack.EMPTY);}
  else if(CrystalDrops.spawn(world,pos.getCenter(),aspects,1)==null)return;
  state.onBlockExploded(world,pos,this);
 }
}
