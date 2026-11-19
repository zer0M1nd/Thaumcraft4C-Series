package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
public final class AmnesiacStoneBlockEntity extends InventoryBlockEntity {
 public void balance(ExperienceStoneBlockEntity stone){
  if(level==null||level.isClientSide||!level.hasChunkAt(worldPosition.above())||!(level.getBlockEntity(worldPosition.above()) instanceof tc4revival.content.BrainJarBlockEntity brain))return;
  int a=brain.experience(),b=stone.experience(),average=(a+b)/2;if(a==b)return;
  if(a<average){int n=Math.min(brain.insertExperience(average-a,true),stone.extractExperience(average-a,true));if(n>0){n=stone.extractExperience(n,false);brain.insertExperience(n,false);}}
  else {int wanted=average-b;if(wanted==0)wanted=1;if(wanted<0)return;int n=Math.min(brain.extractExperience(wanted,true),stone.insertExperience(wanted,true));if(n>0){n=brain.extractExperience(n,false);stone.insertExperience(n,false);}}
 }
 public void releaseBrain(tc4revival.content.BrainJarBlockEntity brain){
  if(level==null||level.isClientSide||brain.experience()<=1)return;
  int amount=brain.extractExperience(brain.experience()-1,false),emitted=emit(level,new Vec3(worldPosition.getX()+.5,worldPosition.getY()+1.25,worldPosition.getZ()+.5),amount);
  if(emitted<amount)brain.insertExperience(amount-emitted,false);
  for(var direction:net.minecraft.core.Direction.values())if(direction!=net.minecraft.core.Direction.UP){var pos=worldPosition.relative(direction);if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ExperienceStoneBlockEntity stone){int n=stone.quickDrain(),used=emit(level,worldPosition.above().getCenter(),n);stone.restoreUnemitted(n-used);}}
 }
 private int ticks,signal;
 public AmnesiacStoneBlockEntity(BlockPos pos,BlockState state){super(Content.AMNESIAC_STONE_TILE.get(),pos,state,0);}
 public int comparator(){return signal;}
 public static int emit(Level level,Vec3 point,int amount){int emitted=0;for(int i=0;i<128&&amount>0;i++){int value=net.minecraft.world.entity.ExperienceOrb.getExperienceValue(amount);if(!level.addFreshEntity(new SuppressedExperienceOrb(level,point.x,point.y,point.z,value,80)))break;amount-=value;emitted+=value;}return emitted;}
 public boolean drain(Player player,int levels,boolean top){if(level==null||level.isClientSide)return false;int before=automagy.logic.ExperienceLevels.total(player.experienceLevel,player.experienceProgress,player.getXpNeededForNextLevel());player.giveExperienceLevels(-levels);int after=automagy.logic.ExperienceLevels.total(player.experienceLevel,player.experienceProgress,player.getXpNeededForNextLevel());int amount=before-after;if(amount<=0)return false;
  Vec3 target=top?new Vec3(worldPosition.getX()+.5,worldPosition.getY()+1.25,worldPosition.getZ()+.5):worldPosition.getCenter().add(player.position().add(0,player.getBbHeight()/2,0)).scale(.5);
  if(!top&&new AABB(worldPosition).contains(target)&&player.getY()>worldPosition.getY()&&level.isEmptyBlock(worldPosition.above()))target=new Vec3(target.x,worldPosition.getY()+1.25,target.z);
  int emitted=emit(level,target,amount);if(emitted<amount)player.giveExperiencePoints(amount-emitted);if(emitted>0)level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,1,2+level.random.nextFloat()*.4f);return emitted>0;
 }
 public static void tick(Level level,BlockPos pos,BlockState state,AmnesiacStoneBlockEntity tile){if(level.isClientSide||++tile.ticks%2!=0)return;
  boolean brain=level.hasChunkAt(pos.above())&&level.getBlockEntity(pos.above()) instanceof tc4revival.content.BrainJarBlockEntity;
  int amount=0,capacity=0;if(brain){amount=((tc4revival.content.BrainJarBlockEntity)level.getBlockEntity(pos.above())).experience();capacity=tc4revival.content.BrainJarBlockEntity.CAPACITY;for(var direction:net.minecraft.core.Direction.values()){var adjacent=pos.relative(direction);if(direction!=net.minecraft.core.Direction.UP&&level.hasChunkAt(adjacent)&&level.getBlockEntity(adjacent) instanceof ExperienceStoneBlockEntity stone){amount+=stone.experience();capacity+=stone.capacity();}}}
  int signal=amount==0?0:1+(int)(14L*amount/capacity);if(tile.signal!=signal){tile.signal=signal;tile.sync();level.updateNeighbourForOutputSignal(pos,state.getBlock());}if(brain)return;
  var bounds=new AABB(pos.getX(),pos.getY()+1,pos.getZ(),pos.getX()+1,pos.getY()+1.5,pos.getZ()+1);
  for(var player:level.getEntitiesOfClass(Player.class,bounds,Player::isAlive))tile.drain(player,30,true);
  int budget=5;for(var item:level.getEntitiesOfClass(ItemEntity.class,bounds,ItemEntity::isAlive)){if(budget==0)break;var result=ExperienceItemRules.find(item.getItem());if(result==null)continue;int count=Math.min(budget,item.getItem().getCount());for(int i=0;i<count;i++){
   ItemEntity remainder=null;if(!result.remainder().isEmpty()){remainder=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),result.remainder().copy());if(!level.addFreshEntity(remainder))break;}
   // One orb per input makes emission atomic if another mod rejects entity creation.
   boolean emitted=level.addFreshEntity(new SuppressedExperienceOrb(level,pos.getX()+.5,pos.getY()+1.25,pos.getZ()+.5,result.experience(),80));if(!emitted){if(remainder!=null)remainder.discard();break;}item.getItem().shrink(1);budget--;if(item.getItem().isEmpty()){item.discard();break;}
  }}
 }
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.putInt("Comparator",signal);}
 @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);signal=Math.max(0,Math.min(15,tag.getInt("Comparator")));}
}
