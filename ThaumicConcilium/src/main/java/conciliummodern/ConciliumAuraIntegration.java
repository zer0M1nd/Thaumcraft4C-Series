package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original Forbidden Magic aura additions, loaded only after Gadomancy has installed its own effects. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConciliumAuraIntegration {
    @SubscribeEvent public static void complete(net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event){
        if(net.minecraftforge.fml.ModList.get().isLoaded("gadomancy"))event.enqueueWork(Linked::register);
    }
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
    public static final class Reload {
        @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event){refresh();}
        @SubscribeEvent public static void synced(net.minecraftforge.event.OnDatapackSyncEvent event){if(event.getPlayer()==null)refresh();}
        private static void refresh(){if(net.minecraftforge.fml.ModList.get().isLoaded("gadomancy"))ExtendedConciliumAura.register();}
    }
    private static final class Linked {
        private static void register(){
            add("infernus",5,8,e->e instanceof ItemEntity,(level,origin,entity)->{
                var drop=(ItemEntity)entity;var input=drop.getItem();
                var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(input),level).orElse(null);if(recipe==null)return;
                var output=recipe.assemble(new SimpleContainer(input),level.registryAccess());if(output.isEmpty())return;
                // Original infernus converts one output item per input item, independent of furnace output count.
                int amount=Math.min(input.getCount(),output.getMaxStackSize());
                if(amount<input.getCount()){
                    var remainder=new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),input.copyWithCount(input.getCount()-amount));
                    remainder.setDeltaMovement(drop.getDeltaMovement());remainder.setDefaultPickUpDelay();
                    if(!level.addFreshEntity(remainder))return;
                }
                drop.setItem(output.copyWithCount(amount));
            });
            if(!fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.NO_LUST))add("luxuria",40,8,e->e instanceof AgeableMob,(level,origin,entity)->{
                if(fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.NO_LUST))return;
                var animal=(AgeableMob)entity;if(animal.getAge()>0)animal.setAge(0);
            });
            add("gula",20,8,e->e instanceof Player,(level,origin,entity)->((Player)entity).getFoodData().eat(1,1));
            add("ira",80,8,e->e instanceof Monster,(level,origin,entity)->{
                var mob=(Monster)entity;var candidates=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(8),target->target!=mob&&target.isAlive());
                if(!candidates.isEmpty()){var target=candidates.get(level.random.nextInt(candidates.size()));mob.setLastHurtByMob(target);mob.setTarget(target);}
            });
            add("invidia",80,8,e->e instanceof LivingEntity&&!(e instanceof Player),(level,origin,entity)->{
                var living=(LivingEntity)entity;var held=living.getMainHandItem();if(!held.isEmpty()&&living.spawnAtLocation(held.copy(),.5f)!=null)living.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            });
            add("desidia",4,8,e->e instanceof LivingEntity,(level,origin,entity)->{
                var living=(LivingEntity)entity;
                for(var effect:java.util.List.copyOf(living.getActiveEffects()))if(!effect.isInfiniteDuration())
                    living.addEffect(new MobEffectInstance(effect.getEffect(),(int)Math.min(Integer.MAX_VALUE-1L,(long)effect.getDuration()+20),effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
            });
            add("superbia",8,32,e->e instanceof Monster,(level,origin,entity)->{
                if(entity.distanceToSqr(origin.getX(),origin.getY(),origin.getZ())>=1024)return;
                // The original target vector encoded world Y; use horizontal outward motion so altitude cannot reverse it.
                var away=new net.minecraft.world.phys.Vec3(entity.getX()-origin.getX(),0,entity.getZ()-origin.getZ());
                if(away.lengthSqr()>1e-10){var motion=away.normalize();entity.setDeltaMovement(motion.x,entity.getDeltaMovement().y,motion.z);entity.hasImpulse=true;}
            });
            ExtendedConciliumAura.register();
        }
        private static void add(String name,int interval,double range,java.util.function.Predicate<Entity> accepts,Effect action){
            String aspect="forbidden_magic:"+name;
            gadomancymodern.aura.AuraEffectRegistry.register(aspect,new gadomancymodern.aura.AuraEffect(){
                @Override public int interval(){return interval;}
                @Override public double range(){return range;}
                @Override public boolean accepts(Entity entity){return accepts.test(entity);}
                @Override public void entity(ServerLevel level,BlockPos origin,Entity entity){action.apply(level,origin,entity);}
            });
        }
        @FunctionalInterface private interface Effect{void apply(ServerLevel level,BlockPos origin,Entity entity);}
    }
    private ConciliumAuraIntegration(){}
}
