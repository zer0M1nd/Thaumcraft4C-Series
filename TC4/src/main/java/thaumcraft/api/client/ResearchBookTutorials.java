package thaumcraft.api.client;

import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.ResearchPage;

/** Optional client tutorial bridge. Core book code has no dependency on a tutorial mod. */
public final class ResearchBookTutorials {
    public interface Handler {
        boolean has(ItemStack stack);
        Optional<ItemStack> structure(String research,ResearchPage page,long cycleMillis);
        boolean open(ItemStack stack,Screen returnTo);
        /** Component IDs also support structures with no obtainable item. */
        default Optional<ResourceLocation> structureId(String research,ResearchPage page,long cycleMillis){
            return structure(research,page,cycleMillis).filter(this::has).map(stack->ForgeRegistries.ITEMS.getKey(stack.getItem()));
        }
        default boolean open(ResourceLocation id,Screen returnTo){
            var item=ForgeRegistries.ITEMS.getValue(id);
            if(item==null)return false;
            var stack=new ItemStack(item);
            return !stack.isEmpty()&&has(stack)&&open(stack,returnTo);
        }
    }
    private static Handler handler;
    private ResearchBookTutorials() {}
    public static void register(Handler value){handler=java.util.Objects.requireNonNull(value);}
    public static boolean has(ItemStack stack){return !stack.isEmpty()&&handler!=null&&handler.has(stack);}
    public static Optional<ItemStack> structure(String research,ResearchPage page,long cycleMillis){
        return handler==null?Optional.empty():handler.structure(research,page,cycleMillis).filter(ResearchBookTutorials::has);
    }
    public static boolean open(ItemStack stack,Screen returnTo){return has(stack)&&handler.open(stack.copy(),returnTo);}
    public static Optional<ResourceLocation> structureId(String research,ResearchPage page,long cycleMillis){
        return handler==null?Optional.empty():handler.structureId(research,page,cycleMillis);
    }
    public static boolean open(ResourceLocation id,Screen returnTo){return handler!=null&&handler.open(id,returnTo);}
}
