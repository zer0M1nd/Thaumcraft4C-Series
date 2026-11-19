package wgmodern;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.ResearchDefinition;
import tc4revival.research.ResearchPage;
import tc4revival.research.ResearchPageRecipes;

/** Resolve optional-metal page choices after registry tags are synchronized, not at data generation. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="witchinggadgets",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ResearchRecipeDisplay {
    public static void setup(){
        thaumcraft.api.research.ResearchPresentation.registerBackground("witchinggadgets:awakened",0,"WITCHGADG",()->
            tc4revival.client.ClientKnowledge.snapshot().map(k->k.researchMarkers().contains("ELDRITCHMINOR")).orElse(false)?Optional.of("witchinggadgets:textures/gui/research/wgresearchbackawoken.png"):Optional.empty());
        thaumcraft.api.research.ResearchRecipeViews.registerRecipeView("witchinggadgets:metals",0,ResearchRecipeDisplay::display);
        thaumcraft.api.research.ResearchRecipeViews.registerPageVisibility("witchinggadgets:metals",0,(key,page)->{
            if(!key.startsWith("witchinggadgets:METALLURGICPERFECTION_")||!(page.recipe() instanceof ResearchPage.Reference reference))return true;
            var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return true;
            return reference.keys().stream().anyMatch(id->level.getRecipeManager().byKey(ResourceLocation.parse(id)).filter(r->!r.getResultItem(level.registryAccess()).isEmpty()).isPresent());
        });
    }
    private static ResearchPageRecipes.Entry display(ResearchPageRecipes.Entry original){
        var key=original.key();if(!key.startsWith("witchinggadgets:"))return original;
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return original;
        var resolved=resolve(original);if(!key.startsWith("witchinggadgets:transmute_"))return resolved;
        var recipe=level.getRecipeManager().byKey(ResourceLocation.parse(key)).orElse(null);
        if(!(recipe instanceof MetalCrucibleRecipe metal)||!(resolved.definition() instanceof ResearchPageRecipes.Crucible crucible))return resolved;
        var output=metal.getResultItem(level.registryAccess());if(output.isEmpty())return resolved;
        var aspects=new LinkedHashMap<String,Integer>();metal.costs().view().forEach((a,n)->aspects.put(a.tag(),n));
        var stack=new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key(ForgeRegistries.ITEMS.getKey(output.getItem()).toString(),0),output.getCount(),false,Optional.empty(),Optional.empty());
        return new ResearchPageRecipes.Entry(resolved.key(),resolved.legacyClass(),new ResearchPageRecipes.Crucible(crucible.research(),crucible.catalyst(),new ResearchPage.Item(stack),aspects));
    }
    private static Map<ResearchPageRecipes.Entry, ResearchPageRecipes.Entry> cache = Collections.synchronizedMap(new IdentityHashMap<>());
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tags(net.minecraftforge.event.TagsUpdatedEvent event) { cache.clear(); }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) { cache.clear(); }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { cache.clear(); }
    public static ResearchPageRecipes.Entry resolve(ResearchPageRecipes.Entry entry) {
        return cache.computeIfAbsent(entry, value -> new ResearchPageRecipes.Entry(value.key(), value.legacyClass(), definition(value.definition())));
    }
    private static ResearchPageRecipes.Definition definition(ResearchPageRecipes.Definition value) {
        if (value instanceof ResearchPageRecipes.Crafting c) return new ResearchPageRecipes.Crafting(c.kind(), node(c.output()), sequence(c.ingredients()), c.research(), c.aspects(), c.width(), c.height(), c.mirrored(), c.matchesInputNbt());
        if (value instanceof ResearchPageRecipes.Infusion c) return new ResearchPageRecipes.Infusion(c.research(), node(c.input()), node(c.output()), sequence(c.ingredients()), c.aspects(), c.instability());
        if (value instanceof ResearchPageRecipes.Crucible c) return new ResearchPageRecipes.Crucible(c.research(), node(c.catalyst()), node(c.output()), c.aspects());
        if (value instanceof ResearchPageRecipes.Enchanting c) return new ResearchPageRecipes.Enchanting(c.research(), c.enchantmentId(), c.enchantmentName(), c.maxLevel(), c.baseXp(), c.instability(), c.aspects(), sequence(c.ingredients()));
        if (value instanceof ResearchPageRecipes.Compound c) return new ResearchPageRecipes.Compound(c.aspects(), c.width(), c.height(), c.depth(), sequence(c.blocks()));
        return value;
    }
    private static ResearchPage.Sequence sequence(ResearchPage.Sequence value) { return new ResearchPage.Sequence(value.elements().stream().map(ResearchRecipeDisplay::node).toList()); }
    private static ResearchPage.Recipe node(ResearchPage.Recipe value) {
        if (value instanceof ResearchPage.Sequence sequence) return sequence(sequence);
        if (!(value instanceof ResearchPage.Ore ore)) return value;
        var choices = new LinkedHashMap<ResourceLocation, ResearchPage.Recipe>();
        for (String name : ore.names()) if (name.startsWith("#")) {
            var id = ResourceLocation.tryParse(name.substring(1));
            if (id == null) continue;
            for (var item : ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, id))) {
                var itemId = ForgeRegistries.ITEMS.getKey(item);
                choices.putIfAbsent(itemId, new ResearchPage.Item(new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key(itemId.toString(), 0), 1, false, Optional.empty(), Optional.empty())));
            }
        }
        return new ResearchPage.Ore(ore.names(), choices.isEmpty() ? ore.elements().stream().map(ResearchRecipeDisplay::node).toList() : List.copyOf(choices.values()));
    }
}
