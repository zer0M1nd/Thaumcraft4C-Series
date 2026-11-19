package dyesmodern;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid=ThaumicDyes.ID)
public final class DyesRunicEvents {
    private static final Map<ServerPlayer,Map<Integer,Long>> COOLDOWNS=new WeakHashMap<>();
    private DyesRunicEvents(){}
    public static int equippedUpgrade(ServerPlayer player,int upgrade){int count=0;for(var stack:player.getArmorSlots())if(stack.getItem() instanceof DyesArmorItem item&&item.runic()&&DyesArmorItem.upgrade(stack,0)==upgrade)count++;return count;}
    /** Modern repair of the original zero-durability guard. Only the selected first slot activates. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player instanceof FakePlayer||RunicShieldEvents.state(player).exactCharge()>0)return;
        for(int upgrade:new int[]{1,3,4,6}){
            int count=equippedUpgrade(player,upgrade);if(count==0||!ready(player,upgrade))continue;
            switch(upgrade){
                case 1->{player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,100*count,count));if(count>=4)player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200+100*(count-4),count-4));sound(player,TCSounds.RUNIC_SHIELD_EFFECT.get());}
                case 3->{player.level().explode(player,player.getX(),player.getY()+player.getBbHeight()/2,player.getZ(),1.5f+count*.5f,Level.ExplosionInteraction.NONE);sound(player,net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT);}
                case 4->{player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,count));sound(player,TCSounds.RUNIC_SHIELD_EFFECT.get());}
                case 6->{RunicShieldEvents.restore(player,8*count);sound(player,TCSounds.RUNIC_SHIELD_CHARGE.get());}
            }
        }
    }
    private static boolean ready(ServerPlayer player,int upgrade){var times=COOLDOWNS.computeIfAbsent(player,p->new HashMap<>());long now=player.serverLevel().getGameTime();if(now<times.getOrDefault(upgrade,Long.MIN_VALUE))return false;times.put(upgrade,now+switch(upgrade){case 1->400;case 6->2400;default->600;});return true;}
    private static void sound(ServerPlayer player,net.minecraft.sounds.SoundEvent sound){player.level().playSound(null,player,sound,net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){COOLDOWNS.remove(event.getEntity());}
    @SubscribeEvent public static void name(PlayerEvent.NameFormat event){
        var player=event.getEntity();int set=0;for(var stack:player.getArmorSlots())if(stack.getItem() instanceof DyesArmorItem item&&item.primal())set++;
        if(set==4&&player.getInventory().contains(new net.minecraft.world.item.ItemStack(TCItems.PRIMORDIAL_PEARL.get())))event.setDisplayname(Component.literal(player.getGameProfile().getName()+" the Warped"));
    }
}
