package conciliummodern;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class CrimsonEvents {
    private static final Map<String,Integer> LOOT=Map.of("minecraft:chests/simple_dungeon",8,"minecraft:chests/jungle_temple",8,"minecraft:chests/desert_pyramid",8,
        "minecraft:chests/abandoned_mineshaft",4,"minecraft:chests/stronghold_corridor",4,"minecraft:chests/stronghold_crossing",4,"minecraft:chests/stronghold_library",10,"thaumcraft:bags/items_2",30);
    @SubscribeEvent public static void loot(LootTableLoadEvent event){
        if(!CrimsonContent.available())return;Integer weight=LOOT.get(event.getName().toString());if(weight==null)return;
        // Forge exposes pool addition but no weighted-entry addition. Append to the existing first pool,
        // preserving all provider conditions, roll counts, functions and other mods' entries.
        try{
            List<LootPool> pools=net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(LootTable.class,event.getTable(),"f_79109_");
            if(pools==null||pools.isEmpty())return;var pool=pools.get(0);
            LootPoolEntryContainer[] entries=net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(LootPool.class,pool,"f_79023_");
            var appended=Arrays.copyOf(entries,entries.length+1);appended[entries.length]=LootItem.lootTableItem(CrimsonContent.ANNALES.get()).setWeight(weight).build();
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(LootPool.class,pool,appended,"f_79023_");
        }catch(RuntimeException failure){throw new IllegalStateException("Cannot append Crimson Annales to "+event.getName(),failure);}
    }
    @SubscribeEvent public static void replace(MobSpawnEvent.FinalizeSpawn event){
        if(!CrimsonContent.available()||!(event.getEntity() instanceof CultistKnightEntity original)||event.isSpawnCancelled()||original.getRandom().nextInt(100)>=ConciliumMobConfig.PALADIN_REPLACEMENT.get())return;
        var level=event.getLevel().getLevel();var replacement=CrimsonContent.PALADIN.get().create(level);if(replacement==null)return;
        replacement.moveTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),original.getXRot());replacement.setCustomName(original.getCustomName());replacement.setCustomNameVisible(original.isCustomNameVisible());
        if(original.hasRestriction())replacement.restrictTo(original.getRestrictCenter(),(int)original.getRestrictRadius());
        replacement.finalizeSpawn(event.getLevel(),event.getDifficulty(),event.getSpawnType(),null,null);if(level.addFreshEntity(replacement))event.setSpawnCancelled(true);
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){if(event.getEntity() instanceof CultistEntity&&event.getNewTarget() instanceof Player player&&PontifexRobe.fullSet(player))event.setNewTarget(null);}
    @SubscribeEvent public static void command(net.minecraftforge.event.entity.player.AttackEntityEvent event){
        if(!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)||!(event.getTarget() instanceof LivingEntity target)||target instanceof CultistMob||!target.isAlive()||!PontifexRobe.fullSet(player)||!CrimsonContent.available()||CrimsonControl.chained(player))return;
        var knowledge=tc4revival.research.KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID());
        int strength=knowledge.map(k->net.minecraft.util.Mth.clamp(k.poolAmount(tc4revival.aspect.TCAspects.catalog().require("fames"))/100,1,200)).orElse(1);
        int regeneration=knowledge.map(k->net.minecraft.util.Mth.clamp(k.poolAmount(tc4revival.aspect.TCAspects.catalog().require("sano"))/100,1,200)).orElse(1);
        for(var cultist:player.level().getEntitiesOfClass(CultistEntity.class,player.getBoundingBox().inflate(32),LivingEntity::isAlive)){
            if(!cultist.canAttack(target))continue;cultist.setTarget(target);
            cultist.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,100,strength));
            cultist.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,100,regeneration));
        }
    }
    @SubscribeEvent public static void initiate(PlayerInteractEvent.EntityInteract event){
        var player=event.getEntity();if(!(event.getTarget() instanceof MadThaumaturge mad)||!mad.isAlive()||!CrimsonContent.available()||!PontifexRobe.fullSet(player)||CrimsonControl.chained(player))return;
        var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if(!java.util.Set.of("taintedmagic:hollow_dagger","taintedmagic:crystal_dagger").contains(String.valueOf(item)))return;
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide));event.setCanceled(true);
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        Mob cultist=switch(mad.getRandom().nextInt(3)){case 0->TCEntities.CULTIST_KNIGHT.get().create(level);case 1->TCEntities.CULTIST_CLERIC.get().create(level);default->CrimsonContent.PALADIN.get().create(level);};
        if(cultist==null)return;cultist.moveTo(mad.getX(),mad.getY(),mad.getZ(),mad.getYRot(),mad.getXRot());cultist.setCustomName(mad.getCustomName());cultist.setCustomNameVisible(mad.isCustomNameVisible());
        cultist.finalizeSpawn(level,level.getCurrentDifficultyAt(mad.blockPosition()),MobSpawnType.CONVERSION,null,null);
        if(!level.addFreshEntity(cultist))return;
        for(var slot:EquipmentSlot.values())if(!mad.getItemBySlot(slot).isEmpty())mad.spawnAtLocation(mad.getItemBySlot(slot).copy());
        mad.discard();cultist.playSound(TCSounds.CRAFT_FAIL.get(),1,1);CrimsonDrainNetwork.initiation(player,cultist);
    }
    private CrimsonEvents(){}
}
