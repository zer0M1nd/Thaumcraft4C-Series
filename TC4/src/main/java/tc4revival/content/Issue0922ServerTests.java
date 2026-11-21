package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Issue0922ServerTests {
    private static ItemStack item(String id,int count){return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)),count);}
    private static FakePlayer altarPlayer(GameTestHelper test,BlockPos matrix){
        var level=test.getLevel();level.setBlockAndUpdate(matrix,TCArcane.INFUSION_MATRIX.get().defaultBlockState());
        level.setBlockAndUpdate(matrix.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})level.setBlockAndUpdate(matrix.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState());
        for(var dir:net.minecraft.core.Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(matrix.relative(dir,3).below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"PlaceInfusion"));player.setGameMode(GameType.SURVIVAL);player.setPos(matrix.getX()+.5,matrix.getY()-2,matrix.getZ()+.5);player.setYRot(270);return player;
    }
    private static ArcanePedestalBlockEntity pedestal(GameTestHelper test,BlockPos pos){return (ArcanePedestalBlockEntity)test.getLevel().getBlockEntity(pos);}
    @GameTest(template="wand_test_empty") public static void jeiPlacementStartsOrderedInfusionInEveryFacing(GameTestHelper test){
        var config=tc4revival.config.TCConfig.SERVER.orderedInfusion;boolean previous=config.get();
        try {
            config.set(true);
            var matrix=test.absolutePos(new BlockPos(5,4,5));
            var recipeId=net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/elemental_sword");
            var recipe=(InfusionRecipe)test.getLevel().getRecipeManager().byKey(recipeId).orElseThrow();
            for(var facing:net.minecraft.core.Direction.Plane.HORIZONTAL){
                var p=altarPlayer(test,matrix);p.setYRot(facing.toYRot());
                for(var pos:InfusionLayout.scan(test.getLevel(),matrix).pedestals())pedestal(test,pos).clearContent();
                pedestal(test,matrix.below(2)).clearContent();
                var center=item("thaumcraft:thaumium_sword",1);
                p.getInventory().setItem(0,center.copy());p.getInventory().setItem(1,item("thaumcraft:air_shard",2));
                p.getInventory().setItem(2,item("thaumcraft:greatwood_log",1));p.getInventory().setItem(3,new ItemStack(Items.DIAMOND));
                // A stray pedestal on the matrix axis must not become a recipe component.
                var vertical=matrix.above(2);test.getLevel().setBlockAndUpdate(vertical,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());
                pedestal(test,vertical).setItem(0,new ItemStack(Items.EMERALD));
                InfusionQuickPlacement.place(p,recipeId,center);
                var components=InfusionLayout.scan(test.getLevel(),matrix).pedestals().stream().map(pos->pedestal(test,pos).getItem(0)).filter(s->!s.isEmpty()).toList();
                test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,components,key->true,true),test.getLevel()),"JEI materials match clockwise recipe from "+facing);
                test.assertTrue(p.getInventory().isEmpty()&&pedestal(test,vertical).getItem(0).is(Items.EMERALD),"Consumes only inventory materials and leaves vertical pedestal alone");
                tc4revival.research.KnowledgeSavedData.get(p.getServer()).update(p.getUUID(),p.getRandom()::nextInt,k->k.rememberResearch("ELEMENTALSWORD"));
                var altar=(InfusionMatrixBlockEntity)test.getLevel().getBlockEntity(matrix);altar.activate();altar.start(p);
                test.assertTrue(altar.crafting(),"Real ordered altar starts after JEI placement from "+facing);
                // Reset process without waiting for this test's deliberately absent essentia supply.
                var reset=new net.minecraft.nbt.CompoundTag();reset.put("Process",new InfusionProcess().save());altar.load(reset);
            }
        }finally{config.set(previous);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void infusionPlacementReusesReturnedItemsAndKeepsCounterclockwiseOrder(GameTestHelper test){
        var matrix=test.absolutePos(new BlockPos(5,4,5));var p=altarPlayer(test,matrix);var center=item("thaumcraft:thaumium_sword",1);
        p.getInventory().setItem(0,center.copy());p.getInventory().setItem(1,item("thaumcraft:air_shard",2));p.getInventory().setItem(2,item("thaumcraft:greatwood_log",1));
        pedestal(test,matrix.west(3).below(2)).setItem(0,new ItemStack(Items.DIAMOND));
        InfusionQuickPlacement.place(p,net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/elemental_sword"),center);
        test.assertTrue(pedestal(test,matrix.below(2)).getItem(0).is(center.getItem()),"Owned sword moves to center");
        var positions=java.util.List.of(matrix.east(3),matrix.north(3),matrix.west(3),matrix.south(3));
        var expected=java.util.List.of("thaumcraft:air_shard","thaumcraft:greatwood_log","minecraft:diamond","thaumcraft:air_shard");
        for(int i=0;i<4;i++)test.assertTrue(pedestal(test,positions.get(i).below(2)).getItem(0).is(item(expected.get(i),1).getItem()),"Top then counterclockwise ingredient "+i);
        test.assertTrue(p.getInventory().isEmpty(),"No duplication: returned diamond was reused");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void infusionPlacementInsufficientMaterialsReturnsOldContents(GameTestHelper test){
        var matrix=test.absolutePos(new BlockPos(5,4,5));var p=altarPlayer(test,matrix);pedestal(test,matrix.below(2)).setItem(0,new ItemStack(Items.DIAMOND));
        InfusionQuickPlacement.place(p,net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/elemental_sword"),item("thaumcraft:thaumium_sword",1));
        test.assertTrue(pedestal(test,matrix.below(2)).isEmpty()&&p.getInventory().countItem(Items.DIAMOND)==1,"Failed new placement still returns previous contents exactly once");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void infusionPlacementAmbiguousMatrixDoesNotTouchInventory(GameTestHelper test){
        var matrix=test.absolutePos(new BlockPos(5,4,5));var p=altarPlayer(test,matrix);test.getLevel().setBlockAndUpdate(matrix.above(),TCArcane.INFUSION_MATRIX.get().defaultBlockState());
        pedestal(test,matrix.below(2)).setItem(0,new ItemStack(Items.DIAMOND));
        InfusionQuickPlacement.place(p,net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/elemental_sword"),item("thaumcraft:thaumium_sword",1));
        test.assertTrue(pedestal(test,matrix.below(2)).getItem(0).is(Items.DIAMOND)&&p.getInventory().isEmpty(),"Ambiguous altar leaves all items untouched");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void workbenchDoubleClickCollectsGridButNeverVirtualOutput(GameTestHelper test) {
        var p=test.absolutePos(new BlockPos(2,2,2));var level=test.getLevel();
        level.setBlockAndUpdate(p,TCArcane.BLOCK.get().defaultBlockState());
        var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(p);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"Issue3"));player.setGameMode(GameType.SURVIVAL);player.setPos(p.getX(),p.getY(),p.getZ());
        var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
        table.setItem(0,new ItemStack(Items.DIAMOND,12));table.setItem(1,new ItemStack(Items.DIAMOND,7));
        player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,5));menu.setCarried(new ItemStack(Items.DIAMOND,1));
        menu.clicked(4,0,ClickType.PICKUP_ALL,player);
        test.assertTrue(menu.getCarried().getCount()==25&&table.getItem(0).isEmpty()&&table.getItem(1).isEmpty(),"Double-click gathers real grid and inventory contents without loss");
        test.assertTrue(!menu.canTakeItemForPickAll(new ItemStack(Items.DIAMOND),menu.getSlot(0)),"Recipe preview is excluded");
        menu.setCarried(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void walkingEldritchFieldReceivesRandomTicksAndExpires(GameTestHelper test) {
        var p=test.absolutePos(new BlockPos(2,2,2));var level=test.getLevel();var field=TCEldritch.FIELD.get();
        level.setBlockAndUpdate(p,field.defaultBlockState());
        test.assertTrue(level.getBlockState(p).isRandomlyTicking(),"Walking trail must participate in random tick scheduling");
        field.randomTick(level.getBlockState(p),level,p,level.random);
        test.assertTrue(level.isEmptyBlock(p),"Random tick clears walking trail without explicit scheduled tick");test.succeed();
    }
}
