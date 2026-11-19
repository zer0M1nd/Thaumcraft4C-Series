package thaumicfutures;
import java.util.List;
@mezz.jei.api.JeiPlugin
public final class FutureJei implements mezz.jei.api.IModPlugin {
    public net.minecraft.resources.ResourceLocation getPluginUid(){return net.minecraft.resources.ResourceLocation.parse("thaumicfutures:jei");}
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime r){
        if(thaumicfutures.expansion.RestoredIntegration.hideOldFurnace()){
            var old=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(thaumicfutures.expansion.RestoredIntegration.OLD_FURNACE);
            if(old!=null&&old!=net.minecraft.world.item.Items.AIR)r.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,List.of(new net.minecraft.world.item.ItemStack(old)));
        }
    }
}
