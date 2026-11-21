package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;

/** Low-health capture preserves the original living entity, UUID and equipment in a Horizons jar. */
public final class AssistantCaptureGoal extends Goal {
    private final GolemEntity golem;private LivingEntity target;private int cooldown,repath;
    public AssistantCaptureGoal(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    public boolean valid(LivingEntity entity){
        if(!(entity instanceof Mob mob)||entity==golem||!entity.isAlive()||entity.isPassenger()||entity.isVehicle()||entity instanceof AbstractGolem||entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                ||entity instanceof ThaumcraftBossEntity||entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("forge:bosses")))||entity instanceof TamableAnimal tame&&tame.isTame()||entity instanceof Animal animal&&animal.isBaby()
                ||!golem.isWithinRestriction(entity.blockPosition())||!golem.getSensing().hasLineOfSight(entity))return false;
        if(golem.configuration().upgrades(4)>0&&entity.getHealth()>golem.getAttributeValue(Attributes.ATTACK_DAMAGE))return false;
        var wanted=new java.util.HashSet<ResourceLocation>();
        for(int slot=0;slot<golem.filters().size();slot++){
            var sample=golem.filters().sample(slot);if(!sample.isEmpty()&&net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(sample.getItem()).equals(ResourceLocation.parse("thaumichorizons:blood_sample"))&&sample.hasTag()){
                var id=ResourceLocation.tryParse(sample.getTag().getCompound("critter").getString("id"));if(id!=null&&!id.getPath().isBlank())wanted.add(id);
            }
        }
        if(!wanted.isEmpty())return wanted.contains(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        if(entity instanceof Enemy)return golem.configuration().upgrades(5)>0;
        return entity instanceof Animal||entity.getType().getCategory()==MobCategory.CREATURE||entity.getType().getCategory()==MobCategory.WATER_CREATURE||entity.getType().getCategory()==MobCategory.AMBIENT;
    }
    @Override public boolean canUse(){
        if(!golem.workEnabled()||!ConciliumGolems.available(ConciliumGolems.ASSISTANT)||!AssistantJarSupplyGoal.emptyJar(golem.carried()))return false;
        target=golem.level().getEntitiesOfClass(LivingEntity.class,golem.getBoundingBox().inflate(golem.configuration().range(),4,golem.configuration().range()),this::valid).stream().min(java.util.Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
        return target!=null&&golem.getNavigation().createPath(target,0)!=null;
    }
    @Override public void start(){repath=0;}
    @Override public boolean canContinueToUse(){return golem.workEnabled()&&target!=null&&valid(target)&&AssistantJarSupplyGoal.emptyJar(golem.carried());}
    @Override public void tick(){
        if(target==null)return;golem.getLookControl().setLookAt(target,30,30);if(--repath<=0){repath=4+golem.getRandom().nextInt(7);golem.getNavigation().moveTo(target,golem.navigationSpeed());}
        if(cooldown>0)cooldown--;double reach=target.getBbWidth()*target.getBbWidth()*4+1;
        if(golem.distanceToSqr(target)>reach||cooldown>0)return;
        cooldown=20-(golem.configuration().advanced()?2:0);golem.action();
        if(!(golem.level() instanceof ServerLevel level))return;
        var actor=ValetCommandGoal.actor(golem);actor.moveTo(golem.getX(),golem.getY(),golem.getZ(),golem.getYRot(),golem.getXRot());
        try(var interaction=thaumcraft.api.golems.GolemInteraction.enter(golem)){
            if(!level.getWorldBorder().isWithinBounds(target.blockPosition())||!level.mayInteract(actor,target.blockPosition())||net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(actor,target)))return;
        }
        if(target.getHealth()>golem.getAttributeValue(Attributes.ATTACK_DAMAGE)){try(var interaction=thaumcraft.api.golems.GolemInteraction.enter(golem)){golem.doHurtTarget(target);}return;}
        var tag=new CompoundTag();if(!target.save(tag)||tag.sizeInBytes()>1048576)return;tag.remove("Leash");tag.remove("Passengers");
        var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumichorizons:creature_jar"));if(item==null||item==net.minecraft.world.item.Items.AIR)return;
        var jar=new ItemStack(item);jar.getOrCreateTag().put("Captured",tag);jar.getOrCreateTag().putString("CreatureName",target.getName().getString());
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,golem.getX(),golem.getY()+1,golem.getZ(),jar);drop.setDefaultPickUpDelay();
        try(var interaction=thaumcraft.api.golems.GolemInteraction.enter(golem)){
            if(!level.addFreshEntity(drop))return;
            var carried=golem.carried();carried.shrink(1);golem.carried(carried);
            if(target instanceof Mob mob&&mob.isLeashed())mob.dropLeash(true,true);target.discard();
            golem.playSound(TCSounds.CRAFT_FAIL.get(),1,1);
        }
    }
    @Override public void stop(){target=null;golem.getNavigation().stop();}
}
