package tc4revival.research;

import java.util.Optional;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.content.AuraNodeType;
import tc4revival.content.NodeJarData;

/** Book-only ingredients that had legacy inventory renderers but no modern obtainable item. */
public enum ResearchStructureDisplay {
    EMPTY_SPACE("book_empty_space", "tc.book.structure.empty"),
    LAVA("book_lava", "block.minecraft.lava"),
    NODE(null, "tc.book.structure.node");
    private final String model, translation;
    ResearchStructureDisplay(String model, String translation) { this.model=model; this.translation=translation; }
    public String model() { return model; }
    public String translation() { return translation; }
    public static Optional<ResearchStructureDisplay> of(ResearchDefinition.Stack stack) {
        if (stack.nbt().isPresent()) return Optional.empty();
        String id=stack.key().item(); int meta=stack.key().metadata();
        if (id.equals("Thaumcraft:blockHole") && meta==15) return Optional.of(EMPTY_SPACE);
        if (id.equals("minecraft:lava") && meta==0) return Optional.of(LAVA);
        if (id.equals("Thaumcraft:blockAiry") && meta==5) return Optional.of(NODE);
        return Optional.empty();
    }
    public static Optional<net.minecraft.world.item.ItemStack> literal(ResearchPage.Recipe recipe,long milliseconds) {
        if (!(recipe instanceof ResearchPage.Literal literal) || !literal.value().equals("slabWood")) return Optional.empty();
        var items=java.util.List.of(net.minecraft.world.item.Items.OAK_SLAB,net.minecraft.world.item.Items.SPRUCE_SLAB,
                net.minecraft.world.item.Items.BIRCH_SLAB,net.minecraft.world.item.Items.JUNGLE_SLAB,
                net.minecraft.world.item.Items.ACACIA_SLAB,net.minecraft.world.item.Items.DARK_OAK_SLAB);
        return Optional.of(new net.minecraft.world.item.ItemStack(items.get((int)Math.floorMod(milliseconds/1000,items.size()))));
    }
    /** ItemNodeRenderer uses four fixed aspects, irrespective of an actual world node. */
    public static NodeJarData node() {
        var aspects=new AspectAmounts();
        for (String tag:java.util.List.of("aer","ignis","terra","aqua")) aspects.add(TCAspects.catalog().require(tag),40);
        return new NodeJarData(aspects,AuraNodeType.NORMAL,null,"");
    }
}
