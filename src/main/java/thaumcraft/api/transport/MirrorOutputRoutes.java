package thaumcraft.api.transport;

import java.util.*;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.ItemMirrorBlockEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** Empty means unrecognized; a present zero means blocked and must not fall back to world ejection. */
public final class MirrorOutputRoutes {
    private static final ExtensionRegistry<BiFunction<ItemMirrorBlockEntity,ItemStack,OptionalInt>> ROUTES=new ExtensionRegistry<>();
    private MirrorOutputRoutes() {}
    public static void register(String id,int priority,BiFunction<ItemMirrorBlockEntity,ItemStack,OptionalInt> route){ROUTES.register(id,priority,route);}
    public static OptionalInt transfer(ItemMirrorBlockEntity mirror,ItemStack offered){
        for(var entry:ROUTES.entries()){
            var result=Objects.requireNonNull(entry.value().apply(mirror,offered.copy()));
            if(result.isPresent()){if(result.getAsInt()<0||result.getAsInt()>offered.getCount())throw new IllegalStateException("Invalid mirror acceptance from "+entry.id());return result;}
        }
        return OptionalInt.empty();
    }
}
