package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.*;
import com.gregtechceu.gtceu.api.machine.multiblock.*;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.pattern.*;
import com.gregtechceu.gtceu.api.recipe.*;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import thaumcraft.api.aspects.EssentiaStorageApi;
import java.util.stream.IntStream;
import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public final class ArcaneMachines {
    public static void displayItems(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(CASING.asStack()); output.accept(GENERATOR.asStack());
        for (var hatch : HATCHES) if (hatch != null) output.accept(hatch.asStack());
    }
    public static final PartAbility ESSENTIA_INPUT=new PartAbility("thaumicomniverse_essentia_input");
    public static BlockEntry<Block> CASING;
    public static MultiblockMachineDefinition GENERATOR;
    public static final MachineDefinition[] HATCHES=new MachineDefinition[GTValues.MAX+1];
    public static GTRecipeType FUELS;
    public static void registerRecipeType(){
        var id=ArcaneGT.id("essentia_fuels");
        FUELS=new GTRecipeType(id,GTRecipeTypes.MULTIBLOCK).setMaxIOSize(0,0,1,0).setMaxSize(IO.IN,EssentiaFuelCapability.CAP,1).setEUIO(IO.OUT);
        FUELS.setRecipeUI(new EssentiaFuelRecipeUI(FUELS));
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE,id,FUELS);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER,id,new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(id,FUELS);
    }
    public static void registerMachines(){
        CASING=ArcaneGT.REGISTRATE.block("arcane_machine_casing",Block::new).initialProperties(()->Blocks.IRON_BLOCK)
                .properties(p->p.strength(5,6).requiresCorrectToolForDrops()).simpleItem().register();
        for(int tier=GTValues.LV;tier<=GTValues.MAX;tier++){
            final int current=tier;
            HATCHES[tier]=ArcaneGT.REGISTRATE.machine(GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT)+"_essentia_input_hatch",holder->new EssentiaHatch(holder,current))
                    .tier(tier).rotationState(RotationState.ALL).abilities(ESSENTIA_INPUT)
                    .tooltips(Component.translatable("thaumicomniverse.gt.hatch",64<<Math.max(0,Math.min(13,tier-1))))
                    .modelProperty(GTMachineModelProperties.IS_FORMED,false)
                    .colorOverlayTieredHullModel(ArcaneGT.id("block/overlay_essentia_input_emissive"),null,
                            com.gregtechceu.gtceu.GTCEu.id("block/overlay/machine/overlay_fluid_hatch"))
                    .register();
        }
        var generator=ArcaneGT.REGISTRATE.multiblock("large_essentia_generator",EssentiaGenerator::new);
        // GTCEu 7.5 inherits these fluent methods instead of overriding their return
        // type. Calling the common base descriptor also works with the 7.2 bridges.
        MachineBuilder<MultiblockMachineDefinition> base=generator;
        base.rotationState(RotationState.ALL).recipeType(FUELS).appearanceBlock(CASING)
                .recipeModifiers(com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier.NO_MODIFIER);
        generator.generator(true).pattern(definition->FactoryBlockPattern.start()
                        .aisle("XXX","XDX","XXX").aisle("XCX","CGC","XCX").aisle("XCX","CGC","XCX").aisle("XXX","XSX","XXX")
                        .where('S',controller(blocks(definition.getBlock())))
                        .where('X',blocks(CASING.get())).where('G',blocks(CASING.get()))
                        .where('C',blocks(CASING.get()).setMinGlobalLimited(3)
                                .or(abilities(ESSENTIA_INPUT).setExactLimit(1))
                                .or(autoAbilities(definition.getRecipeTypes(),false,false,true,true,true,true))
                                .or(autoAbilities(true,true,false)))
                        .where('D',ability(PartAbility.OUTPUT_ENERGY,IntStream.rangeClosed(GTValues.EV,GTValues.MAX).toArray())).build());
        base.workableCasingModel(ArcaneGT.id("block/arcane_machine_casing"),com.gregtechceu.gtceu.GTCEu.id("block/multiblock/generator/large_combustion_engine"))
                .tooltips(Component.translatable("thaumicomniverse.gt.generator",4096));
        GENERATOR=generator.register();
        FUELS.setIconSupplier(()->GENERATOR.asStack());
        EssentiaStorageApi.register(ArcaneGT.id("gt_hatches"),(entity,side)->{
            return entity instanceof IMachineBlockEntity holder&&holder.getMetaMachine() instanceof EssentiaHatch hatch?hatch:null;
        });
    }
    private ArcaneMachines(){}
}
