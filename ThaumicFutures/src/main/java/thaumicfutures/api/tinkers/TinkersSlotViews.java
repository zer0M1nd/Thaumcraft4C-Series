package thaumicfutures.api.tinkers;

import java.util.Objects;
import slimeknights.tconstruct.library.tools.nbt.*;
import thaumcraft.api.util.ExtensionRegistry;

/** Optional Tinkers API. Load and register only when both Futures and Tinkers are installed. */
public final class TinkersSlotViews {
    @FunctionalInterface public interface Modifier {
        IModDataView apply(ToolStack tool,IModDataView stored,IModDataView base,IModDataView current);
    }
    private static final ExtensionRegistry<Modifier> MODIFIERS=new ExtensionRegistry<>();
    private TinkersSlotViews() {}
    public static void registerSlotViewModifier(String id,int priority,Modifier modifier){MODIFIERS.register(id,priority,modifier);}
    public static IModDataView compose(ToolStack tool,IModDataView stored,IModDataView base){
        var current=base;
        for(var entry:MODIFIERS.entries())current=Objects.requireNonNull(entry.value().apply(tool,stored,base,current));
        return current;
    }
}
