package tc4revival.aspect.item;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;
import tc4revival.aspect.recipe.*;

/** Explicit calculation side. A remote-client snapshot never replaces the integrated server's tables. */
public interface ItemAspectAccess {
    AspectCatalog catalog();
    ItemAspectResolver direct();
    ItemAspectResolver equipment();
    ItemBaseAspectSession baseSession();
    LegacyItemIdentity identity();
    Map<String,RecipeAspectData.Key> spawnEggs();
    Optional<RecipeAspectData.Key> potionKey(ItemStack stack);
    AspectAmounts recipeObject(RecipeAspectData.Key key);

    ItemAspectAccess SERVER=new ItemAspectAccess(){
        public AspectCatalog catalog(){return TCAspects.catalog();}
        public ItemAspectResolver direct(){return ItemAspects.directResolver();}
        public ItemAspectResolver equipment(){return ItemStackAspects.equipmentResolver();}
        public ItemBaseAspectSession baseSession(){return ItemAspects.baseSession();}
        public LegacyItemIdentity identity(){return LegacyItemMappings.identity();}
        public Map<String,RecipeAspectData.Key> spawnEggs(){return LegacyItemMappings.spawnEggs();}
        public Optional<RecipeAspectData.Key> potionKey(ItemStack stack){return PotionAspects.key(stack);}
        public AspectAmounts recipeObject(RecipeAspectData.Key key){return RecipeAspects.object(key);}
    };
}
