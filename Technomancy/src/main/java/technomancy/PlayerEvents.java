package technomancy;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Technomancy.ID)
public final class PlayerEvents {
    public static final List<String> TREASURES=List.of("fire_gem","pure_existence_plate","magical_beast_wing");
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().put(Technomancy.ID,PlayerPower.data(event.getOriginal()).copy());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerPower.data(event.getEntity());if(event.getEntity() instanceof ServerPlayer player)TechNetwork.sync(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer player&&player.tickCount%40==0)TechNetwork.sync(player);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||!TechConfig.TREASURE_VILLAGERS.get()||!(event.getEntity() instanceof Villager villager))return;
        var data=villager.getPersistentData();if(data.getBoolean("technomancyTreasureAttempt"))return;
        data.putBoolean("technomancyTreasureAttempt",true);int[] rarity={75,50,75};int selected=-1;
        for(int i=0;i<rarity.length;i++)if(villager.getRandom().nextInt(10000)<=rarity[i]&&(selected==-1||rarity[i]<rarity[selected]))selected=i;
        if(selected>=0)data.putString("technomancyTreasure",TREASURES.get(selected));
    }
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event) {
        if(event.getEntity().level().isClientSide)return;
        var data=event.getEntity().getPersistentData();int seal=data.getInt("technomancySeal");
        if(seal>0)data.putInt("technomancySeal",seal-1);
    }
    public static void treasureEffect(Player player,String treasure) {
        var effect=switch(treasure){case "fire_gem"->MobEffects.FIRE_RESISTANCE;case "pure_existence_plate"->MobEffects.DAMAGE_RESISTANCE;default->MobEffects.SLOW_FALLING;};
        int amplifier=treasure.equals("pure_existence_plate")?1:0;
        player.addEffect(new MobEffectInstance(effect,10,amplifier,true,false));
        if(player.tickCount%20==0)PlayerPower.affinity(player,switch(treasure){case "fire_gem"->PlayerPower.Affinity.FIRE;case "pure_existence_plate"->PlayerPower.Affinity.DARK;default->PlayerPower.Affinity.LIGHT;},1);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        var e=event.getEntity();if(e.level().isClientSide)return;
        if(e instanceof Player p)for(var s:p.getInventory().items)if(s.getItem() instanceof TechItem item&&TREASURES.contains(item.id))onHurt(event,item.id);
        if(TechConfig.TREASURE_VILLAGERS.get()&&e instanceof Villager&&e.getPersistentData().getInt("technomancySeal")==0)onHurt(event,e.getPersistentData().getString("technomancyTreasure"));
    }
    private static void onHurt(LivingHurtEvent event,String id) {
        var attacker=event.getSource().getEntity();
        switch(id) {
            case "fire_gem"->{if(event.getSource().is(DamageTypeTags.IS_FIRE))event.setCanceled(true);if(attacker!=null)attacker.setSecondsOnFire(15);}
            case "pure_existence_plate"->event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1,4));
            case "magical_beast_wing"->{if(attacker!=null){var away=attacker.position().subtract(event.getEntity().position()).normalize();attacker.push(away.x*1.5,2.1,away.z*1.5);attacker.hurtMarked=true;}}
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        var dead=event.getEntity();if(!(dead.level() instanceof ServerLevel level))return;
        if(event.getSource().getEntity() instanceof ServerPlayer player&&dead!=player) {
            boolean hasGem=false;
            for(var stack:player.getInventory().items)if(stack.is(TechRegistry.item("existence_gem"))) {
                hasGem=true;if(stack.getDamageValue()>0){stack.setDamageValue(Math.max(0,stack.getDamageValue()-Math.max(1,ExistenceLogic.value(dead)/2)));break;}
            }
            if(!hasGem)PlayerPower.train(player,1);
        }
        if(TechConfig.TREASURE_VILLAGERS.get()&&dead instanceof Villager&&dead.getPersistentData().getInt("technomancySeal")==0) {
            String treasure=dead.getPersistentData().getString("technomancyTreasure");
            if(treasure.equals("fire_gem")) {
                level.explode(dead,dead.getX(),dead.getY(),dead.getZ(),30,true,Level.ExplosionInteraction.MOB);
                for(var entity:level.getEntitiesOfClass(LivingEntity.class,dead.getBoundingBox().inflate(25)))if(!entity.isInvulnerable())entity.setSecondsOnFire(120);
            }else if(treasure.equals("pure_existence_plate")) {
                for(var entity:level.getEntitiesOfClass(LivingEntity.class,dead.getBoundingBox().inflate(5)))if(!entity.isInvulnerable())entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,1,2));
                var actor=WorldActions.actor(level,dead.blockPosition(),null);
                for(var p:BlockPos.betweenClosed(dead.blockPosition().offset(-5,-5,-5),dead.blockPosition().offset(5,5,5))) {
                    if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||level.isOutsideBuildHeight(p))continue;
                    var state=level.getBlockState(p);
                    if(state.getDestroySpeed(level,p)<0||net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,p,state,actor)))continue;
                    var snap=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,p);
                    if(!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent(snap,level.getBlockState(p.below()),actor)))level.setBlock(p,Blocks.OBSIDIAN.defaultBlockState(),3);
                }
            }else if(treasure.equals("magical_beast_wing"))for(var entity:level.getEntitiesOfClass(LivingEntity.class,dead.getBoundingBox().inflate(25))) {
                var away=entity.position().subtract(dead.position()).normalize();entity.push(away.x*1.5,8,away.z*1.5);entity.hurtMarked=true;
            }
        }
    }
    private PlayerEvents(){}
}
