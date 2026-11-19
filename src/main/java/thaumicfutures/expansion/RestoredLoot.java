package thaumicfutures.expansion;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

@Mod.EventBusSubscriber(modid="thaumicfutures")
public final class RestoredLoot {
    @SubscribeEvent public static void loot(LootTableLoadEvent event){
        String table=event.getName().toString(),kind=null;float chance=.06f;
        float rare=rareEarthChance(table);
        if(rare>0)event.getTable().addPool(LootPool.lootPool().name("omniverse_rare_earth").setRolls(ConstantValue.exactly(1))
                .when(()->new thaumicfutures.FeatureLootCondition(thaumicfutures.FutureFeatures.Group.RESTORATION))
                .when(net.minecraft.world.level.storage.loot.predicates.MatchTool.toolMatches(
                        net.minecraft.advancements.critereon.ItemPredicate.Builder.item().hasEnchantment(
                                new net.minecraft.advancements.critereon.EnchantmentPredicate(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
                                        net.minecraft.advancements.critereon.MinMaxBounds.Ints.atLeast(1)))).invert())
                .when(LootItemRandomChanceCondition.randomChance(rare))
                .add(LootItem.lootTableItem(RestoredContent.RARE_EARTH.get())).build());
        if(table.equals("thaumcraft:entities/wisp"))kind="arcane";
        else if(table.equals("thaumcraft:entities/thaumic_slime"))kind="preserved";
        else if(table.equals("minecraft:chests/desert_pyramid")||table.equals("minecraft:chests/jungle_temple")){kind="ancient";chance=.18f;}
        else if(table.equals("thaumcraft:entities/eldritch_guardian")||table.equals("thaumcraft:entities/eldritch_crab"))kind="eldritch";
        else if(table.equals("minecraft:chests/stronghold_library")){kind="knowledge";chance=.25f;}
        else if(table.equals("thaumcraft:entities/taintacle")||table.equals("thaumcraft:entities/taint_swarm"))kind="twisted";
        if(kind==null)return;
        event.getTable().addPool(LootPool.lootPool().name("omniverse_curio").setRolls(ConstantValue.exactly(1))
                .when(()->new thaumicfutures.FeatureLootCondition(thaumicfutures.FutureFeatures.Group.RESTORATION))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(RestoredContent.CURIOS.get(kind).get())).build());
    }
    private static float rareEarthChance(String table){
        if(table.equals("thaumicfutures:blocks/quartz_ore")||table.equals("thaumicfutures:blocks/deepslate_quartz_ore")
                ||table.equals("thaumcraft:blocks/amber_ore")||table.equals("thaumcraft:blocks/deepslate_amber_ore"))return .05f;
        if(!table.startsWith("minecraft:blocks/"))return 0;
        return switch(table.substring("minecraft:blocks/".length()).replace("deepslate_","")){
            case "diamond_ore"->.05f;case "emerald_ore"->.075f;case "lapis_ore","redstone_ore","nether_quartz_ore"->.01f;
            case "coal_ore"->.001f;default->0;};
    }
    private RestoredLoot(){}
}
