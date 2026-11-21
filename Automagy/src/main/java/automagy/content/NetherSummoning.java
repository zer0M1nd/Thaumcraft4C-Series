package automagy.content;
import automagy.logic.NetherRuneCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;
/** Returns zero when the cycle completes, otherwise the legacy retry delay. */
final class NetherSummoning {
 static int spawn(ServerLevel level,BlockPos origin,int depth,NetherRuneCatalog.Definition rune,int count){return spawn(level,origin,depth,rune,count,null,null);}
 static int spawn(ServerLevel level,BlockPos origin,int depth,NetherRuneCatalog.Definition rune,int count,automagy.api.NetherRune extension,net.minecraft.world.level.block.state.BlockState runeState){if(count<=0)return 0;int delay=0;boolean any=false;double middle=origin.getY()-(depth-1)/2.0;
  for(int n=0;n<count;n++){
   Mob mob;
   if(extension!=null){mob=extension.createCreature(runeState,level);}
   else{
    var typeId=net.minecraft.resources.ResourceLocation.tryParse(rune.output());
    EntityType<?> type=typeId==null?null:net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(typeId);
    if(rune.key().equals("skeleton")&&level.random.nextInt(5)>0)type=EntityType.WITHER_SKELETON;
    Entity raw=type==null?null:type.create(level);mob=raw instanceof Mob living?living:null;if(raw!=null&&mob==null)raw.discard();
   }
   if(mob==null){delay=60;continue;}
   // A factory may never transfer an existing entity, or an entity from another dimension.
   if(mob.level()!=level||mob.isRemoved()||level.getEntity(mob.getUUID())!=null){delay=60;continue;}
   var bounds=new AABB(origin.getX(),middle,origin.getZ(),origin.getX()+1,middle+1,origin.getZ()+1).inflate(6,4,6);
   int nearby=level.getEntitiesOfClass(Mob.class,bounds,m->rune.key().equals("skeleton")?m instanceof net.minecraft.world.entity.monster.AbstractSkeleton:mob.getClass().isInstance(m)).size();
   if(nearby>=9){mob.discard();delay=60;continue;}boolean spawned=false;
   // Finalize once, before checking dimensions (magma cube size and equipment can change).
   mob.moveTo(origin.getX()+.5,middle-mob.getBbHeight()/2,origin.getZ()+.5,0,0);
   net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(mob,level,level.getCurrentDifficultyAt(origin),MobSpawnType.MOB_SUMMONED,null,null);
   if(mob instanceof net.minecraft.world.entity.monster.WitherSkeleton){for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR)mob.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);mob.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));var attack=mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(4);}
   if(mob instanceof net.minecraft.world.entity.monster.ZombifiedPiglin pig&&level.random.nextInt(20)==0)pig.startPersistentAngerTimer();
   for(int attempt=0;attempt<10;attempt++){double x=origin.getX()+.5+(level.random.nextDouble()-level.random.nextDouble())*3,z=origin.getZ()+.5+(level.random.nextDouble()-level.random.nextDouble())*3;mob.moveTo(x,middle-mob.getBbHeight()/2,z,level.random.nextFloat()*360,0);
    var box=mob.getBoundingBox();if(!level.hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))||!level.noCollision(mob)||level.containsAnyLiquid(box))continue;
    if(level.addFreshEntity(mob)){spawned=true;any=true;
     if(extension!=null)try{extension.creatureSpawned(runeState,mob);}catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Automagy rune {} post-spawn callback failed; preserving successful summon",rune.key(),failure);}
     level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ(),12,.3,.3,.3,.05);break;}
    break;
   }if(!spawned){mob.discard();delay=Math.max(delay,10);}
  }return any?0:Math.max(10,delay);
 }
 private NetherSummoning(){}
}
