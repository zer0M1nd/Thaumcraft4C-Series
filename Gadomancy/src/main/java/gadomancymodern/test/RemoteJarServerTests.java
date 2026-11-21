package gadomancymodern.test;

import gadomancymodern.Gadomancy;
import gadomancymodern.automation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class RemoteJarServerTests {
    protected static RemoteJarEntity jar(GameTestHelper test,BlockPos relative){var pos=test.absolutePos(relative);test.getLevel().setBlockAndUpdate(pos,Gadomancy.REMOTE_JAR.get().defaultBlockState());return (RemoteJarEntity)test.getLevel().getBlockEntity(pos);}
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void bindingPortableContentsAndStickyRemoteRecipe(GameTestHelper test){
        var level=test.getLevel();var jar=jar(test,new BlockPos(2,3,2));var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RemoteJar"));var stack=new ItemStack(Gadomancy.REMOTE_JAR_ITEM.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);var item=(gadomancymodern.item.RemoteJarItem)stack.getItem();var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(jar.getBlockPos().getCenter(),Direction.UP,jar.getBlockPos(),false));
        item.onItemUseFirst(stack,context);test.assertTrue(jar.network()!=null&&jar.network().equals(stack.getTag().getUUID("networkId")),"First use creates network on world jar and carried jar");
        var second=jar(test,new BlockPos(4,3,2));second.readItem(stack);var aspect=tc4revival.aspect.TCAspects.catalog().require("aer");second.addToContainer(aspect,24);second.label(aspect,Direction.NORTH);var portable=second.asItem();test.assertTrue(portable.is(Gadomancy.REMOTE_JAR_ITEM.get())&&!portable.getTag().getBoolean("isStickyJar")&&portable.getTag().getUUID("networkId").equals(jar.network()),"Ordinary remote portable retains its own item and network without becoming sticky");
        var sticky=(StickyJarRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("gadomancy:sticky_remote_jar")).orElseThrow();var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(0,portable);grid.setItem(3,new ItemStack(Items.SLIME_BALL));test.assertTrue(sticky.matches(grid,level),"Remote jar is a real sticky-jar recipe input");var output=sticky.assemble(grid,level.registryAccess());var restored=new RemoteJarEntity(second.getBlockPos(),second.getBlockState());restored.readItem(output);restored.support(Direction.WEST);var saved=restored.saveWithoutMetadata();var loaded=new RemoteJarEntity(second.getBlockPos(),second.getBlockState());loaded.load(saved);test.assertTrue(loaded.sticky()&&loaded.support()==Direction.WEST&&loaded.amount()==24&&loaded.filter().equals(aspect)&&loaded.network().equals(jar.network()),"Sticky direction, contents, label and remote network survive save");
        player.setShiftKeyDown(true);item.use(level,player,InteractionHand.MAIN_HAND);test.assertTrue(!stack.hasTag()&&jar.network()!=null,"Sneak air-use clears carried binding only");test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void loadedFullJarTransfersAtOriginalNetworkRate(GameTestHelper test){
        var high=jar(test,new BlockPos(2,4,2));var low=jar(test,new BlockPos(5,4,2));var aspect=tc4revival.aspect.TCAspects.catalog().require("ordo");var id=java.util.UUID.randomUUID();high.network(id);low.network(id);high.addToContainer(aspect,64);
        high.tick();low.tick();test.runAfterDelay(8,()->{test.assertTrue(low.amount()>0&&high.amount()+low.amount()==64,"GTNH full-jar registration fix permits transfer immediately after load");
            var group=new RemoteJarEntity.Network();group.join(high);group.join(low);int before=low.amount();group.update(10000);test.assertTrue(low.amount()==before+1,"One network operation transfers exactly one essentia");group.update(10003);test.assertTrue(low.amount()==before+1,"Strict greater-than three-tick gate prevents early operation");group.update(10004);test.assertTrue(low.amount()==before+2,"Fourth tick can perform the next operation");
            low.empty();low.addToContainer(tc4revival.aspect.TCAspects.catalog().require("ignis"),1);int stored=high.amount();group.update(10008);test.assertTrue(high.amount()==stored&&low.amount()==1,"Different stored aspect blocks transfer without losing source");test.getLevel().removeBlock(low.getBlockPos(),false);group.update(10012);test.assertTrue(high.amount()==stored,"Removed node is evicted without losing source");test.succeed();});
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void mirrorConfigurationChangesRecipeAndResearchTogether(GameTestHelper test){
        var level=test.getLevel();var recipe=(RemoteJarRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("gadomancy:remote_jar")).orElseThrow();var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(int i:new int[]{0,2,3,5})grid.setItem(i,new ItemStack(tc4revival.content.TCArcane.GREATWOOD_LOG.get()));grid.setItem(1,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get()));grid.setItem(4,new ItemStack(tc4revival.content.TCItems.ESSENTIA_MIRROR.get()));boolean prior=tc4revival.config.TCConfig.SERVER.allowMirrors.get();
        try(var temporary=new TemporaryCoreConfig()){tc4revival.config.TCConfig.SERVER.allowMirrors.set(true);test.assertTrue(recipe.matches(grid,level),"Default ingredient is essentia mirror");tc4revival.config.TCConfig.SERVER.allowMirrors.set(false);test.assertTrue(!recipe.matches(grid,level),"Disabled mirror is no longer a valid catalyst");grid.setItem(4,new ItemStack(tc4revival.content.TCItems.material(tc4revival.content.MaterialType.MIRROR_GLASS)));test.assertTrue(recipe.matches(grid,level)&&recipe.displayDefinition().getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString().equals("thaumcraft:mirrored_glass"),"Configured fallback and rendered page both use mirrored glass");var catalog=tc4revival.research.ResearchDefinitions.decode(thaumcraft.api.research.ResearchPrerequisites.materialize(tc4revival.research.ResearchDefinitions.server().json())).catalog();test.assertTrue(!catalog.require("gadomancy:remote_jar").parents().contains("MIRRORESSENTIA"),"Disabled mirror research is not a prerequisite");}test.succeed();
    }
}
