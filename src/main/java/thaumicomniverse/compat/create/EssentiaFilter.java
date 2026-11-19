package thaumicomniverse.compat.create;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Item markers select their stored aspect, independent of container type or quantity. */
public class EssentiaFilter {
    public static boolean test(ItemStack filter,Aspect aspect){return test(filter,aspect,null,ItemStack.EMPTY,0);}
    public static boolean test(ItemStack filter,Aspect aspect,Level level,ItemStack output){return test(filter,aspect,level,output,0);}
    private static boolean test(ItemStack filter,Aspect aspect,Level level,ItemStack output,int depth){
        if(depth>16)return false;
        if(filter.isEmpty())return true;
        if(AllItems.FILTER.isIn(filter)){
            var items=FilterItem.getFilterItems(filter);boolean blacklist=filter.hasTag()&&filter.getTag().getBoolean("Blacklist");
            for(int i=0;i<items.getSlots();i++)if(!items.getStackInSlot(i).isEmpty()&&test(items.getStackInSlot(i),aspect,level,output,depth+1))return !blacklist;
            return blacklist;
        }
        if(filter.hasTag()){
            var tag=filter.getTag();
            var label=TCAspects.catalog().find(tag.getString("AspectFilter")).orElse(null);
            if(label!=null)return label.equals(aspect);
            var stored=AspectNbt.read(tag,TCAspects.catalog()).amounts();
            if(!stored.isEmpty())return stored.contains(aspect);
        }
        return level!=null&&!output.isEmpty()&&FilterItemStack.of(filter.copy()).test(level,output);
    }
}
