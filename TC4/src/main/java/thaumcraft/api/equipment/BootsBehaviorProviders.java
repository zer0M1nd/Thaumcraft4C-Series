package thaumcraft.api.equipment;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.util.ExtensionRegistry;

/** Setup-time policies; unselected equipment and PASS repair decisions retain the host behavior. */
public final class BootsBehaviorProviders {
    public enum RepairDecision { PASS, ACCEPT, REJECT }
    public record MovementContext(Player wearer, ItemStack boots, boolean flying, boolean hovering,
                                  boolean forward, boolean grounded, float baseline) {
        public MovementContext { boots=boots.copy(); }
        @Override public ItemStack boots(){return boots.copy();}
    }
    @FunctionalInterface public interface MovementProvider { float speed(MovementContext context,float current); }
    @FunctionalInterface public interface RepairPolicy { RepairDecision repair(ItemStack boots,ItemStack material); }
    private record Movement(Predicate<ItemStack> selector,MovementProvider provider) {}
    private record Repair(Predicate<ItemStack> selector,RepairPolicy policy) {}
    private static final ExtensionRegistry<Movement> MOVEMENT=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Repair> REPAIR=new ExtensionRegistry<>();
    private BootsBehaviorProviders() {}
    public static void registerMovementProvider(String id,int priority,Predicate<ItemStack> selector,MovementProvider provider){
        MOVEMENT.register(id,priority,new Movement(Objects.requireNonNull(selector),Objects.requireNonNull(provider)));
    }
    public static void registerRepairPolicy(String id,int priority,Predicate<ItemStack> selector,RepairPolicy policy){
        REPAIR.register(id,priority,new Repair(Objects.requireNonNull(selector),Objects.requireNonNull(policy)));
    }
    public static float airSpeed(MovementContext context,float current){
        for(var entry:MOVEMENT.entries())if(entry.value().selector().test(context.boots())){
            float next=entry.value().provider().speed(context,current);
            if(!Float.isFinite(next)||next<0)throw new IllegalArgumentException("Invalid boots speed from "+entry.id());
            current=next;
        }
        return current;
    }
    public static RepairDecision repair(ItemStack boots,ItemStack material){
        for(var entry:REPAIR.entries())if(entry.value().selector().test(boots.copy())){
            var result=Objects.requireNonNull(entry.value().policy().repair(boots.copy(),material.copy()));
            if(result!=RepairDecision.PASS)return result;
        }
        return RepairDecision.PASS;
    }
}
