package emtmodern.item;

import ic2.api.recipe.IRecipeInput;
import ic2.core.recipe.v2.RecipeHolder;
import ic2.core.ref.Ic2RecipeTypes;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid="emt")
public class MaterialProcessing {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CLEAR_ORE_CONFLICTS;
    static{var builder=new ForgeConfigSpec.Builder();CLEAR_ORE_CONFLICTS=builder.comment("Original Clear Amber/Cinnabar Macerating setting; other existing machine inputs remain intact.").comment("移除琥珀矿和朱砂矿的冲突打粉配方，保留本附属配方", "设置说明 / Setting: Remove conflicting amber and cinnabar macerator recipes while retaining this addon's recipes", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("clear_amber_cinnabar_macerating",true);SPEC=builder.build();}
    public static int removeConflicts(RecipeManager manager){
        if(!CLEAR_ORE_CONFLICTS.get())return 0;
        var amber=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft","amber_ore")));
        var cinnabar=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft","cinnabar_ore")));
        List<Recipe<?>> all=new ArrayList<>(manager.getRecipes());int previous=all.size();
        all.removeIf(recipe->!recipe.getId().getNamespace().equals("emt")&&recipe.getType()==Ic2RecipeTypes.MACERATOR&&recipe instanceof RecipeHolder<?,?> holder&&holder.recipe().getInput() instanceof IRecipeInput input&&(input.matches(amber)||input.matches(cinnabar)));
        if(all.size()!=previous)manager.replaceRecipes(all);return previous-all.size();
    }
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event){removeConflicts(event.getServer().getRecipeManager());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void reload(OnDatapackSyncEvent event){if(event.getPlayer()==null)removeConflicts(event.getPlayerList().getServer().getRecipeManager());}
}
