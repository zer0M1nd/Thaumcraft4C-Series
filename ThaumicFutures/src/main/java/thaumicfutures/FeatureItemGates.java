package thaumicfutures;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Existing disabled tools remain movable and saved, but cannot be used for their gameplay benefits. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicfutures")
public final class FeatureItemGates {
    public static boolean disabled(ItemStack stack) {
        if(stack.isEmpty())return false;
        if(stack.getItem() instanceof tc4revival.content.CastingWandItem wand) {
            if(wand.cap(stack).tag.equals("thaumicomniverse:blood")&&!FutureFeatures.Group.EVILCRAFT.enabled())return true;
        }
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id!=null&&id.getNamespace().equals("thaumicfutures")&&FutureFeatures.ownsItem(id.getPath())&&!FutureFeatures.itemEnabled(id.getPath());
    }
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) { if(disabled(event.getItemStack()))event.setCanceled(true); }
    @SubscribeEvent public static void useBlock(PlayerInteractEvent.RightClickBlock event) { if(disabled(event.getItemStack()))event.setCanceled(true); }
    @SubscribeEvent public static void useEntity(PlayerInteractEvent.EntityInteract event) { if(disabled(event.getItemStack()))event.setCanceled(true); }
    @SubscribeEvent public static void attack(AttackEntityEvent event) { if(disabled(event.getEntity().getMainHandItem()))event.setCanceled(true); }
    @SubscribeEvent public static void mine(PlayerEvent.BreakSpeed event) { if(disabled(event.getEntity().getMainHandItem()))event.setCanceled(true); }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) { if(disabled(event.getPlayer().getMainHandItem()))event.setCanceled(true); }
    @SubscribeEvent public static void attributes(ItemAttributeModifierEvent event) { if(disabled(event.getItemStack()))event.clearModifiers(); }
}
