package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.energy.PortableNodeEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class PortableNodeServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void placementRollFixedQualitySaveAndRemoval(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,8,3));var block=ElectroMagicTools.PORTABLE_NODE.get();var state=block.defaultBlockState();level.setBlockAndUpdate(pos,state);var player=GogglesServerTests.player(test);
        block.setPlacedBy(level,pos,state,player,new ItemStack(block));var node=(PortableNodeEntity)level.getBlockEntity(pos);test.assertTrue(!node.aspects().isEmpty()&&node.aspects().total()>0,"Placed portable node rolls the actual environment aura");test.assertTrue(node.nodeType()==AuraNodeType.UNSTABLE&&node.nodeModifier()==AuraNodeModifier.FADING,"Original permanent getter overrides");
        var restored=new PortableNodeEntity(pos,state);restored.load(node.saveWithoutMetadata());test.assertTrue(restored.aspects().view().equals(node.aspects().view())&&restored.nodeType()==AuraNodeType.UNSTABLE&&restored.nodeModifier()==AuraNodeModifier.FADING,"Contents and fixed appearance survive reload");
        var aspect=tc4revival.aspect.TCAspects.catalog().require("ordo");node.configure(new tc4revival.aspect.AspectAmounts().add(aspect,20),AuraNodeType.PURE,AuraNodeModifier.BRIGHT,"test",false);node.take(aspect,5);for(int i=0;i<500;i++)node.tick();test.assertTrue(node.aspects().amount(aspect)<=15,"Fading node never naturally regenerates even after another type was requested");
        test.assertTrue(!block.canEntityDestroy(state,level,pos,player),"Entities cannot destroy portable node");block.onBlockExploded(state,level,pos,null);test.assertTrue(level.getBlockEntity(pos)==node,"Explosion callback leaves node intact");
        test.assertTrue(state.getDestroySpeed(level,pos)==2,"BlockAiry hardness override is two despite old setBlockUnbreakable constructor");player.setShiftKeyDown(true);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);block.use(state,level,pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));test.assertTrue(level.isEmptyBlock(pos)&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1)).isEmpty(),"Sneak empty-hand removal has no essence or portable-item refund");
        var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("emt:portable_node")).orElseThrow();test.assertTrue(recipe.catalystMatches(new ItemStack(TCItems.NODE_JAR.get()))&&recipe.costs().total()==96,"Original jar catalyst and three32-aspect costs load");test.succeed();
    }
}
