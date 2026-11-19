package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Saved output intent: a new item, one tag on the live central stack, or an enchantment level. */
public  class InfusionOutput {
    private final ItemStack item;
    private final String label;
    private final Tag value;
    private final ResourceLocation enchantment;
    private final boolean unchanged;
    private int enchantmentLevels=1;
    private boolean persistentEnchantment;
    private boolean consumed;
    private boolean frozen;
    private String rulesVersion="",recipeId="",research="";
    public String recipeId(){return recipeId;}
    public String research(){return research;}
    public InfusionOutput freeze(String recipe,String research,ItemStack center){
        // Enchantments are an operation on the live stack, never a stale replacement
        // snapshot that can discard newly added capabilities, wear or other tags.
        var result=enchantment!=null ? new InfusionOutput(ItemStack.EMPTY,"",null,enchantment) : consumed?consumed():item(apply(center));
        result.enchantmentLevels=enchantmentLevels;result.persistentEnchantment=persistentEnchantment;result.frozen=true;
        result.recipeId=recipe;result.research=research;result.rulesVersion=thaumcraft.api.crafting.InfusionLifecycle.rulesVersion();return result;
    }
    public static InfusionOutput consumed() { var output=item(ItemStack.EMPTY);output.consumed=true;return output; }
    public boolean consumesCenter() { return consumed; }
    /** Convert addon/old saved replacement results into a non-destructive live enchantment operation. */
    public InfusionOutput asEnchantment(ItemStack center) {
        var proposed=apply(center);
        if(!onlyEnchantmentIncreased(center,proposed))throw new IllegalArgumentException("Enchanting must only increase one enchantment");
        if(enchantment!=null)return this;
        var previous=new java.util.HashMap<String,Integer>();
        for(var value:center.getEnchantmentTags()){var row=(CompoundTag)value;previous.put(row.getString("id"),(int)row.getShort("lvl"));}
        for(var value:proposed.getEnchantmentTags()) {
            var row=(CompoundTag)value;String id=row.getString("id");int delta=row.getShort("lvl")-previous.getOrDefault(id,0);
            if(delta<=0)continue;
            var result=enchant(ResourceLocation.parse(id),delta);
            result.frozen=frozen;result.rulesVersion=rulesVersion;result.recipeId=recipeId;result.research=research;
            if(result.apply(center).isEmpty())throw new IllegalArgumentException("Enchantment cannot be increased");
            return result;
        }
        throw new IllegalArgumentException("Missing enchantment increase");
    }
    private InfusionOutput(ItemStack item,String label,Tag value,ResourceLocation enchantment) {
        this(item,label,value,enchantment,false);
    }
    private InfusionOutput(ItemStack item,String label,Tag value,ResourceLocation enchantment,boolean unchanged) {
        this.unchanged=unchanged;
        this.item=item.copy(); this.label=label; this.value=value==null ? null : value.copy(); this.enchantment=enchantment;
    }
    public static InfusionOutput item(ItemStack stack) { return new InfusionOutput(stack,"",null,null); }
    public static InfusionOutput patch(String label,Tag value) {
        if (label.isEmpty() || value==null) throw new IllegalArgumentException("Invalid infusion tag output");
        return new InfusionOutput(ItemStack.EMPTY,label,value,null);
    }
    public static InfusionOutput enchant(ResourceLocation id) { return new InfusionOutput(ItemStack.EMPTY,"",null,id); }
    public static InfusionOutput enchant(ResourceLocation id,int levels) {
        if(levels<1 || levels>256)throw new IllegalArgumentException("Invalid enchantment increment");
        var result=enchant(id);result.enchantmentLevels=levels;result.persistentEnchantment=true;return result;
    }
    public static InfusionOutput unchanged() { return new InfusionOutput(ItemStack.EMPTY,"",null,null,true); }
    public ItemStack apply(ItemStack central) {
        var result=applyRaw(central);return frozen||consumed||enchantment!=null?result:thaumcraft.api.crafting.InfusionLifecycle.transform(result);
    }
    private ItemStack applyRaw(ItemStack central) {
        if (!item.isEmpty()) return item.copy();
        if (central.isEmpty()) return ItemStack.EMPTY;
        var result=central.copy();
        if (unchanged) return result;
        if (value!=null) result.getOrCreateTag().put(label,value.copy());
        else if (enchantment!=null) {
            var target=ForgeRegistries.ENCHANTMENTS.getValue(enchantment);
            if (target==null) return ItemStack.EMPTY;
            int before=0;
            // Vanilla's helper clamps NBT levels to 255. Read the stored level so
            // level 256 or addon-authored levels cannot be rewritten to the same/lower value.
            for(var value:result.getEnchantmentTags()) {
                var row=(CompoundTag)value;
                if(enchantment.toString().equals(row.getString("id")))before=Math.max(before,row.getShort("lvl"));
            }
            long after=(long)before+enchantmentLevels;
            if(result.getCount()!=1 || after<=before || after>thaumcraft.api.crafting.InfusionEnchantmentLimits.maximum(target,persistentEnchantment))
                return ItemStack.EMPTY;
            // Do not rebuild Enchantments through the registry: that drops unknown
            // enchantments and extension data stored on individual list entries.
            var list=result.getEnchantmentTags().copy();boolean found=false;
            for(int i=0;i<list.size();i++) {
                var row=list.getCompound(i);
                if(enchantment.toString().equals(row.getString("id"))) {row.putShort("lvl",(short)after);found=true;}
            }
            if(!found){var row=new CompoundTag();row.putString("id",enchantment.toString());row.putShort("lvl",(short)after);list.add(row);}
            result.getOrCreateTag().put("Enchantments",list);
        } else return ItemStack.EMPTY;
        return result;
    }
    /** Check the player-visible contract even for addon-supplied enchanting plans. */
    public static boolean onlyEnchantmentIncreased(ItemStack before,ItemStack after) {
        if(before.isEmpty() || after.isEmpty() || before.getCount()!=1 || after.getCount()!=1 || !before.is(after.getItem()))return false;
        var a=before.save(new CompoundTag());var b=after.save(new CompoundTag());
        var oldRows=a.getCompound("tag").getList("Enchantments",Tag.TAG_COMPOUND).copy();
        var newRows=b.getCompound("tag").getList("Enchantments",Tag.TAG_COMPOUND).copy();
        a.getCompound("tag").remove("Enchantments");b.getCompound("tag").remove("Enchantments");
        if(a.getCompound("tag").isEmpty())a.remove("tag");if(b.getCompound("tag").isEmpty())b.remove("tag");
        if(!a.equals(b))return false;
        int increased=0;var seen=new java.util.HashSet<String>();
        for(var value:newRows) {
            var next=((CompoundTag)value).copy();var previous=new CompoundTag();int match=-1;
            if(next.getString("id").isEmpty() || !seen.add(next.getString("id")))return false;
            for(int i=0;i<oldRows.size();i++)if(oldRows.getCompound(i).getString("id").equals(next.getString("id"))){previous=oldRows.getCompound(i).copy();match=i;break;}
            int prior=previous.getShort("lvl"),level=next.getShort("lvl");
            if(match<0 && level<=0)return false;
            if(level<prior)return false;
            if(level>prior && ++increased>1)return false;
            if(match>=0){previous.remove("lvl");next.remove("lvl");if(!previous.equals(next))return false;oldRows.remove(match);}
        }
        return oldRows.isEmpty() && increased==1;
    }
    public CompoundTag save() {
        var tag=new CompoundTag();
        if(frozen){tag.putInt("IntentVersion",1);tag.putString("Rules",rulesVersion);tag.putString("Recipe",recipeId);tag.putString("Research",research);}
        if (consumed) { tag.putBoolean("Consumed",true); return tag; }
        if (unchanged) { tag.putBoolean("Unchanged",true); return tag; }
        if (!item.isEmpty()) tag.put("Item",item.save(new CompoundTag()));
        else if (value!=null) { tag.putString("Label",label); tag.put("Value",value.copy()); }
        else if (enchantment!=null) {tag.putString("Enchantment",enchantment.toString());if(persistentEnchantment)tag.putInt("EnchantmentLevels",enchantmentLevels);}
        return tag;
    }
    public static InfusionOutput load(CompoundTag tag) {
        int version=tag.getInt("IntentVersion");if(version<0||version>1)throw new IllegalArgumentException("Unknown infusion intent version: "+version);
        var result=loadRaw(tag);result.frozen=version==1;result.rulesVersion=tag.getString("Rules");
        result.recipeId=tag.getString("Recipe");result.research=tag.getString("Research");return result;
    }
    private static InfusionOutput loadRaw(CompoundTag tag) {
        if (tag.getBoolean("Consumed")) return consumed();
        if (tag.getBoolean("Unchanged")) return unchanged();
        if (tag.contains("Item",Tag.TAG_COMPOUND)) return item(ItemStack.of(tag.getCompound("Item")));
        if (tag.contains("Value") && !tag.getString("Label").isEmpty()) return patch(tag.getString("Label"),tag.get("Value"));
        var id=ResourceLocation.tryParse(tag.getString("Enchantment"));
        return id==null ? item(ItemStack.EMPTY) : tag.contains("EnchantmentLevels")
                ? enchant(id,Math.max(1,Math.min(256,tag.getInt("EnchantmentLevels")))) : enchant(id);
    }
}
