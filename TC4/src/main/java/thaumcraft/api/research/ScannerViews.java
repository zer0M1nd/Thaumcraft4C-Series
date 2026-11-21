package thaumcraft.api.research;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.entity.player.Player;
import tc4revival.aspect.AspectAmounts;
import tc4revival.research.*;
import tc4revival.network.LensPacket;
import thaumcraft.api.util.ExtensionRegistry;

/** Read-only inventory/lens projections. Server projection occurs before packet comparison and transmission. */
public final class ScannerViews {
    public record Inventory(PlayerKnowledge.ScanKind kind,String identity,AspectAmounts amounts,PlayerKnowledge knowledge){
        public Inventory {amounts=amounts.copy();knowledge=knowledge.copy();}
        @Override public AspectAmounts amounts(){return amounts.copy();}
        @Override public PlayerKnowledge knowledge(){return knowledge.copy();}
    }
    public record Lens(Player player,ScanTarget target,PlayerKnowledge knowledge){
        public Lens {knowledge=knowledge==null?null:knowledge.copy();}
        @Override public PlayerKnowledge knowledge(){return knowledge==null?null:knowledge.copy();}
    }
    private static final ExtensionRegistry<Function<Inventory,Optional<List<InventoryAspectDisplay.Icon>>>> INVENTORY=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiFunction<Lens,LensPacket,LensPacket>> LENS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Function<Player,Optional<LensPacket>>> HINTS=new ExtensionRegistry<>();
    private ScannerViews() {}
    public static void registerInventory(String id,int priority,Function<Inventory,Optional<List<InventoryAspectDisplay.Icon>>> rule){INVENTORY.register(id,priority,rule);}
    public static void registerLens(String id,int priority,BiFunction<Lens,LensPacket,LensPacket> rule){LENS.register(id,priority,rule);}
    public static void registerHint(String id,int priority,Function<Player,Optional<LensPacket>> hint){HINTS.register(id,priority,hint);}
    public static Optional<List<InventoryAspectDisplay.Icon>> inventory(Inventory context){
        for(var entry:INVENTORY.entries()){var result=entry.value().apply(context);if(result.isPresent())return Optional.of(List.copyOf(result.get()));}return Optional.empty();
    }
    public static LensPacket lens(Lens context,LensPacket packet){
        if(context.target()==null)return packet;
        for(var entry:LENS.entries())packet=Objects.requireNonNull(entry.value().apply(context,packet));return packet;
    }
    public static Optional<LensPacket> hint(Player player,Optional<LensPacket> base){
        if(player==null||base.filter(p->!p.name().getString().isEmpty()||!p.aspects().isEmpty()||!p.details().getString().isEmpty()).isPresent())return base;
        for(var entry:HINTS.entries()){var value=entry.value().apply(player);if(value.isPresent())return value;}return base;
    }
}
