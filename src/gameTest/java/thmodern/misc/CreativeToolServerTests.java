package thmodern.misc;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import tc4revival.content.*;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class CreativeToolServerTests {
 @GameTest(template="empty")
 public static void nodeToolCopiesPermanentAspectsWithoutAmplifierState(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlockAndUpdate(pos,TCArcane.AURA_NODE.get().defaultBlockState());
  var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);var air=tc4revival.aspect.TCAspects.catalog().require("aer");node.configure(new tc4revival.aspect.AspectAmounts().add(air,20),AuraNodeType.NORMAL,null,"debug-test",false);
  thaumcraft.api.nodes.NodeApi.influence(node,net.minecraft.resources.ResourceLocation.parse("thaumichorizons:debug_test"),new thaumcraft.api.nodes.NodeApi.Influence(null,false,null,2,1,0),100);
  var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"NodeToolTest"));player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setPos(pos.getCenter());var stack=new ItemStack(CreativeTools.NODE_CHEAT.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
  stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
  test.assertTrue(level.getBlockEntity(pos) instanceof thmodern.plane.VortexEntity,"Normal aura node becomes a rift");var vortex=(thmodern.plane.VortexEntity)level.getBlockEntity(pos);
  test.assertTrue(vortex.cheat()&&vortex.aspects().amount(air)==20&&stack.getCount()==1,"Keeps permanent aspects and reusable stable-tool semantics");level.removeBlock(pos,false);test.succeed();
 }
 @GameTest(template="empty")
 public static void namedEggsResolveAllLegacyCreatures(GameTestHelper test){
  for(var definition:thmodern.creature.HorizonsSpawnEggs.DEFINITIONS){var egg=thmodern.creature.HorizonsSpawnEggs.EGGS.get(definition.name()).get();test.assertTrue(egg.getType(new net.minecraft.nbt.CompoundTag())==definition.type().get(),"Correct egg type "+definition.name());var entity=definition.type().get().create(test.getLevel());test.assertTrue(entity!=null,"Egg creature has a working registered factory "+definition.name());entity.discard();}
  test.assertTrue(thmodern.creature.HorizonsSpawnEggs.EGGS.size()+thmodern.creature.PlanarSlimes.TYPES.size()==23,"All 23 legacy egg variants covered");test.succeed();
 }
}
