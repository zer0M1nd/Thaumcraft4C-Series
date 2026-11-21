package ttmodern;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** CB Microblock exposes a Block/BlockState IMC endpoint; no private registry access is needed. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class MicroblockIntegration {
    static final List<String> MATERIALS=List.of(
        "thaumcraft:cinnabar_ore","thaumcraft:amber_ore","thaumcraft:greatwood_planks","thaumcraft:silverwood_planks",
        "thaumcraft:greatwood_log","thaumcraft:silverwood_log","thaumcraft:greatwood_leaves","thaumcraft:silverwood_leaves",
        "thaumcraft:amber_block","thaumcraft:obsidian_totem","thaumcraft:obsidian_tile","thaumcraft:travel_paving_stone",
        "thaumcraft:warding_paving_stone","thaumcraft:thaumium_block","thaumcraft:tallow_block","thaumcraft:arcane_stone","thaumcraft:arcane_stone_bricks",
        Tinkerer.ID+":dark_quartz_block",Tinkerer.ID+":chiseled_dark_quartz",Tinkerer.ID+":dark_quartz_pillar");
    static List<Block> materials(){var result=new ArrayList<Block>();for(String name:MATERIALS){var id=ResourceLocation.parse(name);if(!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalStateException("Missing microblock material "+id);result.add(ForgeRegistries.BLOCKS.getValue(id));}return List.copyOf(result);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void enqueue(net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent event){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("cb_microblock"))return;
        for(var block:materials())net.minecraftforge.fml.InterModComms.sendTo("cb_microblock","micro_material",()->block);
    }
}
