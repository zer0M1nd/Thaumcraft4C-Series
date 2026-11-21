package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PocketPlaneDataServerTests {
 @GameTest(template="empty")
 public static void allocationsAndGenerationCheckpointSurviveSave(GameTestHelper test){var data=new PocketPlanes();var origin=new PocketPlanes.Origin(test.getLevel().dimension().location(),test.absolutePos(new BlockPos(2,2,2)));var request=UUID.randomUUID();var first=data.reserve(request,"Named realm",Map.of("aer",80),0xabcdef,456L,origin);test.assertTrue(data.reserve(request,"ignored",Map.of(),0,0,origin).id()==first.id(),"Retry reuses allocation without duplicating plane");data.progress(first.id(),"caves",12345,false);var copy=PocketPlanes.load(data.save(new net.minecraft.nbt.CompoundTag()));var recovered=copy.get(first.id()).orElseThrow();test.assertTrue(recovered.cursor()==12345&&recovered.stage().equals("caves")&&!recovered.ready()&&recovered.seed()==456L&&recovered.name().equals("Named realm"),"Pending generation and environment survive restart");var second=copy.reserve(UUID.randomUUID(),"Replacement rift",Map.of(),0,9L,origin);test.assertTrue(second.id()==first.id()+1&&second.center().getZ()-first.center().getZ()==256,"New rift at same location gets distinct non-overlapping plane ID");copy.progress(first.id(),"ready",0,true);test.assertTrue(copy.get(first.id()).orElseThrow().ready(),"Completion is explicit");test.succeed();}
 @GameTest(template="empty")
 public static void sharedPocketDimensionLoadsWithoutSurfaceTerrain(GameTestHelper test){var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,PocketPlanes.DIMENSION);var level=test.getLevel().getServer().getLevel(key);test.assertTrue(level!=null&&level.getMinBuildHeight()==0&&level.getHeight()==256,"Shared plane world is registered at legacy vertical bounds");test.assertTrue(level.getBlockState(new BlockPos(0,128,29000000)).isAir(),"Unallocated realm starts void");test.succeed();}
}
