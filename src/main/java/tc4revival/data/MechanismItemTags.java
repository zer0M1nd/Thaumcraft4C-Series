package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraft.data.tags.ItemTagsProvider;
import tc4revival.content.InfusionLayout;

public class MechanismItemTags extends ItemTagsProvider {
    public MechanismItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<TagsProvider.TagLookup<Block>> blocks, ExistingFileHelper helper) {
        super(output, lookup, blocks, "thaumcraft", helper);
    }
    @Override protected void addTags(HolderLookup.Provider lookup) {
        var builder=tag(InfusionLayout.STABILIZER_ITEMS);
        TableBlockTags.stabilizers().stream().map(Block::asItem).filter(item->item!=net.minecraft.world.item.Items.AIR).distinct().forEach(builder::add);
        for(var color:net.minecraft.world.item.DyeColor.values())
            builder.addOptional(net.minecraft.resources.ResourceLocation.parse("botania:"+color.getName()+"_shiny_flower"));
    }
    @Override public String getName() { return "Thaumcraft infusion stability item tags"; }
}
