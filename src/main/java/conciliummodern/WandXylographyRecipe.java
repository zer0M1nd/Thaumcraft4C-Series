package conciliummodern;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.CastingWandItem;

/** Keep the legacy name tag for monitor/lithographer compatibility. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class WandXylographyRecipe extends CustomRecipe {
    public static final String TAG="Xylography";
    public WandXylographyRecipe(ResourceLocation id,CraftingBookCategory category) { super(id,category); }
    private static ItemStack target(CraftingContainer grid) {
        ItemStack wand=ItemStack.EMPTY;
        for(int i=0;i<grid.getContainerSize();i++) {
            var stack=grid.getItem(i);
            if(stack.isEmpty())continue;
            if(!wand.isEmpty() || !(stack.getItem() instanceof CastingWandItem) || !stack.hasTag())return ItemStack.EMPTY;
            wand=stack;
        }
        return wand;
    }
    @Override public boolean matches(CraftingContainer grid,Level level) { return !target(grid).isEmpty(); }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registry) {
        var wand=target(grid);
        if(wand.isEmpty())return ItemStack.EMPTY;
        var result=wand.copyWithCount(1);
        result.getOrCreateTag().putString(TAG," ");
        return result;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<?> getSerializer() { return NetherMembraneRecipe.XYLOGRAPHY.get(); }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if(event.getEntity().level().isClientSide || event.getEntity() instanceof FakePlayer)return;
        var stack=event.getCrafting();
        if(stack.getItem() instanceof CastingWandItem && stack.hasTag() && " ".equals(stack.getTag().getString(TAG))) {
            stack.getTag().putString(TAG,event.getEntity().getGameProfile().getName());
        }
    }
}
