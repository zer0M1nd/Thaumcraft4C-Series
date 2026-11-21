package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.research.ResearchDefinition;
import tc4revival.research.ResearchDisplayItems;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ItemMigrationServerTests {
    @GameTest(template="wand_test_empty")
    public static void loadedItemIdentitiesAndBookVariantsResolve(GameTestHelper test) {
        var identity=LegacyItemMappings.identity();
        var eggNames=new HashSet<>(Set.of("brainy_zombie_spawn_egg","giant_brainy_zombie_spawn_egg","pech_spawn_egg","wisp_spawn_egg"));
        LegacyMobEggs.entries().forEach(egg->eggNames.add(egg.path()+"_spawn_egg"));
        // These eight modern worldgen variants were added after the 1.7.10 item table.
        var modernOres=Set.of("deepslate_air_infused_stone","deepslate_fire_infused_stone","deepslate_water_infused_stone",
                "deepslate_earth_infused_stone","deepslate_order_infused_stone","deepslate_entropy_infused_stone",
                "deepslate_amber_ore","deepslate_cinnabar_ore");
        var seenModern=new HashSet<String>();
        int registered=0;
        for (var entry:ForgeRegistries.ITEMS.getEntries()) {
            var id=entry.getKey().location();
            if (!id.getNamespace().equals("thaumcraft")) continue;
            registered++;
            if (eggNames.contains(id.getPath())) {
                test.assertTrue(entry.getValue() instanceof SpawnEggItem,"Test-only eggs have the real spawn-egg implementation");
            } else if(modernOres.contains(id.getPath())) {
                seenModern.add(id.getPath());
                test.assertTrue(entry.getValue() instanceof net.minecraft.world.item.BlockItem,"Modern ore has a real block item: "+id);
                var original=id.withPath(id.getPath().substring("deepslate_".length()));
                test.assertTrue(identity.recipeKey(original.toString())!=null,"Modern ore retains its original stone counterpart: "+original);
                test.assertTrue(tc4revival.aspect.item.ItemAspects.direct(new net.minecraft.world.item.ItemStack(entry.getValue()))
                        .filter(amounts->!amounts.isEmpty()).isPresent(),"Modern ore has its authored modern aspects: "+id);
            } else test.assertTrue(identity.recipeKey(id.toString())!=null,"Registered content has a loaded old identity: "+id);
        }
        test.assertTrue(seenModern.equals(modernOres),"Every explicitly audited modern ore is registered");
        for (var id:identity.items().keySet()) test.assertTrue(ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id))
                && ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))!=Items.AIR,"Loaded identity resolves to an actual item: "+id);
        test.assertTrue(new Key("Thaumcraft:ItemNugget",0).equals(identity.recipeKey("minecraft:iron_nugget")),"Vanilla iron nugget retains the old TC identity");
        test.assertTrue(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("thaumcraft:iron_nugget")),"No duplicate iron nugget");
        var book=ResearchDisplayItems.bundled(); int variants=0;
        var missing=new HashSet<String>();
        for (var entry:book.entries().values()) {
            var old=entry.identity();
            var original=new ResearchDefinition.Stack(new Key(old.item(),old.metadata()),3,entry.cycleDamage(),old.nbt(),Optional.empty());
            if (entry.missing().isPresent()) {
                missing.add(old.item()+"@"+old.metadata());
                test.assertTrue(book.resolve(original,0).isEmpty(),"Explicit unresolved display stays absent");
                continue;
            }
            for (int i=0;i<entry.variants().size();i++) {
                var expected=entry.variants().get(i);
                var stack=book.resolve(original,i*1000L).orElseThrow();
                test.assertTrue(!stack.isEmpty() && ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(expected.item()),"Every actual book cycle resolves: "+old);
                test.assertTrue(stack.getCount()==(entry.cycleCount()?1:3),"Book cycle preserves count semantics");
                var copy=stack.copy(); stack.getOrCreateTag().putBoolean("migration_test_mutation",true);
                test.assertTrue(net.minecraft.world.item.ItemStack.matches(copy,book.resolve(original,i*1000L).orElseThrow()),"Resolved book stacks have independent NBT");
                variants++;
            }
        }
        test.assertTrue(missing.equals(Set.of("minecraft:portal@32767","minecraft:end_portal@32767","minecraft:lava@0","Thaumcraft:blockHole@15","Thaumcraft:blockAiry@5")),"No unreviewed missing book identities; five known internal block forms remain explicit");
        tc4revival.TC4Revival.LOGGER.info("PASS loadedItemIdentitiesAndBookVariantsResolve: {} registered items, {} loaded identities, {} resolved book variants; {} explicit unresolved forms",registered,identity.items().size(),variants,missing.size());
        test.succeed();
    }
}
