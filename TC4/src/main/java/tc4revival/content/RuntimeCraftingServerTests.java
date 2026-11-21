package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.recipe.*;
import tc4revival.aspect.sync.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class RuntimeCraftingServerTests {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.event.RegisterCommandsEvent event){
        if(!Boolean.getBoolean("tc4revival.runtimeCraftingTests"))return;
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("tc4runtimecraftingtest")
                .requires(source->source.hasPermission(4))
                .then(net.minecraft.commands.Commands.literal("baseline").executes(context->verify(()->actualWaystoneRecipeAndClientSnapshot(context.getSource().getServer()))))
                .then(net.minecraft.commands.Commands.literal("changed").executes(context->verify(()->actualRecipeReplacementAndRemoval(context.getSource().getServer())))));
    }
    private static int verify(Runnable action){
        try{action.run();return 1;}
        catch(Throwable error){tc4revival.TC4Revival.LOGGER.error("RUNTIME_CRAFTING FAIL",error);return 0;}
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public static void actualWaystoneRecipeAndClientSnapshot(net.minecraft.server.MinecraftServer server){
        var id=ResourceLocation.parse("waystones:warp_stone");
        check(ForgeRegistries.ITEMS.containsKey(id),"This integration test requires Waystones and Balm");
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        var data=RuntimeCraftingAspects.data();
        var recipe=data.recipes().stream().filter(r->r.output().item().equals(id.toString())).findFirst().orElseThrow();
        check(recipe.ingredients().size()==9,"Waystones actual 3x3 recipe extracted");
        var actual=ItemStackAspects.resolve(stack).orElseThrow();
        check(actual.amount(TCAspects.catalog().require("iter"))==12,"Four ender pearls contribute 12 travel");
        check(actual.amount(TCAspects.catalog().require("vitreus"))==9,"Amethyst plus emerald contribute 9 crystal");
        var client=isolated(ServerAspectData.snapshot());
        check(actual.view().equals(ItemStackAspects.resolve(stack,client).orElseThrow().view()),"Server and client snapshot agree without JEI");
        check(RecipeAspects.data().recipes().stream().noneMatch(r->r.kind()==RecipeAspectData.Kind.CRAFTING),"Preset crafting recipes excluded");
        tc4revival.TC4Revival.LOGGER.info("RUNTIME_CRAFTING PASS: Waystones actual recipe server/client {}",actual.view());
    }
    public static void actualRecipeReplacementAndRemoval(net.minecraft.server.MinecraftServer server){
        var id=ResourceLocation.parse("waystones:warp_stone");
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        check(!stack.isEmpty(),"Waystones installed");
        var recipe=new ShapelessRecipe(ResourceLocation.parse("test:changed_warp"),"",CraftingBookCategory.MISC,
                stack.copyWithCount(2),net.minecraft.core.NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.ENDER_PEARL)));
        var manager=new RecipeManager();manager.replaceRecipes(List.of(recipe));
        var files=new TreeMap<>(ServerAspectData.snapshot().files());
        files.put("crafting_aspect_inputs/thaumcraft:runtime",RecipeAspectJson.encode(RuntimeCraftingAspects.extract(manager,server.registryAccess())).toString());
        var changed=ItemStackAspects.resolve(stack,isolated(new AspectDataSet(files))).orElseThrow();
        check(changed.amount(TCAspects.catalog().require("iter"))==1,"Changed actual recipe and output count replace old calculation");
        manager.replaceRecipes(List.of());
        files.put("crafting_aspect_inputs/thaumcraft:runtime",RecipeAspectJson.encode(RuntimeCraftingAspects.extract(manager,server.registryAccess())).toString());
        check(ItemStackAspects.resolve(stack,isolated(new AspectDataSet(files))).isEmpty(),"Removed recipe cannot fall back to a preset");
        tc4revival.TC4Revival.LOGGER.info("RUNTIME_CRAFTING PASS: actual recipe replacement and removal");
    }
    private static AspectCalculationData isolated(AspectDataSet data){return new AspectCalculationData(data,TCAspects.catalog(),
            (id,tag)->ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)).builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(tag))));}
}
