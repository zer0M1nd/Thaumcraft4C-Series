package tc4revival.content;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.BrowserRecipes;

/** Opt-in production checks; never run in a user's ordinary world. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class ClientComputationServerChecks {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void started(net.minecraftforge.event.server.ServerStartedEvent event){
        if(Boolean.getBoolean("tc4.verifyClientComputation"))verify(event.getServer());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void reload(net.minecraftforge.event.OnDatapackSyncEvent event){
        if(event.getPlayer()==null&&Boolean.getBoolean("tc4.verifyClientComputation"))verify(event.getPlayerList().getServer());
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void verifyNbtProjection(MinecraftServer server){
        var manager=new net.minecraft.world.item.crafting.RecipeManager();
        var ingredients=net.minecraft.core.NonNullList.<net.minecraft.world.item.crafting.Ingredient>create();
        ingredients.add(net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT));
        var recipes=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>();
        for(String kind:List.of("plain","empty_variant","named_variant")){
            var output=new ItemStack(Items.BUCKET);
            if(kind.equals("empty_variant"))output.getOrCreateTag().put("Variant",new net.minecraft.nbt.CompoundTag());
            if(kind.equals("named_variant"))output.getOrCreateTag().putString("Variant","copper");
            recipes.add(new net.minecraft.world.item.crafting.ShapelessRecipe(net.minecraft.resources.ResourceLocation.parse("test:"+kind),"",net.minecraft.world.item.crafting.CraftingBookCategory.MISC,output,ingredients));
        }
        manager.replaceRecipes(recipes);
        check(tc4revival.aspect.recipe.RuntimeCraftingAspects.extract(manager,server.registryAccess()).recipes().size()==1,"Meaningful NBT, including arbitrary empty compounds, must not become generic item-ID recipes");
    }
    private static void verify(MinecraftServer server){
        AspectSyncServerChecks.verify(server);
        verifyNbtProjection(server);
        var config=tc4revival.config.TCConfig.SERVER.enhancedInfusionEnchanting;boolean original=config.get();
        int recipes=0,rows=0;
        try{
            for(boolean enhanced:List.of(false,true)){
                config.set(enhanced);
                for(var recipe:server.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get())){
                    if(!recipe.enchantmentRecipe())continue;recipes++;
                    var previews=recipe.enchantmentPreviews();
                    check(!previews.isEmpty(),"Missing enchantment preview: "+recipe.getId());
                    var enchant=EnchantmentHelper.getEnchantments(previews.get(0).book()).keySet().iterator().next();
                    var equipment=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(recipe::acceptsCenter).filter(s->EnchantmentHelper.getEnchantments(s).isEmpty()&&EnchantmentHelper.getItemEnchantmentLevel(enchant,s)==0).findFirst().orElseThrow();
                    int max=previews.stream().mapToInt(InfusionRecipe.EnchantmentPreview::after).max().orElseThrow();
                    check(previews.size()<2*max,"Preview rows must be linear in levels");
                    if(enhanced){
                        check(max==(enchant.getMaxLevel()==1&&enchant!=Enchantments.MENDING?1:Math.min(256,enchant.getMaxLevel()+1)),"Enhanced level limit");
                    }
                    for(var preview:previews){
                        rows++;check(preview.book().is(Items.ENCHANTED_BOOK),"Enchantment output must be a book");
                        var center=equipment.copy();if(preview.before()>0)center.enchant(enchant,preview.before());
                        var components=preview.plan().components().stream().map(c->c.displayStacks().get(0)).toList();
                        var input=new InfusionRecipe.Input(center,components,key->true);
                        check(recipe.matches(input,server.overworld()),"Preview components must actually match: "+recipe.getId());
                        var actual=recipe.plan(input);
                        check(actual.aspects().view().equals(preview.plan().aspects().view())&&actual.instability()==preview.plan().instability()&&actual.xp()==preview.plan().xp(),"Preview costs differ from altar plan: "+recipe.getId()+" enhanced="+enhanced+" levels="+preview.before()+"->"+preview.after()+" equipment="+center.save(new net.minecraft.nbt.CompoundTag())+" actual="+actual.aspects().view()+"/"+actual.instability()+"/"+actual.xp()+" expected="+preview.plan().aspects().view()+"/"+preview.plan().instability()+"/"+preview.plan().xp());
                        check(EnchantmentHelper.getItemEnchantmentLevel(enchant,actual.output().apply(center))==preview.after(),"Preview output level differs from altar");
                        if(enhanced){
                            check(actual.xp()==0,"Enhanced enchanting must not cost XP");
                            check(components.stream().filter(s->s.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:salis_mundus")))).count()==preview.after()-preview.before(),"One world salt per increment");
                        }
                    }
                    check(BrowserRecipes.dynamicItem(server.overworld(),equipment,recipe).isEmpty(),"No per-equipment enchanting rows");
                    for(var row:BrowserRecipes.enchantments(recipe))check(row.kind().equals("infusion_enchantment")&&row.output().is(Items.ENCHANTED_BOOK)&&row.inputs().get(0).isEmpty(),"Dedicated book-only category");
                }
                check(BrowserRecipes.dynamicDefinitions(server.overworld()).stream().noneMatch(InfusionRecipe::enchantmentRecipe),"Dynamic equipment catalog must exclude enchantments");
            }
            config.set(true);
            var template=server.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).stream().filter(InfusionRecipe::enchantmentRecipe).findFirst().orElseThrow().displayDefinition();
            var components=new com.google.gson.JsonArray();
            components.add(com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:diamond\"}}"));
            template.add("components",components);
            for(String name:List.of("mending","silk_touch")){
                template.addProperty("enchantment","minecraft:"+name);
                var fixture=new InfusionRecipe.Serializer().fromJson(net.minecraft.resources.ResourceLocation.parse("test:no_salt_"+name),template);
                var previews=fixture.enchantmentPreviews();int max=previews.stream().mapToInt(InfusionRecipe.EnchantmentPreview::after).max().orElseThrow();
                check(max==(name.equals("mending")?2:1),"Mending exception and max-one enchantment cap");
                for(var preview:previews){
                    var items=preview.plan().components().stream().map(c->c.displayStacks().get(0)).toList();
                    check(items.stream().filter(s->s.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:salis_mundus")))).count()==preview.after()-preview.before(),"Addon recipes without salt must gain the correct salt count");
                    var voidSeed=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:void_seed"));
                    check(items.stream().anyMatch(s->s.is(voidSeed))==(preview.after()>1),"Overlevel preview must require a void seed");
                }
            }
        }finally{config.set(original);}
        check(recipes>=48,"Core legacy enchantments must be included in both modes");
        tc4revival.TC4Revival.LOGGER.info("CLIENT_COMPUTATION PASS: {} enchantment definitions across both modes; {} level previews match actual altar plans; no per-equipment rows",recipes,rows);
    }
}
