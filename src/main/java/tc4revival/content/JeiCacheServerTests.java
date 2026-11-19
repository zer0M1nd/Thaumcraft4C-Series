package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.*;
import tc4revival.compat.jei.ClientQueryCache;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class JeiCacheServerTests {
    @GameTest(template="wand_test_empty")
    public static void wandRecipeIdentityUsesOnlyComponents(GameTestHelper t){
        var wand=TCItems.CASTING_WAND.get();
        for(var rod:List.of(WandParts.Rod.WOOD,WandParts.Rod.GREATWOOD,WandParts.Rod.GREATWOOD_STAFF))
            for(var cap:List.of(WandParts.Cap.IRON,WandParts.Cap.GOLD))for(boolean sceptre:List.of(false,true)){
                var empty=wand.create(rod,cap,sceptre,false);var charged=wand.create(rod,cap,sceptre,true);
                var key=BrowserQueryStacks.recipeKey(empty);
                WandFocusItem.attach(charged,new ItemStack(TCItems.FIRE_FOCUS.get()));
                charged.setHoverName(net.minecraft.network.chat.Component.literal("Used wand"));
                t.assertTrue(key.equals(BrowserQueryStacks.recipeKey(charged)),"Charge, name and installed focus preserve recipe identity");
                var other=wand.create(rod,cap==WandParts.Cap.IRON?WandParts.Cap.GOLD:WandParts.Cap.IRON,sceptre,false);
                t.assertTrue(!key.equals(BrowserQueryStacks.recipeKey(other)),"Cap remains part of the recipe identity");
                other=wand.create(rod,cap,!sceptre,false);
                t.assertTrue(!key.equals(BrowserQueryStacks.recipeKey(other)),"Sceptre remains a distinct wand type");
            }
        t.assertTrue(BrowserQueryStacks.recipeKey(new ItemStack(wand)).equals(BrowserQueryStacks.recipeKey(wand.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false))),"Missing default NBT matches the iron-capped wooden wand recipe");
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void jeiIndexedDynamicRecipesMatchIncrementalCatalog(GameTestHelper t){
        // Enchanting now has an equipment-independent JEI category; runic upgrades
        // remain the real NBT-dependent recipes covered by the incremental path.
        var center=new ItemStack(TCItems.GOGGLES.get());center.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        var rows=new ArrayList<BrowserRecipe>();
        var definitions=BrowserRecipes.dynamicDefinitions(t.getLevel());
        for(var recipe:definitions)rows.addAll(BrowserRecipes.dynamicItem(t.getLevel(),center,recipe));
        java.util.function.Function<BrowserRecipe,String> signature=r->r.id()+r.aspectInputs()+r.instability()+r.xp()+r.output().save(new net.minecraft.nbt.CompoundTag());
        t.assertTrue(rows.stream().anyMatch(r->r.output().is(TCItems.GOGGLES.get())&&r.output().getOrCreateTag().getByte("RS.HARDEN")==3),"Runic goggles retain their next actual upgrade");
        t.assertTrue(rows.stream().map(signature).toList().equals(BrowserRecipes.dynamicItems(t.getLevel(),center).stream().map(signature).toList()),"Splitting work must preserve every recipe and output");
        t.assertTrue(definitions.stream().allMatch(InfusionRecipe::dynamicCenter),"No static recipe visits in NBT update path");
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void jeiCacheEnumeratesPotionVariantsAndReusesStackResults(GameTestHelper t){
        var cache=new ClientQueryCache<net.minecraft.nbt.CompoundTag,ItemStack,BrowserRecipe>();
        var enumerated=new HashSet<net.minecraft.nbt.CompoundTag>();
        BrowserQueryStacks.enumerate(stack->enumerated.add(BrowserQueryStacks.key(stack)));
        for(var potion:ForgeRegistries.POTIONS.getValues())if(potion!=Potions.EMPTY)
            for(var form:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.TIPPED_ARROW)){
                var stack=PotionUtils.setPotion(new ItemStack(form),potion);
                t.assertTrue(enumerated.contains(BrowserQueryStacks.key(stack)),"Every registered potion form is prewarmed");
            }
        var sword=new ItemStack(Items.DIAMOND_SWORD);var key=BrowserQueryStacks.key(sword);
        cache.offer(key,sword,false);var batch=cache.begin(32);
        var row=BrowserRecipes.item(sword).orElseThrow();
        cache.complete(batch.id(),List.of(row),r->BrowserQueryStacks.key(r.inputs().get(0).get(0)));
        sword.setDamageValue(50);sword.setCount(2);sword.setHoverName(net.minecraft.network.chat.Component.literal("Renamed"));
        t.assertTrue(key.equals(BrowserQueryStacks.key(sword)),"Count, damage and name do not change aspects");
        cache.offer(BrowserQueryStacks.key(sword),sword,true);
        t.assertTrue(cache.begin(32).values().isEmpty(),"Known state never sends another request");
        t.assertTrue(BrowserRecipes.item(sword).orElseThrow().aspectOutputs().equals(row.aspectOutputs()),"Normalized values agree with real resolver");
        sword.enchant(Enchantments.SHARPNESS,3);
        t.assertTrue(!key.equals(BrowserQueryStacks.key(sword)),"New enchantments retain a distinct identity");
        cache.offer(BrowserQueryStacks.key(sword),sword,true);
        t.assertTrue(cache.begin(32).values().size()==1,"Only new enchanted stack is queried");
        var healing=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING);
        var poison=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.POISON);
        t.assertTrue(!BrowserQueryStacks.key(healing).equals(BrowserQueryStacks.key(poison)),"Potion contents retain separate cache identities");
        var wand=TCItems.CASTING_WAND.get();
        var charged=wand.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);
        var wandKey=BrowserQueryStacks.key(charged);charged.getOrCreateTag().putInt("aer",3);
        t.assertTrue(wandKey.equals(BrowserQueryStacks.key(charged)),"Wand charge cannot cause repeated queries");
        tc4revival.TC4Revival.LOGGER.info("JEI_CACHE PASS: potion enumeration and stack cache identity");
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void jeiIncrementalLookupKeepsDynamicInfusionAndEmptyResults(GameTestHelper t){
        var player=new net.minecraftforge.common.util.FakePlayer(t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"JeiCache"));
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.SHARPNESS,1);
        var empty=ForgeRegistries.ITEMS.getValues().stream().filter(i->i!=Items.AIR).map(ItemStack::new)
                .filter(s->BrowserRecipes.item(s).orElseThrow().aspectOutputs().isEmpty()).findFirst().orElseThrow();
        var runic=ForgeRegistries.ITEMS.getValues().stream().filter(i->i instanceof RunicGear).findFirst().map(ItemStack::new).orElseThrow();
        runic.getOrCreateTag().putByte("RS.HARDEN",(byte)1);
        var work=BrowserRecipes.items(player,List.of(sword,empty,runic));var rows=new ArrayList<BrowserRecipe>();
        while(!work.done())rows.addAll(work.drain(32,2_000_000L));
        t.assertTrue(rows.stream().filter(r->r.kind().equals("item_aspects")).count()==3,"All stacks have query results including empty aspects");
        t.assertTrue(rows.stream().anyMatch(r->r.kind().equals("item_aspects")&&r.aspectOutputs().isEmpty()),"No-aspect result remains cacheable");
        t.assertTrue(rows.stream().anyMatch(r->r.kind().equals("infusion")&&r.dynamic()),"Incremental new state retains dynamic infusion recipes");
        t.assertTrue(rows.stream().noneMatch(r->r.kind().equals("arcane")||r.kind().equals("crucible")),"Local query never rebuilds global catalog");
        tc4revival.TC4Revival.LOGGER.info("JEI_CACHE PASS: incremental infusion and empty results");
        t.succeed();
    }
}
