package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Modern material equivalence. The public class name remains for addon binary compatibility. */
public class LegacyCraftingCategories {
    public record Category(TagKey<Item> tag, List<ResourceLocation> members) {}
    private static final List<Category> CATEGORIES=create();
    private LegacyCraftingCategories() {}
    public static List<Category> categories() {return CATEGORIES;}
    public static boolean matches(ItemStack offered,ItemStack expected) {
        if(offered.isEmpty() || expected.isEmpty())return false;
        return CATEGORIES.stream().anyMatch(c->offered.is(c.tag()) && expected.is(c.tag()));
    }
    private static List<Category> create() {
        var result=new ArrayList<Category>();
        for(String tag:List.of("minecraft:logs","minecraft:planks","minecraft:wooden_slabs","minecraft:wooden_stairs",
                "minecraft:saplings","minecraft:leaves","minecraft:music_discs","forge:rods/wooden","forge:dyes",
                "forge:glass","forge:glass_panes","forge:sand","forge:sandstone","forge:ingots/brick","forge:ingots/nether_brick",
                "forge:gems/diamond","forge:gems/emerald","forge:gems/quartz","forge:gems/lapis","forge:dusts/redstone",
                "forge:dusts/glowstone","forge:slimeballs","forge:storage_blocks/glowstone","forge:crops/wheat",
                "forge:crops/potato","forge:crops/carrot","forge:stone","forge:cobblestone"))add(result,tag);
        for(String material:List.of("gold","iron","lapis","diamond","redstone","emerald","quartz","coal","copper")) {
            add(result,"forge:ores/"+material);add(result,"forge:storage_blocks/"+material);
        }
        for(String material:List.of("iron","gold","copper")) {add(result,"forge:ingots/"+material);add(result,"forge:nuggets/"+material);}
        for(var type:ShardType.values())if(type!=ShardType.BALANCED)add(result,"forge:shards/"+type.material());
        for(var type:MaterialType.values())for(var tag:type.tags())add(result,tag);
        for(var type:NuggetType.values())type.tag().ifPresent(tag->add(result,tag));
        for(var type:ClusterType.values())add(result,type.tag());
        for(int ore=0;ore<8;ore++)add(result,LegacyOreBlock.tag(ore));
        return List.copyOf(result);
    }
    private static void add(List<Category> result,String tag) {
        var key=ItemTags.create(ResourceLocation.parse(tag));
        if(result.stream().noneMatch(c->c.tag().equals(key)))result.add(new Category(key,List.of()));
    }
}
