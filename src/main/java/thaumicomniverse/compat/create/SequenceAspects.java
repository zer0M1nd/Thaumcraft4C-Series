package thaumicomniverse.compat.create;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.*;

public final class SequenceAspects {
    public static final String ORIGINAL = "OmniverseAssemblyInput";
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);
    public static void setup(){
        thaumcraft.api.aspects.ItemAspectProviders.register("thaumicomniverse:sequence",0,query->{
            var result=resolve(query.stack(),query.access(),query.scanning());
            return result==null?thaumcraft.api.aspects.ItemAspectProviders.Resolution.pass():thaumcraft.api.aspects.ItemAspectProviders.Resolution.handled(result);
        });
    }
    public static Optional<AspectAmounts> resolve(ItemStack stack, ItemAspectAccess access) {return resolve(stack,access,false);}
    private static Optional<AspectAmounts> resolve(ItemStack stack, ItemAspectAccess access,boolean scanning) {
        if (!stack.hasTag() || !stack.getTag().contains("SequencedAssembly") || RESOLVING.get()) return null;
        ItemStack original = ItemStack.of(stack.getTag().getCompound(ORIGINAL));
        if (original.isEmpty()) {
            RecipeManager manager = null;
            if (access == ItemAspectAccess.SERVER) {
                var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) manager = server.getRecipeManager();
            } else if (net.minecraftforge.fml.loading.FMLLoader.getDist().isClient()) manager = Client.recipes();
            var id = ResourceLocation.tryParse(stack.getTag().getCompound("SequencedAssembly").getString("id"));
            var recipe = manager == null || id == null ? null : manager.byKey(id).orElse(null);
            if (recipe instanceof SequencedAssemblyRecipe sequence) {
                var choices = sequence.getIngredient().getItems();
                if (choices.length > 0) original = choices[0];
            }
        }
        if (original.isEmpty()) return Optional.of(new AspectAmounts());
        RESOLVING.set(true);
        try { return ItemStackAspects.resolve(original.copyWithCount(1), access, scanning); }
        finally { RESOLVING.remove(); }
    }
    private static class Client {
        static RecipeManager recipes() { var level=net.minecraft.client.Minecraft.getInstance().level; return level==null?null:level.getRecipeManager(); }
    }
    private SequenceAspects() {}
}
