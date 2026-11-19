package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Timed, synchronized controls; individual casters cannot cancel another caster's control. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class CrimsonControl {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,ThaumicConcilium.ID);
    public static final RegistryObject<MobEffect> CHAINED=EFFECTS.register("ethereal_shackles",()->new ControlEffect(0x90e8ed));
    public static final RegistryObject<MobEffect> ENSLAVED=EFFECTS.register("enslaved",()->new ControlEffect(0x9a173e));
    private static final String SOURCES="thaumicconcilium:enslavers";
    private static final class ControlEffect extends MobEffect {ControlEffect(int color){super(MobEffectCategory.HARMFUL,color);}}
    public static boolean chained(Player player){return player.hasEffect(CHAINED.get());}
    public static void chain(Player player,int ticks){if(!player.level().isClientSide && player.isAlive()&&!player.isSpectator()){
        player.stopUsingItem();player.addEffect(new MobEffectInstance(CHAINED.get(),Math.max(1,ticks),0,false,false,true));
    }}
    public static void enslave(Player player,int ticks){if(!player.level().isClientSide)player.addEffect(new MobEffectInstance(ENSLAVED.get(),Math.max(1,ticks),0,false,false,true));}
    public static void enslave(Player player,LivingEntity source,int ticks){
        if(player.level().isClientSide||source.level()!=player.level()||!source.isAlive()||!player.isAlive()||player.isSpectator()||player.isCreative())return;
        var data=player.getPersistentData().getCompound(SOURCES);data.putLong(source.getUUID().toString(),player.level().getGameTime()+Math.max(1,ticks));player.getPersistentData().put(SOURCES,data);
        enslave(player,ticks);
    }
    public static void release(Player player,LivingEntity source){
        if(player.level().isClientSide)return;var data=player.getPersistentData().getCompound(SOURCES);data.remove(source.getUUID().toString());
        player.getPersistentData().put(SOURCES,data);if(data.isEmpty())player.removeEffect(ENSLAVED.get());
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.getPersistentData().contains(SOURCES))return;
        var data=player.getPersistentData().getCompound(SOURCES);long now=player.level().getGameTime();
        for(String key:Set.copyOf(data.getAllKeys())){LivingEntity source=null;try{if(player.serverLevel().getEntity(UUID.fromString(key)) instanceof LivingEntity living)source=living;}catch(IllegalArgumentException ignored){}
            if(data.getLong(key)<=now||source==null||!source.isAlive()||source.distanceToSqr(player)>1024)data.remove(key);
        }
        if(data.isEmpty()){player.getPersistentData().remove(SOURCES);player.removeEffect(ENSLAVED.get());}else player.getPersistentData().put(SOURCES,data);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent event){
        if(chained(event.getEntity())&&event.isCancelable()){event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Start event){if(event.getEntity() instanceof Player player&&chained(player))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event){if(chained(event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(BlockEvent.BreakEvent event){if(chained(event.getPlayer()))event.setCanceled(true);}
    private CrimsonControl(){}
}
