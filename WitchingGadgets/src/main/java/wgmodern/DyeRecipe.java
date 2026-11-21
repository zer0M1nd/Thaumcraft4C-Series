package wgmodern;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public class DyeRecipe extends CustomRecipe {
    public DyeRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
    @Override public boolean matches(CraftingContainer inv,Level level){int bags=0,dyes=0;for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof BagItem||s.getItem() instanceof AdvancedRobe||s.getItem() instanceof CloakItem cloak&&(cloak.kind.equals("standard")||cloak.kind.equals("plain")))bags++;else if(s.getItem() instanceof DyeItem)dyes++;else return false;}return bags==1&&dyes>0;}
    @Override public ItemStack assemble(CraftingContainer inv,RegistryAccess access){ItemStack bag=ItemStack.EMPTY;var dyes=new ArrayList<DyeItem>();for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(s.getItem() instanceof BagItem||s.getItem() instanceof AdvancedRobe||s.getItem() instanceof CloakItem cloak&&(cloak.kind.equals("standard")||cloak.kind.equals("plain")))bag=s;else if(s.getItem() instanceof DyeItem dye)dyes.add(dye);}return bag.isEmpty()?ItemStack.EMPTY:DyeableLeatherItem.dyeArmor(bag,dyes);}
    @Override public boolean canCraftInDimensions(int x,int y){return x*y>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return Content.DYE_SERIALIZER.get();}
}
