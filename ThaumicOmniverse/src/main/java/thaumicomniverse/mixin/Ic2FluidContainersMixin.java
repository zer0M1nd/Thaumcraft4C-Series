package thaumicomniverse.mixin;

import ic2.core.fluid.FluidHandler;
import ic2.core.util.StackUtil;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep IC2's drain/equality semantics while avoiding comparisons across unrelated item types. */
@Pseudo
@Mixin(targets="ic2.core.recipe.input.RecipeInputFluidContainer",remap=false)
public abstract class Ic2FluidContainersMixin {
    @Shadow private static volatile List<ItemStack> containerItems;

    @Inject(method="getContainerItems",at=@At("HEAD"),cancellable=true,remap=false)
    private static void omniverse$indexContainers(CallbackInfoReturnable<List<ItemStack>> callback) {
        if(containerItems!=null){callback.setReturnValue(containerItems);return;}
        var result=new ArrayList<ItemStack>();
        var byItem=new IdentityHashMap<Item,List<ItemStack>>();
        var container=new MutableObject<ItemStack>();
        for(var item:BuiltInRegistries.ITEM){
            if(item==Items.AIR)continue;
            var stack=new ItemStack(item);
            var drained=FluidHandler.drainMb(stack,Integer.MAX_VALUE,false,container);
            if(drained==null)continue;
            var empty=container.getValue();if(empty.isEmpty())continue;
            var remaining=FluidHandler.drainMb(empty,Integer.MAX_VALUE,true,null);
            if(remaining==null||!remaining.isEmpty())continue;
            // IC2 strict equality requires identical Item identity, then compares NBT.
            var equivalents=byItem.computeIfAbsent(empty.getItem(),ignored->new ArrayList<>());
            if(equivalents.stream().anyMatch(previous->StackUtil.checkItemEqualityStrict(previous,empty)))continue;
            equivalents.add(empty);result.add(empty);
        }
        containerItems=result;callback.setReturnValue(result);
    }
}
