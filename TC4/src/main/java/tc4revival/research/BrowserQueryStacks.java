package tc4revival.research;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Stable query identities; unknown addon NBT is retained rather than assumed irrelevant. */
public class BrowserQueryStacks {
    /** Recipe identity is structural; charge and a removable focus are not wand components. */
    public static CompoundTag recipeKey(ItemStack stack) {
        if (!(stack.getItem() instanceof tc4revival.content.CastingWandItem wand)) return key(stack);
        var canonical=new ItemStack(stack.getItem());
        var tag=canonical.getOrCreateTag();
        tag.putString("rod",wand.rod(stack).tag);
        tag.putString("cap",wand.cap(stack).tag);
        if(wand.isSceptre(stack))tag.putBoolean("sceptre",true);
        return canonical.save(new CompoundTag());
    }
    public static boolean eligible(ItemStack stack){return !stack.isEmpty()&&(!stack.hasTag()||stack.getTag().sizeInBytes()<=16384);}
    public static CompoundTag key(ItemStack stack) {
        var saved=stack.copyWithCount(1).save(new CompoundTag());
        var tag=saved.getCompound("tag");
        // These vanilla bookkeeping fields do not contribute to ItemStackAspects.
        // Custom stored-aspect items retain everything, since their implementation may inspect any field.
        if(!(stack.getItem() instanceof tc4revival.content.StoredAspectItem)){
            tag.remove("Damage");tag.remove("RepairCost");tag.remove("display");
        }
        if(stack.getItem().getClass()==tc4revival.content.CastingWandItem.class)
            for(var name:java.util.List.of("aer","terra","ignis","aqua","ordo","perditio"))tag.remove(name);
        if(tag.isEmpty())saved.remove("tag");
        return saved;
    }
    /** Registry defaults plus all registered potion forms, including vanilla's NBT variants. */
    public static void enumerate(Consumer<ItemStack> out) {
        for(var item:ForgeRegistries.ITEMS.getValues())if(item!=Items.AIR)out.accept(new ItemStack(item));
        for(var potion:ForgeRegistries.POTIONS.getValues())if(potion!=Potions.EMPTY)
            for(var item:java.util.List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.TIPPED_ARROW))
                out.accept(PotionUtils.setPotion(new ItemStack(item),potion));
    }
}
