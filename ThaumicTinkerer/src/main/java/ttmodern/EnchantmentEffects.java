package ttmodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class EnchantmentEffects {
    private static int level(String key,ItemStack stack){return TinkererEnchantments.level(key,stack);}
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(event.getEntity().level().isClientSide||!(event.getSource().getEntity() instanceof LivingEntity attacker))return;
        var item=attacker.getMainHandItem();if(item.isEmpty())return;float damage=event.getAmount();
        if(attacker instanceof Player p&&p.level().isEmptyBlock(p.blockPosition().below()))damage*=1+.25f*level("pounce",p.getItemBySlot(EquipmentSlot.LEGS));
        int critical=level("final_strike",item);if(critical>0&&critical>attacker.getRandom().nextInt(19))damage*=3;
        if(attacker.getHealth()/attacker.getMaxHealth()<.5f)damage*=1+.1f*level("valiance",item);
        int focused=level("focused_strike",item),dispersed=level("dispersed_strike",item);
        if(focused>0||dispersed>0){
            var tag=item.getOrCreateTag();var target=event.getEntity().getUUID();int count=tag.hasUUID("TTLastTarget")&&tag.getUUID("TTLastTarget").equals(target)?Math.min(5,tag.getInt("TTConsecutive")+1):0;
            tag.putUUID("TTLastTarget",target);tag.putInt("TTConsecutive",count);
            if(focused>0)damage=damage/2*(1+.5f*count*focused);
            if(dispersed>0)damage=damage*(1+.6f*dispersed+count/5)/(1+.25f*dispersed+count*2.5f);
        }
        event.setAmount(damage);
        int vamp=level("vampirism",item);if(vamp>0&&damage>0){attacker.heal(vamp);attacker.level().playSound(null,event.getEntity().blockPosition(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,.6f,1);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Player player))return;
        int slow=EnchantmentHelper.getEnchantmentLevel(TinkererEnchantments.get("slow_fall"),player);
        if(slow>0&&!player.isShiftKeyDown()&&player.getDeltaMovement().y<-.0784000015258789&&player.fallDistance>=2.9){
            var motion=player.getDeltaMovement();player.setDeltaMovement(motion.x,motion.y/(1+slow*.33f),motion.z);player.fallDistance=Math.max(2.9f,player.fallDistance-slow/3f);player.hasImpulse=true;
            if(player.level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,player.getX()+.25,player.getY()-1,player.getZ()+.25,1,0,0,0,0);
        }
    }
    @SubscribeEvent public static void draw(LivingEntityUseItemEvent.Tick event){
        if(!(event.getEntity() instanceof Player)||!(event.getItem().getItem() instanceof BowItem))return;
        int quick=level("quick_draw",event.getItem());if(quick<=0)return;
        int extra=quick/6;if(event.getDuration()%(6-quick%6)==0)extra++;event.setDuration(Math.max(2,event.getDuration()-extra));
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){if(event.getEntity() instanceof Player p&&!p.isShiftKeyDown()){int boost=EnchantmentHelper.getEnchantmentLevel(TinkererEnchantments.get("ascent_boost"),p);if(boost>0){var v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*(boost+2)/2d,v.z);p.hasImpulse=true;}}}
    @SubscribeEvent(priority=EventPriority.LOW) public static void fall(LivingFallEvent event){
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide||event.getDistance()<=3)return;
        int shock=level("shockwave",p.getItemBySlot(EquipmentSlot.FEET));if(shock==0)return;
        for(var target:p.level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(p.position(),p.position()).inflate(10),e->e!=p))target.hurt(p.damageSources().fall(),.1f*shock*event.getDistance());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent event){
        if(level("tunnel",event.getPlayer().getMainHandItem())>0)event.getPlayer().getMainHandItem().getOrCreateTag().putFloat("TTTunnelDirection",event.getPlayer().getYRot());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void speed(PlayerEvent.BreakSpeed event){
        var player=event.getEntity();var item=player.getMainHandItem();if(item.isEmpty())return;
        var pos=event.getPosition().orElse(player.blockPosition());float hardness=event.getState().getDestroySpeed(player.level(),pos),speed=event.getNewSpeed();
        int shatter=level("shatter",item);if(shatter>0)speed*=hardness>20?3*shatter:.8f;
        int tunnel=level("tunnel",item);if(tunnel>0&&item.hasTag()&&item.getTag().contains("TTTunnelDirection"))speed*=Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot()-item.getTag().getFloat("TTTunnelDirection")))<50?1+.2f*tunnel:.3f;
        boolean disintegrate=level("disintegrate",item)>0,flaming=level("flaming_touch",item)>0;
        if(disintegrate||flaming){
            boolean wood=event.getState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)&&event.getState().is(net.minecraft.tags.BlockTags.PLANKS)||event.getState().is(net.minecraft.tags.BlockTags.LOGS)||event.getState().getSoundType(player.level(),pos,player)==net.minecraft.world.level.block.SoundType.WOOD;
            if(hardness>=0&&((disintegrate&&hardness<=1.5f)||(flaming&&wood)))speed=Float.MAX_VALUE;
            else {event.setCanceled(true);return;}
        }
        // Speed queries are pure. Normal successful mining applies durability once.
        event.setNewSpeed(speed);
    }
}
