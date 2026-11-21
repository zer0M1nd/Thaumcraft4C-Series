package tc4revival.content;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchStructureServerTests {
    @GameTest(template="wand_test_empty")
    public static void researchStructureDisplayCoverage(GameTestHelper test) {
        int structures=0; var special=java.util.EnumSet.noneOf(ResearchStructureDisplay.class);
        for (var entry:ResearchPageRecipes.bundled().entries().values()) {
            if (!(entry.definition() instanceof ResearchPageRecipes.Compound compound)) continue;
            structures++;
            for (var ingredient:compound.blocks().elements()) check(test,ingredient,special);
        }
        test.assertTrue(structures>0 && special.size()==3,"Every special preview occurs in actual structure recipes");
        var slabs=new java.util.HashSet<net.minecraft.world.item.Item>();
        for (int i=0;i<6;i++) slabs.add(ResearchStructureDisplay.literal(new ResearchPage.Literal("slabWood"),i*1000L).orElseThrow().getItem());
        test.assertTrue(slabs.equals(java.util.Set.of(net.minecraft.world.item.Items.OAK_SLAB,net.minecraft.world.item.Items.SPRUCE_SLAB,
                net.minecraft.world.item.Items.BIRCH_SLAB,net.minecraft.world.item.Items.JUNGLE_SLAB,net.minecraft.world.item.Items.ACACIA_SLAB,
                net.minecraft.world.item.Items.DARK_OAK_SLAB)),"All six old wooden slab subtypes cycle");
        var node=ResearchStructureDisplay.node();
        test.assertTrue(node.type()==AuraNodeType.NORMAL && node.modifier()==null && node.aspects().size()==4,"Legacy display node shape");
        for (String tag:java.util.List.of("aer","ignis","terra","aqua"))
            test.assertTrue(node.aspects().amount(tc4revival.aspect.TCAspects.catalog().require(tag))==40,"Fixed node aspect "+tag);
        node.aspects().add(tc4revival.aspect.TCAspects.catalog().require("aer"),7);
        test.assertTrue(ResearchStructureDisplay.node().aspects().total()==160,"Preview payloads cannot mutate each other");
        for (String name:java.util.List.of("book_empty_space","book_lava"))
            test.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+name)),"Preview is not an obtainable item");
        tc4revival.TC4Revival.LOGGER.info("Research structure display test passed: {} structures, three special kinds, no missing cells",structures);
        test.succeed();
    }
    private static void check(GameTestHelper test,ResearchPage.Recipe ingredient,java.util.Set<ResearchStructureDisplay> special) {
        if (ingredient instanceof ResearchPage.Empty) return;
        if (ingredient instanceof ResearchPage.Sequence sequence) { for (var child:sequence.elements()) check(test,child,special); return; }
        if (ingredient instanceof ResearchPage.Ore ore) { test.assertTrue(!ore.elements().isEmpty(),"Ore preview has choices"); for (var child:ore.elements()) check(test,child,special); return; }
        if (ingredient instanceof ResearchPage.Literal) {
            test.assertTrue(ResearchStructureDisplay.literal(ingredient,0).isPresent(),"Missing literal display "+ingredient); return;
        }
        test.assertTrue(ingredient instanceof ResearchPage.Item,"Expected display ingredient "+ingredient);
        var stack=((ResearchPage.Item)ingredient).stack();
        var kind=ResearchStructureDisplay.of(stack);
        if (kind.isPresent()) { special.add(kind.get()); return; }
        var resolved=ResearchDisplayItems.bundled().resolve(stack,0);
        test.assertTrue(resolved.isPresent()&&!resolved.get().isEmpty(),"Missing compound cell "+stack.key());
    }
}
