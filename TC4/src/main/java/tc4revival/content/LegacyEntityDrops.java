package tc4revival.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keep the original single-handler order: champion, ordinary materials, then dissolution. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class LegacyEntityDrops {
    private LegacyEntityDrops() {}
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        ChampionMobs.drops(event);
        var entity=event.getEntity();var type=entity.getType();String reward=null;
        boolean zombie=entity instanceof net.minecraft.world.entity.monster.Zombie;
        if (zombie) reward="zombie_brain";
        if (entity instanceof Villager) reward="gold_coin";
        if (reward!=null) addTable(event,"legacy_mob_bonus/"+reward);
        LiquidDeathDrops.drops(event);
    }
    public static void addTable(LivingDropsEvent event,String path) {
        var entity=event.getEntity();if(!(entity.level() instanceof ServerLevel level))return;
        var builder=new LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,entity)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,entity.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,event.getSource())
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.KILLER_ENTITY,event.getSource().getEntity())
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_KILLER_ENTITY,event.getSource().getDirectEntity());
        if(event.isRecentlyHit() && entity.getKillCredit() instanceof net.minecraft.world.entity.player.Player player)
            builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER,player).withLuck(player.getLuck());
        var table=level.getServer().getLootData().getLootTable(ResourceLocation.parse("thaumcraft:"+path));
        for(var stack:table.getRandomItems(builder.create(LootContextParamSets.ENTITY)))event.getDrops().add(new ItemEntity(level,entity.getX(),entity.getEyeY(),entity.getZ(),stack));
    }

}
