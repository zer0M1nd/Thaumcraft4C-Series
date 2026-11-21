package thmodern.plane;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class VortexServerTests {
 @GameTest(template="empty")
 public static void voidPuttyConversionPersistsAndRejectsUnrecognizedInput(GameTestHelper test){var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlock(pos,Horizons.VORTEX.get().defaultBlockState(),3);var vortex=(VortexEntity)level.getBlockEntity(pos);vortex.initialize(new tc4revival.aspect.AspectAmounts(),true);for(int i=0;i<50;i++)vortex.tick();var input=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(TCItems.material(MaterialType.VOID),33));level.addFreshEntity(input);test.assertTrue(vortex.convert(input)&&!input.isAlive()&&vortex.pendingOutputs()==3,"Void ingots convert once into legal 16/16/1 putty stacks");var saved=vortex.saveWithoutMetadata();var loaded=new VortexEntity(pos,vortex.getBlockState());loaded.load(saved);test.assertTrue(loaded.pendingOutputs()==3,"Queued output survives save/load");var pearl=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(net.minecraft.world.item.Items.STICK));level.addFreshEntity(pearl);test.assertTrue(!vortex.convert(pearl)&&pearl.isAlive(),"Unrecognized input remains intact");pearl.discard();test.succeed();}
 @GameTest(template="empty")
 public static void unstableRiftDoesNotConsumeCraftingInput(GameTestHelper test){var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlock(pos,Horizons.VORTEX.get().defaultBlockState(),3);var vortex=(VortexEntity)level.getBlockEntity(pos);vortex.initialize(new tc4revival.aspect.AspectAmounts(),false);var input=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(TCItems.material(MaterialType.VOID)));level.addFreshEntity(input);test.assertTrue(!vortex.convert(input)&&input.isAlive(),"Unformed unstable rift does not process crafts");for(int i=0;i<50;i++)vortex.tick();test.assertTrue(!vortex.convert(input),"Formed rift still requires all six beams");for(int i=0;i<6;i++)vortex.renewStabilizer("test"+i,level.getGameTime());test.assertTrue(vortex.convert(input)&&!input.isAlive(),"Six live distinct leases enable crafting");test.succeed();}
}
