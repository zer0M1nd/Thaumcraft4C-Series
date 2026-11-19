package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared native-cluster conversion for excavation and the later bore/elemental-tool callers. */
public  class SpecialMining {
    private static final TagKey<Item> CINNABAR = TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:ores/cinnabar"));
    private SpecialMining() {}
    private static boolean matches(ItemStack stack,String metal) {
        return stack.is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse("forge:ores/"+metal)))
                ||stack.is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse("forge:raw_materials/"+metal)));
    }
    public record Conversion(ResourceLocation id,java.util.function.Predicate<ItemStack> input,
            java.util.function.Supplier<ItemStack> output,float multiplier) {}
    private static final java.util.List<Conversion> ADDITIONS=new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Predicates are evaluated at use time, after tags have loaded; native conversions take precedence. */
    public static synchronized void register(ResourceLocation id,java.util.function.Predicate<ItemStack> input,
            java.util.function.Supplier<ItemStack> output,float multiplier) {
        java.util.Objects.requireNonNull(id);java.util.Objects.requireNonNull(input);java.util.Objects.requireNonNull(output);
        if(!Float.isFinite(multiplier)||multiplier<0)throw new IllegalArgumentException("Invalid mining multiplier");
        if(ADDITIONS.stream().anyMatch(rule->rule.id().equals(id)))throw new IllegalArgumentException("Duplicate mining rule "+id);
        ADDITIONS.add(new Conversion(id,input,output,multiplier));
    }
    public static ItemStack apply(ItemStack drop, int fortune, RandomSource random) {
        float roll = random.nextFloat(); ClusterType cluster = null; float multiplier = 1;
        if (matches(drop,"iron")) cluster = ClusterType.IRON;
        else if (matches(drop,"gold")) { cluster = ClusterType.GOLD; multiplier = .9f; }
        else if (matches(drop,"copper")) cluster = ClusterType.COPPER;
        else if (matches(drop,"cinnabar")) { cluster = ClusterType.CINNABAR; multiplier = .9f; }
        if(cluster==null&&!drop.isEmpty())for(var rule:ADDITIONS)if(rule.input().test(drop.copy())) {
            if(roll>(.2f+fortune*.075f)*rule.multiplier())return drop.copy();
            var output=rule.output().get().copy();if(output.isEmpty())return drop.copy();
            output.setCount(drop.getCount());return output;
        }
        return cluster != null && roll <= (.2f + fortune * .075f) * multiplier ? new ItemStack(TCItems.cluster(cluster), drop.getCount()) : drop.copy();
    }
}
