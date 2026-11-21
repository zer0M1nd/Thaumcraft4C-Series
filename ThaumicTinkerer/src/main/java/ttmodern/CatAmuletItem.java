package ttmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class CatAmuletItem extends Item implements ICurioItem {
    public CatAmuletItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    static boolean aura(Mob mob){
        if(mob.level().isClientSide||!TinkererConfig.KAMI.get())return false;
        for(var player:mob.level().getEntitiesOfClass(Player.class,mob.getBoundingBox().inflate(10,4,10)))if(player.isAlive()&&!player.isSpectator()&&CuriosApi.getCuriosInventory(player).resolve().map(handler->handler.findFirstCurio(stack->stack.is(Tinkerer.CAT_AMULET.get())).isPresent()).orElse(false))return true;
        return false;
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide&&event.getEntity() instanceof Creeper creeper)creeper.goalSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<Player>(creeper,Player.class,6,1,1.2,player->aura(creeper)));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void target(LivingChangeTargetEvent event){
        if(event.getEntity() instanceof Mob mob&&event.getNewTarget() instanceof Player player&&(mob instanceof Creeper||mob.getLastHurtByMob()!=player)&&aura(mob))event.setCanceled(true);
    }
    // Vanilla has no defuse setter. Names are verified against the frozen 1.20.1 mappings;
    // no Thaumcraft classes or registries are accessed or modified here.
    private static final class CreeperFields {
        static final java.lang.reflect.Field SWELL=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Creeper.class,"f_32270_");
        static final java.lang.reflect.Field IGNITED=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Creeper.class,"f_32275_");
    }
    @SuppressWarnings("unchecked") static void calm(Creeper creeper){try{
        CreeperFields.SWELL.setInt(creeper,Math.min(2,CreeperFields.SWELL.getInt(creeper)));
        creeper.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Boolean>)CreeperFields.IGNITED.get(null),false);
    }catch(IllegalAccessException failure){throw new IllegalStateException("Cannot defuse Minecraft 1.20.1 creeper",failure);}}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Creeper creeper&&aura(creeper)){creeper.setTarget(null);creeper.setSwellDir(-1);calm(creeper);}
    }
}
