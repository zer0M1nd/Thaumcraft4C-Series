package gadomancymodern.test;

import gadomancymodern.Gadomancy;
import gadomancymodern.automation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class ProtectorServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=80)
    public static void fillsFromBelowAndExpandsWithRedstone(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(pos,Gadomancy.PROTECTOR.get().defaultBlockState());var tile=(BlockProtectorEntity)level.getBlockEntity(pos);
        level.setBlockAndUpdate(pos.below(),tc4revival.content.TCArcane.ESSENTIA_JAR.get().defaultBlockState());var source=(tc4revival.content.EssentiaJarBlockEntity)level.getBlockEntity(pos.below());source.addToContainer(tile.order(),64);level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        test.assertTrue(tile.capacity()==8&&tile.minimumSuction()==128&&!tile.canOutput(Direction.DOWN)&&tile.canInput(Direction.DOWN)&&!tile.canInput(Direction.UP),"8-unit filtered input-only reservoir below");
        test.runAfterDelay(18,()->{test.assertTrue(source.amount()<64&&tile.amount()>0&&tile.range()==1,"Actual pipe source drained and first growth step completed");test.assertTrue(tile.getBlockState().getBlock().getAnalogOutputSignal(tile.getBlockState(),level,pos)==1&&tile.getBlockState().getLightEmission(level,pos)==1,"Range drives comparator and light");var data=tile.saveWithoutMetadata();var copy=new BlockProtectorEntity(pos,tile.getBlockState());copy.load(data);test.assertTrue(copy.range()==tile.range()&&copy.amount()==tile.amount()&&copy.filter().equals(tile.order()),"Save retains range/saturation/storage and restores fixed filter");test.succeed();});
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void protectsMobsExplosionsAndOnlyItsOwnDimension(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(pos,Gadomancy.PROTECTOR.get().defaultBlockState());var tile=(BlockProtectorEntity)level.getBlockEntity(pos);var data=tile.saveWithoutMetadata();data.putInt("ProtectRange",3);tile.load(data);ProtectorEvents.register(tile);
        var enderman=EntityType.ENDERMAN.create(level);enderman.setPos(pos.getX()+1,pos.getY(),pos.getZ());var grief=new net.minecraftforge.event.entity.EntityMobGriefingEvent(enderman);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(grief);test.assertTrue(grief.getResult()==net.minecraftforge.eventbus.api.Event.Result.DENY,"Protected mob griefing denied");var teleport=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(enderman,1,2,3);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(teleport);test.assertTrue(teleport.isCanceled(),"Enderman teleport inhibited");
        var other=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);test.assertTrue(other!=null&&!ProtectorEvents.protectedAt(other,pos.getCenter()),"Protection cannot leak into another dimension");
        var creeper=EntityType.CREEPER.create(level);creeper.setPos(pos.getCenter());var field=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.world.entity.monster.Creeper.class,"f_32270_");try{field.setInt(creeper,20);ProtectorEvents.resetFuse(creeper);test.assertTrue(field.getInt(creeper)==0,"Actual creeper fuse field reset");}catch(IllegalAccessException error){throw new RuntimeException(error);}
        var target=pos.north();level.setBlockAndUpdate(target,Blocks.GLASS.defaultBlockState());level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,2,net.minecraft.world.level.Level.ExplosionInteraction.TNT);test.assertTrue(level.getBlockState(target).is(Blocks.GLASS),"Protected explosion retains block while normal explosion processing runs");level.removeBlock(pos,false);test.assertTrue(!ProtectorEvents.protectedAt(level,pos.getCenter()),"Unloaded machine unregisters protection");test.succeed();
    }
}
