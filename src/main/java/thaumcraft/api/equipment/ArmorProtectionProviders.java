package thaumcraft.api.equipment;

import java.util.*;
import java.util.function.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import thaumcraft.api.util.ExtensionRegistry;
import tc4revival.content.LegacyArmorAbsorption;

/** Providers describe one piece; the host performs exactly one damage and durability settlement. */
public final class ArmorProtectionProviders {
    public record Context(LivingEntity wearer,DamageSource source,EquipmentSlot slot,ItemStack stack,
                          boolean magic,boolean elemental,boolean bypass,double fortressMultiplier,int fortressCount,int masks) {
        public Context {stack=stack.copy();}
        @Override public ItemStack stack(){return stack.copy();}
    }
    public record Protection(int priority,double ratio,int maximum,boolean skipWear,boolean extinguish) {
        public Protection {if(!Double.isFinite(ratio)||ratio<0||maximum<0)throw new IllegalArgumentException("Invalid armor protection");}
    }
    public record SlotProtection(int slot,Protection protection) {}
    private record Provider(Predicate<ItemStack> selector,Function<Context,Protection> value) {}
    private static final ExtensionRegistry<Provider> PIECES=new ExtensionRegistry<>();
    private static final ExtensionRegistry<ToDoubleBiFunction<EquipmentSlot,ItemStack>> SETS=new ExtensionRegistry<>();
    private ArmorProtectionProviders() {}
    public static void register(String id,int priority,Predicate<ItemStack> selector,Function<Context,Protection> provider){
        PIECES.register(id,priority,new Provider(Objects.requireNonNull(selector),Objects.requireNonNull(provider)));
    }
    public static void registerFortressSetBonus(String id,int priority,ToDoubleBiFunction<EquipmentSlot,ItemStack> provider){SETS.register(id,priority,provider);}
    public static boolean applies(ItemStack stack){return PIECES.entries().stream().anyMatch(entry->entry.value().selector().test(stack.copy()));}
    public static double fortressSetBonus(EquipmentSlot slot,ItemStack stack){
        double result=0;
        for(var entry:SETS.entries()){
            double bonus=entry.value().applyAsDouble(slot,stack.copy());
            if(!Double.isFinite(bonus)||bonus<0)throw new IllegalArgumentException("Invalid armor set bonus "+entry.id());
            result+=bonus;
        }
        return result;
    }
    public static Optional<Protection> protection(Context context){
        for(var entry:PIECES.entries())if(entry.value().selector().test(context.stack()))return Optional.of(Objects.requireNonNull(entry.value().value().apply(context)));
        return Optional.empty();
    }
    public static LegacyArmorAbsorption.Result settle(float damage,List<SlotProtection> input){
        var slots=new HashSet<Integer>();var pieces=new ArrayList<LegacyArmorAbsorption.Piece>();
        for(var slot:input){
            if(slot.slot()<0||slot.slot()>3||!slots.add(slot.slot()))throw new IllegalArgumentException("Duplicate or invalid armor slot");
            var p=slot.protection();pieces.add(new LegacyArmorAbsorption.Piece(slot.slot(),p.priority(),p.ratio(),p.maximum()));
        }
        return LegacyArmorAbsorption.apply(damage,pieces);
    }
}
