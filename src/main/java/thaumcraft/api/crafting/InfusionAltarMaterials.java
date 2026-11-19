package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import tc4revival.content.TCArcane;

/** Addon altar families. Central pedestal, matrix and all eight pillar inputs must agree. */
public final class InfusionAltarMaterials {
    public record Material(Supplier<? extends Block> matrix,Supplier<? extends Block> pedestal,
            Supplier<? extends Block> lowerInput,Supplier<? extends Block> upperInput,
            Supplier<? extends Block> pillar,Supplier<? extends Block> pillarTop) {}
    private static final Map<ResourceLocation,Material> MATERIALS=new LinkedHashMap<>();
    public static final Material ARCANE=new Material(TCArcane.INFUSION_MATRIX,TCArcane.ARCANE_PEDESTAL,
            TCArcane.ARCANE_STONE_BRICKS,TCArcane.ARCANE_STONE,TCArcane.INFUSION_PILLAR,TCArcane.INFUSION_PILLAR_TOP);
    public static synchronized void register(ResourceLocation id,Material material){
        if(MATERIALS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(material))!=null)
            throw new IllegalArgumentException("Duplicate infusion altar material "+id);
    }
    public static Material forMatrix(Block matrix){
        if(matrix==ARCANE.matrix.get())return ARCANE;
        for(var material:MATERIALS.values())if(matrix==material.matrix.get())return material;
        return null;
    }
    private InfusionAltarMaterials(){}
}
