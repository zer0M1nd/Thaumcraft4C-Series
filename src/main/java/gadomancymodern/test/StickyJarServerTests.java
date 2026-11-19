package gadomancymodern.test;

import gadomancymodern.Gadomancy;
import gadomancymodern.automation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class StickyJarServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void inheritedJarStatesRemainAvailable(GameTestHelper test){
        var base=tc4revival.content.TCItems.ESSENTIA_JAR.get();
        var baseState=((net.minecraft.world.item.BlockItem)base).getBlock().defaultBlockState();
        for(var block:java.util.List.of(Gadomancy.STICKY.get(),Gadomancy.REMOTE_JAR.get())){
            var state=block.defaultBlockState();
            test.assertTrue(state.getProperties().containsAll(baseState.getProperties()),"Sticky and remote jars retain every base jar state");
            var pos=new BlockPos(1,2,1);test.setBlock(pos,state);
            test.assertTrue(test.getBlockState(pos).getLightEmission()==baseState.getLightEmission(),"Default light emission matches the parent jar");
        }
        test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void recipePreservesNbtAndRejectsAlreadySticky(GameTestHelper test){
        var recipe=(StickyJarRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("gadomancy:sticky_filled_void_jar")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);var jar=new ItemStack(tc4revival.content.TCItems.FILLED_VOID_JAR.get());jar.setHoverName(net.minecraft.network.chat.Component.literal("kept"));jar.getOrCreateTag().putString("AspectFilter","aer");tc4revival.aspect.AspectNbt.write(jar.getOrCreateTag(),new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("aer"),32));
        grid.setItem(2,jar);grid.setItem(5,new ItemStack(Items.SLIME_BALL));test.assertTrue(recipe.matches(grid,test.getLevel()),"Offset vertical jar and slime recipe");var result=recipe.assemble(grid,test.getLevel().registryAccess());
        var expected=jar.copy();expected.getOrCreateTag().putBoolean("isStickyJar",true);test.assertTrue(ItemStack.isSameItemSameTags(result,expected),"Contents, label and custom name retained");test.assertTrue(!jar.getTag().contains("isStickyJar"),"Input remains untouched during preview");grid.setItem(2,result);test.assertTrue(!recipe.matches(grid,test.getLevel()),"Reject already sticky jar");grid.clearContent();grid.setItem(8,jar);test.assertTrue(!recipe.matches(grid,test.getLevel()),"Bottom row cannot read beyond grid");test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void placementRotatesAllSixConnectionsAndPersists(GameTestHelper test){
        var level=test.getLevel();var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GadJar"));var anchor=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(anchor,Blocks.STONE.defaultBlockState());player.setPos(anchor.getX()+.5,anchor.getY()+2,anchor.getZ()+.5);
        for(Direction clicked:Direction.values()){
            var target=anchor.relative(clicked);level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());var stack=new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get());stack.getOrCreateTag().putBoolean("isStickyJar",true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
            var hit=new net.minecraft.world.phys.BlockHitResult(anchor.getCenter().add(clicked.getStepX()*.5,clicked.getStepY()*.5,clicked.getStepZ()*.5),clicked,anchor,false);
            StickyJars.remember(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,anchor,hit));
            var result=stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            test.assertTrue(result.consumesAction()&&level.getBlockEntity(target) instanceof StickyJarBlockEntity,"Actual original jar placement is replaced with sticky BE");var jar=(StickyJarBlockEntity)level.getBlockEntity(target);
            test.assertTrue(jar.support()==clicked.getOpposite(),"Clicked attachment direction preserved");for(Direction side:Direction.values())test.assertTrue(jar.connectable(side)==(side==clicked),"Only rotated lid connects");
            var aspect=tc4revival.aspect.TCAspects.catalog().require("ordo");jar.addToContainer(aspect,24);jar.label(aspect,Direction.NORTH);var saved=jar.saveWithoutMetadata();var restored=new StickyJarBlockEntity(target,jar.getBlockState());restored.load(saved);
            test.assertTrue(restored.amount()==24&&restored.filter().equals(aspect)&&restored.support()==clicked.getOpposite(),"Storage, filter and orientation survive save");test.assertTrue(StickyJars.sticky(restored.asItem()),"Portable drop retains sticky marker");level.removeBlock(target,false);
        }test.succeed();
    }
}
