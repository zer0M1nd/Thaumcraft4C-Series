package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class DeconstructionTableServerTests {
    private static DeconstructionTableBlockEntity table(GameTestHelper test) { var pos=test.absolutePos(new BlockPos(3,3,3)); test.getLevel().setBlockAndUpdate(pos,TCArcane.DECONSTRUCTION_TABLE.get().defaultBlockState()); return (DeconstructionTableBlockEntity)test.getLevel().getBlockEntity(pos); }
    private static void ticks(DeconstructionTableBlockEntity table,int count) { for(int i=0;i<count;i++) DeconstructionTableBlockEntity.tick(table.getLevel(),table.getBlockPos(),table.getBlockState(),table); }
    @GameTest(template="wand_test_empty")
    public static void deconstructionTimingAndRandom(GameTestHelper test) {
        var table=table(test);var level=test.getLevel();
        table.setItem(0,new ItemStack(Items.PAPER,2));ticks(table,1);
        test.assertTrue(table.remaining()==39&&table.getItem(0).getCount()==2,"First tick starts and decrements the forty-tick cycle");
        ticks(table,38);test.assertTrue(table.remaining()==1&&table.getItem(0).getCount()==2,"Input survives until tick forty");
        ticks(table,1);test.assertTrue(table.remaining()==0&&table.getItem(0).getCount()==1,"Exactly one item is destroyed at tick forty");
        table.load(new CompoundTag());table.setItem(0,new ItemStack(Items.PAPER,2));ticks(table,10);table.setItem(0,new ItemStack(Items.DIAMOND,2));ticks(table,29);
        test.assertTrue(table.remaining()==1&&table.getItem(0).getCount()==2,"Swapping between valid inputs does not reset work");
        table.setItem(0,new ItemStack(Items.STRUCTURE_VOID));ticks(table,1);test.assertTrue(table.remaining()==0&&table.getItem(0).is(Items.STRUCTURE_VOID),"Aspect-free items stops work without consumption");
        int successes=0,failures=0;var distinct=new java.util.HashSet<String>();
        for(int sample=0;sample<128;sample++) {
            table.load(new CompoundTag());var stack=new ItemStack(Items.BOOK,2);table.setItem(0,stack);
            var primals=TCAspects.catalog().toPrimals(ItemStackAspects.resolve(stack).orElseThrow(),AspectCatalog.Reduction.SUM);
            long seed=sample*104729L;var random=net.minecraft.util.RandomSource.create(seed);boolean success=random.nextInt(80)<primals.total();
            var expected=success ? primals.aspects().get(random.nextInt(primals.size())) : null;
            level.random.setSeed(seed);ticks(table,40);
            test.assertTrue(java.util.Objects.equals(expected,table.pending())&&table.getItem(0).getCount()==1,"Roll uses summed recursive primals and a uniform distinct-type index, consuming one regardless of result");
            if(success) { successes++; distinct.add(expected.tag());ticks(table,80);test.assertTrue(table.getItem(0).getCount()==1&&table.remaining()==0,"Unclaimed result blocks subsequent destruction"); } else failures++;
        }
        test.assertTrue(successes>0&&failures>0&&distinct.size()>1,"Seeded cases cover success, failure and multiple primal rewards");
        table.load(new CompoundTag());table.setItem(0,new ItemStack(Items.WATER_BUCKET));ticks(table,40);test.assertTrue(table.isEmpty(),"Container remainder is destroyed like TC4");pass(test,"timing-random-consumption");
    }
    @GameTest(template="wand_test_empty")
    public static void deconstructionClaimPersistenceAndAutomation(GameTestHelper test) {
        var table=table(test);var level=test.getLevel();var primal=TCAspects.catalog().require("aer");
        for(var face:Direction.values()) {
            var handler=table.getCapability(ForgeCapabilities.ITEM_HANDLER,face).orElseThrow(()->new AssertionError("Missing item capability"));
            test.assertTrue(handler.getSlots()==(face==Direction.UP?0:1),"Only the top exposes no automation slots");
            if(face!=Direction.UP) { test.assertTrue(handler.insertItem(0,new ItemStack(Items.PAPER,4),false).isEmpty(),"Side and bottom accept aspect-bearing inputs");test.assertTrue(handler.extractItem(0,4,false).getCount()==4,"Side and bottom permit extraction"); }
        }
        table.setItem(0,new ItemStack(Items.PAPER,3));ticks(table,10);table.setCustomName("Research salvage");
        var saved=table.saveWithoutMetadata();saved.putString("Aspect","aer");table.load(saved);
        test.assertTrue(table.remaining()==0&&table.pending().equals(primal)&&table.getItem(0).getCount()==3&&table.getDisplayName().getString().equals("Research salvage"),"Inventory, pending reward and name persist; work time does not");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DeconstructionTest"));
        player.setPos(table.getBlockPos().getCenter());var original=player.containerMenu;var menu=new DeconstructionTableMenu(1,player.getInventory(),table);var data=KnowledgeSavedData.get(level.getServer());
        data.update(player.getUUID(),player.getRandom()::nextInt,k->{k.setPool(primal,(short)0);return true;});
        test.assertTrue(!table.claim(player)&&table.pending()!=null,"No award without the actual open menu");player.containerMenu=menu;
        try {
            test.assertTrue(!menu.clickMenuButton(player,2)&&menu.clickMenuButton(player,1)&&!menu.clickMenuButton(player,1),"Only the claim action awards, and replay cannot award twice");
            test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().pool(primal)==1&&!table.saveWithoutMetadata().contains("Aspect"),"Knowledge award and pending removal are saved");
            table.load(table.saveWithoutMetadata());test.assertTrue(!menu.clickMenuButton(player,1),"Reload does not resurrect a claimed reward");
            saved.putString("Aspect","aer");table.load(saved);player.setPos(table.getBlockPos().getCenter().add(20,0,0));test.assertTrue(!menu.clickMenuButton(player,1)&&table.pending()!=null,"Out-of-range packets cannot consume or claim");
        } finally { player.containerMenu=original; }
        pass(test,"claim-save-automation");
    }
    @GameTest(template="wand_test_empty")
    public static void deconstructionRecipeAndShiftClick(GameTestHelper test) {
        var table=table(test);var level=test.getLevel();
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SalvageRecipe"));player.setPos(table.getBlockPos().getCenter());
        var menu=new DeconstructionTableMenu(2,player.getInventory(),table);player.getInventory().setItem(9,new ItemStack(Items.PAPER,12));
        test.assertTrue(menu.quickMoveStack(player,1).getCount()==12&&table.getItem(0).getCount()==12&&player.getInventory().getItem(9).isEmpty(),"Shift-click moves eligible stacks into the input");
        test.assertTrue(menu.quickMoveStack(player,0).getCount()==12&&table.isEmpty()&&player.getInventory().getItem(9).getCount()==12,"Shift-click returns input to the first free player slot");
        player.getInventory().setItem(10,new ItemStack(Items.STRUCTURE_VOID,4));test.assertTrue(menu.quickMoveStack(player,2).getCount()==4&&player.getInventory().getItem(0).getCount()==4,"Aspect-free items move between inventory and hotbar");
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(1,new ItemStack(TCItems.THAUMOMETER.get()));grid.setItem(3,new ItemStack(Items.GOLDEN_AXE));grid.setItem(4,new ItemStack(TCItems.TABLE.get()));grid.setItem(5,new ItemStack(Items.GOLDEN_PICKAXE));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("deconstruction_table")).findFirst().orElseThrow();
        test.assertTrue(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(TCItems.DECONSTRUCTION_TABLE.get())&&recipe.visCost(grid).equals(java.util.Map.of(TCAspects.catalog().require("perditio"),20)),"Actual two-row recipe costs twenty entropy vis");
        test.assertTrue(!recipe.unlocked(grid,key->false)&&recipe.unlocked(grid,key->key.equals("DECONSTRUCTOR")),"Recipe requires the old research key");
        grid.getItem(3).setDamageValue(1);test.assertTrue(!recipe.matches(grid,level),"Worn gold tools do not match old metadata-zero ingredients");grid.getItem(3).setDamageValue(0);grid.getItem(3).enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY,1);test.assertTrue(recipe.matches(grid,level),"Unspecified NBT such as enchantments is permitted");
        var drops=net.minecraft.world.level.block.Block.getDrops(table.getBlockState(),level,table.getBlockPos(),table);test.assertTrue(drops.stream().anyMatch(s->s.is(TCItems.DECONSTRUCTION_TABLE.get())),"Breaking the table drops its registered item");pass(test,"recipe-shift-click-drop");
    }
    @GameTest(template="wand_test_empty")
    public static void openMenuAutomaticallyClaimsExactlyOnce(GameTestHelper test) {
        var table=table(test); var level=test.getLevel(); var primal=TCAspects.catalog().require("aer");
        var data=KnowledgeSavedData.get(level.getServer());
        var first=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutoClaimFirst"));
        var second=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutoClaimSecond"));
        first.setPos(table.getBlockPos().getCenter());second.setPos(table.getBlockPos().getCenter());
        var a=new DeconstructionTableMenu(1,first.getInventory(),table);var b=new DeconstructionTableMenu(2,second.getInventory(),table);
        var oldA=first.containerMenu;var oldB=second.containerMenu;
        data.update(first.getUUID(),n->0,k->{k.setPool(primal,(short)0);return null;});
        data.update(second.getUUID(),n->0,k->{k.setPool(primal,(short)0);return null;});
        try {
            var pending=new CompoundTag();pending.putString("Aspect","aer");table.load(pending);
            a.broadcastChanges();test.assertTrue(table.pending()!=null,"Detached menu cannot claim");
            first.containerMenu=a;second.containerMenu=b;
            a.broadcastChanges();b.broadcastChanges();a.broadcastChanges();
            test.assertTrue(table.pending()==null&&data.snapshot(first.getUUID()).orElseThrow().pool(primal)==1&&data.snapshot(second.getUUID()).orElseThrow().pool(primal)==0,"An open menu automatically awards once, even with two viewers");
            table.load(pending);first.containerMenu=oldA;second.containerMenu=oldB;
            a.broadcastChanges();b.broadcastChanges();test.assertTrue(table.pending()!=null,"Closed menus retain the next reward for a real viewer");
        } finally {first.containerMenu=oldA;second.containerMenu=oldB;}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void deconstructionDetachedMenuRejectsPlacement(GameTestHelper test) {
        var table = new DeconstructionTableBlockEntity(BlockPos.ZERO,TCArcane.DECONSTRUCTION_TABLE.get().defaultBlockState());
        var paper = new ItemStack(Items.PAPER);
        test.assertTrue(!table.canPlaceItem(0,paper),"A menu received before its block entity cannot query unavailable world aspect data");
        table.setLevel(test.getLevel()); table.setRemoved();
        test.assertTrue(!table.canPlaceItem(0,paper),"A removed table rejects insertion without evaluating aspects");
        pass(test,"detached-menu-safe");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Deconstruction test passed: {}",name);test.succeed(); }
}
