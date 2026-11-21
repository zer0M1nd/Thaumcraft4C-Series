package tc4revival.content;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface RevealingGear {
    boolean showNodes(ItemStack stack, LivingEntity wearer);
    boolean showIngamePopups(ItemStack stack, LivingEntity wearer);
    /** Register an additive visibility source, e.g. an addon helmet enchantment. The boolean selects nodes vs. HUD. */
    static void registerProvider(net.minecraft.resources.ResourceLocation id,java.util.function.BiPredicate<LivingEntity,Boolean> provider) {
        RevealingGearProviders.register(id,provider);
    }
    static void unregisterProvider(net.minecraft.resources.ResourceLocation id) { RevealingGearProviders.unregister(id); }
    static boolean visible(LivingEntity wearer,boolean nodes) {
        var helmet=wearer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if(helmet.getItem() instanceof RevealingGear gear && (nodes?gear.showNodes(helmet,wearer):gear.showIngamePopups(helmet,wearer)))return true;
        if(RevealingGearProviders.visible(wearer,nodes))return true;
        if(!tc4revival.config.TCConfig.SERVER.revival.gogglesHeadSlot.get())return false;
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(wearer).resolve().flatMap(inventory->inventory.getStacksHandler("head")).map(handler->{
            var stacks=handler.getStacks();for(int index=0;index<stacks.getSlots();index++)if(stacks.getStackInSlot(index).is(TCItems.GOGGLES.get()))return true;return false;
        }).orElse(false);
    }

}

class RevealingGearProviders {
    private static final java.util.concurrent.ConcurrentMap<net.minecraft.resources.ResourceLocation,java.util.function.BiPredicate<LivingEntity,Boolean>> providers=new java.util.concurrent.ConcurrentHashMap<>();
    static void register(net.minecraft.resources.ResourceLocation id,java.util.function.BiPredicate<LivingEntity,Boolean> provider) {
        providers.put(java.util.Objects.requireNonNull(id),java.util.Objects.requireNonNull(provider));
    }
    static void unregister(net.minecraft.resources.ResourceLocation id) { providers.remove(id); }
    static boolean visible(LivingEntity wearer,boolean nodes) {
        for(var provider:providers.values())if(provider.test(wearer,nodes))return true;
        return false;
    }
}
