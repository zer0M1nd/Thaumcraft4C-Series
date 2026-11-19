package tc4revival.aspect.item;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;
import tc4revival.research.BrowserQueryStacks;
import tc4revival.research.ScanIdentity;

/** One data-generation's table. Unknown results are cached too; callers never mutate its values. */
public class ItemAspectCache {
    public record Result(String identity,AspectAmounts amounts) {}
    private final ItemAspectAccess data;
    private final Map<CompoundTag,Result> values=new HashMap<>();
    public ItemAspectCache(ItemAspectAccess data){this.data=data;}
    public int size(){return values.size();}
    public void remove(CompoundTag key){values.remove(key);}
    public Result resolve(ItemStack stack){return resolve(stack,key(stack,data));}
    public Result resolve(ItemStack stack,CompoundTag key){
        var value=values.computeIfAbsent(key,k->new Result(ScanIdentity.item(stack,data),
                ItemStackAspects.resolve(stack,data).orElseGet(AspectAmounts::new)));
        return new Result(value.identity(),value.amounts().copy());
    }
    /** Keep exactly the state read by ItemStackAspects. No ItemStack.save/capability serialization
     * for ordinary items, even when an addon writes a large or continuously changing tag. */
    public static CompoundTag key(ItemStack stack){
        return key(stack,null);
    }
    public static CompoundTag key(ItemStack stack,ItemAspectAccess data){
        if(stack.getItem() instanceof StoredAspectItem
                || stack.getItem() instanceof CastingWandItem && stack.getItem().getClass()!=CastingWandItem.class)
            return BrowserQueryStacks.key(stack); // Extension callbacks may inspect arbitrary stack state.
        var key=new CompoundTag();key.putString("id",ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        var tag=stack.getTag();
        if(tag==null)return key;
        if(data!=null){
            String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            if(data.direct().hasNbtRules(id)){var selected=data.direct().nbtCacheKey(id,tag);if(!selected.isEmpty())key.put("aspect_nbt",selected);}
            if(data.equipment().hasNbtRules(id)){var selected=data.equipment().nbtCacheKey(id,tag);if(!selected.isEmpty())key.put("aspect_bonus_nbt",selected);}
        }
        copy(tag,key,stack.is(Items.ENCHANTED_BOOK)?"StoredEnchantments":"Enchantments");
        if(stack.is(Items.POTION)||stack.is(Items.SPLASH_POTION)||stack.is(Items.LINGERING_POTION)||stack.is(Items.TIPPED_ARROW)){
            copy(tag,key,"Potion");copy(tag,key,PotionAspects.LEGACY_METADATA_TAG);
        }
        if(stack.is(Items.FILLED_MAP))copy(tag,key,"map");
        if(stack.getItem() instanceof CastingWandItem){
            copy(tag,key,"rod");copy(tag,key,"cap");copy(tag,key,"tc4_metadata");
        }
        if(stack.getItem() instanceof ItemMirrorItem)key.putInt("metadata",ItemMirrorItem.legacyMetadata(stack));
        if(stack.getItem() instanceof EssentiaMirrorItem)key.putInt("metadata",EssentiaMirrorItem.legacyMetadata(stack));
        if(stack.getItem() instanceof SinisterStoneItem)key.putInt("metadata",SinisterStoneItem.legacyMetadata(stack));
        return key;
    }
    private static void copy(CompoundTag from,CompoundTag to,String name){
        var value=from.get(name);if(value!=null)to.put(name,value.copy());
    }
}
