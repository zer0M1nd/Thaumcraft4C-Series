package tc4revival.content;

import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class LegacyChestServerTests {
    private LegacyChestServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void chestBooksAreInitializedBeforeRollsAndSharedWithinOneChest(GameTestHelper test) {
        var pool=new LegacyChestContents(List.of(new LegacyChestContents.Entry(new ItemStack(Items.ENCHANTED_BOOK),1,1,1,true)));
        var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(712) {
            int nextSlot;
            @Override public int nextInt(int bound) { return bound==27 ? nextSlot++%27 : super.nextInt(bound); }
        };
        var chest=new SimpleContainer(27); pool.fill(chest,8,random);
        var filled=new java.util.ArrayList<ItemStack>();
        for (int slot=0;slot<27;slot++) if (!chest.getItem(slot).isEmpty()) filled.add(chest.getItem(slot));
        test.assertTrue(filled.size()==8,"Eight independent rolls populated eight deliberately distinct slots");
        for (var stack:filled) test.assertTrue(stack.is(Items.ENCHANTED_BOOK) && !net.minecraft.world.item.EnchantedBookItem.getEnchantments(stack).isEmpty(),"Every book contains actual native enchantments");
        test.assertTrue(filled.stream().map(s->s.getTag().toString()).distinct().count()>1,"Books are rolled independently");
        test.assertTrue(!pool.entries().get(0).stack().hasTag(),"Chest generation never writes the rolled book back to the global pool");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void weightBoundariesAndSlotOverwritesPreserveTheOldSequence(GameTestHelper test) {
        var pool=new LegacyChestContents(List.of(new LegacyChestContents.Entry(new ItemStack(Items.IRON_INGOT),1,4,10),new LegacyChestContents.Entry(new ItemStack(Items.BREAD),1,1,3)));
        test.assertTrue(pool.select(bound->9).stack().is(Items.IRON_INGOT) && pool.select(bound->10).stack().is(Items.BREAD),"Intervals are consecutive and preserve table order");
        var chest=new SimpleContainer(3); chest.setItem(1,new ItemStack(Items.DIAMOND));
        var sequence=new Sequence(new int[]{13,4,13,1},new int[]{0,3,12,0});
        pool.fill(chest,2,sequence);
        test.assertTrue(sequence.finished() && chest.getItem(1).is(Items.DIAMOND) && chest.getItem(1).getCount()==1,"Existing contents survive loot insertion");
        test.assertTrue(chest.getItem(0).is(Items.IRON_INGOT)&&chest.getItem(0).getCount()==4 && chest.getItem(2).is(Items.BREAD),"Both rolls occupy available slots"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void oversizedResultsSplitIntoSinglesAndNeverMutateTemplates(GameTestHelper test) {
        var source=new ItemStack(Items.IRON_SWORD); source.getOrCreateTag().putString("owner","old");
        var pool=new LegacyChestContents(List.of(new LegacyChestContents.Entry(source,3,3,1)));
        source.getOrCreateTag().putString("owner","changed");
        var chest=new SimpleContainer(3); var sequence=new Sequence(new int[]{1,1},new int[]{0,0});
        pool.fill(chest,1,sequence);
        test.assertTrue(sequence.finished() && chest.getItem(0).getCount()==1 && chest.getItem(1).getCount()==1 && chest.getItem(2).getCount()==1,"Three nonstackable results occupy three separate slots");
        chest.getItem(0).getOrCreateTag().putString("owner","edited");
        test.assertTrue(chest.getItem(1).getTag().getString("owner").equals("old") && pool.entries().get(0).stack().getTag().getString("owner").equals("old"),"Generated items and templates do not share mutable NBT"); test.succeed();
    }
    private static  class Sequence implements IntUnaryOperator {
        private final int[] bounds,values; private int index;
        Sequence(int[] bounds,int[] values) { this.bounds=bounds; this.values=values; }
        @Override public int applyAsInt(int bound) {
            if (index>=bounds.length || bounds[index]!=bound) throw new AssertionError("Unexpected random bound or order at "+index+": "+bound);
            return values[index++];
        }
        boolean finished() { return index==values.length; }
    }
}
