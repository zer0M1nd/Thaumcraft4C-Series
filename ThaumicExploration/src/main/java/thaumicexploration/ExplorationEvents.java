package thaumicexploration;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.content.*;

public class ExplorationEvents {
    public static boolean hotbar(Player player,Item item){for(int i=0;i<10;i++)if(player.getInventory().getItem(i).is(item))return true;return false;}
    @SubscribeEvent public void tick(LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide){if(entity instanceof Player p)ExplorationBoots.restoreStep(p);return;}
        if(EnchantmentHelper.getItemEnchantmentLevel(ExplorationMod.NIGHT_VISION.get(),entity.getItemBySlot(EquipmentSlot.HEAD))>0){
            var effect=entity.getEffect(MobEffects.NIGHT_VISION);if(effect==null||effect.getDuration()<202)entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,202,1,false,false));
        }
        if(entity instanceof Creeper creeper&&creeper.hasEffect(ExplorationMod.BOUND_EFFECT.get()))creeper.setSwellDir(-1);
        if(entity instanceof Player player){WandBehaviors.tickCap(player);taint(player);}
    }
    @SubscribeEvent public void attack(LivingHurtEvent event){
        var target=event.getEntity();if(target.level().isClientSide)return;
        if(event.getSource().getEntity() instanceof LivingEntity attacker){
            var held=attacker.getMainHandItem();int binding=EnchantmentHelper.getItemEnchantmentLevel(ExplorationMod.BINDING.get(),held);
            if(binding>0)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,1));
            if(binding>1&&(target instanceof EnderMan||target instanceof Creeper||target instanceof Player))target.addEffect(new MobEffectInstance(ExplorationMod.BOUND_EFFECT.get(),50,1));
            int disarm=Math.min(3,EnchantmentHelper.getItemEnchantmentLevel(ExplorationMod.DISARM.get(),held));
            if(disarm>0&&!(target instanceof Player)&&!target.getMainHandItem().isEmpty()&&target.getRandom().nextInt(10-2*disarm)==0){
                var drop=target.getMainHandItem().copy();target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.spawnAtLocation(drop);
            }
            if(held.getItem() instanceof CastingWandItem wand&&wand.rod(held).tag.equals("thaumicexploration:necromancer_staff"))target.addEffect(new MobEffectInstance(MobEffects.WITHER,100));
        }
        if(target instanceof Player player){
            if(hotbar(player,ExplorationMod.item("charm_no_taint"))&&player.getRandom().nextInt(4)<3){
                String type=event.getSource().getMsgId();
                if(event.getSource().getEntity() instanceof TaintedMob||type.equals("taint")||type.equals("tentacle")||type.equals("swarm")){event.setCanceled(true);return;}
            }
            if(player.onGround()&&top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).map(h->h.findFirstCurio(ExplorationMod.item("tentacle_ring")).isPresent()).orElse(false)){
                var spots=new java.util.ArrayList<BlockPos>();var level=player.level();
                for(var pos:BlockPos.betweenClosed(player.blockPosition().offset(-2,0,-2),player.blockPosition().offset(2,0,2)))
                    if(level.isEmptyBlock(pos)&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))spots.add(pos.immutable());
                if(!spots.isEmpty()){
                    var pos=spots.get(player.getRandom().nextInt(spots.size()));var minion=ExplorationMod.MINION.get().create(level);
                    if(minion!=null){minion.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);level.addFreshEntity(minion);minion.playSound(TCSounds.TENTACLE.get(),.5F,1);}
                }
            }
        }
    }
    @SubscribeEvent public void drops(LivingDropsEvent event){if(event.getSource().is(ExplorationDamage.SOULS))event.setCanceled(true);}
    @SubscribeEvent public void experience(LivingExperienceDropEvent event){if(event.getEntity().getLastDamageSource()!=null&&event.getEntity().getLastDamageSource().is(ExplorationDamage.SOULS))event.setDroppedExperience(0);}
    @SubscribeEvent public void teleport(EntityTeleportEvent.EnderEntity event){if(event.getEntityLiving().hasEffect(ExplorationMod.BOUND_EFFECT.get()))event.setCanceled(true);}
    @SubscribeEvent public void pearl(EntityTeleportEvent.EnderPearl event){if(event.getPlayer().hasEffect(ExplorationMod.BOUND_EFFECT.get()))event.setCanceled(true);}
    @SubscribeEvent public void target(LivingChangeTargetEvent event){if(event.getEntity() instanceof TaintedMob&&event.getNewTarget() instanceof Player p&&hotbar(p,ExplorationMod.item("charm_taint")))event.setNewTarget(null);}
    @SubscribeEvent public void jump(LivingEvent.LivingJumpEvent event){
        if(!(event.getEntity() instanceof Player p)||!(p.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ExplorationBoots boots))return;
        if(boots.meteor&&p.isShiftKeyDown()){
            var look=p.getLookAngle();double y=Math.max(look.y,Math.abs(look.x+look.z));
            var ascent=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.parse("thaumic_tinkerer:ascent_boost"));
            int rank=ascent==null?0:EnchantmentHelper.getEnchantmentLevel(ascent,p);double boost=rank==0?1:1+(rank+2)/4D;
            p.setDeltaMovement(p.getDeltaMovement().add(look.x*4*boost,y/1.5*boost,look.z*4*boost));
        } else p.setDeltaMovement(p.getDeltaMovement().add(0,(double).275F,0));
    }
    private void taint(Player p){
        var tag=p.getPersistentData();boolean tainted=hotbar(p,ExplorationMod.item("charm_taint"));
        if(tag.getBoolean("txTainted")&&!tainted&&!p.getAbilities().instabuild)p.hurt(ExplorationDamage.source(p.level(),ExplorationDamage.WITHDRAWAL),999);
        tag.putBoolean("txTainted",tainted);if(!tainted){tag.putInt("txGrace",0);return;}
        var flux=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse("thaumcraft:flux_taint"));if(flux!=null)p.removeEffect(flux);
        if(TCBiomes.isTainted(p.level(),p.blockPosition())){
            tag.putInt("txGrace",0);p.removeEffect(ExplorationMod.WITHDRAWAL.get());
            int tick=tag.getInt("txTaintFood");if(p.getFoodData().getFoodLevel()<4&&++tick>80){p.getFoodData().eat(1,0);tick=0;}tag.putInt("txTaintFood",tick);
        } else {
            int grace=tag.getInt("txGrace")+1;
            if(grace>100&&!p.hasEffect(ExplorationMod.WITHDRAWAL.get())){
                for(int i=0;i<10;i++){var s=p.getInventory().getItem(i);if(s.is(ExplorationMod.core("tainted_tendril"))||s.is(ExplorationMod.core("tainted_goo"))){s.shrink(1);grace=0;break;}}
                if(grace>100)p.addEffect(new MobEffectInstance(ExplorationMod.WITHDRAWAL.get(),100,1));
            }
            tag.putInt("txGrace",grace);
        }
    }
}
