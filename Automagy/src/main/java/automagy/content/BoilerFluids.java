package automagy.content;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.BlockScanTargets;
import net.minecraft.world.item.ItemStack;
/** Resolve the fluid's block, never its filled bucket (which includes container aspects). */
public final class BoilerFluids {
    public static AspectAmounts aspects(FluidStack fluid){
        if(fluid.isEmpty())return new AspectAmounts();
        return aspects(fluid,tc4revival.aspect.item.ItemAspectAccess.SERVER,BlockScanTargets.mapping());
    }
    public static AspectAmounts aspects(FluidStack fluid,tc4revival.aspect.item.ItemAspectAccess access,tc4revival.aspect.item.BlockScanMapping mapping){
        return thaumcraft.api.aspects.AspectProductionRules.fluid("automagy:boiler",fluid,rawAspects(fluid,access,mapping));
    }
    private static AspectAmounts rawAspects(FluidStack fluid,tc4revival.aspect.item.ItemAspectAccess access,tc4revival.aspect.item.BlockScanMapping mapping){
        if(fluid.isEmpty())return new AspectAmounts();
        if(net.minecraftforge.common.ForgeMod.MILK.get().isSame(fluid.getFluid())){var result=new AspectAmounts();for(String key:java.util.List.of("aqua","fames","sano"))tc4revival.aspect.TCAspects.catalog().find(key).ifPresent(a->result.add(a,2));return result;}
        if(SoupFluids.MUSHROOM.source.get().isSame(fluid.getFluid())||SoupFluids.VISHROOM.source.get().isSame(fluid.getFluid())){
            var result=new AspectAmounts();var values=new java.util.LinkedHashMap<>(java.util.Map.of("fames",3,"herba",1,"tenebrae",1,"aqua",1));
            if(SoupFluids.VISHROOM.source.get().isSame(fluid.getFluid())){values.put("praecantatio",2);values.put("venenum",1);}
            values.forEach((key,n)->tc4revival.aspect.TCAspects.catalog().find(key).ifPresent(a->result.add(a,n)));return result;
        }
        var state=fluid.getFluid().defaultFluidState().createLegacyBlock();if(state.isAir())return new AspectAmounts();
        var mapped=mapping.resolve(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(),BlockScanTargets.properties(state));
        if(mapped.isPresent()){
            var target=mapped.get();
            if(target.modernItem()!=null)return tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(target.modernItem()))),access).orElseGet(AspectAmounts::new);
            return new tc4revival.aspect.item.StackAspectBonuses(access.catalog()).apply(access.recipeObject(access.identity().scanKey(target.legacyKey())),null,null,java.util.List.of());
        }
        return tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(state.getBlock()),access).orElseGet(AspectAmounts::new);
    }
    private BoilerFluids(){}
}
